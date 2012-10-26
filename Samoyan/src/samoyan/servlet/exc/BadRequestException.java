package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

public class BadRequestException extends HttpException
{
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_BAD_REQUEST;
	}

	@Override
	public String getHttpTitle()
	{
		return "Bad request";
	}
}
