package samoyan.apps.messaging;

import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.InternalMessage;
import samoyan.database.InternalMessageRecipientStore;
import samoyan.database.InternalMessageStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.PageNotFoundException;

public class ReadMessagePage extends MessagingPage
{
	public final static String COMMAND = MessagingPage.COMMAND + "/msg";
	public final static String PARAM_ID = "id";
	
	private InternalMessage msg;
	
	@Override
	public void init() throws Exception
	{
		this.msg = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.msg==null)
		{
			throw new PageNotFoundException();
		}
		
		boolean isRecipient = InternalMessageRecipientStore.getInstance().isRecipient(this.msg.getID(), getContext().getUserID());
		boolean isSender = this.msg.getSenderUserID().equals(getContext().getUserID());
		if (!isRecipient && !isSender)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		if (!Util.isEmpty(this.msg.getSubject()))
		{
			return this.msg.getSubject();
		}
		else
		{
			return getString("messaging:Read.NoSubject");
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<style>BLOCKQUOTE{margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex;}</style>");
		
		renderMessage(this.msg);
	}
	
	private void renderMessage(InternalMessage msg) throws Exception
	{
		User sender = UserStore.getInstance().load(msg.getSenderUserID());
		
		write("<table width=\"100%\"><tr valign=top><td rowspan=2 width=\"1%\">");
		if (sender.getAvatar()!=null)
		{
			writeImage(sender.getAvatar(), Image.SIZE_THUMBNAIL, sender.getDisplayName(), null);
		}
		else
		{
			writeImageSized("user-silhouette.png", null, null, 72, 72); // !$! Hardcoded sizes
		}
		write("</td><td>");
		
		write("<b>");
		writeEncode(sender.getDisplayName());
		write("</b><br><span class=Faded>");
		writeEncode(getString("messaging:Read.To"));
		write(" ");
		boolean first = true;
		List<UUID> recipientIDs = InternalMessageRecipientStore.getInstance().queryRecipientsOfMessage(this.msg.getID());
		for (UUID recID : recipientIDs)
		{
			User rec = UserStore.getInstance().load(recID);
			if (rec==null) continue;
			if (!first)
			{
				write(" ,");
			}
			if (recID.equals(getContext().getUserID()))
			{
				writeEncode(getString("messaging:Read.Me"));
			}
			else
			{
				writeEncode(rec.getDisplayName());
			}
			first = false;
		}
		write("</span>");
		
		write("</td><td align=right>");
		writeEncodeDateOrTime(msg.getCreatedDate());
		write("</td><td nowrap width=\"1%\">");
		writeImageSized("msg-reply.png", getString("messaging:Read.Reply"), getPageURL(ComposePage.COMMAND, new ParameterMap(ComposePage.PARAM_REPLY, msg.getID().toString())), 0, 0);
		writeImageSized("msg-replyall.png", getString("messaging:Read.ReplyAll"), getPageURL(ComposePage.COMMAND, new ParameterMap(ComposePage.PARAM_REPLY_ALL, msg.getID().toString())), 0, 0);
		writeImageSized("msg-forward.png", getString("messaging:Read.Forward"), getPageURL(ComposePage.COMMAND, new ParameterMap(ComposePage.PARAM_FORWARD, msg.getID().toString())), 0, 0);
		write("</td></tr>");
		
		write("<tr><td colspan=3>");
		write(msg.getBody());
		write("</td></tr></table>");
	}
}
