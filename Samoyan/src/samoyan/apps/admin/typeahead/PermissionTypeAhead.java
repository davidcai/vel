package samoyan.apps.admin.typeahead;

import java.util.List;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.TypeAhead;
import samoyan.database.PermissionStore;

public class PermissionTypeAhead extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/permission.typeahead";

	public PermissionTypeAhead()
	{
		super();
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String q) throws Exception
			{
				List<String> permissions = PermissionStore.getInstance().searchByName(q);
				for (String perm : permissions)
				{
					addOption(perm);
				}
			}
		});
	}
}
