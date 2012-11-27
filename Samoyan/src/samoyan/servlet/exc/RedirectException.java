/**
 * Project: Zhibit
 * File:	RedirectException.java
 * Author:  Yaniv Gvily
 * Created: Feb 23, 2010
 *
 * Copyright © 2010 Zhibit LLC. All rights reserved.
 */
package samoyan.servlet.exc;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import samoyan.servlet.RequestContext;

/**
 * @author brian
 *
 */
public class RedirectException extends HttpException
{
	private String method = "GET";
	private boolean secureSocket = false;
	private String command = null;
	private Map<String, String> parameters = null;
	
	protected RedirectException()
	{	
	}
	
	protected void setMethod(String method)
	{
		this.method = method;
	}

	protected void setSecureSocket(boolean secureSocket)
	{
		this.secureSocket = secureSocket;
	}

	protected void setCommand(String command)
	{
		this.command = command;
	}

	protected void setParameters(Map<String, String> params)
	{
		this.parameters = params;
	}

	public RedirectException(String command, Map<String, String> params)
	{
		RequestContext ctx = RequestContext.getCurrent();
		if (ctx!=null)
		{
			this.secureSocket = ctx.isSecureSocket();
		}
		this.method = "GET";
		this.command = command;
		this.parameters = params;
	}

	public RedirectException(boolean secureSocket, String method, String command, Map<String, String> params)
	{
		this.secureSocket = secureSocket;
		this.method = method;
		this.command = command;
		this.parameters = params;
	}
	
	@Override
	public String getHttpTitle()
	{
		return "Found";
	}
	
	@Override
	public int getHttpCode()
	{
		return HttpServletResponse.SC_FOUND;
	}
	
	public boolean isSecureSocket()
	{
		return secureSocket;
	}

	public String getCommand()
	{
		return command;
	}

	public String getMethod()
	{
		return method;
	}

	public Map<String, String> getParameters()
	{
		return parameters;
	}
}
