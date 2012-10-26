package samoyan.apps.master;

import samoyan.apps.admin.AdminHomePage;
import samoyan.apps.master.RootPage;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

/**
 * Non-admin users are redirected to this page after a successful login.
 * @author brian
 *
 */
public class WelcomePage extends WebPage
{
	public final static String COMMAND = "welcome";

	@Override
	public void renderHTML() throws Exception
	{
		if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), Permission.SYSTEM_ADMINISTRATION))
		{
			// Redirect to the admin's root page
			throw new RedirectException(AdminHomePage.COMMAND, null);
		}
		else
		{
			// Redirect to the root page
			throw new RedirectException(RootPage.COMMAND, null);
		}
	}
	
	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}
}
