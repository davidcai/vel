package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

public class BadRequestException extends HttpException
{
	private static final long serialVersionUID = 4991883248518648395L;

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
