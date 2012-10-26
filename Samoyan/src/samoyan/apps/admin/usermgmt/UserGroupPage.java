package samoyan.apps.admin.usermgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.PermissionTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.Util;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class UserGroupPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/user-group";
	
	public final static String PARAM_ID = "id";

	private UserGroup group = null;

	@Override
	public void init() throws Exception
	{
		RequestContext ctx = getContext();
		
		UUID grpID = getParameterUUID(PARAM_ID);
		if (grpID!=null)
		{
			this.group = UserGroupStore.getInstance().open(grpID);
			if (this.group==null)
			{
				throw new PageNotFoundException();
			}
		}
		else
		{
			this.group = new UserGroup();
		}
	}

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		
		String name = validateParameterString("name", 1, UserGroup.MAXSIZE_NAME);
		
		// Check if already exists
		UserGroup existing = UserGroupStore.getInstance().loadByName(name);
		if (existing!=null && (this.group.isSaved()==false || existing.getID().equals(this.group.getID())==false))
		{
			// Group already exists under same name
			throw new WebFormException("name", getString("admin:UserGroup.DuplicateName"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		// Save
		this.group.setName(getParameterString("name"));
		UserGroupStore.getInstance().save(this.group);
		
		// Commit permissions
		List<String> permNames = new ArrayList<String>();
		Integer permCount = getParameterInteger("permissions");
		for (int i=0; i<permCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("permission_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getValue()))
			{
				permNames.add(kvp.getValue());
			}
		}

		Set<String> currentPerms = PermissionStore.getInstance().getPermissions(this.group.getID());
		for (String p : currentPerms)
		{
			if (permNames.contains(p)==false)
			{
				PermissionStore.getInstance().deauthorize(this.group.getID(), p);
			}
		}
		for (String p : permNames)
		{
			if (currentPerms.contains(p)==false)
			{
				PermissionStore.getInstance().authorize(this.group.getID(), p);
			}
		}

		// Redirect to list page
		throw new RedirectException(UserGroupListPage.COMMAND, null);
	}

	@Override
	public String getTitle() throws Exception
	{
		if (this.group.isSaved())
		{
			return this.group.getName();
		}
		else
		{
			return getString("admin:UserGroup.Title");
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);

		twoCol.writeRow(getString("admin:UserGroup.GroupName"));
		twoCol.writeTextInput("name", this.group.getName(), 32, UserGroup.MAXSIZE_NAME);

		twoCol.writeSpaceRow();

		// Permissions
		twoCol.writeRow(getString("admin:UserGroup.Permissions"));
		new ControlArray<String>(twoCol, "permissions", new ArrayList<String>(PermissionStore.getInstance().getPermissions(this.group.getID())))
		{
			@Override
			public void renderRow(int rowNum, String p)
			{
				writeTypeAheadInput("permission_" + rowNum, p, p, 40, Permission.MAXSIZE_NAME, getPageURL(PermissionTypeAhead.COMMAND));
			}
		}.render(); 

		twoCol.render();
		
		write("<br>");
		writeSaveButton(this.group);
		
		// Postback
		writeHiddenInput(PARAM_ID, null);
		
		writeFormClose();
	}
}
