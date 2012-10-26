package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

public class UnavailableException extends HttpException
{
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_SERVICE_UNAVAILABLE;
	}

	@Override
	public String getHttpTitle()
	{
		return "Service Unavailable";
	}
}
