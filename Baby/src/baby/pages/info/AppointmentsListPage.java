package baby.pages.info;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import samoyan.controls.WideLinkGroupControl;
import samoyan.core.DateFormatEx;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class AppointmentsListPage extends AppointmentsBasePage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/appointments/list";
	
	@Override
	public void renderHTML() throws Exception
	{
		writeTabs();
		writeAddButton();
		
		UUID userID = getContext().getUserID();
		
		//
		// Upcoming appointments ordered by ascending dates
		//
		
		write("<h2>");
		writeEncode(getString("information:AppointmentsList.Upcoming"));
		write("</h2>");
		
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getAfter(userID, cal.getTime(), true, false);
		
		write("<div class=\"UpcomingAppointments\">");
		writeList(appointmentIDs);
		write("</div>");
		
		write("<br>");
		
		//
		// Previous appointments ordered by descending dates
		//
		
		write("<h2>");
		writeEncode(getString("information:AppointmentsList.Previous"));
		write("</h2>");
		
		cal = Calendar.getInstance(getTimeZone(), getLocale());
		appointmentIDs = AppointmentStore.getInstance().getBefore(userID, cal.getTime(), false, true);
		
		write("<div class=\"PreviousAppointments\">");
		writeList(appointmentIDs);
		write("</div>");
	}
	
	private void writeList(List<UUID> appointmentIDs) throws Exception
	{
		if (appointmentIDs.isEmpty() == false)
		{
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			Calendar calToday = Calendar.getInstance(getTimeZone(), getLocale());
			calToday = clearHours(calToday);
			
			DateFormat dfDate = DateFormatEx.getDateInstance(getLocale(), getTimeZone());
			DateFormat dfTime = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
			
			WideLinkGroupControl wlg = new WideLinkGroupControl(this);
			for (UUID appointmentID : appointmentIDs)
			{
				Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
				cal.setTime(appointment.getDateTime());
				
				String weekDay = getDescriptiveWeekDay(cal);
				String date = (weekDay == null) ? dfDate.format(appointment.getDateTime()) : weekDay;
				String time = dfTime.format(appointment.getDateTime());
				
				wlg.addLink()
					.setTitle(appointment.getDescription())
					.setValue(getString("information:AppointmentsList.DateTime", date, time))
					.setURL(getAppointmentPageURL(appointment.getID()));
			}
			wlg.render();
		}
		else
		{
			write("<div class=\"PaddedPageContent\">");
			writeEncode(getString("information:AppointmentsList.NoAppointment"));
			write("</div>");
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:AppointmentsList.Title");
	}
}
