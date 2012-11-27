package baby.pages.master;

import java.util.ArrayList;
import java.util.List;

import baby.app.BabyConsts;
import baby.pages.content.ContentHomePage;
import baby.pages.info.InformationHomePage;
import baby.pages.profile.MedicalCenterPage;
import baby.pages.profile.StagePage;
import samoyan.apps.admin.SystemOverviewPage;
import samoyan.apps.guidedsetup.StartPage;
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
		boolean author = (user!=null && PermissionStore.getInstance().isUserGrantedPermission(user.getID(), BabyConsts.PERMISSION_CONTENT_MANAGEMENT));

		// Initialize the guided setup
		if (user!=null && user.getGuidedSetupPages()==null && !admin && !author)
		{
			List<String> guidedSetupPages = new ArrayList<String>();
			guidedSetupPages.add(StagePage.COMMAND);
			guidedSetupPages.add(MedicalCenterPage.COMMAND);

			user = UserStore.getInstance().open(ctx.getUserID());
			user.setGuidedSetupPages(guidedSetupPages);
			UserStore.getInstance().save(user);
		}

		// Redirect
		if (user.isGuidedSetup())
		{
			throw new RedirectException(StartPage.COMMAND, null);
		}
		else if (admin)
		{
			throw new RedirectException(SystemOverviewPage.COMMAND, null);
		}
		else if (author)
		{
			throw new RedirectException(ContentHomePage.COMMAND, null);
		}
		else
		{
			throw new RedirectException(InformationHomePage.COMMAND, null);
		}
	}
}
