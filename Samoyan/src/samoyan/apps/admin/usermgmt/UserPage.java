package samoyan.apps.admin.usermgmt;

import java.util.*;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.log.QueryLogPage;
import samoyan.apps.admin.typeahead.PermissionTypeAhead;
import samoyan.apps.admin.typeahead.UserGroupTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.PhoneInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.*;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.UserTerminatedLogEntry;

public class UserPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/user";
	public final static String PARAM_ID = "id";

	private User user;
	
	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		
		// User name
		String loginName = validateParameterString("loginname", User.MINSIZE_LOGINNAME, User.MAXSIZE_LOGINNAME);
		if (loginName.equals(this.user.getLoginName())==false)
		{
			loginName = loginName.toLowerCase(Locale.US).trim();
			User userByName = UserStore.getInstance().loadByLoginName(loginName);
			if (userByName!=null && userByName.getID().equals(this.user.getID())==false)
			{
				throw new WebFormException("loginname", getString("admin:User.LoginNameTaken"));
			}
			if (loginName.matches("[a-zA-Z0-9_\\x2d]*")==false)
			{
				throw new WebFormException("loginname", getString("common:Errors.InvalidValue"));
			}
			if (Util.isUUID(loginName))
			{
				throw new WebFormException("loginname", getString("common:Errors.InvalidValue"));
			}
		}
		
		// Password
		String password = ctx.getParameter("password"); // ctx.getParameter will not trim spaces
		if (!Util.isEmpty(password))
		{
			validateParameterString("password", User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		}

		// Name
		validateParameterString("name", User.MINSIZE_NAME, User.MAXSIZE_NAME);
		
		// Email
		String email = validateParameterString("email", 1, User.MAXSIZE_EMAIL);
		if (Util.isValidEmailAddress(email)==false)
		{
			throw new WebFormException("email", getString("common:Errors.InvalidValue"));
		}

		// Mobile
		if (!Util.isEmpty(getParameterString("mobile")))
		{
			validateParameterPhone("mobile");
		}
		
		// Phone
		if (!Util.isEmpty(getParameterString("phone")))
		{
			validateParameterPhone("phone");
		}
	}
	
	@Override
	public void commit() throws Exception
	{	
		RequestContext ctx = getContext();

		if (this.user.getLoginName().equalsIgnoreCase(getParameterString("loginname"))==false)
		{
			this.user.setLoginName(UserStore.getInstance().generateUniqueLoginName(getParameterString("loginname")));
		}
		if (!Util.isEmpty(ctx.getParameter("password")))
		{
			this.user.setPassword(ctx.getParameter("password"));
		}
		this.user.setName(getParameterString("name"));
		this.user.setEmail(getParameterString("email"));
		this.user.setMobile(getParameterPhone("mobile"));
		this.user.setMobileVerified(isParameter("mobileverified"));
		this.user.setPhone(getParameterPhone("phone"));
		this.user.setPhoneVerified(isParameter("phoneverified"));
		this.user.setSuspended(isParameter("suspended"));
		this.user.setAvatar(getParameterImage("avatar"));

		UserStore.getInstance().save(this.user);
		
		// Commit groups
		List<UUID> groupIDs = new ArrayList<UUID>();
		Integer groupCount = getParameterInteger("groups");
		for (int i=0; i<groupCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("group_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				UserGroup lg = UserGroupStore.getInstance().loadByName(kvp.getKey());
				if (lg!=null)
				{
					groupIDs.add(lg.getID());
				}
			}
		}

		List<UUID> currentGroups = UserUserGroupLinkStore.getInstance().getGroupsForUser(this.user.getID());
		for (UUID g : currentGroups)
		{
			if (groupIDs.contains(g)==false)
			{
				UserUserGroupLinkStore.getInstance().expel(this.user.getID(), g);
			}
		}
		for (UUID g : groupIDs)
		{
			if (currentGroups.contains(g)==false)
			{
				UserUserGroupLinkStore.getInstance().join(this.user.getID(), g);
			}
		}
		
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
		
		Set<String> currentPerms = PermissionStore.getInstance().getPermissions(this.user.getID());
		for (String p : currentPerms)
		{
			if (permNames.contains(p)==false)
			{
				PermissionStore.getInstance().deauthorize(this.user.getID(), p);
			}
		}
		for (String p : permNames)
		{
			if (currentPerms.contains(p)==false)
			{
				PermissionStore.getInstance().authorize(this.user.getID(), p);
			}
		}
		
		// Terminate?
		boolean terminated = isParameter("terminated");
		if (this.user.isTerminated()==false && terminated)
		{
			UserStore.getInstance().remove(this.user.getID());

			UserTerminatedLogEntry log = new UserTerminatedLogEntry(this.user.getID());
			log.setUserID(this.user.getID());
			LogEntryStore.log(log);
			
			throw new RedirectException(UserListPage.COMMAND, null);
		}
		else
		{
			// Redirect to self in order to clear form submission
			throw new RedirectException(ctx.getCommand(), new ParameterMap(PARAM_ID, this.user.getID().toString()).plus(RequestContext.PARAM_SAVED, ""));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return this.user.getLoginName();
	}

	@Override
	public void init() throws Exception
	{
		RequestContext ctx = getContext();
		
		UUID userID = getParameterUUID(PARAM_ID);
		if (userID==null)
		{
			throw new PageNotFoundException();
		}
		this.user = UserStore.getInstance().open(userID);
		if (this.user==null)
		{
			throw new PageNotFoundException();
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		Server fed = ServerStore.getInstance().loadFederation();
		
		if (!this.user.isTerminated() &&
			!this.user.getID().equals(ctx.getUserID()) &&
			!PermissionStore.getInstance().isUserGrantedPermission(this.user.getID(), Permission.SYSTEM_ADMINISTRATION))
		{
			new LinkToolbarControl(this)
				.addLink(	getString("admin:User.Impersonate"),
							getPageURL(ImpersonatePage.COMMAND, new ParameterMap(ImpersonatePage.PARAM_ID, this.user.getID().toString())),
							"icons/basic1/key_16.png")
				.render();
		}
		
		writeFormOpen();
		writeHiddenInput(PARAM_ID, this.user.getID().toString());
		
		TwoColFormControl twoCol = new TwoColFormControl(this);

//		// ID
//		twoCol.writeRow(getString("admin:User.ID"));
//		twoCol.writeEncode(this.user.getID().toString());
		
		// User name
		twoCol.writeRow(getString("admin:User.LoginName"));
		twoCol.writeTextInput("loginname", this.user.getLoginName(), 20, User.MAXSIZE_LOGINNAME);
		
		// Password
		twoCol.writeRow(getString("admin:User.Password"));
		twoCol.writePasswordInput("password", null, 20, User.MAXSIZE_PASSWORD);

		// Joined
		twoCol.writeRow(getString("admin:User.Joined"));
		twoCol.writeEncodeDateTime(this.user.getDateJoined());
		
		// Last active
		twoCol.writeRow(getString("admin:User.LastActive"));
		if (this.user.getLastActive()!=null)
		{
			twoCol.writeEncodeDateTime(this.user.getLastActive());
			if (UserStore.getInstance().getActive().contains(this.user.getID()))
			{
				twoCol.write(" ");
				twoCol.writeEncode(getString("admin:User.OnlineNow"));
			}
			
			twoCol.write(" <small>");
			twoCol.writeLink(getString("admin:User.Log"), getPageURL(QueryLogPage.COMMAND, new ParameterMap(QueryLogPage.PARAM_LOGINNAME, this.user.getLoginName())));
			twoCol.write("</small>");
		}
		else
		{
			twoCol.write("-");
		}

		twoCol.writeSpaceRow();

		// Name
		twoCol.writeRow(getString("admin:User.Name"));
		twoCol.writeTextInput("name", this.user.getName(), 40, User.MAXSIZE_NAME);

		// Email
		twoCol.writeRow(getString("admin:User.Email"));
		twoCol.writeTextInput("email", this.user.getEmail(), 40, User.MAXSIZE_EMAIL);
		
		// Mobile
		twoCol.writeRow(getString("admin:User.Mobile"));
		new PhoneInputControl(twoCol, "mobile")
			.limitCountries(fed.getSMSCountries())
			.setInitialValue(this.user.getMobile())
			.render();
		twoCol.write(" ");
		twoCol.writeCheckbox("mobileverified", getString("admin:User.Verified"), this.user.isMobileVerified());
		
		// Phone
		twoCol.writeRow(getString("admin:User.Phone"));
		new PhoneInputControl(twoCol, "phone")
			.limitCountries(fed.getSMSCountries())
			.setInitialValue(this.user.getPhone())
			.render();
		twoCol.write(" ");
		twoCol.writeCheckbox("phoneverified", getString("admin:User.Verified"), this.user.isPhoneVerified());

		twoCol.writeSpaceRow();

		// Avatar
		twoCol.writeRow(getString("admin:User.Avatar"));
		twoCol.writeImageInput("avatar", this.user.getAvatar());
	
		twoCol.writeSpaceRow();

		// Groups
		twoCol.writeRow(getString("admin:User.Groups"));
		String groups = "";
		List<UUID> groupIDs = new ArrayList<UUID>();
		groupIDs.addAll(UserUserGroupLinkStore.getInstance().getGroupsForUser(this.user.getID()));
		new ControlArray<UUID>(twoCol, "groups", groupIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID groupID) throws Exception
			{
				UserGroup g = UserGroupStore.getInstance().load(groupID);
				writeTypeAheadInput("group_" + rowNum, g!=null?g.getName():null, g!=null?g.getName():null, 40, UserGroup.MAXSIZE_NAME, getPageURL(UserGroupTypeAhead.COMMAND));
			}
		}.render();

		twoCol.writeSpaceRow();

		// Permissions
		twoCol.writeRow(getString("admin:User.Permissions"));
		String permissions = "";
		List<String> permNames = new ArrayList<String>();
		permNames.addAll(PermissionStore.getInstance().getPermissions(this.user.getID()));
		new ControlArray<String>(twoCol, "permissions", permNames)
		{
			@Override
			public void renderRow(int rowNum, String p)
			{
				writeTypeAheadInput("permission_" + rowNum, p, p, 40, Permission.MAXSIZE_NAME, getPageURL(PermissionTypeAhead.COMMAND));
			}
		}.render();

		twoCol.writeSpaceRow();

		// Flags
		twoCol.writeRow(getString("admin:User.Flags"));
		twoCol.writeCheckbox("suspended", getString("admin:User.Suspended"), this.user.isSuspended());
		twoCol.write(" ");
		twoCol.writeCheckbox("terminated", getString("admin:User.Terminated"), this.user.isTerminated());

		twoCol.render();
		write("<br>");
		
		writeSaveButton("save", this.user);
		
		writeFormClose();
	}
}
