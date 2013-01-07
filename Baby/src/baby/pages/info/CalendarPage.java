package baby.pages.info;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.LinkToolbarControl;

import baby.controls.BadgedCalendarControl;
import baby.controls.BadgedCalendarControl.Badge;
import baby.database.Appointment;
import baby.database.AppointmentStore;
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
		// Add button
		if (getContext().getUserAgent().isSmartPhone())
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
			new LinkToolbarControl(this)
				.addLink(getString("information:Calendar.AddLink"), getPageURL(EditAppointmentPage.COMMAND), "icons/standard/pencil-16.png")
				.render();
		}
		
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
		
		// Calendar
		write("<div class=\"CalendarContainer\">");
		
		BadgedCalendarControl calCtrl = new BadgedCalendarControl(this);
		if (cal.get(Calendar.YEAR) != yyyy || cal.get(Calendar.MONTH) != mm - 1)
		{
			calCtrl.setHighlightSelectedDay(false);
		}
		
		calCtrl.setDay(yyyy, mm, dd);
		cal.set(yyyy, mm - 1, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		Date from = cal.getTime();
		
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();

		// Appointment dues
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getByDate(getContext().getUserID(), from, to, true);
		for (UUID appointmentID : appointmentIDs)
		{
			Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
			
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(appointment.getDateTime());
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(Badge.AppointmentDue);			
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
		
		calCtrl.setCommand(AppointmentsChoicePage.COMMAND, null);
		calCtrl.render();
		
		write("</div>"); //-- .CalendarContainer
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Calendar.Title");
	}
}
