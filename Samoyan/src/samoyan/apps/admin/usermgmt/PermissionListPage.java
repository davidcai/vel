package samoyan.apps.admin.usermgmt;

import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.DataTableControl;
import samoyan.core.ParameterMap;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.servlet.exc.WebFormException;

public class PermissionListPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/permissions";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter("chk_" + Permission.SYSTEM_ADMINISTRATION))
		{
			throw new WebFormException("chk_" + Permission.SYSTEM_ADMINISTRATION, getString("admin:PermList.CantRemove"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("delete"))
		{
			for (String param : getContext().getParameterNamesThatStartWith("chk_"))
			{
				PermissionStore.getInstance().removeByName(param.substring(4));
			}
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:PermList.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
//		// Search
//		writeFormOpen();
//		writeTypeAheadInput("q", null, null, 20, Permission.MAXSIZE_NAME, getPageURL(PermissionTypeAhead.COMMAND));
//		write(" ");
//		writeButton(getString("admin:PermList.Search"));
//		writeFormClose();
//		write("<br>");

		// Fetch permissions
		List<String> all = PermissionStore.getAll();
		
		if (all.size()==0)
		{
			writeEncode(getString("admin:PermList.NoResults"));
			return;
		}
		
		writeFormOpen();

		new DataTableControl<String>(this, "perms", all)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("admin:PermList.PermissionName"));
				column(getString("admin:PermList.Principles")).align("right").alignHeader("right");
			}

			@Override
			protected void renderRow(String perm) throws Exception
			{
				cell();
				writeCheckbox("chk_" + perm, null, false);
				
				cell();
				writeEncode(perm);
				
				cell();
				List<UUID> principles = PermissionStore.getInstance().getPrinciples(perm);
				if (principles.size()>0)
				{
					writeLink(String.valueOf(principles.size()), getPageURL(PrincipleListPage.COMMAND, new ParameterMap(PrincipleListPage.PARAM_PERMISSION, perm)));
				}
				else
				{
					write(String.valueOf(principles.size()));
				}
			}
		}.render();
		
		write("<br>");
		writeRemoveButton("delete");
		
		writeFormClose();
	}
}
