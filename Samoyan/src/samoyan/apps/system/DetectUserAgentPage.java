package samoyan.apps.system;

import samoyan.servlet.WebPage;

public class DetectUserAgentPage extends WebPage
{
	public final static String COMMAND = "detect-user-agent";
	public final static String PARAM_REDIRECT_COMMAND = "c";
	public final static String PARAM_REDIRECT_PARAM_PREFIX = "p_";
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		write("<html><head>");
		writeIncludeJS("detectUserAgent.js");
		write("<head><body>");
		
		write("</body></html>");
	}
	
	@Override
	public boolean isEnvelope() throws Exception
	{
		// TODO Auto-generated method stub
		return super.isEnvelope();
	}
	
	
}
