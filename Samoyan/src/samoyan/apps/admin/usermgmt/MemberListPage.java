package samoyan.apps.admin.usermgmt;

import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.UserTypeAhead;
import samoyan.controls.DataTableControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserGroupStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.database.UserStore;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class MemberListPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/members";
	public final static String PARAM_ID = "id";
	
	@Override
	public String getTitle() throws Exception
	{
		return UserGroupStore.getInstance().load(getParameterUUID(PARAM_ID)).getName();
	}

	@Override
	public void validate() throws Exception
	{
		Pair<String, String> joinKvp = getParameterTypeAhead("join");
		if (joinKvp!=null && !Util.isEmpty(joinKvp.getValue()))
		{
			User join = UserStore.getInstance().loadByLoginName(joinKvp.getKey());
			if (join==null)
			{
				throw new WebFormException("join", getString("admin:Members.InvalidLoginName"));
			}
			else if (UserUserGroupLinkStore.getInstance().getUsersForGroup(getParameterUUID(PARAM_ID)).contains(join.getID()))
			{
				throw new WebFormException("join", getString("admin:Members.AlreadyMember"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		Pair<String, String> joinKvp = getParameterTypeAhead("join");
		if (joinKvp!=null && !Util.isEmpty(joinKvp.getValue()))
		{
			User join = UserStore.getInstance().loadByLoginName(joinKvp.getKey());
			if (join!=null)
			{
				UserUserGroupLinkStore.getInstance().join(join.getID(), getParameterUUID(PARAM_ID));
			}
		}
		
		if (isParameter("expel"))
		{
			for (String paramName : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UserUserGroupLinkStore.getInstance().expel(UUID.fromString(paramName.substring(4)), getParameterUUID(PARAM_ID));
			}
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, getParameterString(PARAM_ID)));
	}

	@Override
	public void renderHTML() throws Exception
	{
		// Add user
		writeFormOpen();
		writeTypeAheadInput("join", null, null, 30, User.MAXSIZE_NAME, getPageURL(UserTypeAhead.COMMAND));
		write(" ");
		writeButton(getString("admin:Members.Join"));
		writeHiddenInput(PARAM_ID, null); // Repost
		writeFormClose();
		write("<br>");

		// Fetch group members
		List<UUID> members = UserUserGroupLinkStore.getInstance().getUsersForGroup(getParameterUUID(PARAM_ID));
		if (members.size()==0)
		{
			writeEncode(getString("admin:Members.NoResults"));
			return;
		}
		
		writeFormOpen();

		new DataTableControl<UUID>(this, "members", members)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("admin:Members.LoginName"));
				column(getString("admin:Members.Name"));
			}

			@Override
			protected void renderRow(UUID userID) throws Exception
			{
				cell();
				writeCheckbox("chk_" + userID.toString(), null, false);
				
				cell();
				User user = UserStore.getInstance().load(userID);
				if (user!=null)
				{
					writeLink(user.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString())));
				}
				
				cell();
				if (user!=null && !Util.isEmpty(user.getName()))
				{
					writeEncode(user.getName());
				}
			}
		}.render();

		write("<br>");
		writeButtonRed("expel", getString("admin:Members.Expel"));
		
		writeHiddenInput(PARAM_ID, null); // Repost
		writeFormClose();
	}
}
