package samoyan.apps.admin;

import java.util.TimeZone;

import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.ServerStore;
import samoyan.servlet.WebPage;

public abstract class AdminPage extends WebPage
{
	public final static String COMMAND = "admin";
	
	@Override
	public boolean isAuthorized() throws Exception
	{
		return PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), Permission.SYSTEM_ADMINISTRATION);
	}

	@Override
	public TimeZone getTimeZone()
	{
		// !$! Always server time zone?
		try
		{
			return ServerStore.getInstance().loadFederation().getTimeZone();
		}
		catch (Exception e)
		{
			return TimeZone.getDefault();
		}
	}
}
