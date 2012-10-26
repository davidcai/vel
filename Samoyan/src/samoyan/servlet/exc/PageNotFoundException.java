/**
 * Project: Zhibit
 * File:	PageNotFoundException.java
 * Author:  Yaniv Gvily
 * Created: Oct 20, 2005
 *
 * Copyright © 2005 Zhibit LLC. All rights reserved.
 */
package samoyan.servlet.exc;

import javax.servlet.http.HttpServletResponse;

/**
 * This exception is thrown to indicate an HTTP error 404.
 *
 * @author Yaniv Gvily
 * @version 1.0
 */
public class PageNotFoundException extends HttpException
{
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_NOT_FOUND;
	}

	@Override
	public String getHttpTitle()
	{
		return "Page not found";
	}
}
