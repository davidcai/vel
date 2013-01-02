package samoyan.apps.master;

import java.io.IOException;
import java.util.*;

import samoyan.apps.master.WelcomePage;
import samoyan.controls.LoginControl;
import samoyan.core.BCrypt;
import samoyan.core.Cache;
import samoyan.core.LocaleEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.LogEntryStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.LoginDeniedLogEntry;
import samoyan.syslog.LoginOKLogEntry;

public class LoginPage extends WebPage
{
	public final static String COMMAND = "login";
	
	public final static String PARAM_LOGINNAME = "ln";
	public final static String PARAM_PASSWORD = "pw";
	public final static String PARAM_KEEP = "keep";
	public final static String PARAM_REDIRECT_COMMAND = "c";
	public final static String PARAM_REDIRECT_METHOD = "m";
	public final static String PARAM_REDIRECT_PARAM_PREFIX = "p_";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("master:Login.Title");
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();

		// Get params
		String loginName = validateParameterString(PARAM_LOGINNAME, User.MINSIZE_LOGINNAME, User.MAXSIZE_LOGINNAME);
		String password = validateParameterString(PARAM_PASSWORD, User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		
//		if (userName==null && password==null)
//		{
//			return;
//		}
//		userName = userName.toLowerCase(Locale.US).trim();
		
		// Check if this IP was last denied access within the last 2 seconds, then always deny
		Long lastDenied = (Long) Cache.get("logindenied:" + ctx.getIPAddress());
		boolean quickRepost = (lastDenied!=null && lastDenied + 2000L > System.currentTimeMillis());
		
		
		// Locate the user record by user name or email
		User user = null;
		if (Util.isValidEmailAddress(loginName))
		{
			List<UUID> userIDs = UserStore.getInstance().getByEmail(loginName);
			if (userIDs.size()==1)
			{
				user = UserStore.getInstance().load(userIDs.get(0));
			}
		}
		else
		{
			user = UserStore.getInstance().loadByLoginName(loginName);
		}
		
		// Check validity of password
		if (user!=null && user.isPassword(password) && !quickRepost)
		{
			// First time users must initialize their password
			if (user.getLastActive()==null)
			{
				throw new RedirectException(InitPasswordPage.COMMAND,
											new ParameterMap(InitPasswordPage.PARAM_LOGINNAME, user.getLoginName())
											   .plus(InitPasswordPage.PARAM_BCRYPTED_USER_ID, BCrypt.hashpw(user.getID().toString())));
			}
			
			return; // Access granted
		}
		else
		{
			// Access denied
			
			// Log the event
			LoginDeniedLogEntry log = new LoginDeniedLogEntry();
			log.setUserID(user!=null? user.getID() : null);
			LogEntryStore.log(log);
			
			// Remember when this IP was denied access
			Cache.insert("logindenied:" + ctx.getIPAddress(), new Long(System.currentTimeMillis()));
			
			throw new WebFormException(getString("master:Login.AccessDenied"));
		}		
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		
		// Locate the user record by user name or email
		String userName = getParameterString(PARAM_LOGINNAME);
		User user = null;
		if (Util.isValidEmailAddress(userName))
		{
			List<UUID> userIDs = UserStore.getInstance().getByEmail(userName);
			if (userIDs.size()==1)
			{
				user = UserStore.getInstance().load(userIDs.get(0));
			}
		}
		else
		{
			user = UserStore.getInstance().loadByLoginName(userName);
		}
		
		// User authenticated
		
		// Create auth token and set as cookie
		boolean keep = isParameter(PARAM_KEEP);
		UUID authToken = AuthTokenStore.getInstance().createAuthToken(user.getID(), getContext().getUserAgent().getString(), keep, getParameterString(RequestContext.PARAM_APPLE_PUSH_TOKEN));
		setCookie(RequestContext.COOKIE_AUTH, authToken.toString());

		
		// Log the event
		LoginOKLogEntry log = new LoginOKLogEntry();
		log.setUserID(user.getID());
		LogEntryStore.log(log);
		
		// Set timezone and locale, if empty
		if (user.getTimeZone()==null || user.getLocale()==null)
		{
			user = UserStore.getInstance().open(user.getID());
			if (user.getTimeZone()==null)
			{
				user.setTimeZone(ctx.getTimeZone());
			}
			if (user.getLocale()==null)
			{
				Server fed = ServerStore.getInstance().loadFederation();
				user.setLocale(LocaleEx.bestMatch(fed.getLocales(), ctx.getLocales()));
			}
			UserStore.getInstance().save(user);
		}
		
		// Redirect
		String method = ctx.getParameter(PARAM_REDIRECT_METHOD);
		if (method==null || (method.equalsIgnoreCase("GET")==false && method.equalsIgnoreCase("POST")==false))
		{
			method = "GET";
		}
		
		String command = ctx.getParameter(PARAM_REDIRECT_COMMAND);

		Map<String, String> params = new HashMap<String, String>();
		for (String p : ctx.getParameters().keySet())
		{
			if (p.startsWith(PARAM_REDIRECT_PARAM_PREFIX))
			{
				params.put(p.substring(PARAM_REDIRECT_PARAM_PREFIX.length()), ctx.getParameter(p));
			}
		}
		
		// Redirect to welcome page by default
		if (command==null)
		{
			method = "GET";
			command = WelcomePage.COMMAND;
			params.clear();
		}
		
		throw new RedirectException(false, method, command, params);	
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<div align=center>"); // center

		renderLogo(); // subclass
		write("<br>");
		new LoginControl(this).postbackRedirect(true).showPrompt(true).showRememberMe(true).render();
		
		write("</div>");
	}
	
	@Override
	public int getXRobotFlags() throws Exception
	{
		return NO_INDEX;
	}

	/**
	 * To be overridden by subclass to render an image above the login form.
	 * @throws IOException 
	 */
	protected void renderLogo() throws Exception
	{
	}
}
