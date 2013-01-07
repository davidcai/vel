package baby.pages.info;

import java.text.DateFormat;

import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.XCoShortenUrl;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.pages.BabyPage;

public class AppointmentReminderNotif extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/appointment-reminder.notif";
	
	/** The appointment ID */
	public final static String PARAM_ID = "id";
	
	private Appointment appt;
	
	@Override
	public void init() throws Exception
	{
		this.appt = AppointmentStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.appt==null)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Redirect to the view appointment page
		throw new RedirectException(EditAppointmentPage.COMMAND, new ParameterMap(EditAppointmentPage.PARAM_ID, this.appt.getID()));
	}
	
	@Override
	public String getTitle() throws Exception
	{
		DateFormat df = DateFormatEx.getSimpleInstance("EEE", this.getLocale(), this.getTimeZone());
		return getString("information:ApptReminder.Title", this.appt.getDescription(), df.format(this.appt.getDateTime()), this.appt.getDateTime());
	}
	
	@Override
	public void renderSimpleHTML() throws Exception
	{
		// Email
		
		writeEncode(getString("information:ApptReminder.Email"));
		write("<br><br>");
		
		write("<b><big>");
		writeEncode(this.appt.getDescription());
		write("</big></b><br>");

		DateFormat df = DateFormatEx.getSimpleInstance("EEEE", this.getLocale(), this.getTimeZone());
		writeEncode(df.format(this.appt.getDateTime()));
		write(", ");
		writeEncodeDateTime(this.appt.getDateTime());
		write(" (");
		writeEncode(this.getTimeZone().getDisplayName(this.getLocale()));
		write(")");
		write("<br><br>");
		
		String url = getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ID, this.appt.getID()));
		writeLink(getString("information:ApptReminder.EditAppt"), url);
	}
	
	@Override
	public void renderShortText() throws Exception
	{
		// SMS, Apple Push
		
		Server fed = ServerStore.getInstance().loadFederation();
		
		write(getTitle());
		write(" ");
		String url = getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ID, this.appt.getID()));
		XCoShortenUrl.shorten(fed.getXCoAPIKey(), url);
	}
	
	@Override
	public void renderText() throws Exception
	{
		renderShortText();
	}
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		write("<block><prompt>");
		write("<break time=\"500ms\"/>");
		
		DateFormat df = DateFormatEx.getSimpleInstance("EEEE", this.getLocale(), this.getTimeZone());
		writeEncode(getString("information:ApptReminder.Voice", this.appt.getDescription(), df.format(this.appt.getDateTime()), this.appt.getDateTime(), Setup.getAppTitle(this.getLocale())));
		
		write("<break time=\"500ms\"/>");
		write("</prompt></block>");
	}
}
