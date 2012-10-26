package samoyan.apps.admin.tools;

import samoyan.apps.admin.AdminPage;
import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class AdHocNotif extends WebPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/adhoc.notif";

	public static final String PARAM_BODY = "body";
	public static final String PARAM_SUBJECT = "subject";
	
	@Override
	public void renderHTML() throws Exception
	{
		write(getParameterString(PARAM_BODY));
	}

	@Override
	public void renderSimpleHTML() throws Exception
	{
		write(getParameterString(PARAM_BODY));
	}

	@Override
	public void renderShortText() throws Exception
	{
		writeEncode(Util.htmlToText(getParameterString(PARAM_BODY)));
	}

	@Override
	public void renderText() throws Exception
	{
		writeEncode(Util.htmlToText(getParameterString(PARAM_BODY)));
	}

	
	@Override
	public void renderVoiceXML() throws Exception
	{
		write("<block><prompt>");
		writeEncode(Util.htmlToText(getParameterString(PARAM_BODY)));
		write("</prompt></block>");
	}

	@Override
	public String getTitle() throws Exception
	{
		return getParameterString(PARAM_SUBJECT);
	}

	@Override
	public boolean isActionable() throws Exception
	{
		return false;
	}	
}
