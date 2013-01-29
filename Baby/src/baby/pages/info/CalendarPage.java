package baby.pages.info;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import samoyan.controls.BigCalendarControl;
import samoyan.controls.ButtonInputControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.WideLinkGroupControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Day;
import samoyan.core.ParameterMap;
import baby.controls.TimelineSliderControl;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class CalendarPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/calendar";
	
	public final static String PARAM_YYYY = "y";
	public final static String PARAM_MM = "m";
	public final static String PARAM_DD = "d";
	
	@Override
	public void renderHTML() throws Exception
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		
		
		// Get base date
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		Integer yyyy = getParameterInteger(PARAM_YYYY);
		if (yyyy == null)
		{
			yyyy = cal.get(Calendar.YEAR);
		}
		Integer mm = getParameterInteger(PARAM_MM);
		if (mm == null)
		{
			mm = cal.get(Calendar.MONTH) + 1; // 0-based to 1-based
		}
		Integer dd = getParameterInteger(PARAM_DD);
		if (dd == null)
		{
			dd = cal.get(Calendar.DAY_OF_MONTH);
		}
		
		cal.set(yyyy, mm - 1, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();
				
		Day today = new Day(yyyy, mm, dd);

		// Add button
		if (phone)
		{
			writeFormOpen("GET", EditAppointmentPage.COMMAND);
			new ButtonInputControl(this, null)
				.setValue(getString("information:Calendar.AddHotButton"))
				.setMobileHotAction(true)
				.setAttribute("class", "NoShow")
				.render();
			writeFormClose();
		}
		else
		{
			String url = getPageURL(EditAppointmentPage.COMMAND,
									new ParameterMap(EditAppointmentPage.PARAM_DATE_YEAR, yyyy)
									.plus(EditAppointmentPage.PARAM_DATE_MON, mm)
									.plus(EditAppointmentPage.PARAM_DATE_DAY, dd));
			new LinkToolbarControl(this)
				.addLink(getString("information:Calendar.AddLink"), url, "icons/standard/simple-clock-16.png")
				.render();
		}

		// Appointments
		final HashSet<Integer> apptSet = new HashSet<Integer>();
		
		List<UUID> todaysAppointmentIDs = new ArrayList<UUID>();
		
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getByDate(getContext().getUserID(), from, to, true);
		for (UUID appointmentID : appointmentIDs)
		{
			Appointment appointment = AppointmentStore.getInstance().load(appointmentID);

			cal.setTime(appointment.getDateTime());
			apptSet.add(cal.get(Calendar.DAY_OF_MONTH));
			
			if (appointment.getDateTime().before(today.getDayStart(getTimeZone()))==false &&
				appointment.getDateTime().before(today.getDayEnd(getTimeZone()))==true)
			{
				todaysAppointmentIDs.add(appointmentID);
			}
		}
		
		// Checklists
		final HashSet<Integer> checklistSet = new HashSet<Integer>();
		List<UUID> todaysChecklistIDs = new ArrayList<UUID>();
		
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		Stage stage = mother.getPregnancyStage();
		int lowStage = TimelineSliderControl.getLowRange(stage.toInteger());
		int highStage = TimelineSliderControl.getHighRange(stage.toInteger());
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryByTimeline(lowStage, highStage);
		for (UUID checklistID : checklistIDs)
		{
			Checklist checklist = ChecklistStore.getInstance().load(checklistID);
			Date chklstDue = mother.calcDateOfStage(checklist.getTimelineTo(), getTimeZone());
			
			if (chklstDue != null && chklstDue.before(from) == false && chklstDue.before(to) == true)
			{
				cal.setTime(chklstDue);
				checklistSet.add(cal.get(Calendar.DAY_OF_MONTH));
				
				if (chklstDue.before(today.getDayStart(getTimeZone())) == false && 
					chklstDue.before(today.getDayEnd(getTimeZone())) == true)
				{
					todaysChecklistIDs.add(checklistID);
				}
			}
		}
		
		if (!phone)
		{
			write("<table width=\"100%\"><tr><td width=1>");
		}
		
		// Render control
		cal.setTime(new Date()); // Now
		final int month = mm;
		new BigCalendarControl(this)
		{
			@Override
			protected void renderCell(int yyyy, int mm, int dd) throws Exception
			{
				if (mm==month)
				{
					if (apptSet.contains(dd))
					{
						writeImage("icons/standard/simple-clock-16.png", null);
					}
					else if (checklistSet.contains(dd))
					{
						writeImage("icons/standard/checkmark-16.png", null);
					}
				}
			}
//			@Override
//			protected boolean isCellEnabled(int yyyy, int mm, int dd)
//			{
//				return (mm==month && apptSet.contains(dd));
//			}
		}
//		.setCommand(AppointmentsChoicePage.COMMAND, null)
//		.highlightSelectedDay(cal.get(Calendar.YEAR)==yyyy && cal.get(Calendar.MONTH)==mm-1)
		.render();
		
		if (!phone)
		{
			write("</td><td>");
		}
		else
		{
			write("<br>");
		}

		// List of appointments and/or checklist dues for selected day
		if (todaysAppointmentIDs.isEmpty() == false || todaysChecklistIDs.isEmpty() == false)
		{
			DateFormat dfTime = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
			WideLinkGroupControl wlg = new WideLinkGroupControl(this);
			for (UUID appointmentID : todaysAppointmentIDs)
			{
				Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
				String url = getPageURL(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, appointmentID.toString()));
				wlg.addLink()
					.setTitle(appointment.getDescription())
					.setValue(dfTime.format(appointment.getDateTime()))
					.setURL(url);
			}
			for (UUID checklistID : todaysChecklistIDs)
			{
				Checklist checklist = ChecklistStore.getInstance().load(checklistID);
				//String url = getPageURL(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, appointmentID.toString()));
				wlg.addLink()
					.setTitle(checklist.getTitle())
					.setValue(checklist.getSection());
				//	.setURL(url);
			}
			wlg.render();
		}
		else
		{
			writeEncode(getString("information:Calendar.NoEvents", today.getDayStart(getTimeZone())));
		}

		if (!phone)
		{
			write("</td></tr></table>");
		}

//		// Delivery due
//		Date due = mother.getDueDate(getTimeZone());
//		if (due == null)
//		{
//			due = mother.getBirthDate(getTimeZone());
//		}
//		if (due != null)
//		{
//			Calendar c = Calendar.getInstance(getTimeZone());
//			c.setTime(due);
//			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
//				.add(Badge.DeliveryDue);		
//		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Calendar.Title");
	}
}
