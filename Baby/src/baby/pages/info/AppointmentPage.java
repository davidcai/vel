package baby.pages.info;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Util;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class AppointmentPage extends AppointmentsBasePage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/appointment";
	
	public final static String PARAM_ID = "id";
	
	@Override
	public void renderHTML() throws Exception
	{
		UUID appointmentID = getParameterUUID(PARAM_ID);
		Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
		
		write("<div class=\"PaddedPageContent\">");
		
		if (appointment != null)
		{
			// Date
			write("<h2>");
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			cal.setTime(appointment.getDateTime());
			writeEncode(getDescriptiveDate(cal));
			write("</h2>");
			
			// Time
			DateFormat dfTime = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
			writeEncode(dfTime.format(appointment.getDateTime()));
			
			// Description
			write("<br>");
			writeEncode(appointment.getDescription());
			
			// Type
			write("<br>");
			writeEncode(appointment.getType());
			
			// Ask doctor
			if (Util.isEmpty(appointment.getAskMyDoctor()) == false)
			{
				write("<br><br>");
				write("<b>");
				writeEncode(getString("information:Appointment.AskMyDoctor"));
				write("</b>");
				write("<br>");
				writeEncode(appointment.getAskMyDoctor());
			}

			// Edit button
			if (getContext().getUserAgent().isSmartPhone() == false)
			{
				write("<br><br>");
			}
			writeFormOpen("GET", EditAppointmentPage.COMMAND);
			new ButtonInputControl(this, null)
				.setValue(getString("information:Appointment.EditHotButton"))
				.setMobileHotAction(true)
				.setAttribute("class", getContext().getUserAgent().isSmartPhone() ? "NoShow" : null)
				.render();
			writeHiddenInput(EditAppointmentPage.PARAM_ID, appointment.getID().toString());
			writeFormClose();
		}
		else
		{
			writeEncode(getString("information:Appointment.Unavailable"));
		}
		
		write("</div>");
	}
		
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Appointment.Title");
	}
}
