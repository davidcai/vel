package samoyan.apps.master;

import javax.servlet.http.HttpServletResponse;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class ErrorPage extends WebPage
{
	public final static String COMMAND = "error";

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:Error.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write(Util.textToHtml(getString("master:Error.Body")));
	}
	
	@Override
	public int getStatusCode() throws Exception
	{
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}
}
