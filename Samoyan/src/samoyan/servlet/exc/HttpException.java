package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

public class HttpException extends Exception
{
	public final static int SEVERITY_INFO = 0;
	public final static int SEVERITY_WARNING = 1;
	public final static int SEVERITY_ERROR = 2;
	
	public HttpException()
	{
		super();
	}
	public HttpException(String msg)
	{
		super(msg);
	}

	public String getHttpTitle()
	{
		return "Internal server error";
	}
	
	public int getHttpCode()
	{
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}
}
