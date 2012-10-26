package samoyan.apps.profile;

import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;

public class PasswordChangedNotif extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/password-changed.notif";

	@Override
	public void renderSimpleHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		write(Util.textToHtml(getString("profile:PasswordChangedNotif.Body", user.getLoginName())));
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:PasswordChangedNotif.Title");
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		return this.getContext().getUserID()!=null;
	}
}
