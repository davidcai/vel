package elert.pages.schedule;

import java.text.DateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.ControlArray;
import samoyan.controls.DaysOfMonthChooserControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ServiceAreaUserLinkStore;
import elert.database.Subscription;
import elert.database.SubscriptionFacilityLinkStore;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.database.UserEx;
import elert.database.UserExStore;
import elert.pages.ElertPage;
import elert.pages.typeahead.HomeFacilityTypeAhead;
import elert.pages.typeahead.HomePhysicianTypeAhead;
import elert.pages.typeahead.HomeProcedureTypeAhead;

/**
 * Page for scheduler to edit a patient's subscription.
 * @author brian
 *
 */
public final class VerifySubscriptionPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/verify-subscription";
	public final static String PARAM_ID = "id";

	private Subscription sub;
	private User patient;
	private UserEx patientEx;
	private boolean readOnly;
	private String warning;
	
	@Override
	public void init() throws Exception
	{
		this.sub = SubscriptionStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.sub==null)
		{
			throw new PageNotFoundException();
		}

		this.patient = UserStore.getInstance().load(this.sub.getUserID());
		if (this.patient==null)
		{
			throw new PageNotFoundException();
		}
		this.patientEx = UserExStore.getInstance().loadByUserID(this.sub.getUserID());
		
		// Read only mode?
		this.warning = null;
		this.readOnly = false;
		if (this.sub.isFinalized())
		{
			// Finalized subscriptions cannot be edited
			this.readOnly = true;
			this.warning = getString("schedule:VerifySub.FinalizedSubError");
		}
		else if (this.sub.isExpired())
		{
			// Expired subscriptions cannot be edited
			this.readOnly = true;
			this.warning = getString("schedule:VerifySub.ExpiredSubError");
		}
		else
		{
			List<UUID> homeServiceAreas = ServiceAreaUserLinkStore.getInstance().getHomeSerivceAreasForUser(getContext().getUserID());
			if (homeServiceAreas.contains(this.sub.getServiceAreaID())==false)
			{
				// Schedulers can't edit subscriptions outside their home service areas
				this.readOnly = true;
				this.warning = getString("schedule:VerifySub.NotInHomeServiceArea");
			}
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		DateFormat df = DateFormatEx.getDateInstance(getLocale(), getTimeZone());
		return this.patient.getDisplayName() + " " + df.format(this.sub.getCreatedDate());
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (isParameter("remove"))
		{
			writeEncode(getString("schedule:VerifySub.RemovedConfirmation"));
			return;
		}
		
		if (this.warning!=null)
		{
			write("<div class=WarningMessage>");
			writeEncode(this.warning);
			write("</div>");
		}
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeSubtitleRow(getString("schedule:VerifySub.PatientInfo"));
		
		twoCol.writeRow(getString("schedule:VerifySub.PatientName"));
		twoCol.writeLink(	this.patient.getDisplayName(),
							getPageURL(PatientProfilePage.COMMAND, new ParameterMap(PatientProfilePage.PARAM_ID, this.patient.getID().toString())));
		
		if (!Util.isEmpty(this.patientEx.getMRN()))
		{
			twoCol.writeRow(getString("schedule:VerifySub.MRN"));
			twoCol.writeEncode(this.patientEx.getMRN());
		}
		
		if (this.patient.getBirthday()!=null)
		{
			twoCol.writeRow(getString("schedule:VerifySub.DateOfBirth"));
			twoCol.writeEncodeDay(this.patient.getBirthday());
		}
		
		// ---
		
		twoCol.writeSubtitleRow(getString("schedule:VerifySub.ProcedureInfo"));

		twoCol.writeRow(getString("schedule:VerifySub.Procedure"));
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		if (this.readOnly)
		{
			for (int p=0; p<procIDs.size(); p++)
			{
				Procedure proc = ProcedureStore.getInstance().load(procIDs.get(p));
				if (p>0)
				{
					twoCol.write(", ");
				}
				twoCol.writeEncode(proc.getName());
			}
		}
		else
		{
			new ControlArray<UUID>(twoCol, "procs", procIDs)
			{
				@Override
				public void renderRow(int rowNum, UUID procID) throws Exception
				{
					String procName = null;
					if (procID!=null)
					{
						Procedure proc = ProcedureStore.getInstance().load(procID);
						procName = proc.getName();
					}
	
					writeTypeAheadInput("proc" + rowNum, procID, procName, 40, Procedure.MAXSIZE_NAME, getPageURL(HomeProcedureTypeAhead.COMMAND));
				}
			}.render();
		}
		
		twoCol.writeRow(getString("schedule:VerifySub.Duration"));
		if (this.readOnly)
		{
			twoCol.writeEncodeLong(this.sub.getDuration());
		}
		else
		{
			twoCol.writeNumberInput("duration", this.sub.getDuration(), 4, 0, Subscription.MAX_DURATION);
		}
		twoCol.write(" ");
		twoCol.writeEncode(getString("schedule:VerifySub.Minutes"));

		twoCol.writeRow(getString("schedule:VerifySub.Priority"));
		if (this.readOnly)
		{
			twoCol.writeEncode(getString(this.sub.isUrgent()? "schedule:VerifySub.PriorityUrgent" : "schedule:VerifySub.PriorityNormal"));
		}
		else
		{
			twoCol.writeRadioButton("urgent", getString("schedule:VerifySub.PriorityNormal"), "0", this.sub.isUrgent()? "1":"0");
			twoCol.write(" ");
			twoCol.writeRadioButton("urgent", getString("schedule:VerifySub.PriorityUrgent"), "1", this.sub.isUrgent()? "1":"0");
		}
		
		List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(this.sub.getID());
		if (this.readOnly)
		{
			if (physicianIDs.size()>0)
			{
				twoCol.writeRow(getString("schedule:VerifySub.Physician"));
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
		}
		else
		{
			twoCol.writeSpaceRow();

			twoCol.writeRow(getString("schedule:VerifySub.Physician"));
			new ControlArray<UUID>(twoCol, "physicians", physicianIDs)
			{
				@Override
				public void renderRow(int rowNum, UUID physicianID) throws Exception
				{
					String physicianName = null;
					if (physicianID!=null)
					{
						User physician = UserStore.getInstance().load(physicianID);
						physicianName = physician.getDisplayName();
					}
	
					writeTypeAheadInput("physician" + rowNum, physicianID, physicianName, 40, User.MAXSIZE_NAME, getPageURL(HomePhysicianTypeAhead.COMMAND));
				}
			}.render();
		}
		
		if (this.sub.isAcceptOtherPhysician())
		{
			twoCol.write("<small><br> ");
			twoCol.writeEncode(getString("schedule:VerifySub.AcceptOtherPhysician"));
			twoCol.write("</small>");
		}
				
		List<UUID> facilityIDs = SubscriptionFacilityLinkStore.getInstance().getFacilitiesForSubscription(this.sub.getID());
		if (this.readOnly)
		{
			if (facilityIDs.size()>0)
			{
				twoCol.writeRow(getString("schedule:VerifySub.Facility"));
				for (int f=0; f<facilityIDs.size(); f++)
				{
					UUID facilityID = facilityIDs.get(f);
					Facility facility = FacilityStore.getInstance().load(facilityID);
					if (f>0)
					{
						twoCol.write(", ");
					}
					twoCol.writeEncode(facility.getName());
				}
			}
		}
		else
		{
			twoCol.writeSpaceRow();
			twoCol.writeRow(getString("schedule:VerifySub.Facility"));

			new ControlArray<UUID>(twoCol, "facilities", facilityIDs)
			{
				@Override
				public void renderRow(int rowNum, UUID facilityID) throws Exception
				{
					String facilityName = null;
					if (facilityID!=null)
					{
						Facility facility = FacilityStore.getInstance().load(facilityID);
						facilityName = facility.getName();
					}
	
					writeTypeAheadInput("facility" + rowNum, facilityID, facilityName, 40, User.MAXSIZE_NAME, getPageURL(HomeFacilityTypeAhead.COMMAND));
				}
			}.render();

//			for (int f=0; f<myFacilities.size(); f++)
//			{
//				UUID facilityID = myFacilities.get(f);
//				Facility facility = FacilityStore.getInstance().load(facilityID);
//				
//				twoCol.writeRow(f==0? getString("schedule:VerifySub.Facility") : "");
//				twoCol.writeCheckbox("facility_" + facilityID.toString(), SubscriptionFacilityLinkStore.getInstance().isFacilityLinkedToSubscription(facilityID, this.sub.getID()));
//				twoCol.write(" ");
//				twoCol.writeEncode(facility.getName());
//			}
		}
		
		if (this.readOnly)
		{
			if (this.sub.getOriginalDate()!=null)
			{
				twoCol.writeRow(getString("schedule:VerifySub.OriginalDate"));
				twoCol.writeEncodeDay(this.sub.getOriginalDate());
			}
		}
		else
		{
			twoCol.writeSpaceRow();

			twoCol.writeRow(getString("schedule:VerifySub.OriginalDate"));
			twoCol.writeDateInput("originaldate", this.sub.getOriginalDate());
		}
		
		// ---
		
		twoCol.writeSubtitleRow(getString("schedule:VerifySub.AvailabilityInfo"));

		twoCol.writeRow(getString("schedule:VerifySub.Status"));
		if (this.sub.isAlwaysAvailable())
		{
			twoCol.writeEncode(getString("schedule:VerifySub.AvailableAlways"));
		}
		else
		{
			twoCol.writeEncode(getString("schedule:VerifySub.AvailableMatrix"));
		}
		
		if (this.sub.isAlwaysAvailable()==false)
		{
			twoCol.writeSpaceRow();
			twoCol.writeRow(getString("schedule:VerifySub.Calendar"));
			
			int COLS = getContext().getUserAgent().isSmartPhone()? 2 : 3; // Must be divider of Subscription.MAX_AVAILABILITY_MONTHS
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			cal.setTime(sub.getCreatedDate());
			twoCol.write("<table>");
			for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
			{
				if (i%COLS==0)
				{
	//				if (i>0)
	//				{
	//					twoCol.write("<tr><td colspan=");
	//					twoCol.write(COLS);
	//					twoCol.write(">&nbsp;</td></tr>");
	//				}
					twoCol.write("<tr>");
				}
				twoCol.write("<td>");
				
				int yyyy = cal.get(Calendar.YEAR);
				int mm = cal.get(Calendar.MONTH)+1;
				DaysOfMonthChooserControl ctrl = new DaysOfMonthChooserControl(twoCol)
					.readOnly()
					.setMonth(yyyy, mm)
					.disableBefore(new Date())
					.setName("avail." + String.valueOf(yyyy) + "." + String.valueOf(mm));
				
				BitSet bs = this.sub.getAvailable(yyyy, mm);
				for (int b=0; b<bs.length(); b++)
				{
					if (bs.get(b))
					{
						ctrl.select(b+1);
					}
				}
				ctrl.render();
				
				twoCol.write("</td>");
				if (i%COLS==COLS-1)
				{
					twoCol.write("</tr>");
				}
				
				cal.add(Calendar.MONTH, 1);
			}
			twoCol.write("</table>");

			if (!Util.isEmpty(this.sub.getReason()))
			{
				twoCol.writeSpaceRow();
				twoCol.writeRow(getString("schedule:VerifySub.Reason"));
				twoCol.writeEncode(this.sub.getReason());
			}	
		}
		
		twoCol.render();
		
		// ---
		
		write("<br>");
		if (!this.readOnly)
		{
			writeButton("save", getString("schedule:VerifySub.VerifyBtn"));
			write(" ");
			
			String prompt = Util.jsonEncode(getString("schedule:VerifySub.RemovePrompt"));
			new ButtonInputControl(this, "remove")
				.setStrong(true)
				.setValue(getString("controls:Button.Remove"))
				.setAttribute("onclick", "return confirm('" + prompt + "')")
				.render();
			
			boolean phone = getContext().getUserAgent().isSmartPhone();
			if (this.sub.getVerifiedByUserID()!=null)
			{
				if (this.sub.getVerifiedByUserID().equals(getContext().getUserID()))
				{
					if (phone)
					{
						write("<br><br>");
					}
					else
					{
						write(" ");
					}
					write("<small>");
					writeEncode(getString("schedule:VerifySub.LastVerifiedByYou", this.sub.getVerifiedDate()));
					write("</small>");
				}
				else
				{
					User verifier = UserStore.getInstance().load(this.sub.getVerifiedByUserID());
					if (phone)
					{
						write("<br><br>");
					}
					else
					{
						write(" ");
					}
					write("<small>");
					writeEncode(getString("schedule:VerifySub.LastVerifiedByOther", this.sub.getVerifiedDate(), verifier.getName()));
					write("</small>");
				}
			}
		
			write("<br><br>");
			writeCheckbox("notify", getString("schedule:VerifySub.NotifyPatient"), false);
		}
		
		// Postback
		writeHiddenInput(PARAM_ID, null);
		
		writeFormClose();
	}

	@Override
	public void commit() throws Exception
	{
		if (isParameter("remove"))
		{
			if (SubscriptionStore.getInstance().canRemoveBean(this.sub.getID()))
			{
				// Remove from the database outright
				SubscriptionStore.getInstance().remove(this.sub.getID());
			}
			else
			{
				// Keep in database, but mark as removed
				this.sub.setRemoved(true);
				SubscriptionStore.getInstance().save(this.sub);
			}
			
			if (isParameter("notify"))
			{
				throw new RedirectException(EmailPatientPage.COMMAND, new ParameterMap(EmailPatientPage.PARAM_PATIENT_ID, this.sub.getUserID().toString()));
			}
		}
		
		if (isParameter("save"))
		{
			this.sub.setDuration(getParameterInteger("duration"));
			this.sub.setUrgent(getParameterString("urgent").equals("1"));
			
			this.sub.setOriginalDate(getParameterDate("originaldate"));
			
			this.sub.setVerifiedBy(getContext().getUserID());
			this.sub.setVerifiedDate(new Date());
			
			SubscriptionStore.getInstance().save(this.sub);
	
			// Facility links
			SubscriptionFacilityLinkStore.getInstance().clearFacilitiesOfSubscription(this.sub.getID());
			int n = getParameterInteger("facilities");
			for (int i=0; i<n; i++)
			{
				Pair<String, String> facilityKvp = getParameterTypeAhead("facility" + i);
				if (facilityKvp!=null && !Util.isEmpty(facilityKvp.getKey()))
				{
					SubscriptionFacilityLinkStore.getInstance().addFacilityToSubscription(UUID.fromString(facilityKvp.getKey()), this.sub.getID());
				}
			}
	
	//		List<UUID> myFacilities = FacilityStore.getInstance().queryByUser(getContext().getUserID());
	//		for (int f=0; f<myFacilities.size(); f++)
	//		{
	//			UUID facilityID = myFacilities.get(f);
	//			if (isParameter("facility_" + facilityID.toString()))
	//			{
	//				SubscriptionFacilityLinkStore.getInstance().addFacilityToSubscription(facilityID, this.sub.getID());
	//			}
	//			else
	//			{
	//				SubscriptionFacilityLinkStore.getInstance().removeFacilityFromSubscription(facilityID, this.sub.getID());
	//			}
	//		}
			
			// Physician links
			SubscriptionPhysicianLinkStore.getInstance().clearPhysiciansOfSubscription(this.sub.getID());
			n = getParameterInteger("physicians");
			for (int i=0; i<n; i++)
			{
				Pair<String, String> physicianKvp = getParameterTypeAhead("physician" + i);
				if (physicianKvp!=null && !Util.isEmpty(physicianKvp.getKey()))
				{
					SubscriptionPhysicianLinkStore.getInstance().addPhysicianToSubscription(UUID.fromString(physicianKvp.getKey()), this.sub.getID());
				}
			}
			
			// Procedure links
			SubscriptionProcedureLinkStore.getInstance().clearProceduresOfSubscription(this.sub.getID());
			n = getParameterInteger("procs");
			for (int i=0; i<n; i++)
			{
				Pair<String, String> procKvp = getParameterTypeAhead("proc" + i);
				if (procKvp!=null && !Util.isEmpty(procKvp.getKey()))
				{
					SubscriptionProcedureLinkStore.getInstance().addProcedureToSubscription(UUID.fromString(procKvp.getKey()), this.sub.getID());
				}
			}
	
			if (isParameter("notify"))
			{
				throw new RedirectException(EmailPatientPage.COMMAND, new ParameterMap(EmailPatientPage.PARAM_PATIENT_ID, this.sub.getUserID().toString()));
			}
			else
			{
				throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, this.sub.getID().toString()).plus(RequestContext.PARAM_SAVED, ""));
			}
		}
	}

	@Override
	public void validate() throws Exception
	{
		if (this.readOnly)
		{
			throw new WebFormException(getString("schedule:VerifySub.FinalizedSubError"));
		}
		
		validateParameterInteger("duration", 0, Subscription.MAX_DURATION);

		if (!Util.isEmpty(getParameterString("originaldate")))
		{
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			cal.setTime(this.sub.getCreatedDate());
			Date minDate = cal.getTime();
			cal.add(Calendar.YEAR, 1);
			Date maxDate = cal.getTime();
			validateParameterDate("originaldate", minDate, maxDate);
		}
		
		// !$! Validate physicians, etc.
		
		// Validate facility is in service area of subscription
		boolean hasFacilities = false;
		int n = getParameterInteger("facilities");
		for (int i=0; i<n; i++)
		{
			Pair<String, String> facilityKvp = getParameterTypeAhead("facility" + i);
			if (facilityKvp!=null && !Util.isEmpty(facilityKvp.getKey()))
			{
				Facility facility = FacilityStore.getInstance().load(UUID.fromString(facilityKvp.getKey()));
				if (facility==null)
				{
					throw new WebFormException("facility"+i, getString("common:Errors.InvalidValue"));
				}
				if (facility.getServiceAreaID().equals(this.sub.getServiceAreaID())==false)
				{
					throw new WebFormException("facility"+i, getString("schedule:VerifySub.FacilityNotInServiceArea"));
				}
				hasFacilities = true;
			}
		}
		if (hasFacilities==false)
		{
			// At least one facility must be chosen
			throw new WebFormException("facility"+n, getString("common:Errors.MissingField"));
		}		
	}
}
