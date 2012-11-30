package samoyan.apps.admin.usermgmt;

import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.UserGroupTypeAhead;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.database.*;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

public class UserGroupListPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/user-group-list";

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:UserGroupList.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		List<UUID> all = UserGroupStore.getInstance().getAllIDs();

		// Create new
		new LinkToolbarControl(this)
			.addLink(	getString("admin:UserGroupList.CreateNewGroup"), getPageURL(UserGroupPage.COMMAND), "icons/basic1/pencil_16.png")
			.render();
		
		if (all.size()==0)
		{
			writeEncode(getString("admin:UserGroupList.NoGroups"));
			return;
		}

		// Search
		writeFormOpen();
		writeTypeAheadInput("q", null, null, 30, UserGroup.MAXSIZE_NAME, getPageURL(UserGroupTypeAhead.COMMAND));
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		write("<br>");
		
		writeFormOpen();

		new DataTableControl<UUID>(this, "groups", all)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("admin:UserGroupList.GroupName")).width(84);
				column(getString("admin:UserGroupList.UserCount")).width(15).align("right").alignHeader("right");
			}

			@Override
			protected void renderRow(UUID groupID) throws Exception
			{
				UserGroup grp = UserGroupStore.getInstance().load(groupID);
				
				cell();
				writeCheckbox("chk_" + grp.getID().toString(), null, false);	
				
				cell();
				writeLink(grp.getName(), getPageURL(UserGroupPage.COMMAND, new ParameterMap(UserGroupPage.PARAM_ID, grp.getID().toString())));

				cell();
				int count = UserUserGroupLinkStore.getInstance().getUsersForGroup(grp.getID()).size();
				writeLink(String.valueOf(count), getPageURL(MemberListPage.COMMAND, new ParameterMap(MemberListPage.PARAM_ID, grp.getID().toString())));
			}
		}.render();
		
		write("<br>");
		writeRemoveButton("delete");
		writeFormClose();
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		
		if (isParameter("q"))
		{
			Pair<String, String> q = getParameterTypeAhead("q");
			UserGroup group = UserGroupStore.getInstance().loadByName(q.getKey());
			if (group!=null)
			{
				throw new RedirectException(UserGroupPage.COMMAND, new ParameterMap(UserGroupPage.PARAM_ID, group.getID().toString()));
			}
		}
		
		// Remove checked groups
		if (isParameter("delete"))
		{
			for (String param : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UserGroupStore.getInstance().remove(UUID.fromString(param.substring(4)));
			}
		}
		
		// Redirect to self in order to clear form submission
		throw new RedirectException(ctx.getCommand(), null);
	}
}
