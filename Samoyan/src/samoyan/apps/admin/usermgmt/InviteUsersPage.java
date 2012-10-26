package samoyan.apps.admin.usermgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.PermissionTypeAhead;
import samoyan.apps.admin.typeahead.UserGroupTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.DataTableControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.LogEntryStore;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.NewUserLogEntry;

public class InviteUsersPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/invite-users";
	
	private List<UUID> createdUsers = null;
		
	@Override
	public void validate() throws Exception
	{
		// Recipients
		boolean hasRecipients = false;
		Integer recs = getParameterInteger("recs");
		for (int i=0; i<recs; i++)
		{
			String token = getParameterString("rec"+i);
			if (Util.isEmpty(token))
			{
				continue;
			}
			
			String name = null;
			String email = null;
			
			int p = token.indexOf("<");
			int q = token.lastIndexOf(">");
			if (p>=0 && q>p)
			{
				email = token.substring(p+1, q).trim();
				if (p>=0)
				{
					name = token.substring(0,  p).trim();
					if (name.startsWith("\""))
					{
						name = name.substring(1);
					}
					if (name.endsWith("\""))
					{
						name = name.substring(0, name.length()-1);
					}
				}
			}
			else
			{
				email = token;
			}
			
			if (!Util.isValidEmailAddress(email))
			{
				// Invalid email
				throw new WebFormException("rec"+i, getString("admin:InviteUsers.InvalidEmail"));
			}
			
			if (isParameter("skipexisting") && UserStore.getInstance().getByEmail(email).size()>0)
			{
				// User with same email already exists
				throw new WebFormException("rec"+i, getString("admin:InviteUsers.EmailExists"));
			}

			hasRecipients = true;
		}
		if (!hasRecipients)
		{
			String[] fields = new String[recs];
			for (int i=0; i<fields.length; i++)
			{
				fields[i] = "rec" + i;
			}
			
			throw new WebFormException(fields, getString("common:Errors.MissingField"));
		}
				
		validateParameterString("subject", 1, 256);		
		
		String message = getParameterRichEdit("message");
		if (Util.isEmptyHTML(message))
		{
			throw new WebFormException("message", getString("common:Errors.MissingField"));
		}

		// Groups
		List<UUID> groupIDs = new ArrayList<UUID>();
		Integer groupCount = getParameterInteger("groups");
		for (int i=0; i<groupCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("group_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				UserGroup lg = UserGroupStore.getInstance().loadByName(kvp.getKey());
				if (lg==null)
				{
					throw new WebFormException("group_"+i, getString("common:Errors.InvalidValue"));
				}
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		String recipients = getParameterString("recipients");
		String subject = getParameterString("subject");
		String message = getParameterRichEdit("message");
				
		// Groups
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

		// Permissions 
		List<String> perms = new ArrayList<String>();
		Integer permCount = getParameterInteger("permissions");
		for (int i=0; i<permCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("permission_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getValue()))
			{
				perms.add(kvp.getValue());
			}
		}

		// Iterate over recipient list
		this.createdUsers = new ArrayList<UUID>();
		Integer recs = getParameterInteger("recs");
		for (int i=0; i<recs; i++)
		{
			String token = getParameterString("rec"+i);
			if (Util.isEmpty(token))
			{
				continue;
			}
						
			String name = null;
			String email = null;
			
			int p = token.indexOf("<");
			int q = token.lastIndexOf(">");
			if (p>=0 && q>p)
			{
				email = token.substring(p+1, q).trim();
				if (p>=0)
				{
					name = token.substring(0,  p).trim();
					if (name.startsWith("\""))
					{
						name = name.substring(1);
					}
					if (name.endsWith("\""))
					{
						name = name.substring(0, name.length()-1);
					}
				}
			}
			else
			{
				email = token;
			}
									
			// Create the user
			User user = new User();
			user.setEmail(email);
			String tempPassword = Util.randomPassword(User.MINSIZE_PASSWORD);
			user.setPassword(tempPassword);
			if (!Util.isEmpty(name))
			{
				user.setName(name);
				user.setLoginName(UserStore.getInstance().generateUniqueLoginName(name));
			}
			else
			{
				user.setName("");
				user.setLoginName(UserStore.getInstance().generateUniqueLoginName(email.substring(0, email.indexOf("@"))));
			}
			UserStore.getInstance().save(user);
			
			// Groups
			for (UUID groupID : groupIDs)
			{
				UserUserGroupLinkStore.getInstance().join(user.getID(), groupID);
			}
			
			// Permissions
			for (String perm : perms)
			{
				PermissionStore.getInstance().authorize(user.getID(), perm);
			}
			
			// Send invite by email
			Notifier.send(	Channel.EMAIL,
							null,
							user.getID(),
							null,
							InviteUsersNotif.COMMAND,
							new ParameterMap(InviteUsersNotif.PARAM_SUBJECT, subject)
									   .plus(InviteUsersNotif.PARAM_BODY, message)
									   .plus(InviteUsersNotif.PARAM_TEMP_PASSWORD, tempPassword));

			// Log the event
			LogEntryStore.log(new NewUserLogEntry(user.getID()));

			// Success
			this.createdUsers.add(user.getID());
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (this.createdUsers!=null)
		{
			new DataTableControl<UUID>(this, "results", this.createdUsers)
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column(getString("admin:InviteUsers.LoginName"));
					column(getString("admin:InviteUsers.Name"));
					column(getString("admin:InviteUsers.Email"));
				}

				@Override
				protected void renderRow(UUID userID) throws Exception
				{
					User user = UserStore.getInstance().load(userID);
										
					cell();
					writeLink(user.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString())));

					cell();
					writeEncode(user.getDisplayName());
					
					cell();
					writeEncode(user.getEmail());
				}
			}
			.setPageSize(Integer.MAX_VALUE) // No scrolling
			.render();

			return;
		}
				
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);

		twoCol.writeTextRow(getString("admin:InviteUsers.RecipientsHelp"));
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("admin:InviteUsers.Recipients"), getString("admin:InviteUsers.RecipientsExampleHelp"));
		new ControlArray<String>(twoCol, "recs", new ArrayList<String>())
		{
			@Override
			public void renderRow(int rowNum, String rowElement) throws Exception
			{
				writeTextInput("rec"+rowNum, null, 60, User.MAXSIZE_EMAIL);
			}
		}.render();
		
//		twoCol.writeRow(getString("admin:InviteUsers.Recipients"), getString("admin:InviteUsers.RecipientsHelp"));
//		twoCol.writeTextAreaInput("recipients", null, 60, 3, 0);
		
		twoCol.writeRow("");
		twoCol.writeCheckbox("skipexisting", getString("admin:InviteUsers.ExcludeExistingEmails"), true);
		
		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("admin:InviteUsers.MessageHelp"));
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("admin:InviteUsers.Subject"));
		twoCol.writeTextInput("subject", null, 80, 256);

		twoCol.writeRow(getString("admin:InviteUsers.Message"), getString("admin:InviteUsers.MessageAppendHelp"));
		twoCol.writeRichEditField("message", null, 80, 10);
//		twoCol.write(Util.textToHtml(getString("admin:InviteUsersNotif.LoginInstructions", "USERNAME", "PASSWORD")));
//		twoCol.write("<br><br>");
//		twoCol.writeEncode(getPageURL(LoginPage.COMMAND));
		
		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("admin:InviteUsers.GroupsPermsHelp"));
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("admin:InviteUsers.Groups"));
		new ControlArray<Object>(twoCol, "groups", null)
		{
			@Override
			public void renderRow(int rowNum, Object nothing)
			{
				writeTypeAheadInput("group_" + rowNum, null, null, 40, UserGroup.MAXSIZE_NAME, getPageURL(UserGroupTypeAhead.COMMAND));
			}
		}.render();
		
		twoCol.writeRow(getString("admin:InviteUsers.Permissions"));
		new ControlArray<Object>(twoCol, "permissions", null)
		{
			@Override
			public void renderRow(int rowNum, Object nothing)
			{
				writeTypeAheadInput("permission_" + rowNum, null, null, 40, Permission.MAXSIZE_NAME, getPageURL(PermissionTypeAhead.COMMAND));
			}
		}.render();
		
		twoCol.render();
		write("<br>");
		
		writeButton("save", getString("admin:InviteUsers.Send"));
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:InviteUsers.Title");
	}
}
