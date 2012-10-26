package elert.pages.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.PhysicianOpeningLinkStore;
import elert.database.Procedure;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureOpeningLinkStore;
import elert.database.ProcedureStore;
import elert.database.ServiceAreaUserLinkStore;
import elert.database.Subscription;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.database.UserEx;
import elert.database.UserExStore;
import elert.pages.ElertPage;
import elert.pages.patient.ChosenNotif;
import elert.pages.patient.UnavailNotif;

public final class FinalizePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/finalize";
	public static final String PARAM_ELERT_IDS = "elerts";
	
	private Opening opening = null;
	private List<Elert> elerts = null;
	private List<Subscription> subscriptions = null;
	
	@Override
	public void init() throws Exception
	{
		List<UUID> homeServiceAreas = ServiceAreaUserLinkStore.getInstance().getHomeSerivceAreasForUser(getContext().getUserID());
		
		this.elerts = new ArrayList<Elert>();
		this.subscriptions = new ArrayList<Subscription>();
		for (String elertStr : Util.tokenize(getParameterString(PARAM_ELERT_IDS), ","))
		{
			UUID elertID = UUID.fromString(elertStr);
			Elert elert = ElertStore.getInstance().load(elertID);
			if (elert==null)
			{
				continue;
			}
						
			// Validate eLert is in home service area of scheduler
			if (homeServiceAreas.contains(elert.getServiceAreaID())==false)
			{
				continue;
			}
			
			// Validate that all input eLerts are connected to same opening
			if (this.opening!=null && this.opening.getID().equals(elert.getOpeningID())==false)
			{
				continue;
			}
			
			// Validate that subscription is not open
			Subscription sub = SubscriptionStore.getInstance().load(elert.getSubscriptionID());
			if (sub.isFinalized())
			{
				continue;
			}
			
			if (this.opening==null)
			{
				Opening op = OpeningStore.getInstance().load(elert.getOpeningID());
				if (op.isClosed())
				{
					continue;
				}
				this.opening = op;
			}

			this.elerts.add(elert);
			this.subscriptions.add(sub);
		}
		
		if (this.opening==null)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		int count = 0;
		int totalDuration = 0;
		for (int i=0; i<this.elerts.size(); i++)
		{
			Subscription sub = this.subscriptions.get(i);
			if (isParameter("chk_" + sub.getID()))
			{
				totalDuration += sub.getDuration();
				count ++;
			}
		}
		
		// At least one must be selected
		if (count==0)
		{
			List<String> fields = new ArrayList<String>();
			for (Subscription sub : this.subscriptions)
			{
				fields.add("chk_" + sub.getID().toString());
			}
			throw new WebFormException(fields, getString("common:Errors.MissingField"));
		}
		
		if (totalDuration > this.opening.getDuration())
		{
			List<String> fields = new ArrayList<String>();
			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
			{
				fields.add(p);
			}
			throw new WebFormException(fields, getString("schedule:Finalize.ExceedingDuration", this.opening.getDuration()));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		Date now = new Date();
		
		int totalDuration = 0;
		for (int i=0; i<this.elerts.size(); i++)
		{
			Elert elert = this.elerts.get(i);
			Subscription sub = this.subscriptions.get(i);

			if (isParameter("chk_" + sub.getID()))
			{
				// Finalize the chosen subscriptions
				sub = (Subscription) sub.clone(); // Open for writing
				sub.setFinalized(true);
				SubscriptionStore.getInstance().save(sub);
				
				// Send confirmation notif to chosen
				Notifier.send(sub.getUserID(), elert.getID(), ChosenNotif.COMMAND, new ParameterMap(ChosenNotif.PARAM_ELERT_ID, elert.getID().toString()));
				
				totalDuration += sub.getDuration();
				
				// Record decision on eLert
				elert = (Elert) elert.clone(); // Open for writing
				elert.setDecision(Elert.DECISION_CHOSEN);
				elert.setDateDecision(now);
				ElertStore.getInstance().save(elert);
			}
		}
		
		// Calc the amount of time left for the opening
		int reducedDuration = this.opening.getDuration() - totalDuration;

		// Calc the minimum duration of any procedure at the facility of the opening
		int minProcDuration = Integer.MAX_VALUE;
		List<UUID> procIDs = ProcedureFacilityLinkStore.getInstance().getProceduresAssignedToFacility(this.opening.getFacilityID());
		for (UUID procID : procIDs)
		{
			Procedure proc = ProcedureStore.getInstance().load(procID);
			minProcDuration = Math.min(minProcDuration, proc.getDuration());
		}
		
		// Update the opening
		Opening opening = (Opening) this.opening.clone(); // Open for writing
		opening.setDuration(reducedDuration);
		if (reducedDuration < minProcDuration)
		{
			// Close the opening if there are no procedures in this facility that take less time than the time remaining
			opening.setClosed(true);
		}
		OpeningStore.getInstance().save(opening);
		
		// Send "no longer available" notif to those not chosen whose procedure requires more time than is now available
		List<UUID> allElerts = ElertStore.getInstance().queryByOpeningID(this.opening.getID());
		for (int i=0; i<allElerts.size(); i++)
		{
			Elert elert = ElertStore.getInstance().load(allElerts.get(i));
			Subscription sub = SubscriptionStore.getInstance().load(elert.getSubscriptionID());
			
			if (elert.getDecision()==Elert.DECISION_NONE && sub.getDuration() > reducedDuration)
			{
				// Record decision on eLert
				elert = (Elert) elert.clone(); // Open for writing
				elert.setDecision(Elert.DECISION_NOT_CHOSEN);
				elert.setDateDecision(now);
				ElertStore.getInstance().save(elert);
	
				if (elert.getReply()!=Elert.REPLY_DECLINED)
				{
					Notifier.send(sub.getUserID(), elert.getID(), UnavailNotif.COMMAND, new ParameterMap(UnavailNotif.PARAM_ELERT_ID, elert.getID().toString()));
				}
			}
		}
		
		throw new RedirectException(OpeningPage.COMMAND, new ParameterMap(OpeningPage.PARAM_ID, this.opening.getID().toString()));
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:Finalize.Title", this.opening.getDateTime());
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		renderOpeningDetails();
		write("<br>");
		writeEncode(getString("schedule:Finalize.Help"));
		write("<br><br>");
		
		write("<table>");
		for (int i=0; i<this.elerts.size(); i++)
		{
			Elert elert = this.elerts.get(i);
			Subscription sub = this.subscriptions.get(i);
			User patient = UserStore.getInstance().load(elert.getPatientID());
			UserEx patientEx = UserExStore.getInstance().loadByUserID(elert.getPatientID());
			
			write("<tr>");
			
			// Checkbox
			write("<td>");
			writeCheckbox("chk_" + sub.getID(), null, false);
			write("</td>");

			// Elert status
			write("<td>");
			Integer status = elert.getReply();
			if (status==Elert.REPLY_ACCEPTED)
			{
				writeImage("elert/circle-v.png", getString("elert:Legend.Accepted"));
			}
			else if (status==Elert.REPLY_DECLINED)
			{
				writeImage("elert/circle-x.png", getString("elert:Legend.Declined"));
			}
			else
			{
				writeImage("elert/circle-q.png", getString("elert:Legend.DidNotReply"));
			}
			write("</td>");

			// Urgent?
			write("<td>");
			if (sub.isUrgent())
			{
				writeImage("elert/urgent.png", getString("elert:Legend.Urgent"));
			}
			write("</td>");
			
			// Match
			write("<td>");
			Integer match = OpeningStore.getInstance().matchPercentage(this.opening, sub);
			if (match!=null)
			{
				writeEncodeLong(match);
				write("%");
			}
			write("</td>");
			
//			// Avatar
//			write("<td>");
//			if (patient.getAvatar()!=null)
//			{
//				writeImage(patient.getAvatar(), Image.SIZE_THUMBNAIL, patient.getDisplayName(), null);
//			}
//			write("</td>");
			
			write("<td>");
			
				TwoColFormControl twoCol = new TwoColFormControl(this);
				
				// Patient profile
				twoCol.writeRow(getString("schedule:Finalize.Patient"));
				twoCol.writeLink(	patient.getDisplayName(),
							getPageURL(PatientProfilePage.COMMAND, new ParameterMap(PatientProfilePage.PARAM_ID, patient.getID().toString())));
				
				if (!Util.isEmpty(patientEx.getMRN()))
				{
					twoCol.writeRow(getString("schedule:Finalize.MRN"));
					twoCol.writeEncode(patientEx.getMRN());
				}
				
				if (patient.getBirthday()!=null)
				{
					twoCol.writeRow(getString("schedule:Finalize.DateOfBirth"));
					twoCol.writeEncodeDay(patient.getBirthday());
				}
				
				if (!Util.isEmpty(patient.getEmail()))
				{
					twoCol.writeRow(getString("schedule:Finalize.Email"));
					twoCol.writeEncode(patient.getEmail());
				}
				
				if (!Util.isEmpty(patient.getMobile()))
				{
					twoCol.writeRow(getString("schedule:Finalize.Mobile"));
					twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(patient.getMobile()));
				}
				
				if (!Util.isEmpty(patient.getPhone()))
				{
					twoCol.writeRow(getString("schedule:Finalize.Phone"));
					twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(patient.getPhone()));
				}
								
				// Subscription profile
				twoCol.writeRow(getString("schedule:Finalize.Procedure"));

				List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(sub.getID());
				for (int p=0; p<procIDs.size(); p++)
				{
					Procedure proc = ProcedureStore.getInstance().load(procIDs.get(p));
					if (p>0)
					{
						twoCol.write(", ");
					}
					if (!Util.isEmptyHTML(proc.getInstructions()))
					{
						twoCol.writeTooltip(proc.getName(), Util.htmlToText(proc.getInstructions()));
					}
					else
					{
						twoCol.writeEncode(proc.getName());
					}
				}

				twoCol.writeRow(getString("schedule:Finalize.Duration"));
				twoCol.writeEncodeLong(sub.getDuration());
				twoCol.write(" ");
				twoCol.writeEncode(getString("schedule:Finalize.Minutes"));

				List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(sub.getID());
				if (physicianIDs.size()>0)
				{
					twoCol.writeRow(getString("schedule:Finalize.Physician"));
					for (int p=0; p<physicianIDs.size(); p++)
					{
						User physician = UserStore.getInstance().load(physicianIDs.get(p));
						if (p>0)
						{
							twoCol.write(", ");
						}
						twoCol.writeEncode(physician.getDisplayName());
					}
				}
				
				twoCol.writeSpaceRow();
				
				twoCol.render();
			
			write("</td>");
			
			write("</tr>");
		}
		
		write("</table>");
		
		writeButton(getString("schedule:Finalize.Finalize"));

		writeLegend();

		writeHiddenInput(PARAM_ELERT_IDS, null);
		writeFormClose();
	}
	
	private void renderOpeningDetails() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:Finalize.DateTime"));
		twoCol.writeEncodeDateTime(this.opening.getDateTime());

		twoCol.write(" (");
		twoCol.writeEncodeLong(this.opening.getDuration());
		if (this.opening.getOriginalDuration()!=this.opening.getDuration())
		{
			twoCol.write("<span class=Faded>/");
			twoCol.writeEncodeLong(opening.getOriginalDuration());
			twoCol.write("</span>");
		}
		twoCol.write(" ");
		twoCol.writeEncode(getString("schedule:Finalize.Minutes"));
		twoCol.write(")");
		
		Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
		twoCol.writeRow(getString("schedule:Finalize.Facility"));
		twoCol.writeEncode(facility.getName());
		
		if (!Util.isEmpty(this.opening.getRoom()))
		{
			twoCol.write(" (");
			twoCol.writeEncode(this.opening.getRoom());
			twoCol.write(")");
		}
		
		List<UUID> procedures = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(this.opening.getID());
		for (int i=0; i<procedures.size(); i++)
		{
			Procedure proc = ProcedureStore.getInstance().load(procedures.get(i));
			if (i==0)
			{
				twoCol.writeRow(getString("schedule:Finalize.Procedure"));
			}
			else
			{
				twoCol.write(", ");
			}
			twoCol.writeEncode(proc.getName());
		}

		
		List<UUID> physicians = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(this.opening.getID());
		for (int i=0; i<physicians.size(); i++)
		{
			User physician = UserStore.getInstance().load(physicians.get(i));
			if (i==0)
			{
				twoCol.writeRow(getString("schedule:Finalize.Physician"));
			}
			else
			{
				twoCol.write(", ");
			}
			twoCol.writeEncode(physician.getDisplayName());
		}
		
		twoCol.render();
	}
}
