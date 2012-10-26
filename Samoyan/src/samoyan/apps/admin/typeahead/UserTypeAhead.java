package samoyan.apps.admin.typeahead;

import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;

public class UserTypeAhead extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/user.typeahead";

	public UserTypeAhead()
	{
		super();
		setChild(new TypeAhead()
		{
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
		});
	}
}
