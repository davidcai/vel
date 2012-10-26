package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import elert.app.ElertConsts;
import samoyan.apps.system.TypeAhead;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;

public class SchedulerTypeAhead extends WebPage
{
	public static final String COMMAND = "scheduler.typeahead";
		
	public SchedulerTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				UserGroup grp = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_SCHEDULERS);
				
				List<UUID> ids = UserStore.getInstance().searchByNameInGroup(query, grp.getID());
				for(UUID id : ids)
				{
					User scheduler = UserStore.getInstance().load(id);
					addOption(scheduler.getID(), scheduler.getDisplayName());
				}
			}
		});
	}	
}
