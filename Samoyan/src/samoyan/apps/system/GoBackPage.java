package samoyan.apps.system;

import samoyan.servlet.WebPage;

public class GoBackPage extends WebPage
{
	public static final String COMMAND = "go-back";

	@Override
	public void renderHTML() throws Exception
	{
		write("<script type=\"text/javascript\">backPopAndRedirect(null);</script>");
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}

	@Override
	public boolean isCacheable() throws Exception
	{
		return false;
	}

	@Override
	public boolean isLog() throws Exception
	{
		return false;
	}

	@Override
	public int getXRobotFlags() throws Exception
	{
		return NO_FOLLOW | NO_INDEX | NO_ARCHIVE;
	}
}
