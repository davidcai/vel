package samoyan.apps.profile;

import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;

public class LoginNameChangedNotif extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/login-name-changed.notif";

	public final static String PARAM_OLD_LOGINNAME = "old";
	
	@Override
	public void renderSimpleHTML() throws Exception
	{
		String oldUserName = getParameterString(PARAM_OLD_LOGINNAME);
		User user = UserStore.getInstance().load(getContext().getUserID());
		write(Util.textToHtml(getString("profile:LoginNameChangedNotif.Body", user.getLoginName(), oldUserName)));
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:LoginNameChangedNotif.Title");
	}
	
	@Override
	public boolean isAuthorized() throws Exception
	{
		return this.getContext().getUserID()!=null;
	}
}
