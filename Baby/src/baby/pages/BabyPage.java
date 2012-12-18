package baby.pages;

import samoyan.apps.messaging.MessagingPage;
import samoyan.apps.profile.ProfilePage;
import samoyan.database.PermissionStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import baby.app.BabyConsts;

public class BabyPage extends WebPage
{
	public final static String COMMAND_INFORMATION = "info";
	public final static String COMMAND_SCRAPBOOK = "scrapbook";
	public static final String COMMAND_CONTENT = "content";
	public static final String COMMAND_PROFILE = ProfilePage.COMMAND;

	@Override
	public boolean isAuthorized() throws Exception
	{
		RequestContext ctx = getContext();
		String cmd1 = ctx.getCommand(1);
		
		if (cmd1.equalsIgnoreCase(COMMAND_CONTENT))
		{
			return PermissionStore.getInstance().isUserGrantedPermission(ctx.getUserID(), BabyConsts.PERMISSION_CONTENT_MANAGEMENT);
		}
		else if (cmd1.equalsIgnoreCase(COMMAND_INFORMATION) ||
				cmd1.equalsIgnoreCase(COMMAND_SCRAPBOOK) ||
				cmd1.equalsIgnoreCase(ProfilePage.COMMAND) ||
				cmd1.equalsIgnoreCase(MessagingPage.COMMAND))
		{
			return ctx.getUserID() != null;
		}
		else
		{
			return true;
		}
	}
}
