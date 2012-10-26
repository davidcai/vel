/**
 * Project: Underground
 * File:	ForbiddenRequest.java
 * Author:  Yaniv Gvily
 * Created: Oct 20, 2004
 */
package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

/**
 * A forbidden HTTP request is thrown in response to an invalid HTTP method request.
 *
 * @author Yaniv Gvily
 * @version 1.0
 */
public class DisallowedMethodException extends HttpException
{
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
	}
	@Override
	public String getHttpTitle()
	{
		return "Method not allowed";
	}
}
