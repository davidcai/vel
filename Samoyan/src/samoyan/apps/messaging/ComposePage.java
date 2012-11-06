package samoyan.apps.messaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.InternalMessage;
import samoyan.database.InternalMessageRecipient;
import samoyan.database.InternalMessageRecipientStore;
import samoyan.database.InternalMessageStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class ComposePage extends MessagingPage
{
	public final static String COMMAND = MessagingPage.COMMAND + "/compose";
	public final static String PARAM_REPLY = "re";
	public final static String PARAM_REPLY_ALL = "reall";
	public final static String PARAM_FORWARD = "fwd";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("messaging:Compose.Title");
	}
	
	private String quote(InternalMessage msg)
	{
		if (Util.isEmptyHTML(msg.getBody()))
		{
			return null;
		}
		else
		{
			return "<br><br><style>BLOCKQUOTE{margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex;}</style><blockquote>" + msg.getBody() + "</blockquote>";
		}
	}
	
	private String modifySubject(String prefix, String subject)
	{
		if (subject==null)
		{
			return prefix;
		}
		else if (subject.startsWith(prefix)==false)
		{
			return prefix + subject;
		}
		else
		{
			return subject;
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		List<UUID> to = new ArrayList<UUID>();
		String content = null;
		UUID threadID = null;
		String subject = null;
		
		if (isParameter(PARAM_REPLY))
		{
			InternalMessage reply = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_REPLY));
			if (reply!=null && InternalMessageRecipientStore.getInstance().isRecipient(reply.getID(), ctx.getUserID()))
			{
				to.add(reply.getSenderUserID());
				content = quote(reply);
				threadID = reply.getThreadID();
				subject = modifySubject(getString("messaging:Compose.Re"), reply.getSubject());
			}
		}
		else if (isParameter(PARAM_REPLY_ALL))
		{
			InternalMessage reply = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_REPLY_ALL));
			if (reply!=null && InternalMessageRecipientStore.getInstance().isRecipient(reply.getID(), ctx.getUserID()))
			{
				to.addAll(InternalMessageRecipientStore.getInstance().queryRecipientsOfMessage(reply.getID()));
				content = quote(reply);
				threadID = reply.getThreadID();
				subject = modifySubject(getString("messaging:Compose.Re"), reply.getSubject());
			}
		}
		else if (isParameter(PARAM_FORWARD))
		{
			InternalMessage reply = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_FORWARD));
			if (reply!=null && InternalMessageRecipientStore.getInstance().isRecipient(reply.getID(), ctx.getUserID()))
			{
				content = quote(reply);
				threadID = reply.getThreadID();
				subject = modifySubject(getString("messaging:Compose.Fwd"), reply.getSubject());
			}
		}
		
		
		InternalMessage replyAll = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_REPLY_ALL));
		InternalMessage forward = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_FORWARD));
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// To
		twoCol.writeRow(getString("messaging:Compose.To"));
		new ControlArray<UUID>(twoCol, "to", to)
		{
			@Override
			public void renderRow(int rowNum, UUID userID) throws Exception
			{
				User user = UserStore.getInstance().load(userID);
				writeTypeAheadInput("to_" + rowNum, user!=null?user.getLoginName():null, user!=null?user.getDisplayName():null, 40, User.MAXSIZE_LOGINNAME, getPageURL(UserTypeAhead.COMMAND));
			}
		}.render();
		
		// Subject
		twoCol.writeRow(getString("messaging:Compose.Subject"));
		twoCol.writeTextInput("subject", subject, 80, InternalMessage.MAXSIZE_SUBJECT);
		
		// Body
		twoCol.writeRow(getString("messaging:Compose.Body"));
		twoCol.writeRichEditField("body", content, 80, 10);

		twoCol.render();
		
		write("<br>");
		writeButton("send", getString("messaging:Compose.Send"));
				
		writeFormClose();
	}
	
	@Override
	public void validate() throws Exception
	{
		// To
		int countAddressees = 0;
		Integer userCount = getParameterInteger("to");
		for (int i=0; i<userCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("to_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()))
			{
				User u = UserStore.getInstance().loadByLoginName(kvp.getKey());
				if (u==null)
				{
					throw new WebFormException("to_"+i, getString("messaging:Compose.UnknownRecipient", kvp.getValue()));
				}
				countAddressees++;
			}
		}
		
		// Check number of recipients
		if (countAddressees==0)
		{
			throw new WebFormException("to", getString("messaging:Compose.NoRecipients"));
		}
		
		// Subject and body
		String subject = validateParameterString("subject", 0, InternalMessage.MAXSIZE_SUBJECT);
		String html = getParameterRichEdit("body");
		
		if (Util.isEmptyHTML(html) && Util.isEmpty(subject))
		{
			throw new WebFormException(new String[] {"subject", "body"}, getString("common:Errors.MissingField"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		// Thread
		InternalMessage reply = null;
		if (isParameter(PARAM_REPLY))
		{
			reply = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_REPLY));
		}
		else if (isParameter(PARAM_REPLY_ALL))
		{
			reply = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_REPLY_ALL));
		}
		else if (isParameter(PARAM_FORWARD))
		{
			reply = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_FORWARD));
		}
		
		// Create the message
		InternalMessage msg = new InternalMessage();
		msg.setSubject(getParameterString("subject"));
		msg.setBody(getParameterRichEdit("body"));
		msg.setSenderUserID(getContext().getUserID());
		if (reply!=null && InternalMessageRecipientStore.getInstance().isRecipient(reply.getID(), ctx.getUserID()))
		{
			msg.setThreadID(reply.getThreadID());
		}
		InternalMessageStore.getInstance().save(msg);
		
		// Assign for each recipient
		Set<String> sentTo = new HashSet<String>();
		Integer userCount = getParameterInteger("to");
		for (int i=0; i<userCount; i++)
		{
			Pair<String, String> kvp = getParameterTypeAhead("to_"+i);
			if (kvp!=null && !Util.isEmpty(kvp.getKey()) && sentTo.contains(kvp.getKey())==false)
			{
				sentTo.add(kvp.getKey());
				
				InternalMessageRecipient rec = new InternalMessageRecipient();
				rec.setInternalMessageID(msg.getID());
				rec.setRecipientUserID(UserStore.getInstance().loadByLoginName(kvp.getKey()).getID());
				InternalMessageRecipientStore.getInstance().save(rec);
				
				// Send notif
				Notifier.send(rec.getRecipientUserID(), null, YouGotMailNotif.COMMAND, new ParameterMap(YouGotMailNotif.PARAM_ID, msg.getID()));
			}
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), null);
	}
}
