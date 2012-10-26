package samoyan.apps.system;

import java.util.Date;

import samoyan.database.Notification;
import samoyan.database.NotificationStore;
import samoyan.servlet.WebPage;

/**
 * This page is called when a voice call connects with the phone number, but no response is detected to the
 * initial "press 1 to continue" prompt. 
 * @author brian
 *
 */
public final class UnresponsiveVoiceCallPage extends WebPage
{
	public final static String COMMAND = "unresponsive-voice-call";
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		// Find the notification linked to the Voxeo session ID
		String externalID = getParameterString("session.sessionid");
		Notification notif = NotificationStore.getInstance().openByExternalID(externalID);
		if (notif!=null)
		{
			NotificationStore.getInstance().reportError(notif, new Date());
			NotificationStore.getInstance().save(notif);
		}		
	}
}
