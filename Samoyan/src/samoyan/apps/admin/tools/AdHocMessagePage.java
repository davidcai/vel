package samoyan.apps.admin.tools;

import java.util.*;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.UserGroupTypeAhead;
import samoyan.apps.admin.typeahead.UserTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.WebFormException;

public class AdHocMessagePage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/adhoc-notif";

	private Map<String, Integer> messageCount;
	
	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		
		int countAddressees = 0;
				
		// Users
		Integer userCount = getParameterInteger("users");
		for (int i=0; i<userCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("user_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				User u = UserStore.getInstance().loadByLoginName(kvp.getKey());
				if (u==null)
				{
					throw new WebFormException("user_"+i, getString("admin:AdHocMessage.InvalidLoginName", kvp.getValue()));
				}
				countAddressees++;
			}
		}
						
		// Groups
		Integer groupCount = getParameterInteger("groups");
		for (int i=0; i<groupCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("group_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				UserGroup lg = UserGroupStore.getInstance().loadByName(kvp.getKey());
				if (lg==null)
				{
					throw new WebFormException("group_"+i, getString("admin:AdHocMessage.InvalidGroupName", kvp.getValue()));
				}
				countAddressees++;
			}
		}
		
		// Check number of recipients
		if (countAddressees==0)
		{
			throw new WebFormException(new String[] {"groups", "users"}, getString("admin:AdHocMessage.NoRecipients"));
		}
		
		// Channels
		int countChannels = 0;
		for (String channel : Channel.getAll())
		{
			if (isParameter(channel))
			{
				countChannels ++;
			}
		}
		if (countChannels==0)
		{
			throw new WebFormException(Channel.getAll(), getString("common:Errors.MissingField"));
		}

		// Subject and body
		boolean mandateSubject = isParameter(Channel.EMAIL);
		validateParameterString("subject", mandateSubject?1:0, 128);
		
		String html = getParameterRichEdit("body");
		if (Util.isEmptyHTML(html))
		{
			throw new WebFormException("body", getString("common:Errors.MissingField"));
		}
		
		// Date
		validateParameterDate("date");
	}
	
	@Override
	public void commit() throws Exception
	{
		Set<UUID> userIDs = new HashSet<UUID>();

		// Users
		Integer userCount = getParameterInteger("users");
		for (int i=0; i<userCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("user_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				User u = UserStore.getInstance().loadByLoginName(kvp.getKey());
				if (u!=null)
				{
					userIDs.add(u.getID());
				}
			}
		}
		
		// Groups
		Integer groupCount = getParameterInteger("groups");
		for (int i=0; i<groupCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("group_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				UserGroup lg = UserGroupStore.getInstance().loadByName(kvp.getKey());
				if (lg!=null)
				{
					userIDs.addAll(UserUserGroupLinkStore.getInstance().getUsersForGroup(lg.getID()));
				}
			}
		}
		
		// Content
		String subject = getParameterString("subject");
		String body = getParameterString("body");
		Map<String, String> notifParams = new ParameterMap(AdHocNotif.PARAM_SUBJECT, subject).plus(AdHocNotif.PARAM_BODY, body);
		
		// Send
		Server fed = ServerStore.getInstance().loadFederation();
		Date date = getParameterDate("date");
		
		this.messageCount = new HashMap<String, Integer>();
		for (String channel : Channel.getPush())
		{
			if (isParameter(channel)==true && fed.isChannelEnabled(channel)==true)
			{
				for (UUID userID : userIDs)
				{
					Notifier.send(channel, date, userID, null, AdHocNotif.COMMAND, notifParams);
					
					// !$! Consider delayed schedule
					
					Integer count = this.messageCount.get(channel);
					if (count==null)
					{
						this.messageCount.put(channel, 1);
					}
					else
					{
						this.messageCount.put(channel, (1+count));
					}
				}
			}
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:AdHocMessage.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (this.messageCount!=null && this.messageCount.size()>0)
		{
			for (String channel : this.messageCount.keySet())
			{
				writeEncode(getString("admin:AdHocMessage.MessagesSent", this.messageCount.get(channel), Channel.getDescription(channel, getLocale())));
				write("<br>");
			}
			return;
		}
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Users
		twoCol.writeRow(getString("admin:AdHocMessage.Users"));
		new ControlArray<Object>(twoCol, "users", null)
		{
			@Override
			public void renderRow(int rowNum, Object nothing)
			{
				writeTypeAheadInput("user_" + rowNum, null, null, 40, User.MAXSIZE_LOGINNAME, getPageURL(UserTypeAhead.COMMAND));
			}
		}.render();

		twoCol.writeSpaceRow();

		// Groups
		twoCol.writeRow(getString("admin:AdHocMessage.Groups"));
		new ControlArray<Object>(twoCol, "groups", null)
		{
			@Override
			public void renderRow(int rowNum, Object nothing)
			{
				writeTypeAheadInput("group_" + rowNum, null, null, 40, UserGroup.MAXSIZE_NAME, getPageURL(UserGroupTypeAhead.COMMAND));
			}
		}.render();

		twoCol.writeSpaceRow();

		// Channels
		Server fed = ServerStore.getInstance().loadFederation();
		
		twoCol.writeRow(getString("admin:AdHocMessage.Channels"));
		for (String channel : Channel.getPush())
		{
			if (fed.isChannelEnabled(channel)==true)
			{
				twoCol.writeCheckbox(channel, Channel.getDescription(channel, getLocale()), false);
				twoCol.write(" ");
			}
		}
		
		twoCol.writeSpaceRow();

		// Subject
		twoCol.writeRow(getString("admin:AdHocMessage.Subject"));
		twoCol.writeTextInput("subject", null, 80, 128);
		
		// Body
		twoCol.writeRow(getString("admin:AdHocMessage.Body"));
		twoCol.writeRichEditField("body", null, 80, 10);

		twoCol.writeSpaceRow();

		// Schedule
		twoCol.writeRow(getString("admin:AdHocMessage.Schedule"));
		twoCol.writeDateTimeInput("date", new Date());

		twoCol.render();
		
		write("<br>");
		writeButton("send", getString("admin:AdHocMessage.Send"));
		
		writeFormClose();
	}
}
