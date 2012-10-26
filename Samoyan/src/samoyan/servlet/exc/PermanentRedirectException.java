package samoyan.servlet.exc;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class PermanentRedirectException extends RedirectException
{	
	public PermanentRedirectException(String command, Map<String, String> params)
	{
		super(command, params);
	}
	
	public PermanentRedirectException(boolean secureSocket, String method, String command, Map<String, String> params)
	{
		super(secureSocket, method, command, params);
	}
	
	@Override
	public String getHttpTitle()
	{
		return "Moved permanently";
	}
	
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_MOVED_PERMANENTLY;
	}
}
