package baby.pages.info;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.WideLinkGroupControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Day;
import samoyan.core.ParameterMap;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class AppointmentsListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/appointments";
	
	@Override
	public void renderHTML() throws Exception
	{
		// Add button
		if (getContext().getUserAgent().isSmartPhone())
		{
			writeFormOpen("GET", EditAppointmentPage.COMMAND);
			new ButtonInputControl(this, EditAppointmentPage.PARAM_EDIT)
				.setValue(getString("information:AppointmentsList.AddHotButton"))
				.setMobileHotAction(true)
				.setAttribute("class", "NoShow")
				.render();
			writeFormClose();
		}
		else
		{
			new LinkToolbarControl(this)
				.addLink(getString("information:AppointmentsList.AddLink"), getPageURL(EditAppointmentPage.COMMAND), "icons/standard/simple-clock-16.png")
				.render();
		}

		
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
			Day today = new Day(getTimeZone(), new Date());

			boolean phone = getContext().getUserAgent().isSmartPhone();
			DateFormat dfDow = DateFormatEx.getSimpleInstance(phone?"EEE":"EEEE','", getLocale(), getTimeZone());
			DateFormat dfDate = phone? DateFormatEx.getMiniDateInstance(getLocale(), getTimeZone()) : DateFormatEx.getDateInstance(getLocale(), getTimeZone());
			DateFormat dfTime = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
			
			WideLinkGroupControl wlg = new WideLinkGroupControl(this);
			for (UUID appointmentID : appointmentIDs)
			{
				Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
				
				StringBuilder dateStr = new StringBuilder();
				
				Day d = new Day(getTimeZone(), appointment.getDateTime());
				if (d.equals(today))
				{
					dateStr.append(getString("information:AppointmentsList.Today"));
				}
				else
				{
					dateStr.append(dfDow.format(appointment.getDateTime()));
				}
				dateStr.append(" ");
				dateStr.append(dfDate.format(appointment.getDateTime()));
				dateStr.append(" ");
				dateStr.append(dfTime.format(appointment.getDateTime()));
				
				String url = getPageURL(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, appointmentID));
				
				wlg.addLink()
					.setTitle(appointment.getDescription())
					.setValue(dateStr.toString())
					.setURL(url);
			}
			wlg.render();
		}
		else
		{
			writeEncode(getString("information:AppointmentsList.NoAppointment"));
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:AppointmentsList.Title");
	}
}
