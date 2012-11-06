package mind.pages.patient.reminders;

import java.util.Date;
import java.util.UUID;

import mind.database.Dose;
import mind.database.DoseStore;
import mind.database.Drug;
import mind.database.DrugStore;
import mind.database.Patient;
import mind.database.PatientStore;
import mind.database.Prescription;
import mind.database.PrescriptionStore;
import samoyan.controls.ActionListControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class DoseReminderNotif extends RemindersPage
{
	public final static String COMMAND = RemindersPage.COMMAND + "/dose-reminder.notif";

	public final static String PARAM_DOSE_ID = "dose";

	private Dose dose;
	private Prescription rx;
	private Patient patient;
	private Drug drug;
	private String drugName;
		
	@Override
	public void validate() throws Exception
	{
		String action = getParameterString(RequestContext.PARAM_ACTION);

		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			if (action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionYes"))==false &&
				action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionNo"))==false)
			{
				throw new WebFormException(getString("mind:DoseReminderNotif.ResponseError", action));
			}
		}
		else
		{
			try
			{
				Integer reasonCode = Integer.parseInt(action);
				if (reasonCode<1 || reasonCode>8)
				{
					throw new WebFormException(getString("mind:DoseReminderNotif.ResponseError", action));
				}
			}
			catch (NumberFormatException nfe)
			{
				// Text is OK as reason
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		String action = getParameterString(RequestContext.PARAM_ACTION);
		
		this.dose = DoseStore.getInstance().open(this.dose.getID()); // Open for writing
		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			if (action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionYes")))
			{
				this.dose.setResolution(Dose.TAKEN);
				this.dose.setResolutionDate(new Date());
				DoseStore.getInstance().save(this.dose);				
			}
			else if (action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionNo")))
			{
				this.dose.setResolution(Dose.SKIPPED);
				this.dose.setResolutionDate(new Date());
				DoseStore.getInstance().save(this.dose);
				
				// Increase the amount of doses remaining on the prescription
				Prescription rx = PrescriptionStore.getInstance().open(this.dose.getPrescriptionID());
				rx.setDosesRemaining(rx.getDosesRemaining()+1);
				PrescriptionStore.getInstance().save(rx);
			}
		}
		else
		{
			String reason;
			try
			{
				Integer i = Integer.parseInt(action);
				reason = getString("mind:DoseReminderNotif.SkipReason_" + i);
			}
			catch (NumberFormatException nfe)
			{
				// Text is OK as reason
				reason = action;
			}

			if (reason.length()>Dose.MAXSIZE_SKIP_REASON)
			{
				reason = reason.substring(0, Dose.MAXSIZE_SKIP_REASON);
			}
			this.dose.setSkipReason(reason);
			DoseStore.getInstance().save(this.dose);
		}
	}

	@Override
	public boolean isActionable() throws Exception
	{
		return this.dose.getResolution()==Dose.UNRESOLVED ||
				(this.dose.getResolution()==Dose.SKIPPED && Util.isEmpty(this.dose.getSkipReason()));
	}

	@Override
	public void init() throws Exception
	{
		UUID doseID = getParameterUUID(PARAM_DOSE_ID);
		
		this.dose = DoseStore.getInstance().load(doseID);
		if (this.dose==null)
		{
// !$!
Debug.logln("Dose cannot be loaded. Check why.");
Debug.logln(getContext().toString());
			throw new PageNotFoundException();
		}
		
		this.rx = PrescriptionStore.getInstance().load(this.dose.getPrescriptionID());
		this.drug = DrugStore.getInstance().load(this.rx.getDrugID());
		this.patient = PatientStore.getInstance().load(this.rx.getPatientID());
		
		this.drugName = this.drug.getName();
		if (!Util.isEmpty(this.rx.getNickname()) && Channel.isPush(getContext().getChannel()))
		{
			this.drugName = "\"" + rx.getNickname() + "\"";
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:DoseReminderNotif.Title", this.drugName, this.dose.getTakeDate());
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (!this.isActionable())
		{
			// Dose is fully resolved
			if (this.isCommitted())
			{
				throw new RedirectException(DoseListPage.COMMAND, null);
			}
			else
			{
				throw new PageNotFoundException();
			}
		}

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Drug name
		twoCol.writeRow(getString("mind:DoseReminderNotif.Drug"));
		twoCol.writeEncode(this.drug.getName());
		if (this.drug.getGenericName()!=null && this.drug.getGenericName().equals(this.drug.getName())==false)
		{
			twoCol.write(" (");
			twoCol.writeEncode(this.drug.getGenericName());
			twoCol.write(")");
		}
		if (!Util.isEmpty(rx.getDoseInfo()))
		{
			twoCol.write(" (");
			twoCol.writeEncode(rx.getDoseInfo());
			twoCol.write(")");
		}
		if (!Util.isEmpty(this.drug.getDescription()))
		{
			twoCol.write("<br><div class=HalfLineSpace></div><small>");
			twoCol.write(this.drug.getDescription());
			twoCol.write("</small>");
		}
		
		// Time
		twoCol.writeRow(getString("mind:DoseReminderNotif.Time"));
		twoCol.writeEncodeDateTime(this.dose.getTakeDate());
		
		boolean space = false;
		
		// Purpose
		if (!Util.isEmpty(this.rx.getPurpose()))
		{
			if (!space)
			{
				twoCol.writeSpaceRow();
				space = true;
			}
			
			twoCol.writeRow(getString("mind:DoseReminderNotif.Purpose"));
			twoCol.writeEncode(this.rx.getPurpose());
		}
		
		// Instructions
		if (!Util.isEmpty(this.rx.getInstructions()))
		{
			if (!space)
			{
				twoCol.writeSpaceRow();
				space = true;
			}
			
			twoCol.writeRow(getString("mind:DoseReminderNotif.Instructions"));
			twoCol.writeEncode(this.rx.getInstructions());
		}
		
		// Doctor
		if (!Util.isEmpty(this.rx.getDoctorName()))
		{
			if (!space)
			{
				twoCol.writeSpaceRow();
				space = true;
			}
			
			twoCol.writeRow(getString("mind:DoseReminderNotif.Doctor"));
			twoCol.writeEncode(this.rx.getDoctorName());
		}
		
		twoCol.render();
		write("<br>");

		// Response
		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			new ActionListControl(this)
				.setPrompt(getString("mind:DoseReminderNotif.ActionPrompt"))
				.addAction(getString("mind:DoseReminderNotif.ActionYes"), getString("mind:DoseReminderNotif.ActionYesHelp"))
				.addAction(getString("mind:DoseReminderNotif.ActionNo"), getString("mind:DoseReminderNotif.ActionNoHelp"))
				.render();
		}
		else
		{
			ActionListControl actionListCtrl = new ActionListControl(this);
			actionListCtrl.setPrompt(getString("mind:DoseReminderNotif.SkippedPrompt"));
			for (int i=1; i<=8; i++)
			{
				actionListCtrl.addAction(String.valueOf(i), getString("mind:DoseReminderNotif.SkipReason_" + i));
			}
			actionListCtrl.addAction(null, getString("mind:DoseReminderNotif.OtherReason"));
			actionListCtrl.render();
		}
	}

	@Override
	public void renderSimpleHTML() throws Exception
	{
		if (!this.isActionable())
		{
			// Dose is fully resolved
			throw new PageNotFoundException();
		}
		
		// Greeting
		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			writeEncode(getString("mind:DoseReminderNotif.Body", Setup.getAppOwner(getLocale())));
		}
		else if (this.dose.getResolution()==Dose.SKIPPED && Util.isEmpty(this.dose.getSkipReason()))
		{
			writeEncode(getString("mind:DoseReminderNotif.BodySkipped"));
		}
		write("<br><br>");
		
		// Dose information
		write("<b>");
		writeEncode(this.drugName);
		if (!Util.isEmpty(this.rx.getDoseInfo()))
		{
			write(" (");
			writeEncode(this.rx.getDoseInfo());
			write(")");
		}
		write(" @ ");
		writeEncodeDateTime(this.dose.getTakeDate());
		write("</b><br>");
		if (this.dose.getResolution()==Dose.UNRESOLVED && !Util.isEmpty(this.rx.getInstructions()))
		{
			write("<br><i>");
			writeEncode(getString("mind:DoseReminderNotif.SpecialInstructions", this.rx.getInstructions()));
			write("</i><br>");
		}
		write("<br>");
		
		// Response
		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			new ActionListControl(this)
				.setPrompt(getString("mind:DoseReminderNotif.ActionPrompt"))
				.addAction("mind/circle-v.png", getString("mind:DoseReminderNotif.ActionYes"), getString("mind:DoseReminderNotif.ActionYesHelp"))
				.addAction("mind/circle-x.png", getString("mind:DoseReminderNotif.ActionNo"), getString("mind:DoseReminderNotif.ActionNoHelp"))
				.render();
		}
		else
		{
			ActionListControl actionListCtrl = new ActionListControl(this);
			actionListCtrl.setPrompt(getString("mind:DoseReminderNotif.SkippedPrompt"));
			for (int i=1; i<=8; i++)
			{
				actionListCtrl.addAction(String.valueOf(i), getString("mind:DoseReminderNotif.SkipReason_" + i));
			}
			actionListCtrl.addAction(null, getString("mind:DoseReminderNotif.OtherReason"));
			actionListCtrl.render();
		}
	}
	
	@Override
	public void renderShortText() throws Exception
	{
		if (!this.isActionable())
		{
			// Dose is fully resolved
			throw new PageNotFoundException();
		}
		
		// Greeting
		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			write(	getString("mind:DoseReminderNotif.SMSBody",
					Setup.getAppTitle(getLocale()),
					DateFormatEx.getMiniTimeInstance(getLocale(), getTimeZone()).format(this.dose.getTakeDate()),
					this.drugName,
					getString("mind:DoseReminderNotif.ActionYes"),
					getString("mind:DoseReminderNotif.ActionNo")));
		}
		else if (this.dose.getResolution()==Dose.SKIPPED && Util.isEmpty(this.dose.getSkipReason()))
		{
			write(	getString("mind:DoseReminderNotif.SMSBodySkipped",
					Setup.getAppTitle(getLocale()),
					DateFormatEx.getMiniTimeInstance(getLocale(), getTimeZone()).format(this.dose.getTakeDate()),
					this.drugName));
		}
	}
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		// !$! Voice notif not yet implemented
		throw new PageNotFoundException();
	}
	
/*
	This code was written by Ilya. It is kept here for reference when reimplementing the voice API.
 
 # {0} - App.Owner
DoseReminderNotif.VoiceBody = This is a medication reminder from {0}. It''s time to take your next dose of
DoseReminderNotif.VoiceBodySkipped = Please tell us why you skipped your dose of
DoseReminderNotif.VoiceActionPrompt = Please, press 1 if you have taken this dose. Press 2 if you have skipped this dose.

	@Override
	public void renderVoiceXML() throws Exception
	{		
		StringBuilder builder = new StringBuilder();
		if(!this.isActionable())
		{
			// Dose is fully resolved - send vxml to end the call
			builder.append("<form>");
			builder.append("<block>");
			builder.append("<prompt>");
			builder.append("Thank you. Goodbye!");
			builder.append("</prompt>");
			builder.append("</block>");
			builder.append("</form>");
		}			
		else if(this.dose.getResolution() == Dose.UNRESOLVED)
		{			
			//initial vxml document			
			builder.append("<form id=\"welcome\">");
			builder.append("<field name=\"userinput\">");
			builder.append("<prompt>");
			builder.append(getString("mind:DoseReminderNotif.VoiceBody", Setup.getAppOwner(getLocale())));			
			builder.append("<break strength=\"weak\"/>").append(this.drugName);
			//if(!Util.isEmpty(this.rx.getDoseInfo()))
			//	builder.append(this.rx.getDoseInfo());			
			
			builder.append(" at ");
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", getLocale());
			String dateText = dateFormat.format(this.dose.getTakeDate());
 			builder.append("<say-as interpret-as=\"vxml:date\">");
 			builder.append(dateText);
			builder.append("</say-as>");
			dateFormat.applyPattern("h:mm a");
			String timeText = dateFormat.format(this.dose.getTakeDate());
			builder.append("<say-as interpret-as=\"vxml:time\">");
			builder.append(timeText);
			builder.append("<break strength=\"weak\"/>");
			builder.append("</say-as>");
			
			builder.append(getString("mind:DoseReminderNotif.VoiceActionPrompt"));
			
			builder.append("</prompt>");
			builder.append("<grammar xml:lang=\"en-US\" root=\"MYRULE\" mode=\"dtmf\">");
			builder.append("<rule id=\"MYRULE\" scope=\"public\">");
			builder.append("<one-of>");
			builder.append("<item> 1 </item>");
			builder.append("<item> 2 </item>");
			builder.append("</one-of>");
			builder.append("</rule>");
			builder.append("</grammar>");
			builder.append("<noinput>");
			builder.append("<prompt>");
			builder.append("The system is waiting for your response.");
			builder.append("</prompt>");
			builder.append("<reprompt />");
			builder.append("</noinput>");
			builder.append("<nomatch>");
			builder.append("<prompt>");
			builder.append("Wrong number pressed. Please try again.");
			builder.append("</prompt>");
			builder.append("<reprompt />");
			builder.append("</nomatch>");
			builder.append("</field>");
			builder.append("<filled namelist=\"userinput\" mode=\"all\">");
			builder.append("<var name=\"action\" expr=\"'YES'\"/>"); 
			builder.append("<var name=\"callerid\" expr=\"session.callerid\"/>"); 
			builder.append("<if cond=\"userinput==2\">");
			builder.append("<assign name=\"action\" expr=\"'NO'\"/>");
			builder.append("</if>");
			builder.append("<submit next=\"http://");
			builder.append(Setup.getHost()).append(":").append(Setup.getPort());
			builder.append(Controller.getServletPath()).append("/").append(VoiceXMLPage.COMMAND);
			builder.append("\" namelist=\"action callerid\" method=\"post\"/>");  
			builder.append("</filled>");
			builder.append("</form>");		
		}
		else if(this.dose.getResolution() == Dose.SKIPPED && Util.isEmpty(this.dose.getSkipReason()))
		{
			builder.append("<form id=\"skip\">");
			builder.append("<field name=\"action\">");
			builder.append("<prompt>");
			builder.append(getString("mind:DoseReminderNotif.VoiceBodySkipped"));	
			builder.append("<break strength=\"weak\"/>").append(this.drugName);				

			for(int i = 1; i <= 8; i++)
			{
				builder.append("<break strength=\"weak\"/>");
				builder.append(" for ");
				builder.append(getString("mind:DoseReminderNotif.SkipReason_" + i));
				builder.append(" press ").append(i);
			}
			
			builder.append("</prompt>");
			builder.append("<grammar xml:lang=\"en-US\" root=\"MYRULE\" mode=\"dtmf\">");
			builder.append("<rule id=\"MYRULE\" scope=\"public\">");
			builder.append("<one-of>");
			builder.append("<item> 1 </item>");
			builder.append("<item> 2 </item>");
			builder.append("<item> 3 </item>");
			builder.append("<item> 4 </item>");
			builder.append("<item> 5 </item>");
			builder.append("<item> 6 </item>");
			builder.append("<item> 7 </item>");
			builder.append("<item> 8 </item>");
			builder.append("</one-of>");
			builder.append("</rule>");
			builder.append("</grammar>");
			builder.append("<noinput>");
			builder.append("<prompt>");
			builder.append("The system is waiting for your response.");
			builder.append("</prompt>");
			builder.append("<reprompt />");
			builder.append("</noinput>");
			builder.append("<nomatch>");
			builder.append("<prompt>");
			builder.append("Wrong number pressed. Please try again.");
			builder.append("</prompt>");
			builder.append("<reprompt />");
			builder.append("</nomatch>");
			builder.append("</field>");
			builder.append("<filled namelist=\"action\" mode=\"all\">");
			builder.append("<var name=\"callerid\" expr=\"session.callerid\"/>"); 
			builder.append("<submit next=\"http://");
			builder.append(Setup.getHost()).append(":").append(Setup.getPort());
			builder.append(Controller.getServletPath()).append("/").append(VoiceXMLPage.COMMAND);
			builder.append("\" namelist=\"action callerid\" method=\"post\"/>"); 
			builder.append("</filled>");
			builder.append("</form>");						
		}				
		write(builder.toString());		
	}
*/
}
