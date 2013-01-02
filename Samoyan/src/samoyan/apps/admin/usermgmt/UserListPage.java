package samoyan.apps.admin.usermgmt;

import java.util.*;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.UserTypeAhead;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.database.QueryIterator;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.RedirectException;

public class UserListPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/user-list";

	@Override
	public void commit() throws Exception
	{
		Pair<String, String> q = getParameterTypeAhead("q");
		User user = UserStore.getInstance().loadByLoginName(q.getKey());
		if (user!=null)
		{
			throw new RedirectException(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString()));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:UserList.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		// Invite/import
		new LinkToolbarControl(this)
			.addLink(	getString("admin:UserList.InviteUsers"), getPageURL(InviteUsersPage.COMMAND), "icons/standard/open-envelope-16.png")
			.addLink(	getString("admin:UserList.ImportUsers"), getPageURL(ImportUsersPage.COMMAND), "icons/standard/cardboard-box-16.png")
			.render();

		// Search
		writeFormOpen();
		writeTypeAheadInput("q", null, null, 30, User.MAXSIZE_NAME, getPageURL(UserTypeAhead.COMMAND));
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		write("<br>");
		
		// Fetch users
		QueryIterator<User> iter = UserStore.getInstance().queryAllGhost();
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("admin:UserList.NoResults"));
				return;
			}

			new DataTableControl<User>(this, "users", iter)
			{
				private Set<UUID> online = new HashSet<UUID>(UserStore.getInstance().getActive());

				@Override
				protected void defineColumns()
				{
					column(getString("admin:UserList.LoginName"));
					column(getString("admin:UserList.Name"));
					column(getString("admin:UserList.LastLogin"));
				}
				
				@Override
				protected boolean isRenderRow(User user)
				{
					return !user.isTerminated();
				}

				@Override
				protected void renderRow(User user) throws Exception
				{
					cell();
					if (user.isTerminated())
					{
						write("<strike class=Faded>");
					}
					else if (user.isSuspended())
					{
						write("<span class=Faded>");
					}
					writeLink(user.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString())));
					if (user.isTerminated())
					{
						write("</strike>");
					}
					else if (user.isSuspended())
					{
						write("</span>");
					}
				
					cell();
					writeEncode(user.getName());
		
					cell();
					if (online.contains(user.getID()))
					{
						write("<b>");
					}
					if (user.getLastActive()!=null)
					{
						writeEncodeDateOrTime(user.getLastActive());
					}
					if (online.contains(user.getID()))
					{
						write("</b>");
					}
				}
			}.render();
		}
		finally
		{
			iter.close();
		}
	}
}
