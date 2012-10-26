package baby.pages.master;

import baby.pages.info.InformationHomePage;
import samoyan.apps.admin.SystemOverviewPage;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

public class WelcomePage extends samoyan.apps.master.WelcomePage
{
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		boolean admin = (user!=null && PermissionStore.getInstance().isUserGrantedPermission(user.getID(), Permission.SYSTEM_ADMINISTRATION));

		if (admin)
		{
			throw new RedirectException(SystemOverviewPage.COMMAND, null);
		}
		else
		{
			throw new RedirectException(InformationHomePage.COMMAND, null);
		}
	}
}
