package samoyan.apps.system;

import java.util.Map;
import java.util.UUID;

import samoyan.database.Notification;
import samoyan.database.NotificationStore;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;

/**
 * This page accepts a notification ID as parameter, loads its command and parameters from the event database,
 * and redirects the client there.
 * @author brian
 *
 */
public class NotificationPage extends WebPage
{
	public final static String COMMAND = "notif";
	
	public final static String PARAM_NOTIF_ID = "id";

	@Override
	public void renderHTML() throws Exception
	{
		Notification notif = NotificationStore.getInstance().load(getParameterUUID(PARAM_NOTIF_ID));
		if (notif==null)
		{
			throw new PageNotFoundException();
		}
		
		if (notif.getStatusCode()!=Notification.STATUS_SENT && notif.getStatusCode()!=Notification.STATUS_DELIVERED)
		{
			throw new PageNotFoundException();
		}
				
		String command = notif.getCommand();
		Map<String, String> params = notif.getParameters();
		
		throw new RedirectException(command, params);
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		UUID userID = getContext().getUserID();
		if (userID==null)
		{
			return false;
		}
		
		Notification notif = NotificationStore.getInstance().load(getParameterUUID(PARAM_NOTIF_ID));
		if (notif==null)
		{
			throw new PageNotFoundException();
		}
		
		return notif.getUserID().equals(userID);
	}
}
