package samoyan.apps.master;

import java.io.IOException;
import java.util.*;

import samoyan.apps.master.JoinPage;
import samoyan.apps.master.WelcomePage;
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
import samoyan.servlet.UserAgent;
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
		if (user!=null && user.isPassword(password))
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
		setCookie(RequestContext.COOKIE_AUTH, AuthTokenStore.getInstance().createAuthToken(user.getID(), getContext().getUserAgent().getString(), keep).toString());

		
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
		RequestContext ctx = getContext();
		UserAgent ua = ctx.getUserAgent();
		
		writeFormOpen();
		
		write("<div align=center>"); // center

//		if (ua.isSmartPhone()==false)
//		{
//			write("<br><br><br>");
//		}
		renderLogo(); // subclass
//		if (ua.isSmartPhone()==false)
//		{
//			write("<br><br><br>");
//		}
//		else
		{
			write("<br>");
		}
		
		// Prompt
//		if (!Util.isEmpty(loginUser) || !Util.isEmpty(loginPassword))
//		{
//			// An unsuccessful attempt was made to login
//			writeEncode(getString("master:Login.AccessDenied"));
//		}
//		else
		{
			writeEncode(getString("master:Login.EnterCredentials"));
		}
		
		// Register for access
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isOpenRegistration())
		{
			write("<br><small>");
	
			StringBuffer regLink = new StringBuffer();
			regLink.append("<a href=\"");
			regLink.append(getPageURL(JoinPage.COMMAND));
			regLink.append("\">");
			regLink.append(Util.htmlEncode(getString("master:Login.Register")));
			regLink.append("</a>");
			
			String pattern = Util.htmlEncode(getString("master:Login.NewMember", "$link$"));
			pattern = Util.strReplace(pattern, "$link$", regLink.toString());
			write(pattern);
	
			write("</small>");
		}
		write("<br><br>");
		
		write("<table align=center><tr valign=middle><td align=left>"); // Inner
		writeEncode(getString("master:Login.LoginName"));
		write("</td><td align=right>");
		super.writeTextInput(PARAM_LOGINNAME, null, 20, User.MAXSIZE_LOGINNAME);
		write("</td></tr><tr valign=middle><td align=left>");
		writeEncode(getString("master:Login.Password"));
		write("</td><td align=right>");
		super.writePasswordInput(PARAM_PASSWORD, null, 20, User.MAXSIZE_PASSWORD);
		write("</td></tr>");
		
		write("<tr><td colspan=2>"); // Inner
		
		write("<table width=\"100%\"><tr valign=middle><td align=left>"); // Button table
		
			// Forgot your password?
			write("<small><a href=\"");
			write(getPageURL(PasswordResetPage.COMMAND));
			write("\">");
			writeEncode(getString("master:Login.ForgotPassword"));
			write("</a>&nbsp;</small>");

		write("</td><td align=right nowrap>"); // Button table
		
			super.writeButton(getString("master:Login.Login"));
			
		write("</td></tr></table>"); // Button table
		
			write("</td></tr><tr><td colspan=2 align=center>"); // Inner
			
			write("<small><br>");
			super.writeCheckbox(PARAM_KEEP, null, false);
			write(" ");
			writeTooltip(getString("master:Login.KeepLogin"), getString("master:Login.KeepHelp"));
			write("</small>");
		
		write("</td></tr></table>"); // Inner
		
		write("</div>"); // center
		
		// Postback redirection params
		for (String p : ctx.getParameters().keySet())
		{
			if (p.startsWith(PARAM_REDIRECT_PARAM_PREFIX) ||
				p.equals(PARAM_REDIRECT_METHOD) ||
				p.equals(PARAM_REDIRECT_COMMAND))
			{
				writeHiddenInput(p, null);
			}
		}
		
		writeFormClose();
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
