package mind.pages.patient.reminders;

import java.util.Date;
import java.util.UUID;

import mind.database.Dose;
import mind.database.DoseStore;
import mind.database.Drug;
import mind.database.DrugStore;
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
	private Drug drug;
	private String drugName;
		
	@Override
	public void validate() throws Exception
	{
		String action = getParameterString(RequestContext.PARAM_ACTION);

		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			if (action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionYes"))==false &&
				action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionNo"))==false &&
				action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionTaken"))==false &&
				action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionSkipped"))==false)
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
			if (action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionYes")) || action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionTaken")))
			{
				this.dose.setResolution(Dose.TAKEN);
				this.dose.setResolutionDate(new Date());
				DoseStore.getInstance().save(this.dose);				
			}
			else if (action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionNo")) || action.equalsIgnoreCase(getString("mind:DoseReminderNotif.ActionSkipped")))
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
		if (!this.isActionable())
		{
			// Dose is fully resolved
			throw new PageNotFoundException();
		}
		
		// Greeting
		if (this.dose.getResolution()==Dose.UNRESOLVED)
		{
			write("<block><prompt bargein=\"true\" bargeintype=\"hotword\">");
			writeEncode(getString("mind:DoseReminderNotif.VoiceBody", Setup.getAppOwner(getLocale()), this.drugName, this.dose.getTakeDate()));
			write("<break time=\"500ms\"/>");
			write("</prompt></block>");

			new ActionListControl(this)
				.setPrompt(getString("mind:DoseReminderNotif.ActionPrompt"))
				.addAction(getString("mind:DoseReminderNotif.ActionTaken"), getString("mind:DoseReminderNotif.ActionYesHelp"))
				.addAction(getString("mind:DoseReminderNotif.ActionSkipped"), getString("mind:DoseReminderNotif.ActionNoHelp"))
				.render();
		}
		else if (this.dose.getResolution()==Dose.SKIPPED && Util.isEmpty(this.dose.getSkipReason()))
		{
			write("<block><prompt>");
			write("<break time=\"500ms\"/>");
			write("</prompt></block>");

			ActionListControl actionListCtrl = new ActionListControl(this);
			actionListCtrl.setPrompt(getString("mind:DoseReminderNotif.SkippedPrompt"));
			for (int i=1; i<=8; i++)
			{
				actionListCtrl.addAction(String.valueOf(i), getString("mind:DoseReminderNotif.VoiceSkipReason_" + i));
			}
			actionListCtrl.render();
		}
	}	
}
