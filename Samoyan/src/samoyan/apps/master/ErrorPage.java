package samoyan.apps.master;

import javax.servlet.http.HttpServletResponse;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class ErrorPage extends WebPage
{
	public final static String COMMAND = "error";

	private int errorCode = 0;
	
	@Override
	public void init() throws Exception
	{
		String errorCode = getContext().getCommand(2);
		try
		{
			this.errorCode = Integer.parseInt(errorCode);
		}
		catch (Exception e)
		{
			this.errorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		
		if (getString("master:Error.Title." + this.errorCode)==null)
		{
			this.errorCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("master:Error.Title." + this.errorCode);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write(Util.textToHtml(getString("master:Error.Body." + this.errorCode)));
	}
	
	@Override
	public int getStatusCode() throws Exception
	{
		return this.errorCode;
	}
}
