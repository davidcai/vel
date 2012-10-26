package samoyan.apps.admin.usermgmt;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.master.LoginPage;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;

public class InviteUsersNotif extends WebPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/inviteusers.notif";
	
	public final static String PARAM_SUBJECT = "subject";
	public final static String PARAM_BODY = "body";
	public final static String PARAM_TEMP_PASSWORD = "pw";
	
	@Override
	public void renderSimpleHTML() throws Exception
	{
		String html = getParameterString(PARAM_BODY);
				
		if (!Util.isEmptyHTML(html))
		{
			write(Util.trimHTML(html));
			write("<br><br>");
		}
		
		User user = UserStore.getInstance().load(getContext().getUserID());
				
		write(Util.textToHtml(getString("admin:InviteUsersNotif.LoginInstructions", user.getLoginName(), getParameterString(PARAM_TEMP_PASSWORD))));
		write("<br><br>");
		writeLink(getPageURL(LoginPage.COMMAND), getPageURL(LoginPage.COMMAND));
	}

	@Override
	public String getTitle() throws Exception
	{
		return getParameterString(PARAM_SUBJECT);
	}
}
