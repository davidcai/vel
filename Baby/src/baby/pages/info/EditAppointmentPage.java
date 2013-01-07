package baby.pages.info;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Notification;
import samoyan.database.NotificationStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class EditAppointmentPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/appointment";
	
	public final static String PARAM_ID = "id";
	public final static String PARAM_EDIT = "edit";
	
	private final static String PARAM_DESC = "desc";
	private final static String PARAM_TYPE = "type";
	private final static String PARAM_DATE_YEAR = "y";
	private final static String PARAM_DATE_MON = "m";
	private final static String PARAM_DATE_DAY = "d";
	private final static String PARAM_DATE_TIME = "t";
	private final static String PARAM_ASKMYDOCTOR = "askdr";
	private final static String PARAM_REMINDER_ONE_DAY = "remind1d";
	private final static String PARAM_REMINDER_TWO_DAYS = "remind2d";
	private final static String PARAM_REMINDER_TWO_HOURS = "remind2h";
	private final static String PARAM_REMINDER_ONE_HOUR = "remind1h";
	private final static String PARAM_SAVE = "save";
	private final static String PARAM_REMOVE = "remove";
	
	private Appointment appt;
	private boolean readOnly;
	
	@Override
	public void init() throws Exception
	{
		this.appt = AppointmentStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.appt==null)
		{
			this.appt = new Appointment();
		}
		else if (this.appt.getUserID().equals(getContext().getUserID())==false)
		{
			// Security check: make sure that appt is owned by this user
			throw new PageNotFoundException();
		}
		
		this.readOnly = (getContext().getUserAgent().isSmartPhone() && !isParameter(PARAM_EDIT));
	}
	
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
		if (isParameter(PARAM_SAVE))
		{
			// Copy user inputs into the current appointment and save it
			this.appt.setUserID(getContext().getUserID());
			this.appt.setDescription(getParameterString(PARAM_DESC));
			this.appt.setType(getParameterString(PARAM_TYPE));
			this.appt.setDateTime(getParameterDateTime());
			this.appt.setReminderOneDay(isParameter(PARAM_REMINDER_ONE_DAY));
			this.appt.setReminderTwoDays(isParameter(PARAM_REMINDER_TWO_DAYS));
			this.appt.setReminderOneHour(isParameter(PARAM_REMINDER_ONE_HOUR));
			this.appt.setReminderTwoHours(isParameter(PARAM_REMINDER_TWO_HOURS));
			this.appt.setAskMyDoctor(getParameterString(PARAM_ASKMYDOCTOR));
			AppointmentStore.getInstance().save(this.appt);
			
			// Delete unsent reminders for this appointment
			// We use the appointment ID as the eventID
			for (UUID notifID : NotificationStore.getInstance().getByEventID(this.appt.getID()))
			{
				Notification notif = NotificationStore.getInstance().load(notifID);
				if (notif.getStatusCode()==Notification.STATUS_UNSENT)
				{
					NotificationStore.getInstance().remove(notifID);
				}
			}
			
			// Schedule reminders
			if (this.appt.isReminderOneHour())
			{
				scheduleReminder(Calendar.HOUR_OF_DAY, 1);
			}
			if (this.appt.isReminderTwoHours())
			{
				scheduleReminder(Calendar.HOUR_OF_DAY, 2);
			}
			if (this.appt.isReminderOneDay())
			{
				scheduleReminder(Calendar.DATE, 1);
			}
			if (this.appt.isReminderTwoDays())
			{
				scheduleReminder(Calendar.DATE, 2);
			}
			
//			if (getContext().getUserAgent().isSmartPhone())
//			{
//				// Go back to view page
//				throw new RedirectException(AppointmentPage.COMMAND, 
//					new ParameterMap(PARAM_SAVE, "").plus(AppointmentPage.PARAM_ID, appointment.getID().toString()));
//			}
			
			throw new RedirectException(AppointmentsListPage.COMMAND, null);
		}
		else if (isParameter(PARAM_REMOVE))
		{
			AppointmentStore.getInstance().remove(this.appt.getID());
			
			throw new RedirectException(AppointmentsListPage.COMMAND, null);
		}
	}
	
	private void scheduleReminder(int field, int amount) throws Exception
	{
		Calendar when = Calendar.getInstance(getTimeZone(), getLocale());
		when.setTime(this.appt.getDateTime());
		when.add(field, -amount);
		if (when.getTimeInMillis() > System.currentTimeMillis())
		{
			for (String channel : Channel.getPush())
			{
				// We use the appointment ID as the eventID
				Notifier.send(channel, when.getTime(), this.appt.getUserID(), this.appt.getID(), AppointmentReminderNotif.COMMAND, new ParameterMap(AppointmentReminderNotif.PARAM_ID, appt.getID()));
			}
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
	
	@Override
	public void renderHTML() throws Exception
	{
		if (this.readOnly)
		{
			renderViewOnly();
		}
		else
		{
			renderEditForm();
		}
	}
	
	private void renderViewOnly() throws Exception
	{
		// Date
		write("<h2>");
		writeEncode(this.appt.getDescription());
		write("</h2>");
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Type
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("information:EditAppointment.Type"));
		twoCol.writeEncode(this.appt.getType());
		
		// Time
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("information:EditAppointment.DateTime"));
		twoCol.writeEncodeDateTime(this.appt.getDateTime());
				
		// Ask doctor
		if (!Util.isEmpty(this.appt.getAskMyDoctor()))
		{
			twoCol.writeSpaceRow();
			twoCol.writeRow(getString("information:EditAppointment.AskMyDoctor"));
			twoCol.writeEncode(this.appt.getAskMyDoctor());
		}
		
		twoCol.render();

		// Edit button
		if (getContext().getUserAgent().isSmartPhone() == false)
		{
			write("<br>");
		}
		
		writeFormOpen("GET", getContext().getCommand());
		new ButtonInputControl(this, null)
			.setValue(getString("information:EditAppointment.Edit"))
			.setMobileHotAction(true)
			.setAttribute("class", getContext().getUserAgent().isSmartPhone() ? "NoShow" : null)
			.render();
		writeHiddenInput(PARAM_ID, this.appt.getID().toString());
		writeHiddenInput(PARAM_EDIT, "");
		
		writeFormClose();
	}

	private void renderEditForm() throws Exception
	{
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
		twoCol.writeTextInput(PARAM_DESC, this.appt.getDescription(), 40, Appointment.MAXSIZE_DESCRIPTION);
		
		// Type
		twoCol.writeRow(getString("information:EditAppointment.Type"));
		SelectInputControl select = new SelectInputControl(twoCol, PARAM_TYPE);
		select.setInitialValue(this.appt.getType());
		for (String s : BabyConsts.SECTIONS_APPOINTMENT)
		{
			select.addOption(s,s);
		}
		select.render();
		
		// DateTime
		writeDateTime(twoCol, this.appt.getDateTime());
		
		// Remind me
		twoCol.writeRow(getString("information:EditAppointment.RemindMe"));
		twoCol.writeCheckbox(PARAM_REMINDER_ONE_HOUR, getString("information:EditAppointment.OneHourBefore"), this.appt.isReminderOneHour());
		twoCol.write("&nbsp;");
		twoCol.writeCheckbox(PARAM_REMINDER_TWO_HOURS, getString("information:EditAppointment.TwoHoursBefore"), this.appt.isReminderTwoHours());
		twoCol.write("&nbsp;");
		twoCol.writeCheckbox(PARAM_REMINDER_ONE_DAY, getString("information:EditAppointment.OneDayBefore"), this.appt.isReminderOneDay());
		twoCol.write("&nbsp;");
		twoCol.writeCheckbox(PARAM_REMINDER_TWO_DAYS, getString("information:EditAppointment.TwoDaysBefore"), this.appt.isReminderTwoDays());
		
		// Ask my doctor
		twoCol.writeRow(getString("information:EditAppointment.AskMyDoctor"));
		twoCol.writeTextAreaInput(PARAM_ASKMYDOCTOR, this.appt.getAskMyDoctor(), 70, 5, 0);
		
		twoCol.render();
		
		// Postbacks
		writeHiddenInput(PARAM_ID, null);
		writeHiddenInput(PARAM_EDIT, null);
		
		// Buttons and links
		write("<br>");
		writeSaveButton(PARAM_SAVE, this.appt);
		if (this.appt.isSaved())
		{
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
		}
		
		writeFormClose();
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
