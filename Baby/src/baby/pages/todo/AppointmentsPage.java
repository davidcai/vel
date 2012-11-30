package baby.pages.todo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.controls.ChecklistControl;
import baby.controls.TimelineControl;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class AppointmentsPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_TODO + "/appointments";
	
	public final static String PARAM_ID = "id";
	public final static String PARAM_DESC = "desc";
	public final static String PARAM_TYPE = "type";
	public final static String PARAM_DATE_YEAR = "y";
	public final static String PARAM_DATE_MON = "m";
	public final static String PARAM_DATE_DAY = "d";
	public final static String PARAM_DATE_TIME = "t";
	public final static String PARAM_REMINDME_ONE_DAY_BEFORE = "remind1d";
	public final static String PARAM_REMINDME_FOUR_HOURS_BEFORE = "remind4h";
	public final static String PARAM_REMINDME_TWO_HOURS_BEFORE = "remind2h";
	public final static String PARAM_REMINDME_ONE_HOUR_BEFORE = "remind1h";
	public final static String PARAM_ASKMYDOCTOR = "askdr";
	public final static String PARAM_SAVE = "save";
	public final static String PARAM_REMOVE = "remove";
	public final static String PARAM_NEW = "new";
	
	protected Appointment curAppointment;
	private List<UUID> appointmentIDs = new ArrayList<UUID>();
	
	@Override
	public void init() throws Exception
	{
		// Get all appointmentIDs sorted by DateTime in descending order
		this.appointmentIDs = AppointmentStore.getInstance().getAll(getContext().getUserID());
		
		if (isParameter(PARAM_NEW) == false) 
		{
			// Open current appointment by appointment ID if ID param exists in URL
			this.curAppointment = AppointmentStore.getInstance().open(getParameterUUID(PARAM_ID));
			
			// If current appointment is null, find the upcoming appointment
			if (this.curAppointment == null)
			{
				Calendar cal = Calendar.getInstance(getTimeZone());
				for (UUID id : this.appointmentIDs)
				{
					Appointment appointment = AppointmentStore.getInstance().load(id);
					if (cal.getTime().compareTo(appointment.getDateTime()) <= 0)
					{
						this.curAppointment = (Appointment) appointment.clone();
					}
					else
					{
						break;
					}
				}
			}
		}
		
		if (this.curAppointment == null)
		{
			this.curAppointment = new Appointment();
			
			// Set to today
			this.curAppointment.setDateTime(Calendar.getInstance(getTimeZone()).getTime());
		}
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
					getString("todo:Appointments.InvalidDate"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			// Copy user inputs into the current appointment and save it
			this.curAppointment.setUserID(getContext().getUserID());
			this.curAppointment.setDescription(getParameterString(PARAM_DESC));
			this.curAppointment.setType(getParameterString(PARAM_TYPE));
			this.curAppointment.setDateTime(getParameterDateTime());
			this.curAppointment.setRemindMeOneDayBefore(isParameter(PARAM_REMINDME_ONE_DAY_BEFORE));
			this.curAppointment.setRemindMeFourHoursBefore(isParameter(PARAM_REMINDME_FOUR_HOURS_BEFORE));
			this.curAppointment.setRemindMeTwoHoursBefore(isParameter(PARAM_REMINDME_TWO_HOURS_BEFORE));
			this.curAppointment.setRemindMeOneHourBefore(isParameter(PARAM_REMINDME_ONE_HOUR_BEFORE));
			this.curAppointment.setAskMyDoctor(getParameterString(PARAM_ASKMYDOCTOR));
			AppointmentStore.getInstance().save(this.curAppointment);
			
			// Redirect
			Map<String, String> params = new ParameterMap(PARAM_SAVE, "");
			
			if (isParameterNotEmpty(PARAM_ID))
			{
				params.put(PARAM_ID, getParameterUUID(PARAM_ID).toString());
			}
			
			throw new RedirectException(AppointmentsPage.COMMAND, params);
		}
		else if (isParameter(PARAM_REMOVE))
		{
			AppointmentStore.getInstance().remove(this.curAppointment.getID());
			
			throw new RedirectException(AppointmentsPage.COMMAND, new ParameterMap(PARAM_REMOVE, ""));
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
		writeHorizontalNav(AppointmentsPage.COMMAND);
		
		writeEncode(getString("todo:Appointments.Help"));
		write("<br><br>");
		
		// Appointment form
		writeAppointmentForm();
		
		if (this.curAppointment.isSaved())
		{
			writeArticlesAndChecklists();
		}
	
		// Appointment history
		writeAppointmentHistory();
	}

	private void writeAppointmentForm() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Description
		twoCol.writeRow(getString("todo:Appointments.Description"));
		twoCol.writeTextInput(PARAM_DESC, this.curAppointment.getDescription(), 40, Appointment.MAXSIZE_DESCRIPTION);
		
		// Type
		twoCol.writeRow(getString("todo:Appointments.Type"));
		new SelectInputControl(twoCol, PARAM_TYPE)
			.addOption(BabyConsts.SECTION_CHECKUP, BabyConsts.SECTION_CHECKUP)
			.addOption(BabyConsts.SECTION_ULTRASOUND, BabyConsts.SECTION_ULTRASOUND)
			.addOption(BabyConsts.SECTION_WELL_BABY, BabyConsts.SECTION_WELL_BABY)
			.setInitialValue(this.curAppointment.getType())
			.render();
		
		// DateTime
		writeDateTime(twoCol, this.curAppointment.getDateTime());
		
//		// Remind me
//		twoCol.writeRow(getString("todo:Appointments.RemindMe"));
//		twoCol.writeCheckbox(PARAM_REMINDME_ONE_DAY_BEFORE, getString("todo:Appointments.OneDayBefore"), this.curAppointment.isRemindMeOneDayBefore());
//		twoCol.write("&nbsp;");
//		twoCol.writeCheckbox(PARAM_REMINDME_FOUR_HOURS_BEFORE, getString("todo:Appointments.FourHoursBefore"), this.curAppointment.isRemindMeFourHoursBefore());
//		twoCol.write("&nbsp;");
//		twoCol.writeCheckbox(PARAM_REMINDME_TWO_HOURS_BEFORE, getString("todo:Appointments.TwoHoursBefore"), this.curAppointment.isRemindMeTwoHoursBefore());
//		twoCol.write("&nbsp;");
//		twoCol.writeCheckbox(PARAM_REMINDME_ONE_HOUR_BEFORE, getString("todo:Appointments.OneHourBefore"), this.curAppointment.isRemindMeOneHourBefore());
		
		// Ask my doctor
		twoCol.writeRow(getString("todo:Appointments.AskMyDoctor"));
		twoCol.writeTextAreaInput(PARAM_ASKMYDOCTOR, this.curAppointment.getAskMyDoctor(), 70, 5, 0);
		
		twoCol.render();
		
		// Postbacks
		if (isParameterNotEmpty(PARAM_ID))
		{
			writeHiddenInput(PARAM_ID, getParameterString(PARAM_ID));
		}
		if (isParameter(PARAM_NEW))
		{
			writeHiddenInput(PARAM_NEW, "");
		}
		
		// Buttons and links
		write("<br>");
		writeSaveButton(PARAM_SAVE, this.curAppointment);
		if (this.curAppointment.isSaved())
		{
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
			
			// New appointment link
			write("&nbsp;&nbsp;");
			writeLink(getString("todo:Appointments.NewAppointment"), getPageURL(AppointmentsPage.COMMAND, new ParameterMap(PARAM_NEW, "")));
		}
		
		writeFormClose();
	}
	
	private void writeDateTime(TwoColFormControl twoCol, Date datetime)
	{
		twoCol.writeRow(getString("todo:Appointments.DateTime"));
		
		Calendar calToday = Calendar.getInstance(getTimeZone(), getLocale());
		int y = calToday.get(Calendar.YEAR);
		int m = calToday.get(Calendar.MONTH);
		int d = calToday.get(Calendar.DAY_OF_MONTH);
		int h = calToday.get(Calendar.HOUR_OF_DAY);
		
		// Month
		SelectInputControl mon = new SelectInputControl(twoCol, PARAM_DATE_MON);
		DateFormat df = new SimpleDateFormat("MMMM");
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
		time.setInitialValue(cal.get(Calendar.HOUR_OF_DAY) * 60 + (cal.get(Calendar.MINUTE) / 30 + 1) * 30);
		
		mon.render();
		twoCol.write("&nbsp;");
		day.render();
		twoCol.write("&nbsp;");
		year.render();
		twoCol.write("&nbsp;");
		time.render();
	}
	
	private void writeArticlesAndChecklists() throws Exception
	{
		// Related Articles
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		Stage stage = mother.getPregnancyStage(this.curAppointment.getDateTime());
		int lowStage = TimelineControl.getLowRange(stage.toInteger());
		int highStage = TimelineControl.getHighRange(stage.toInteger());
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySectionAndTimeline(this.curAppointment.getType(), lowStage, highStage);
		if (articleIDs.isEmpty() == false)
		{
			write("<br>");
			for (UUID articleID : articleIDs)
			{
				Article article = ArticleStore.getInstance().load(articleID);
				
				write("<h3>");
				writeEncode(article.getTitle());
				write("</h3>");
				write(article.getHTML());
			}
		}
		
		// Related checklists
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryBySectionAndTimeline(this.curAppointment.getType(), lowStage, highStage);
		if (checklistIDs.isEmpty() == false)
		{
			write("<br>");
			write("<br>");
			for (UUID checklistID : checklistIDs)
			{
				Checklist checklist = ChecklistStore.getInstance().load(checklistID);
				write("<h3>");
				writeEncode(checklist.getTitle());
				write("</h3>");
				new ChecklistControl(this, getContext().getUserID(), checklistID).render();
			}
		}
		
		write("<br>");
	}
	
	private void writeAppointmentHistory() throws Exception
	{
		if (this.appointmentIDs.isEmpty())
		{
			return;
		}
		else
		{
			write("<br><hr><br>");

			write("<h2>");
			writeEncode(getString("todo:Appointments.History"));
			write("</h2>");
			
			write("<table>");
			for (UUID id : this.appointmentIDs)
			{
				Appointment appointment = AppointmentStore.getInstance().load(id);
				
				write("<tr>");
				
				write("<td style=\"text-align: right;\">");
				writeEncodeDate(appointment.getDateTime());
				write("</td>");
				
				write("<td style=\"text-align: right;\">");
				writeEncodeTime(appointment.getDateTime());
				write("</td>");
				
				write("<td>");
				String caption = getString("todo:Appointments.DescAndType", appointment.getDescription(), appointment.getType());
				writeLink(caption, getPageURL(AppointmentsPage.COMMAND, new ParameterMap(PARAM_ID, appointment.getID().toString())));
				write("</td>");
				
				write("</tr>");
			}
			write("</table>");
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("todo:Appointments.Title");
	}
}
