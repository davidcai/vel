package baby.pages.info;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class EditAppointmentPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/appointment/edit";
	
	public final static String PARAM_ID = "id";
	public final static String PARAM_DESC = "desc";
	public final static String PARAM_TYPE = "type";
	public final static String PARAM_DATE_YEAR = "y";
	public final static String PARAM_DATE_MON = "m";
	public final static String PARAM_DATE_DAY = "d";
	public final static String PARAM_DATE_TIME = "t";
	public final static String PARAM_ASKMYDOCTOR = "askdr";
//	public final static String PARAM_REMINDME_ONE_DAY_BEFORE = "remind1d";
//	public final static String PARAM_REMINDME_FOUR_HOURS_BEFORE = "remind4h";
//	public final static String PARAM_REMINDME_TWO_HOURS_BEFORE = "remind2h";
//	public final static String PARAM_REMINDME_ONE_HOUR_BEFORE = "remind1h";
	public final static String PARAM_SAVE = "save";
	public final static String PARAM_REMOVE = "remove";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			validateParameterString(PARAM_DESC, 1, Appointment.MAXSIZE_DESCRIPTION);
			if (getParameterDateTime() == null) 
			{
				throw new WebFormException(new String[] { PARAM_DATE_MON, PARAM_DATE_DAY, PARAM_DATE_YEAR }, 
					getString("information:EditAppointment.InvalidDate"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		Appointment appointment = getEditingAppointment();
		
		if (isParameter(PARAM_SAVE))
		{
			// Copy user inputs into the current appointment and save it
			appointment.setUserID(getContext().getUserID());
			appointment.setDescription(getParameterString(PARAM_DESC));
			appointment.setType(getParameterString(PARAM_TYPE));
			appointment.setDateTime(getParameterDateTime());
//			appointment.setRemindMeOneDayBefore(isParameter(PARAM_REMINDME_ONE_DAY_BEFORE));
//			appointment.setRemindMeFourHoursBefore(isParameter(PARAM_REMINDME_FOUR_HOURS_BEFORE));
//			appointment.setRemindMeTwoHoursBefore(isParameter(PARAM_REMINDME_TWO_HOURS_BEFORE));
//			appointment.setRemindMeOneHourBefore(isParameter(PARAM_REMINDME_ONE_HOUR_BEFORE));
			appointment.setAskMyDoctor(getParameterString(PARAM_ASKMYDOCTOR));
			AppointmentStore.getInstance().save(appointment);
			
//			if (getContext().getUserAgent().isSmartPhone())
//			{
//				// Go back to view page
//				throw new RedirectException(AppointmentPage.COMMAND, 
//					new ParameterMap(PARAM_SAVE, "").plus(AppointmentPage.PARAM_ID, appointment.getID().toString()));
//			}
			
			throw new RedirectException(AppointmentsListPage.COMMAND, new ParameterMap(PARAM_SAVE, ""));
		}
		else if (isParameter(PARAM_REMOVE))
		{
			AppointmentStore.getInstance().remove(appointment.getID());
			
			throw new RedirectException(AppointmentsListPage.COMMAND, new ParameterMap(PARAM_REMOVE, ""));
		}
	}
	
	private Date getParameterDateTime()
	{
		Date date = null;
		
		try
		{
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			cal.clear();
			cal.setLenient(false);
			
			int time = getParameterInteger(PARAM_DATE_TIME);
			int hour = time / 60;
			int min = time % 60;
			
			cal.set(getParameterInteger(PARAM_DATE_YEAR), getParameterInteger(PARAM_DATE_MON), getParameterInteger(PARAM_DATE_DAY), hour, min, 0);
			cal.set(Calendar.MILLISECOND, 0);
			
			date = cal.getTime();
		}
		catch (Exception e)
		{
			date = null;
		}
		
		return date;
	}
	
	private Appointment getEditingAppointment() throws Exception
	{
		Appointment appointment = null;
		if (isParameterNotEmpty(PARAM_ID))
		{
			UUID id = getParameterUUID(PARAM_ID);
			appointment = AppointmentStore.getInstance().open(id);
		}
		if (appointment == null)
		{
			appointment = new Appointment();
			
			// Set to today
			appointment.setDateTime(Calendar.getInstance(getTimeZone()).getTime());
		}
		
		return appointment;
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<div class=\"PaddedPageContent\">");
		
		Appointment appointment = getEditingAppointment();
		
		UserAgent ua = getContext().getUserAgent();
		if (ua.isSmartPhone() == false)
		{
			writeEncode(getString("information:EditAppointment.Help"));
			write("<br><br>");
		}
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Description
		twoCol.writeRow(getString("information:EditAppointment.Description"));
		twoCol.writeTextInput(PARAM_DESC, appointment.getDescription(), 40, Appointment.MAXSIZE_DESCRIPTION);
		
		// Type
		twoCol.writeRow(getString("information:EditAppointment.Type"));
		SelectInputControl select = new SelectInputControl(twoCol, PARAM_TYPE);
		select.setInitialValue(appointment.getType());
		for (String s : BabyConsts.SECTIONS_APPOINTMENT)
		{
			select.addOption(s,s);
		}
		select.render();
		
		// DateTime
		writeDateTime(twoCol, appointment.getDateTime());
		
//		// Remind me
//		twoCol.writeRow(getString("information:EditAppointment.RemindMe"));
//		twoCol.writeCheckbox(PARAM_REMINDME_ONE_DAY_BEFORE, getString("information:EditAppointment.OneDayBefore"), appointment.isRemindMeOneDayBefore());
//		twoCol.write("&nbsp;");
//		twoCol.writeCheckbox(PARAM_REMINDME_FOUR_HOURS_BEFORE, getString("information:EditAppointment.FourHoursBefore"), appointment.isRemindMeFourHoursBefore());
//		twoCol.write("&nbsp;");
//		twoCol.writeCheckbox(PARAM_REMINDME_TWO_HOURS_BEFORE, getString("information:EditAppointment.TwoHoursBefore"), appointment.isRemindMeTwoHoursBefore());
//		twoCol.write("&nbsp;");
//		twoCol.writeCheckbox(PARAM_REMINDME_ONE_HOUR_BEFORE, getString("information:EditAppointment.OneHourBefore"), appointment.isRemindMeOneHourBefore());
		
		// Ask my doctor
		twoCol.writeRow(getString("information:EditAppointment.AskMyDoctor"));
		twoCol.writeTextAreaInput(PARAM_ASKMYDOCTOR, appointment.getAskMyDoctor(), 70, 5, 0);
		
		twoCol.render();
		
		// Postbacks
		if (isParameterNotEmpty(PARAM_ID))
		{
			writeHiddenInput(PARAM_ID, getParameterString(PARAM_ID));
		}
		
		// Buttons and links
		write("<br>");
		writeSaveButton(PARAM_SAVE, appointment);
		if (appointment.isSaved())
		{
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
		}
		
		writeFormClose();
		
		write("</div>");
	}
	
	private void writeDateTime(TwoColFormControl twoCol, Date datetime)
	{
		twoCol.writeRow(getString("information:EditAppointment.DateTime"));
		
		Calendar calToday = Calendar.getInstance(getTimeZone(), getLocale());
		int y = calToday.get(Calendar.YEAR);
		int m = calToday.get(Calendar.MONTH);
		int d = calToday.get(Calendar.DAY_OF_MONTH);
		int h = calToday.get(Calendar.HOUR_OF_DAY);
		
		// Month
		SelectInputControl mon = new SelectInputControl(twoCol, PARAM_DATE_MON);
		DateFormat df = new SimpleDateFormat(getContext().getUserAgent().isSmartPhone() ? "MMM" : "MMMM");
		df.setTimeZone(calToday.getTimeZone());
		Calendar cal = Calendar.getInstance(getTimeZone());
		cal.clear();
		cal.set(y, 0, d, h, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		for (int i = 0; i < 12; i++)
		{
			mon.addOption(df.format(cal.getTime()), i);
			cal.add(Calendar.MONTH, 1);
		}
		
		// Day
		SelectInputControl day = new SelectInputControl(twoCol, PARAM_DATE_DAY);
		for (int i = 1; i <= 31; i++)
		{
			day.addOption(String.valueOf(i), i);
		}
		
		// Year
		SelectInputControl year = new SelectInputControl(twoCol, PARAM_DATE_YEAR);
		cal.setTime(datetime);
		int yCur = cal.get(Calendar.YEAR);
		if (yCur < y - 1)
		{
			year.addOption(String.valueOf(yCur), yCur);
		}
		for (int i = y - 1; i <= y + 1; i++)
		{
			year.addOption(String.valueOf(i), i);
		}
		
		// Time
		SelectInputControl time = new SelectInputControl(twoCol, PARAM_DATE_TIME);
		df = new SimpleDateFormat("h:mm a");
		df.setTimeZone(calToday.getTimeZone());
		cal.clear();
		cal.set(y, m, d, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		for (int i = 0; i < 48; i++)
		{
			time.addOption(df.format(cal.getTime()), i * 30);
			cal.add(Calendar.MINUTE, 30);
		}

		cal.setTime(datetime);
		mon.setInitialValue(cal.get(Calendar.MONTH));
		day.setInitialValue(cal.get(Calendar.DAY_OF_MONTH));
		year.setInitialValue(cal.get(Calendar.YEAR));
		time.setInitialValue(cal.get(Calendar.HOUR_OF_DAY) * 60 + ((int) (cal.get(Calendar.MINUTE) / 30)) * 30);
		
		mon.render();
		twoCol.write("&nbsp;");
		day.render();
		twoCol.write("&nbsp;");
		year.render();
		twoCol.write("&nbsp;");
		time.render();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:EditAppointment.Title");
	}
}
