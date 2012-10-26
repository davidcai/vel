package samoyan.apps.admin.typeahead;

import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserUserGroupLinkStore;

public class UserGroupTypeAhead extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/usergroup.typeahead";

	public UserGroupTypeAhead()
	{
		super();
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String q) throws Exception
			{
				List<UUID> groupIDs = UserGroupStore.getInstance().searchByName(q);
				for (UUID groupID : groupIDs)
				{
					UserGroup group = UserGroupStore.getInstance().load(groupID);
					if (group!=null)
					{
						addOption(	group.getName(),
									group.getName(),
									Util.htmlEncode(group.getName() + " (" + UserUserGroupLinkStore.getInstance().getUsersForGroup(groupID).size() + ")"));
					}
				}
			}
		});
	}
}
