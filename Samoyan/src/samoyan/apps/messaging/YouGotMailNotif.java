package samoyan.apps.messaging;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.core.XCoShortenUrl;
import samoyan.database.InternalMessage;
import samoyan.database.InternalMessageRecipientStore;
import samoyan.database.InternalMessageStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.PageNotFoundException;

/**
 * Notifies the user that they have an internal messsage waiting for them.
 * @author brian
 *
 */
public class YouGotMailNotif extends MessagingPage
{
	public final static String COMMAND = MessagingPage.COMMAND + "/yougotmail.notif";
	
	/** The ID of the message that just arrived for the user. */
	public static final String PARAM_ID = "id";
	
	private InternalMessage msg;
	
	@Override
	public void init() throws Exception
	{
		this.msg = InternalMessageStore.getInstance().load(getParameterUUID(PARAM_ID)); 
		if (this.msg==null)
		{
			throw new PageNotFoundException();
		}
		if (InternalMessageRecipientStore.getInstance().isRecipient(this.msg.getID(), getContext().getUserID())==false)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("messaging:YouGotMail.Subject", Setup.getAppTitle(getLocale()));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		renderSimpleHTML();
	}
	
	@Override
	public void renderSimpleHTML() throws Exception
	{
		StringBuilder link = new StringBuilder();
		link.append("<a href=\"");
		link.append(getPageURL(ReadMessagePage.COMMAND, new ParameterMap(ReadMessagePage.PARAM_ID, this.msg.getID())));
		link.append("\">");
		link.append(Util.htmlEncode(getString("messaging:YouGotMail.ViewMesssage")));
		link.append("</a>");
		
		String pattern = Util.htmlEncode(getString("messaging:YouGotMail.HTML", Setup.getAppTitle(getLocale()), "$link$"));
		pattern = Util.strReplace(pattern, "$link$", link.toString());
		
		write(pattern);
	}

	@Override
	public void renderText() throws Exception
	{
		renderShortText();
	}
	
	@Override
	public void renderShortText() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		String shortenedURL = XCoShortenUrl.shorten(fed.getXCoAPIKey(), getPageURL(ReadMessagePage.COMMAND, new ParameterMap(ReadMessagePage.PARAM_ID, this.msg.getID())));
		
		write(getString("messaging:YouGotMail.Text", Setup.getAppTitle(getLocale()), shortenedURL));
	}
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		write("<block><prompt bargein=\"false\">");
		writeEncode(getString("messaging:YouGotMail.Voice", Setup.getAppTitle(getLocale())));
		write("</prompt></block>");
	}
}
