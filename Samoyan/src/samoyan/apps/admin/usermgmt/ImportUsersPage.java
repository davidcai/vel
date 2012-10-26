package samoyan.apps.admin.usermgmt;

import java.util.*;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.DataTableControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.*;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.NewUserLogEntry;

public class ImportUsersPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/import-users";

	private List<User> usersCreated = new ArrayList<User>();
	private int countUsersFailed = 0;
	private List<UserGroup> groupsCreated = new ArrayList<UserGroup>();
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:ImportUsers.Title");
	}

	@Override
	public void validate() throws Exception
	{
		// Must have at least the first row with the column headers
		String text = validateParameterString("text", 1, -1);
		if (text.indexOf("\r\n")<0)
		{
			throw new WebFormException("text", getString("common:Errors.InvalidValue"));
		}
		
		validateParameterString("subject", 1, 256);		
		
		String message = getParameterRichEdit("message");
		if (Util.isEmptyHTML(message))
		{
			throw new WebFormException("message", getString("common:Errors.MissingField"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		Date now = new Date();
		
		String subject = getParameterString("subject");
		String message = getParameterRichEdit("message");

		String text = getParameterString("text");
		StringTokenizer lines = new StringTokenizer(text, "\r\n");
		
		// First line is the column headers
		String line = lines.nextToken();
		List<String> headers = new ArrayList<String>();
		StringTokenizer columns = new StringTokenizer(line, "\t");
		while (columns.hasMoreTokens())
		{
			headers.add(columns.nextToken().trim());
		}
		
		boolean createGroups = isParameter("creategroups");
		
		// Run over all lines
		while (lines.hasMoreTokens())
		{
			line = lines.nextToken();
			line = Util.strReplace(line, "\t", "\t "); // Hack to get StringTokenizer to identify \t\t as empty column
			
			User user = new User();
			List<String> groups = new ArrayList<String>();
			
			// Parse the line
			String pwSet = null;
			int c = 0;
			columns = new StringTokenizer(line, "\t");
			while (columns.hasMoreTokens())
			{
				String column = columns.nextToken().trim();
				if (Util.isEmpty(column))
				{
					// Do nothing
				}
				else if (headers.get(c).equalsIgnoreCase("id"))
				{
					user.setLoginName(UserStore.getInstance().generateUniqueLoginName(column));
				}
				else if (headers.get(c).equalsIgnoreCase("name"))
				{
					user.setName(column);
				}
				else if (headers.get(c).equalsIgnoreCase("email") && Util.isValidEmailAddress(column))
				{
					user.setEmail(column);
				}
				else if (headers.get(c).equalsIgnoreCase("phone"))
				{
					user.setPhone(column);
					user.setPhoneVerified(true);
				}
				else if (headers.get(c).equalsIgnoreCase("mobile"))
				{
					user.setMobile(column);
					user.setMobileVerified(true);
				}
				else if (headers.get(c).equalsIgnoreCase("password") && !Util.isEmpty(column))
				{
					user.setPassword(column);
					pwSet = column;
				}
				else if (headers.get(c).equalsIgnoreCase("groups"))
				{
					StringTokenizer grps = new StringTokenizer(column, ";");
					while (grps.hasMoreTokens())
					{
						groups.add(grps.nextToken().trim());
					}
				}
				
				c++;
			}
			
			// Create the user
			if (Util.isEmpty(user.getEmail()))
			{
				// Email is mandatory
				countUsersFailed ++;
				continue;
			}
			if (Util.isEmpty(user.getLoginName()))
			{
				if (!Util.isEmpty(user.getName()))
				{
					user.setLoginName(UserStore.getInstance().generateUniqueLoginName(user.getName()));
				}
				else
				{
					int p = user.getEmail().indexOf("@");
					user.setLoginName(UserStore.getInstance().generateUniqueLoginName(user.getEmail().substring(0, p)));
				}
			}
//			if (UserStore.getInstance().loadByLoginName(user.getLoginName())!=null)
//			{
//				// User with same loginname already exists
//				countUsersFailed ++;
//				continue;
//			}
			if (pwSet==null)
			{
				pwSet = Util.randomPassword(User.MINSIZE_PASSWORD);
				user.setPassword(pwSet);
			}
			
			UserStore.getInstance().save(user);
			usersCreated.add(user);
			
			// Login groups
			for (String g : groups)
			{
				UserGroup lg = UserGroupStore.getInstance().loadByName(g);
				if (lg==null && createGroups)
				{
					// Create group on demand
					lg = new UserGroup();
					lg.setName(g);
					UserGroupStore.getInstance().save(lg);
					groupsCreated.add(lg);
				}
				
				if (lg!=null)
				{
					UserUserGroupLinkStore.getInstance().join(user.getID(), lg.getID());
				}
			}

			// Send invite by email
			Notifier.send(	Channel.EMAIL,
							null,
							user.getID(),
							null,
							InviteUsersNotif.COMMAND,
							new ParameterMap(InviteUsersNotif.PARAM_SUBJECT, subject)
									   .plus(InviteUsersNotif.PARAM_BODY, message)
									   .plus(InviteUsersNotif.PARAM_TEMP_PASSWORD, pwSet));

			// Log the event
			LogEntryStore.log(new NewUserLogEntry(user.getID()));
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		if (this.isCommitted())
		{
			writeEncode(getString("admin:ImportUsers.LoginsCreated", usersCreated.size()));
			if (usersCreated.size()>0)
			{
				new DataTableControl<User>(this, "users", usersCreated)
				{
					@Override
					protected void defineColumns() throws Exception
					{
						column(getString("admin:ImportUsers.LoginName"));
						column(getString("admin:ImportUsers.Name"));
						column(getString("admin:ImportUsers.Email"));
					}

					@Override
					protected void renderRow(User user) throws Exception
					{
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
			}
			
			if (countUsersFailed>0)
			{
				write("<br><br>");
				writeEncode(getString("admin:ImportUsers.LoginsFailed", countUsersFailed));
			}
			if (groupsCreated.size()>0)
			{
				write("<br><br>");
				writeEncode(getString("admin:ImportUsers.GroupsCreated", groupsCreated.size()));
				write("<br><br><table>");

				new DataTableControl<UserGroup>(this, "groups", groupsCreated)
				{
					@Override
					protected void defineColumns() throws Exception
					{
						column(getString("admin:ImportUsers.Group"));
					}

					@Override
					protected void renderRow(UserGroup lg) throws Exception
					{
						cell();
						writeLink(lg.getName(), getPageURL(UserGroupPage.COMMAND, new ParameterMap(UserGroupPage.PARAM_ID, lg.getID().toString())));
					}
				}.render();
			}
		}
		else
		{
			writeFormOpen();

			TwoColFormControl twoCol = new TwoColFormControl(this);

			twoCol.writeTextRow(getString("admin:ImportUsers.CSVHelp"));
			twoCol.writeSpaceRow();

			twoCol.writeRow(getString("admin:ImportUsers.CSV"));
			twoCol.writeTextAreaInput("text", null, 80, 5, -1);

			twoCol.writeRow(getString("admin:ImportUsers.Options"));
			twoCol.writeCheckbox("creategroups", getString("admin:ImportUsers.CreateGroups"), true);

			twoCol.writeSpaceRow();
			twoCol.writeTextRow(getString("admin:ImportUsers.MessageHelp"));
			twoCol.writeSpaceRow();

			twoCol.writeRow(getString("admin:ImportUsers.Subject"));
			twoCol.writeTextInput("subject", null, 80, 256);

			twoCol.writeRow(getString("admin:ImportUsers.Message"), getString("admin:ImportUsers.MessageAppendHelp"));
			twoCol.writeRichEditField("message", null, 80, 10);
			
			twoCol.render();
			
			write("<br>");
			writeButton("csv", getString("admin:ImportUsers.Import"));
			
			writeFormClose();
		}
	}
}
