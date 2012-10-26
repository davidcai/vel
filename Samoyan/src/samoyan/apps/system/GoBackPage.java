package samoyan.apps.system;

import samoyan.servlet.RequestContext;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;

public class GoBackPage extends WebPage
{
	public static final String COMMAND = "go-back";

	public static final String PARAM_STEPS = "steps";

	@Override
	public String getTitle() throws Exception
	{
		Integer steps = getParameterInteger(PARAM_STEPS);
		if (steps==null)
		{
			steps = 1;
		}

		return getString("system:GoBack.Title", (int) Math.abs(steps));
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		Integer steps = getParameterInteger(PARAM_STEPS);
		if (steps==null)
		{
			steps = 1;
		}
		steps = Math.abs(steps);
				
		write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		write("<html><head>");
		write("<meta name=robots content=\"noindex,nofollow,noarchive\">");
		
		write("<title>");
		writeEncode(getTitle());
		write("</title>");
		
		write("<script type=\"text/javascript\">window.history.go(");
		write(-steps-1);
		write(");</script>");
		
		write("</head><body>");

		write("<noscript>");
		write("<a href=\"");
		write(UrlGenerator.getPageURL(ctx.isSecureSocket(), ctx.getHost(), ctx.getCommand(), ctx.getParameters()));
		write("\">");
		writeEncode(getString("system:GoBack.Continue"));
		write("</a>");
		write("</noscript>");
		
		write("</body></html>");
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
	public boolean isEnvelope() throws Exception
	{
		return false;
	}

	@Override
	public int getXRobotFlags() throws Exception
	{
		return NO_FOLLOW | NO_INDEX | NO_ARCHIVE;
	}
}
