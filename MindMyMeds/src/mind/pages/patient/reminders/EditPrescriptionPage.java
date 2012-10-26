package mind.pages.patient.reminders;

import java.util.*;

import mind.database.*;
import mind.pages.DrugChooserTypeAhead;
import mind.pages.patient.PatientPage;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.BadRequestException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class EditPrescriptionPage extends PatientPage
{
	public final static String COMMAND = RemindersPage.COMMAND + "/rx";
	
	private Prescription rx = null;
	
	@Override
	public void init() throws Exception
	{
		RequestContext ctx = getContext();
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		
		UUID rxID = getParameterUUID("id");
		if (rxID!=null)
		{
			this.rx = PrescriptionStore.getInstance().open(rxID);
			if (this.rx.getPatientID().equals(patient.getID())==false)
			{
				throw new BadRequestException();
			}
		}
		if (this.rx==null)
		{
			this.rx = new Prescription();
			this.rx.setPatientID(patient.getID());
		}
	}

	@Override
	public void validate() throws Exception
	{		
		// Drug
		validateParameterString("drug", 1, Drug.MAXSIZE_NAME);
		
		// Nickname
		validateParameterString("nickname", 0, Prescription.MAXSIZE_NICKNAME);
		
		// Dose info
		validateParameterString("doseinfo", 0, Prescription.MAXSIZE_DOSE_INFO);

		// Frequency
		validateParameterInteger("freq", 1, 15);
		
		// Hours
		boolean found = false;
		for (int h=0; h<24; h++)
		{
			if (!Util.isEmpty(getParameterString("hour_" + h)))
			{
				found = true;
				break;
			}
		}
		if (!found)
		{
			final String[] HOUR_FIELDS = new String[24];
			for (int i=0; i<24; i++)
			{
				HOUR_FIELDS[i] = "hour_" + i;
			}
			
			throw new WebFormException(HOUR_FIELDS, getString("common:Errors.MissingField"));
		}
		
		// Next day
		Calendar cal = Calendar.getInstance(TimeZoneEx.GMT);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date minDate = cal.getTime();
		cal.add(Calendar.YEAR, 1);
		Date maxDate = cal.getTime();
	
		validateParameterDate("next", minDate, maxDate);
		
		// Doses remaining
		validateParameterInteger("dosesleft", 0, 999);
		
		validateParameterString("doctor", 0, Prescription.MAXSIZE_DOCTOR_NAME);
		validateParameterString("purpose", 0, Prescription.MAXSIZE_PURPOSE);
		validateParameterString("instructions", 0, Prescription.MAXSIZE_INSTRUCTIONS);
	}
	
	@Override
	public void commit() throws Exception
	{	
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		boolean isSaved = this.rx.isSaved();
		
		// Create the drug, if not in database
		String drugName = getParameterString("drug");
		Drug drug = DrugStore.getInstance().loadByName(drugName, patient.getID());
		if (drug==null)
		{
			// Create individual drug for only this user
			drug = new Drug();
			drug.setName(drugName);
			drug.setGenericName(drugName);
			drug.setPatientID(patient.getID());
			DrugStore.getInstance().save(drug);
		}

		// Persist
		this.rx.setDrugID(drug.getID());
		this.rx.setNickname(getParameterString("nickname"));
		this.rx.setDoseInfo(getParameterString("doseinfo"));
		
		this.rx.setFreqDays(getParameterInteger("freq"));
		
		QuarterHourBitSet bitmap = this.rx.getQuarterHourBitmap();
		for (int h=0; h<24; h++)
		{
			String mVal = getParameterString("hour_" + h);
			for (int m=0; m<60; m+=15)
			{
				if (Util.isEmpty(mVal))
				{
					bitmap.clear(h, m);
				}
				else if (Integer.parseInt(mVal)==m)
				{
					bitmap.set(h, m);
				}
				else
				{
					bitmap.clear(h, m);
				}
			}
		}
		this.rx.setQuarterHourBitmap(bitmap);
		
		int dosesRemaining = getParameterInteger("dosesleft");
		Date nextDate = getParameterDate("next");
		
		this.rx.setDosesRemaining(dosesRemaining);
		
		Date now = new Date();
		if (nextDate.before(now) && dosesRemaining>0)
		{
			this.rx.setNextDoseDate(now);
		}
		else
		{
			this.rx.setNextDoseDate(nextDate);
		}
		this.rx.progressNextDoseDate(user.getTimeZone());
		this.rx.setDoctorName(getParameterString("doctor"));
		this.rx.setPurpose(getParameterString("purpose"));
		this.rx.setInstructions(getParameterString("instructions"));
		
		PrescriptionStore.getInstance().save(this.rx);
		
		if (isSaved==false)
		{
			// Redirect to Rx list page
			throw new RedirectException(PrescriptionListPage.COMMAND, null);
		}
		else
		{
			// Redirect to self in order to clear form submission
			throw new RedirectException(ctx.getCommand(), new ParameterMap("id", this.rx.getID().toString()));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString(this.rx.isSaved()==false? "mind:EditRx.TitleNew" : "mind:EditRx.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		boolean smartPhone = ctx.getUserAgent().isSmartPhone();
		
		writeIncludeCSS("mind/hourchooser.css");
		writeIncludeJS("mind/hourchooser.js");

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeSubtitleRow(getString("mind:EditRx.Drug"));

		// Drug name
		Drug drug = DrugStore.getInstance().load(this.rx.getDrugID());
		String drugName = null;
		if (ctx.getMethod().equalsIgnoreCase("GET"))
		{
			drugName = ctx.getParameter("drug");
		}
		if (drugName==null && drug!=null)
		{
			drugName = drug.getName();
		}
		twoCol.writeRow(getString("mind:EditRx.Drug"));
		twoCol.writeTypeAheadInput("drug", drugName, drugName, 40, Drug.MAXSIZE_NAME, getPageURL(DrugChooserTypeAhead.COMMAND));
		
		// Drug nickname
		twoCol.writeRow(getString("mind:EditRx.Nickname"), getString("mind:EditRx.NicknameHelp", Setup.getAppTitle(getLocale())));
		twoCol.writeTextInput("nickname", this.rx.getNickname(), 20, Prescription.MAXSIZE_NICKNAME);
		
		// Dose info
		twoCol.writeRow(getString("mind:EditRx.DoseInfo"), getString("mind:EditRx.DoseInfoHelp"));
		twoCol.writeTextInput("doseinfo", this.rx.getDoseInfo(), 20, Prescription.MAXSIZE_DOSE_INFO);
		
		
		twoCol.writeSubtitleRow(getString("mind:EditRx.Regimen"));
		
		// Frequency
		twoCol.writeRow(getString("mind:EditRx.Frequency"));
		SelectInputControl select = new SelectInputControl(twoCol, "freq");
		select.setInitialValue(this.rx.getFreqDays());
		for (int i=1; i<=15; i++)
		{
			if (i==1)
			{
				select.addOption(getString("mind:EditRx.Daily"), i);
			}
			else if (i==7)
			{
				select.addOption(getString("mind:EditRx.Weekly"), i);
			}
			else if (i==14)
			{
				select.addOption(getString("mind:EditRx.Biweekly"), i);
			}
			else if (i%7==0)
			{
				select.addOption(getString("mind:EditRx.EveryWeeks", i/7), i);
			}
			else
			{
				select.addOption(getString("mind:EditRx.EveryDays", i), i);
			}
		}
		select.render();
		
		// Hours
		QuarterHourBitSet bitmap = this.rx.getQuarterHourBitmap();
		twoCol.writeRow(getString("mind:EditRx.Hours"));
		twoCol.write("<table class=HourTable><tr>");
		if (!smartPhone)
		{
			twoCol.write("<td>");
			twoCol.writeImage("mind/coffee.png", null);
			twoCol.write("</td>");
		}
		for (int i=6; i<=11; i++)
		{
			twoCol.write("<td align=center>");
//			twoCol.writeEncode(i%12==0? 12 : i%12);
//			twoCol.write((i%24)>=12?"p":"a");
//			twoCol.write("<br>");
//			twoCol.writeCheckbox("hour_" + (i%24), bitmap.isSet((i%24), 0));
			writeHour(twoCol, i, bitmap);
			twoCol.write("</td>");
		}
		twoCol.write("</tr><tr>");
		if (!smartPhone)
		{
			twoCol.write("<td>");
			twoCol.writeImage("mind/sun.png", null);
			twoCol.write("</td>");
		}
		for (int i=12; i<=17; i++)
		{
			twoCol.write("<td align=center>");
//			twoCol.writeEncode(i%12==0? 12 : i%12);
//			twoCol.write((i%24)>=12?"p":"a");
//			twoCol.write("<br>");
//			twoCol.writeCheckbox("hour_" + (i%24), bitmap.isSet((i%24), 0));
			writeHour(twoCol, i, bitmap);
			twoCol.write("</td>");
		}
		twoCol.write("</tr><tr>");
		if (!smartPhone)
		{
			twoCol.write("<td>");
			twoCol.writeImage("mind/moon.png", null);
			twoCol.write("</td>");
		}
		for (int i=18; i<=23; i++)
		{
			twoCol.write("<td align=center>");
//			twoCol.writeEncode(i%12==0? 12 : i%12);
//			twoCol.write((i%24)>=12?"p":"a");
//			twoCol.write("<br>");
//			twoCol.writeCheckbox("hour_" + (i%24), bitmap.isSet((i%24), 0));
			writeHour(twoCol, i, bitmap);
			twoCol.write("</td>");
		}
		twoCol.write("</tr><tr>");
		if (!smartPhone)
		{
			twoCol.write("<td>");
			twoCol.writeImage("mind/sleep.png", null);
			twoCol.write("</td>");
		}
		for (int i=0; i<=5; i++)
		{
			twoCol.write("<td align=center>");
//			twoCol.writeEncode(i%12==0? 12 : i%12);
//			twoCol.write((i%24)>=12?"p":"a");
//			twoCol.write("<br>");
//			twoCol.writeCheckbox("hour_" + (i%24), bitmap.isSet((i%24), 0));
			writeHour(twoCol, i, bitmap);
			twoCol.write("</td>");
		}
		twoCol.write("</tr></table>");
		twoCol.writeSpaceRow();
		
		// Next day
		twoCol.writeRow(getString("mind:EditRx.NextDay"));
		Calendar cal = Calendar.getInstance(user.getTimeZone());
		if (this.rx.getNextDoseDate()!=null)
		{
			cal.setTime(this.rx.getNextDoseDate());
		}
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		twoCol.writeDateInput("next", cal.getTime());

		// Doses remaining
		twoCol.writeRow(getString("mind:EditRx.DosesRemaining"), getString("mind:EditRx.DosesRemainingHelp"));
		twoCol.writeTextInput("dosesleft", this.rx.getDosesRemaining(), 3, 3);

		
		
		twoCol.writeSubtitleRow(getString("mind:EditRx.AdditionalInformation"));
		
		// Doctor name
		twoCol.writeRow(getString("mind:EditRx.Doctor"), getString("mind:EditRx.DoctorHelp"));
		twoCol.writeTextInput("doctor", this.rx.getDoctorName(), 40, Prescription.MAXSIZE_DOCTOR_NAME);

		// Purpose
		twoCol.writeRow(getString("mind:EditRx.Purpose"), getString("mind:EditRx.PurposeHelp"));
		twoCol.writeTextInput("purpose", this.rx.getPurpose(), 40, Prescription.MAXSIZE_PURPOSE);
		
		// Instructions
		twoCol.writeRow(getString("mind:EditRx.Instructions"), getString("mind:EditRx.InstructionsHelp"));
		twoCol.writeTextInput("instructions", this.rx.getInstructions(), 40, Prescription.MAXSIZE_INSTRUCTIONS);

		twoCol.writeSpaceRow();

		twoCol.render();
		
		write("<br>");
		writeSaveButton(this.rx);
		
		// Postback
		writeHiddenInput("id", null);
		
		writeFormClose();
	}
	
	private void writeHour(WebPage twoCol, int hr, QuarterHourBitSet bitmap)
	{
		int min = -1;
		for (int m=0; m<60; m+=15)
		{
			if (bitmap.get((hr%24), m))
			{
				min = m;
				break;
			}
		}
		
		if (isFormException())
		{
			String postedMin = getContext().getParameter("hour_" + (hr%24));
			if (Util.isEmpty(postedMin))
			{
				min = -1;
			}
			else
			{
				min = Integer.parseInt(postedMin);
			}
		}

		twoCol.write("<div class=Hour h=");
		twoCol.writeEncode(hr%12==0? 12 : hr%12);
		if (min>=0)
		{
			twoCol.write(" m=");
			twoCol.write(min);
		}
		twoCol.write("><span>");
		twoCol.writeEncode(hr%12==0? 12 : hr%12);
		twoCol.write(":");
		if (min<=0)
		{
			twoCol.write("00");
		}
		else
		{
			twoCol.write(min);
		}
		twoCol.write("</span><br><small>");
		twoCol.write((hr%24)>=12?"pm":"am");
		twoCol.write("</small><input type=hidden name=hour_");
		twoCol.write(hr%24);
		twoCol.write(" value=\"");
		if (min>=0)
		{
			twoCol.write(min);
		}
		twoCol.write("\"></div>");
	}
}
