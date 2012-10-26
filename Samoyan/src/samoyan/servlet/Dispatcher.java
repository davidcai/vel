package samoyan.servlet;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import samoyan.core.Util;
import samoyan.database.LogEntryStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.BadRequestException;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.SecureSocketException;
import samoyan.servlet.exc.UnauthorizedException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.ActionLogEntry;

public final class Dispatcher
{
	private static Map<String, Class<? extends WebPage>> pages = new ConcurrentHashMap<String, Class<? extends WebPage>>();
	private static Class<? extends WebPage> envelope = null;
	
	public static void bindPage(String cmd, Class<? extends WebPage> page)
	{
		pages.put(cmd, page);
	}

	public static void bindEnvelope(Class<? extends WebPage> envPage)
	{
		envelope = envPage;
	}

	public static WebPage lookup(RequestContext ctx) throws Exception
	{
		Class<? extends WebPage> pageClass = null;

//		String cmd1 = ctx.getCommand(1);
//		String cmd2 = ctx.getCommand(2);
//		Class<? extends WebPage> pageClass = null;
//		if (cmd2!=null)
//		{
//			pageClass = pages.get(cmd1 + "/" + cmd2);
//		}
//		if (pageClass==null)
//		{
//			pageClass = pages.get(cmd1);
//		}

		// For guided setup pages, dispatch based on what comes after the "setup/" prefix
		String setupPrefix = UrlGenerator.COMMAND_SETUP + "/";
		String cmd = ctx.getCommand();
		if (cmd.startsWith(setupPrefix))
		{
			cmd = cmd.substring(setupPrefix.length());
			
			int slash = cmd.length();
			do
			{
				pageClass = pages.get(cmd.substring(0, slash));
				slash = cmd.lastIndexOf("/", slash-1);
			}
			while (slash>0 && pageClass==null);
		}

		// Look up by exact command
		cmd = ctx.getCommand();
		if (pageClass==null)
		{
			int slash = cmd.length();
			do
			{
				pageClass = pages.get(cmd.substring(0, slash));
				slash = cmd.lastIndexOf("/", slash-1);
			}
			while (slash>0 && pageClass==null);
		}
						
		// Lookup by suffix
		if (pageClass==null)
		{
//			String cmd = ctx.getCommand();
			int dot = cmd.lastIndexOf(".");
			if (dot>=0)
			{
				pageClass = pages.get("*" + cmd.substring(dot));
			}
		}
		
		if (pageClass==null)
		{
			return null;
		}
		
		// Instantiate
		WebPage pg = pageClass.newInstance();
		if (pg==null)
		{
			return null;
		}

		// Create envelope if needed
		if (pg.isEnvelope() && envelope!=null)
		{
			WebPage env = (WebPage) envelope.newInstance();
			if (env!=null)
			{
				env.setChild(pg);
				pg = env;
			}
		}

		return pg;
	}
	
	/**
	 * Executes the <code>WebPage</code> corresponding to the <code>RequestContext</code>.
	 * @param ctx
	 */
	public static void execute(WebPage page, RequestContext ctx) throws Exception
	{
		// Attach the request context to this thread
		RequestContext prevCtx = RequestContext.setCurrent(ctx);
		
		try
		{
			// Check authorization
			if (page.isAuthorized()==false)
			{
				throw new UnauthorizedException();
			}

			// Redirect from HTTP to HTTPS and vice versa, as needed
			// But do not redirect POST requests from HTTPS to HTTP since they cause infinite redirection loop
			boolean ssl = page.isSecureSocket() && Setup.isSSL();
			if (ssl!=ctx.isSecureSocket() &&
				Channel.isSupportsSecureSocket(ctx.getChannel()) &&
				(ctx.getMethod().equalsIgnoreCase("GET") || ssl==true))
			{
				throw new SecureSocketException();
			}
			
			// Update last activity date of user once every 1/4 session
			Date now = new Date();
			User user = UserStore.getInstance().load(ctx.getUserID());
			if (user!=null &&
				(ctx.getMethod().equalsIgnoreCase("POST") || Channel.isPush(ctx.getChannel())==false) &&
				(user.getLastActive()==null || user.getLastActive().getTime() + Setup.getSessionLength() / 4L < now.getTime()))
			{
				user = (User) user.clone();
				user.setLastActive(now);
				UserStore.getInstance().save(user);
			}
			
			page.init();
			
			if (ctx.getMethod().equalsIgnoreCase("POST"))
			{
				// Counter XSS attacks by checking that form data includes the session ID
				String sessionParam = ctx.getParameter(RequestContext.PARAM_SESSION);
				boolean sessionParamMatch = sessionParam!=null && sessionParam.equals(ctx.getSessionID().toString());
				if (page.isProtectXSS() && ctx.getSessionID()!=null && !sessionParamMatch)
				{
					throw new BadRequestException();
				}
				
				// Validate and commit the form
				if (page.isActionable())
				{
					try
					{
						page.validate();

						// Actions
						if (!Util.isEmpty(ctx.getParameter(RequestContext.PARAM_ACTION)))
						{	
							// Log the event
							LogEntryStore.log(new ActionLogEntry());
						}

						page.setCommitted(true);
						page.commit(); // May throw RedirectException, PageNotFoundException, etc.
					}
					catch (WebFormException webFormExc)
					{
						page.setFormException(webFormExc);
					}
				}
				else
				{
					// Page does not support POST
					throw new PageNotFoundException();
				}
			}
			page.render();
		}
		finally
		{
			// Restore the request context for this thread
			RequestContext.setCurrent(prevCtx);
		}
	}

	/**
	 * Returns the title of the <code>WebPage</code> corresponding to the <code>RequestContext</code>.
	 * The page may need to be partially executed.
	 * @param ctx
	 */
	public static String getTitle(WebPage page, RequestContext ctx) throws Exception
	{
		// Attach the request context to this thread
		RequestContext prevCtx = RequestContext.setCurrent(ctx);
		try
		{
			// Check authorization
			if (page.isAuthorized()==false)
			{
				throw new UnauthorizedException();
			}
			page.init();
			return page.getTitle();
		}
		finally
		{
			// Restore the request context for this thread
			RequestContext.setCurrent(prevCtx);
		}
	}
}
