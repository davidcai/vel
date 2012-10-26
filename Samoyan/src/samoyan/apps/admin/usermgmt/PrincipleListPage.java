package samoyan.apps.admin.usermgmt;

import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.DataTableControl;
import samoyan.core.ParameterMap;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.database.UserStore;

public class PrincipleListPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/principles";
	public final static String PARAM_PERMISSION = "perm";
		
	@Override
	public void commit() throws Exception
	{
		if (isParameter("deauth"))
		{
			for (String param : getContext().getParameterNamesThatStartWith("chk_"))
			{
				PermissionStore.getInstance().deauthorize(UUID.fromString(param.substring(4)), getParameterString(PARAM_PERMISSION));
			}
		}
	}

	@Override
	public void renderHTML() throws Exception
	{		
		writeFormOpen();

		List<UUID> principleIDs = PermissionStore.getInstance().getPrinciples(getParameterString(PARAM_PERMISSION));
		if (principleIDs.size()==0)
		{
			writeEncode(getString("admin:Principles.NoResults"));
			return;
		}

		new DataTableControl<UUID>(this, "perms", principleIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("admin:Principles.Principle"));
			}

			@Override
			protected void renderRow(UUID principleID) throws Exception
			{
				cell();
				writeCheckbox("chk_" + principleID.toString(), null, false);
				
				cell();
				User user = UserStore.getInstance().load(principleID);
				if (user!=null)
				{
					writeLink(user.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString())));
				}
				else
				{
					UserGroup grp = UserGroupStore.getInstance().load(principleID);
					if (grp!=null)
					{
						writeLink(grp.getName(), getPageURL(UserGroupPage.COMMAND, new ParameterMap(UserGroupPage.PARAM_ID, grp.getID().toString())));
						write(" (");
						writeEncodeLong(UserUserGroupLinkStore.getInstance().getUsersForGroup(grp.getID()).size());
						write(")");
					}
				}
			}
		}.render();
		
		write("<br>");
		writeButtonRed("deauth", getString("admin:Principles.Deauthorize"));
		writeHiddenInput(PARAM_PERMISSION, null); // Repost
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getParameterString(PARAM_PERMISSION);
	}
}
