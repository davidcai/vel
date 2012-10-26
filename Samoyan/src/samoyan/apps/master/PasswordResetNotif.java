package samoyan.apps.master;

import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.WebPage;

public final class PasswordResetNotif extends WebPage
{
	public final static String COMMAND = "password-reset.notif";

	@Override
	public void renderSimpleHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		writeEncode(getString("master:PasswordResetNotif.Text", user.getPasswordResetCode(), user.getLoginName()));
	}

	@Override
	public void renderShortText() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		write(getString("master:PasswordResetNotif.ShortText", user.getPasswordResetCode(), user.getLoginName()));
	}

	@Override
	public void renderText() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		write(getString("master:PasswordResetNotif.Text", user.getPasswordResetCode(), user.getLoginName()));
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:PasswordResetNotif.Title");
	}
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());

		String msg = Util.htmlEncode(getString("master:PasswordResetNotif.Voice", "$digits$"));
		StringBuilder digits = new StringBuilder();
		for (int i=0; i<user.getPasswordResetCode().length(); i++)
		{
			digits.append("<break time=\"200ms\"/>");
			digits.append(user.getPasswordResetCode().charAt(i));
		}
		msg = Util.strReplace(msg, "$digits$", digits.toString());
		
		write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		write("<vxml version=\"2.1\" xml:lang=\"");
		writeEncode(getLocale().getLanguage());
		if (!Util.isEmpty(getLocale().getCountry()))
		{
			write("-");
			writeEncode(getLocale().getCountry());
		}
		write("\">");
		write("<form>");

		write("<block>");
		for (int i=0; i<10; i++)
		{
			write("<prompt bargein=\"false\">");
			write(msg);
			write("</prompt>");
			write("<break time=\"2s\"/>");
		}
		write("</block>");
		
		write("</form>");
		write("</vxml>");
	}
	
	@Override
	public boolean isEnvelope() throws Exception
	{
		if (getContext().getChannel().equalsIgnoreCase(Channel.VOICE))
		{
			// For the verification message
			return false;
		}
		else
		{
			return super.isEnvelope();
		}
	}
}
