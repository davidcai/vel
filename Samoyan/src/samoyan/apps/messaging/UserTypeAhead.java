package samoyan.apps.messaging;

import java.util.List;
import java.util.UUID;

import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;

public class UserTypeAhead extends TypeAhead
{
	public final static String COMMAND = MessagingPage.COMMAND + "/user.typeahead";

	@Override
	protected void doQuery(String q) throws Exception
	{
		List<UUID> userIDs = UserStore.getInstance().searchByName(q);
		for (UUID userID : userIDs)
		{
			User usr = UserStore.getInstance().load(userID);
			if (usr!=null)
			{
				addOption(usr.getLoginName(), usr.getDisplayName(), Util.htmlEncode(usr.getDisplayName() + " <" + usr.getLoginName() + ">"));
			}
		}
	}
	
	@Override
	public boolean isAuthorized() throws Exception
	{
		return getContext().getUserID()!=null;
	}
}
