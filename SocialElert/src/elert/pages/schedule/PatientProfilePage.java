package elert.pages.schedule;

import java.util.List;
import java.util.UUID;

import elert.app.ElertConsts;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.Subscription;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.database.UserEx;
import elert.database.UserExStore;
import elert.pages.ElertPage;
import samoyan.controls.DataTableControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;

public final class PatientProfilePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/patient-profile"; 
	public final static String PARAM_ID = "id"; 
	
	private User patient;
	private UserEx patientEx;
	
//	@Override
//	public void commit() throws Exception
//	{
//		if (isParameter("remove"))
//		{
//			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
//			{
//				UUID subID = UUID.fromString(p.substring(4));
//				if (SubscriptionStore.getInstance().canRemoveBean(subID))
//				{
//					// Remove from the database outright
//					SubscriptionStore.getInstance().remove(subID);
//				}
//				else
//				{
//					// Keep in database, but mark as removed
//					Subscription sub = SubscriptionStore.getInstance().open(subID);
//					sub.setRemoved(true);
//					SubscriptionStore.getInstance().save(sub);
//				}
//			}
//		}
//	}

	@Override
	public void init() throws Exception
	{
		this.patient = UserStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.patient==null)
		{
			throw new PageNotFoundException();
		}
		this.patientEx = UserExStore.getInstance().loadByUserID(getParameterUUID(PARAM_ID));
	}

	@Override
	public String getTitle() throws Exception
	{
		return this.patient.getDisplayName();
	}

	@Override
	public void renderHTML() throws Exception
	{
		renderProfile();
		write("<br>");
		
		renderSubscriptionList();
	}

	private void renderSubscriptionList() throws Exception
	{
		final List<UUID> subIDs = SubscriptionStore.getInstance().getByUserID(this.patient.getID());
		
		write("<h2>");
		writeEncode(getString("schedule:PatientProfile.ActiveSubscriptions"));
		write("</h2>");
		
		if (subIDs.size()==0)
		{
			writeEncode(getString("schedule:PatientProfile.NoSubscriptions"));
			write("<br>");
			
			return;
		}
	
//		writeFormOpen();
		
		new DataTableControl<UUID>(this, "subs", subIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
//				column("").width(1); // Checkbox
				column("").width(1); // Verified
				column("").width(1); // Urgent
				column(getString("schedule:PatientProfile.Procedure"));
				column(getString("schedule:PatientProfile.Physician"));
				column(getString("schedule:PatientProfile.Created"));
				column("").width(1); // Finalized
			}

			@Override
			protected void renderRow(UUID subID) throws Exception
			{
				Subscription sub = SubscriptionStore.getInstance().load(subID);
				
//				cell();
//				writeCheckbox("chk_" + subID.toString(), null, false);

				cell();
				if (sub.getVerifiedByUserID()!=null)
				{
					writeImage("elert/verified.png", getString("elert:Legend.Verified"));
				}
				
				cell();
				if (sub.isUrgent())
				{
					writeImage("elert/urgent.png", getString("elert:Legend.Urgent"));
				}

				cell();
				write("<a href=\"");
				write(getPageURL(VerifySubscriptionPage.COMMAND, new ParameterMap(VerifySubscriptionPage.PARAM_ID, sub.getID().toString())));
				write("\">");
				List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(subID);
				for (UUID procID : procIDs)
				{
					Procedure proc = ProcedureStore.getInstance().load(procID);
					writeEncode(proc.getName());
					write("<br>");
				}
				write("</a>");
								
				cell();
				List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(subID);
				for (UUID physicianID : physicianIDs)
				{
					User physician = UserStore.getInstance().load(physicianID);
					writeEncode(physician.getDisplayName());
					write("<br>");
				}
				
				cell();
				if (sub.isExpired())
				{
					write("<strike>");
				}
				writeEncodeDateOrTime(sub.getCreatedDate());
				if (sub.isExpired())
				{
					write("</strike>");
				}
				
				cell();
				if (sub.isFinalized())
				{
					writeImage("elert/finalized.png", getString("elert:Legend.Finalized"));
				}
			}
		}.render();
		
//		write("<br>");
//		writeRemoveButton("remove");
//		write("<br><br>");
//		writeEncode(getString("schedule:PatientProfile.RemoveWarning"));
//		
//		writeHiddenInput(PARAM_ID, null);
		
		writeLegend();
		
//		writeFormClose();
	}
	
	private void renderProfile() throws Exception
	{
		RequestContext ctx = getContext();
		boolean phone = ctx.getUserAgent().isSmartPhone();
		
		Image avatar = this.patient.getAvatar();
		if (avatar!=null)
		{
			if (!phone)
			{
				write("<table><tr><td>");
			}
			writeImage(avatar, ElertConsts.IMAGESIZE_SQUARE_150, this.patient.getDisplayName(), null);
			if (!phone)
			{
				write("</td><td>");
			}
			else
			{
				write("<br>");
			}
		}
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:PatientProfile.Name"));
		twoCol.writeEncode(this.patient.getDisplayName());
		
		if (!Util.isEmpty(this.patientEx.getMRN()))
		{
			twoCol.writeRow(getString("schedule:PatientProfile.MRN"));
			twoCol.writeEncode(this.patientEx.getMRN());
		}
		
		if (this.patient.getBirthday()!=null)
		{
			twoCol.writeRow(getString("schedule:PatientProfile.DateOfBirth"));
			twoCol.writeEncodeDay(this.patient.getBirthday());
		}
		
		if (!Util.isEmpty(this.patient.getEmail()))
		{
			twoCol.writeRow(getString("schedule:PatientProfile.Email"));
			twoCol.writeLink(	this.patient.getEmail(),
								getPageURL(EmailPatientPage.COMMAND, new ParameterMap(EmailPatientPage.PARAM_PATIENT_ID, patient.getID().toString())));
		}
		
		if (!Util.isEmpty(this.patient.getMobile()))
		{
			twoCol.writeRow(getString("schedule:PatientProfile.Mobile"));
			twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(this.patient.getMobile()));
		}
		
		if (!Util.isEmpty(this.patient.getPhone()))
		{
			twoCol.writeRow(getString("schedule:PatientProfile.Phone"));
			twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(this.patient.getPhone()));
		}
		
		twoCol.render();
		
		if (avatar!=null && !phone)
		{
			write("</td></tr></table>");
		}
	}

}
