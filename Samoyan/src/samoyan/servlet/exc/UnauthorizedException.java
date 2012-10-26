package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

public class UnauthorizedException extends HttpException
{
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_UNAUTHORIZED;
	}

	@Override
	public String getHttpTitle()
	{
		return "Unauthorized";
	}
}
