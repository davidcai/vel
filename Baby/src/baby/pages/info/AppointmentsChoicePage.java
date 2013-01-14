package baby.pages.info;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.WideLinkGroupControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Day;
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
	
	private Day day;
	
	@Override
	public void init() throws Exception
	{
		this.day = new Day(getParameterInteger(PARAM_YYYY), getParameterInteger(PARAM_MM), getParameterInteger(PARAM_DD));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Get appointments for the day
		Date from = day.getDayStart(getTimeZone());
		Date to = day.getDayEnd(getTimeZone());
				
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getByDate(getContext().getUserID(), from, to, false);
		
		// Redirect to the appointment page if only one appointment exists for the day
		if (appointmentIDs.size() == 1)
		{
			throw new RedirectException(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, appointmentIDs.get(0)));
		}
				
		// List
		if (appointmentIDs.isEmpty() == false)
		{
			writeEncode(getString("information:AppointmentsChoice.Help", from));
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
		return getString("information:AppointmentsChoice.Title", this.day.getDayStart(getTimeZone()));
	}
}
