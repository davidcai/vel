package samoyan.apps.master;

import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;

public class JoinNotif extends WebPage
{
	public final static String COMMAND = "join.notif";

	@Override
	public void renderSimpleHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		writeEncode(getString("master:JoinNotif.Body", user.getLoginName()));
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:JoinNotif.Title");
	}
}
