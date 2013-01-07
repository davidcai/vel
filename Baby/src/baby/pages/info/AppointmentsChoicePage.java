package baby.pages.info;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.WideLinkGroupControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.servlet.exc.RedirectException;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class AppointmentsChoicePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/day-appointments";
	
	public final static String PARAM_YYYY = "y";
	public final static String PARAM_MM = "m";
	public final static String PARAM_DD = "d";
	
	@Override
	public void renderHTML() throws Exception
	{
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
		
		// Get appointments for the day
		cal.set(yyyy, mm - 1, dd, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		
		cal.add(Calendar.DATE, 1);
		Date to = cal.getTime();
		
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getByDate(getContext().getUserID(), from, to, false);
		
		// Redirect to the appointment page if only one appointment exists for the day
		if (appointmentIDs.size() == 1)
		{
			throw new RedirectException(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, appointmentIDs.get(0)));
		}
				
		// Date
		write("<h2>");
		cal.set(yyyy, mm - 1, dd, 0, 0, 0);
		writeEncodeDate(cal.getTime());
		write("</h2>");
		
		// List
		if (appointmentIDs.isEmpty() == false)
		{
			writeEncode(getString("information:AppointmentsChoice.Help"));
			write("<br><br>");
			
			DateFormat dfTime = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
			
			WideLinkGroupControl wlg = new WideLinkGroupControl(this);
			for (UUID appointmentID : appointmentIDs)
			{
				Appointment appointment = AppointmentStore.getInstance().load(appointmentID);

				String url = getPageURL(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, appointmentID.toString()));

				wlg.addLink()
					.setTitle(appointment.getDescription())
					.setValue(dfTime.format(appointment.getDateTime()))
					.setURL(url);
			}
			wlg.render();
		}
		else
		{
			writeEncode(getString("information:AppointmentsChoice.NoAppointment"));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("information:AppointmentsChoice.Title");
	}
}
