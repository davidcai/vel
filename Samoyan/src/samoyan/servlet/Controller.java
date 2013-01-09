package samoyan.servlet;

import java.io.*;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import javax.servlet.*;
import javax.servlet.http.*;

import samoyan.apps.admin.AdminApp;
import samoyan.apps.guidedsetup.GuidedSetupApp;
import samoyan.apps.master.LoginPage;
import samoyan.apps.master.MasterApp;
import samoyan.apps.messaging.MessagingApp;
import samoyan.apps.profile.ProfileApp;
import samoyan.apps.system.SystemApp;
import samoyan.core.*;
import samoyan.core.image.JaiImage;
import samoyan.core.image.LargestCropSizer;
import samoyan.core.image.ShrinkToFitSizer;
import samoyan.core.less.LessEngine;
import samoyan.core.less.LessException;
import samoyan.database.AuthTokenStore;
import samoyan.database.CountryStore;
import samoyan.database.Database;
import samoyan.database.Image;
import samoyan.database.ImageStore;
import samoyan.database.InternalMessageRecipientStore;
import samoyan.database.InternalMessageStore;
import samoyan.database.LogEntryStore;
import samoyan.database.LogTypeStore;
import samoyan.database.MobileCarrierStore;
import samoyan.database.NotificationStore;
import samoyan.database.PermissionStore;
import samoyan.database.Query;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.TrackbackStore;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.email.EmailServer;
import samoyan.notif.Notifier;
import samoyan.servlet.exc.*;
import samoyan.servlet.exc.UnavailableException;
import samoyan.sms.SmsServer;
import samoyan.syslog.RenderFileLogEntry;
import samoyan.syslog.RenderHTMLLogEntry;
import samoyan.syslog.RenderImageLogEntry;
import samoyan.syslog.RenderNotFoundLogEntry;
import samoyan.syslog.SystemShutdownLogEntry;
import samoyan.syslog.SystemStartLogEntry;
import samoyan.tasks.CleanTempFilesRecurringTask;
import samoyan.tasks.DeleteExpiredAuthTokensRecurringTask;
import samoyan.tasks.DeleteOldLogEntriesRecurringTask;
import samoyan.tasks.DeleteOldTrackbacksRecurringTask;
import samoyan.tasks.TaskManager;
import samoyan.twitter.TwitterServer;

public class Controller extends HttpServlet
{
	private static long clientCacheExpires = 1000L;

	private static long startTime = 0;
	private static boolean initOK = false;
	private static long totalHitCount = 0;
	private static long pageHitCount = 0;
	private static LessEngine lessEngine = null;
	private static Boolean lessEngineLock = new Boolean(false);
	
	private static HttpServlet instance = null;
	
	public final void init(ServletConfig cfg) throws ServletException
	{
		super.init(cfg);
		
		instance = this;
		startTime = System.currentTimeMillis();

		try
		{
			// Init Debug console
			Debug.init(Setup.isDebug());
			
			// Init cache
			Cache.init(Setup.getCacheCapacity());
			
			// Global settings
			clientCacheExpires = Setup.getClientCacheExpires();
//			TimeZone.setDefault(Setup.getTimeZone());
//			Locale.setDefault(Setup.getLocale());
			
			// Init JAI
			JaiImage.init(true);
			ImageStore.getInstance().bindSizer(Image.SIZE_FULL, new ShrinkToFitSizer(Image.MAX_WIDTH, Image.MAX_HEIGHT));
			ImageStore.getInstance().bindSizer(Image.SIZE_THUMBNAIL, new LargestCropSizer(72, 72));
			
			// Load database object model
			UserStore.getInstance().define();
			CountryStore.getInstance().define();
			LogEntryStore.getInstance().define();
			LogTypeStore.getInstance().define();
			MobileCarrierStore.getInstance().define();
			NotificationStore.getInstance().define();
			PermissionStore.getInstance().define();
			ServerStore.getInstance().define();
			TrackbackStore.getInstance().define();
			UserGroupStore.getInstance().define();
			AuthTokenStore.getInstance().define();
			InternalMessageStore.getInstance().define();
			InternalMessageRecipientStore.getInstance().define();
			
			UserUserGroupLinkStore.getInstance().define();
			
			this.preStart(); // Call subclass			
			
			// Init database
			Database.createInstance(
				Setup.getDatabaseDriver(),
				Setup.getDatabaseURL(), 
				Setup.getDatabaseUser(), 
				Setup.getDatabasePassword());

			// Init system log
			LogEntryStore.start();
			
			synchronousUpgrade();
			
			Dispatcher.bindEnvelope(EnvelopePage.class);

			// Channel servers
			EmailServer.init();
			SmsServer.init();
			TwitterServer.init();
						
			// Notifier
			Notifier.init();
			
			// Apps
			SystemApp.init();
			MasterApp.init();
			AdminApp.init();
			ProfileApp.init();
			GuidedSetupApp.init();
			MessagingApp.init();
			
			// Tasks
			TaskManager.addRecurring(new DeleteOldLogEntriesRecurringTask());
			TaskManager.addRecurring(new DeleteOldTrackbacksRecurringTask());
			TaskManager.addRecurring(new CleanTempFilesRecurringTask());
			TaskManager.addRecurring(new DeleteExpiredAuthTokensRecurringTask());
			
			// Subclass
			this.start(); // Call subclass
			
			// Log system start event
			SystemStartLogEntry logEvent = new SystemStartLogEntry(System.currentTimeMillis() - startTime);
			logEvent.setTime(new Date(startTime));
			LogEntryStore.log(logEvent);

			initOK = true;
		}
		catch (Exception e)
		{
			Debug.logStackTrace(e);
			initOK = false;
		}
	}
	
	private void synchronousUpgrade() throws Exception
	{
		// Upgrade
		Server fed = ServerStore.getInstance().loadFederation();
		String VERSION = "2012-10-09";
		if (fed.getPlatformUpgradeVersion().compareTo(VERSION)<0)
		{
			// - - - begin synchronous upgrade
			Query q = new Query();
			q.update("delete from props where typ='Img'");
			q.close();
			// - - - end synchronous upgrade
			
			fed = (Server) fed.clone();
			fed.setPlatformUpgradeVersion(VERSION);
			ServerStore.getInstance().save(fed);
		}
	}

	public final void destroy()
	{
		try
		{
			long start = System.currentTimeMillis();
			
			// Subclass
			this.terminate(); // Call subclass
			System.out.println(Setup.getAppID() + " shutdown 1: " + (System.currentTimeMillis()-start));
			
			// Execution manager
			TaskManager.terminateAll();
			System.out.println(Setup.getAppID() + " shutdown 2: " + (System.currentTimeMillis()-start));
			
			// Notifier
			Notifier.terminate();
			System.out.println(Setup.getAppID() + " shutdown 3: " + (System.currentTimeMillis()-start));
			
			// Channel servers
			TwitterServer.terminate();
			System.out.println(Setup.getAppID() + " shutdown 4: " + (System.currentTimeMillis()-start));
			
			SmsServer.terminate();
			System.out.println(Setup.getAppID() + " shutdown 5: " + (System.currentTimeMillis()-start));
			
			EmailServer.terminate();
			System.out.println(Setup.getAppID() + " shutdown 6: " + (System.currentTimeMillis()-start));
			
			
			// Log system shutdown event
			SystemShutdownLogEntry logEvent = new SystemShutdownLogEntry(System.currentTimeMillis() - start);
			logEvent.setTime(new Date(start));
			LogEntryStore.log(logEvent);
			System.out.println(Setup.getAppID() + " shutdown 7: " + (System.currentTimeMillis()-start));
			
			// Terminate system log
			LogEntryStore.terminate();
			System.out.println(Setup.getAppID() + " shutdown 8: " + (System.currentTimeMillis()-start));
			
			// Close database
			Database.getInstance().close();			
			System.out.println(Setup.getAppID() + " shutdown 9: " + (System.currentTimeMillis()-start));
		}
		catch (Exception e)
		{
			Debug.logStackTrace(e);
		}

		super.destroy();
	}
	
	/**
	 * To be overridden by subclass to initialize the controller.
	 * Called after the platform database is defined, but before the system is started.
	 * Subclasses should use this method to define the database.
	 */
	protected void preStart() throws Exception
	{	
	}

	/**
	 * To be overridden by subclass to initialize the controller.
	 * Called after the database is started and the platform started.
	 */
	protected void start() throws Exception
	{	
	}
	
	/**
	 * To be overridden by subclass to terminate the controller.
	 */
	protected void terminate() throws Exception
	{	
	}
		
	public final static String getServletPath()
	{
		String servletPath = instance.getServletContext().getContextPath();
		
//		// !$! Hack to support switchboard dispatching
//		if (servletPath.startsWith("/" + Setup.getHost().replace('.', '_')))
//		{
//			servletPath = servletPath.substring(1+Setup.getHost().length());
//		}

		// Support switchboard dispatching
		if (Setup.getPath()!=null)
		{
			servletPath = Setup.getPath();
		}
		
		return servletPath;
	}
	public final static long getStartTime()
	{
		return startTime;
	}
	public final static long getTotalHitCount()
	{
		return totalHitCount;
	}
	public final static long getPageHitCount()
	{
		return pageHitCount;
	}
	public final static InputStream getResourceAsStream(String resourceName)
	{
		return instance.getServletContext().getResourceAsStream(resourceName);
	}

	@Override
	protected final void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		long time = System.currentTimeMillis();
		totalHitCount ++;

		RequestContext prevCtx = null;
		RequestContext ctx = null;
		Throwable exc = null;
		try
		{
			if (initOK==false)
			{
				throw new UnavailableException();
			}
			
			// Get and parse the request
			ctx = createRequestContext(request);
			
			// Verify HTTP method (deny TRACE, DELETE, PUT, etc.)
			String method = request.getMethod();
			if (method.equalsIgnoreCase("GET")==false &&
				method.equalsIgnoreCase("POST")==false &&
				method.equalsIgnoreCase("HEAD")==false)
			{
				throw new DisallowedMethodException();
			}
			
			// Attach the request context to this thread
			prevCtx = RequestContext.setCurrent(ctx);
			
			// Execute the request
			serviceInternal(ctx, request, response);		
		}
		catch (OutOfMemoryError memErr)
		{
			exc = memErr;
			
			System.gc();
			System.gc();
			System.gc();
			
			outputException(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error", memErr);
		}
		catch (SecureSocketException sslExc)
		{
			RedirectException redirectExc = new RedirectException(!ctx.isSecureSocket(), ctx.getMethod(), ctx.getCommand(), ctx.getParameters());
			outputRedirect(ctx, redirectExc, response);
		}
		catch (RedirectException redirectExc)
		{
			outputRedirect(ctx, redirectExc, response);
		}
		catch (UnauthorizedException unauthExc)
		{
			Map<String, String> params = new HashMap<String, String>();
			for (String p : ctx.getParameters().keySet())
			{
				params.put(LoginPage.PARAM_REDIRECT_PARAM_PREFIX + p, ctx.getParameter(p));
			}
			if (ctx.getMethod().equalsIgnoreCase("GET")==false)
			{
				params.put(LoginPage.PARAM_REDIRECT_METHOD, ctx.getMethod());
				params.put(RequestContext.PARAM_SESSION, ctx.getSessionID().toString());
			}
			params.put(LoginPage.PARAM_REDIRECT_COMMAND, ctx.getCommand());
			
			RedirectException loginRedirect = new RedirectException(true, ctx.getMethod(), LoginPage.COMMAND, params);
			outputRedirect(ctx, loginRedirect, response);
		}
		catch (HttpException httpExc)
		{
			exc = httpExc;			
			outputException(request, response, httpExc.getHttpCode(), httpExc.getHttpTitle(), httpExc);
		}
		catch (Throwable genErr)
		{
			exc = genErr;
			outputException(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error", genErr);
			
			LogEntryStore.log(genErr);
		}
		finally
		{
			// Restore the request context for this thread
			RequestContext.setCurrent(prevCtx);
			
			StringBuffer debug = new StringBuffer(1024);
			if (ctx!=null)
			{
				debug.append(ctx.toString());
			}
			long duration = System.currentTimeMillis() - time;
			debug.append(duration);
			debug.append("ms");
			if (exc!=null)
			{
				debug.append(" (exception)");
			}
			debug.append("\r\n");
						
			if (exc!=null)
			{
				debug.append(Util.exceptionDesc(exc));
			}
			
			Debug.logln(debug.toString());
		}		
	}
		
	private RequestContext createRequestContext(HttpServletRequest request) throws Exception
	{
		RequestContext ctx = new RequestContext();

		// Channel
		ctx.setChannel(Channel.WEB);

		// SSL?
		ctx.setSecureSocket(request.getScheme().equalsIgnoreCase("https"));
		
		// Method
		ctx.setMethod(request.getMethod().toUpperCase(Locale.US));
		
		// Host
		ctx.setHost(request.getServerName().toLowerCase(Locale.US)); // !$! Int'l domain names (IDN) not supported
		ctx.setPort(request.getLocalPort());
		
		// URI
		String uri = Util.urlDecode(request.getRequestURI());
		String contextPath = request.getContextPath();
		if (contextPath.length()>0)
		{
			// Remove the servlet context from the URI
			uri = uri.substring(contextPath.length());
		}
		if (uri.length()>0 && uri.charAt(0)=='/')
		{
			// Remove the first "/" from the URI
			uri = uri.substring(1);
		}
		ctx.setCommand(uri);
		
		// Headers
		Enumeration<String> hdrEnum = request.getHeaderNames();
		while (hdrEnum.hasMoreElements())
		{
			String hdr = (String) hdrEnum.nextElement();
			String hdrVal = request.getHeader(hdr);
			ctx.setHeader(hdr, hdrVal);
		}
		
		// Accept gZip?
		String acceptEncoding = request.getHeader("Accept-Encoding");
	    ctx.setAcceptGzip(acceptEncoding!=null && acceptEncoding.indexOf("gzip")>=0);
	    
		// Cookie data
		Cookie[] reqCookies = request.getCookies();
		for (int i=0; reqCookies!=null && i<reqCookies.length; i++)
		{
			ctx.setCookie(reqCookies[i].getName(), reqCookies[i].getValue());
		}
			
		// Screen dimensions
		int screenWidth = 0;
		int screenHeight = 0;
		float pixelRatio = 1F;
		String screen = ctx.getCookie(RequestContext.COOKIE_OVERRIDE_SCREEN);
		if (screen==null) screen = ctx.getCookie(RequestContext.COOKIE_SCREEN);
		if (screen!=null)
		{
			int p = screen.indexOf("x"); // e.g. 1280x900x1 or 480x800x1.5
			int q = screen.indexOf("x", p+1);
			if (p>0 && q>p)
			{
				try
				{
					screenWidth = Integer.parseInt(screen.substring(0, p));
					screenHeight = Integer.parseInt(screen.substring(p+1, q));
					pixelRatio = Float.parseFloat(screen.substring(q+1));
				}
				catch (Exception e)
				{
					screenWidth = 0;
					screenHeight = 0;
					pixelRatio = 1F;
				}
			}
		}

		// User agent. Spider?
		String userAgentString = ctx.getCookie(RequestContext.COOKIE_OVERRIDE_USER_AGENT);
		if (userAgentString==null)
		{
			userAgentString = request.getHeader("user-agent");
		}
		else
		{
			userAgentString = Util.urlDecode(userAgentString);
		}
		if (!Util.isEmpty(userAgentString))
		{
			ctx.setUserAgent(userAgentString, screenWidth, screenHeight, pixelRatio);
		}
		if (ctx.getUserAgent().isVoxeo())
		{
			ctx.setChannel(Channel.VOICE);
		}				

		// Cookie authentication
		String authCookieStr = ctx.getCookie(RequestContext.COOKIE_AUTH);
		if (Util.isUUID(authCookieStr))
		{
			ctx.setUserID(AuthTokenStore.getInstance().validateAuthToken(UUID.fromString(authCookieStr), userAgentString));
		}
		
		// Session
		String sessionCookieStr = ctx.getCookie(RequestContext.COOKIE_SESSION);
		if (Util.isUUID(sessionCookieStr))
		{
			ctx.setSessionID(UUID.fromString(sessionCookieStr));
		}
		else
		{
			ctx.setSessionID(UUID.randomUUID());
		}

		

		// IP
		ctx.setIPAddress(request.getRemoteAddr());
		
		// Locales
		Enumeration<Locale> reqLocales = request.getLocales();
		while (reqLocales.hasMoreElements())
		{
			ctx.getLocales().add(reqLocales.nextElement());
		}
		
		// Time zone
		String tzStr = ctx.getCookie(RequestContext.COOKIE_TIMEZONE);
		if (!Util.isEmpty(tzStr))
		{
			TimeZone tz = TimeZone.getTimeZone(tzStr);
			if (tz.getID().equals(tzStr))
			{
				ctx.setTimeZone(tz);
			}
		}
		String offsetStr = ctx.getCookie(RequestContext.COOKIE_TIMEZONE_OFFSET);
		if (!Util.isEmpty(offsetStr))
		{
			try
			{
				int offsetMillis = -60 * 1000 * (int) Float.parseFloat(offsetStr);
				if (ctx.getTimeZone()==null || ctx.getTimeZone().getOffset(System.currentTimeMillis())!=offsetMillis)
				{
					ctx.setTimeZone(TimeZoneEx.getByOffsetNow(offsetMillis));
				}
			}
			catch (NumberFormatException exc)
			{
				// Invalid cookie value, ignore
			}
		}
		
		// Standard parameters
		String charsetName = request.getParameter("charset");
		if (Util.isEmpty(charsetName))
		{
			charsetName = request.getParameter(RequestContext.PARAM_CHARSET);
		}
		try
		{
			if (!Util.isEmpty(charsetName) && !Charset.isSupported(charsetName))
			{
				charsetName = null;
			}
		}
		catch (Exception e)
		{
			charsetName = null;
		}
		if (Util.isEmpty(charsetName))
		{
			charsetName = "UTF-8"; // Default to UTF-8
		}

		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements())
		{
			String prmName = (String) names.nextElement();
			String prmValue = request.getParameter(prmName);
			
			if (request.getCharacterEncoding()==null)
			{
				if (charsetName.equalsIgnoreCase("UTF-8"))
				{
					prmValue = Util.utf8Decode(prmValue); // Unicode support
				}
				else
				{
					prmValue = Util.charsetDecode(prmValue, charsetName);
				}
			}
			
			ctx.setParameter(prmName, prmValue);
		}
		
		// Multi-part parameters
		String contentType = request.getHeader("content-type");
//		int totalCount = 0;
		if (contentType!=null && contentType.startsWith("multipart/form-data"))
		{
//			int contentLen = request.getIntHeader("content-length");

			// multipart/form-data
			int p = contentType.indexOf("boundary=");
			String boundary = contentType.substring(p+9);
			
			ServletInputStream in = request.getInputStream();
			byte[] buffer = new byte[1024];
			
			// First line is always the boundary
			int count = in.readLine(buffer, 0, buffer.length);
//			totalCount += count;
			do
			{
				// Next come the MIME headers
				String partName = null;
				String fileName = null;
				boolean octet = false;
				while (true)
				{
					count = in.readLine(buffer, 0, buffer.length);
					if (count<=2) break;
//					totalCount += count;

					String line = new String(buffer, 0, count);
					if (line.startsWith("Content-Disposition: form-data; name=\""))
					{
						int pp = line.indexOf("name=\"");
						int qq = line.indexOf("\"", pp+6);
						partName = line.substring(pp+6, qq);
						
						// Treat all files as streams
						pp = line.indexOf("filename=\"");
						if (pp>=0)
						{
							pp += 10;
							qq = line.indexOf("\"", pp);
							fileName = line.substring(pp, qq);
							octet = true;
						}
					}
					else if (line.startsWith("Content-Type: application/octet-stream"))
					{
						octet = true;
					}
				}
				if (count<=0) break; // End of stream
				if (partName==null) continue;
				
				// Then the content itself
				if (octet==false)
				{
					StringBuffer partContent = new StringBuffer();
					while (true)
					{
						count = in.readLine(buffer, 0, buffer.length);
						
						// Check if we reached the end of the transmission
						// Might happen on premature socket termination
						if (count<=0) break;

						// Check if this line is the boundary, if so, exit loop
						String line = new String(buffer, 0, count, "utf-8"); // Unicode support
						if (count<=boundary.length()+6 && count>=boundary.length()+2)
						{
							if (line.indexOf(boundary)>=0) break;
						}

//						totalCount += count;

//						partContent.append(Util.htmlDecode(line));
						partContent.append(line);
					}
					
					// Remove the last newline
					if (partContent.length()>=2)
					{
						partContent.delete(partContent.length()-2, partContent.length());
					}
					
					String partVal = partContent.toString();
					ctx.setParameter(partName, partVal);
				}
				else
				{
					byte[] prevBuffer = null;
					int prevCount = 0;
					PostedFileOutputStream partContent = new PostedFileOutputStream();
					
					while (true)
					{
						count = in.readLine(buffer, 0, buffer.length);
						
						// Check if we reached the end of the transmission
						// Might happen on premature socket termination
						if (count<=0) break;
						
						// Check if this line is the boundary, if so, exit loop
						if (count<=boundary.length()+6 && count>=boundary.length()+2)
						{
							String line = new String(buffer, 0, count);
							if (line.indexOf(boundary)>=0) break;
						}
						
//						totalCount += count;

						// Write the content of the previous line
						if (prevBuffer!=null)
						{
							partContent.write(prevBuffer, 0, prevCount);
						}
						prevBuffer = buffer;
						prevCount = count;
						buffer = new byte[buffer.length];
					}
					if (prevCount>=2)
					{
						partContent.write(prevBuffer, 0, prevCount-2); // Omit last newline
					}
					
					partContent.close();
					if (partContent.size()>0)
					{
						ctx.setPostedFile(partName, partContent.getFile());
						if (fileName!=null) ctx.setParameter(partName, fileName);
					}
				}
			}
			while(true);
		}
		else
		{
			ServletInputStream in = request.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			boolean hasPayload = false;
			byte[] buffer = new byte[1024];
			int count = 0;
			while ((count = in.read(buffer))>0)
			{
				baos.write(buffer, 0, count);
				hasPayload = true;
			}

			if (hasPayload)
			{
				ctx.setPayload(baos.toByteArray());
			}
		}
		
		// Authentication
		String authParam = ctx.getParameter(RequestContext.PARAM_AUTH);
		if (Util.isUUID(authParam))
		{
			ctx.setUserID(AuthTokenStore.getInstance().validateAuthToken(UUID.fromString(authParam), userAgentString));
			ctx.getParameters().remove(RequestContext.PARAM_AUTH);
			ctx.setCookie(RequestContext.COOKIE_AUTH, authParam);
		}
		
		// Command
		String command = ctx.getParameter(RequestContext.PARAM_COMMAND);
		if (!Util.isEmpty(command))
		{
			if (command.startsWith("/"))
			{
				// Remove the first "/" from the command
				command = command.substring(1);
			}
			ctx.setCommand(command);
			ctx.getParameters().remove(RequestContext.PARAM_COMMAND);
		}
		
		// Channel override
		if (Setup.isDebug())
		{
			String channelParam = ctx.getParameter(RequestContext.PARAM_CHANNEL);
			if (channelParam!=null)
			{
				ctx.setChannel(channelParam);
			}
			ctx.getParameters().remove(RequestContext.PARAM_CHANNEL);
		}
				
		return ctx;
	}
	
	private void outputRedirect(RequestContext ctx, RedirectException redirectExc, HttpServletResponse response) throws IOException
	{
		boolean fromHttps = ctx.isSecureSocket();
		boolean webChannel = ctx.getChannel().equalsIgnoreCase(Channel.WEB);
		boolean post = redirectExc.getMethod().equalsIgnoreCase("POST");
		
		if (post && webChannel)
		{
			// Must post params in a form using JavaScript form submission
			response.setContentType("text/html; charset=UTF-8");

			StringBuffer buf = new StringBuffer();
			buf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
			buf.append("<html><head>");
			buf.append("<meta name=robots content=\"noindex,nofollow,noarchive\">");
			
			buf.append("</head><body>");
			
			// If the redirection is initiated from HTTPS, we always post to an HTTPS to prevent browser warnings
			String url = UrlGenerator.getPageURL(fromHttps || redirectExc.isSecureSocket(), null, redirectExc.getCommand(), null);
			buf.append("<form method=post action=\"");
			buf.append(Util.htmlEncode(url));
			buf.append("\" accept-charset=\"UTF-8\">");
			
			Map<String, String> params = redirectExc.getParameters();
			if (params!=null)
			{
				for (String key : params.keySet())
				{
					buf.append("<input type=hidden name=\"");
					buf.append(Util.htmlEncode(key));
					buf.append("\" value=\"");
					buf.append(Util.htmlEncode(params.get(key)));
					buf.append("\">");
				}
			}
			
			buf.append("<noscript>");
			buf.append("<input type=submit value=Continue>");
			buf.append("</noscript>");
			
			buf.append("<input id=autosubmit type=submit value=Continue style='visibility:hidden;'>");
			
			buf.append("</form>");
			buf.append("<script type=\"text/javascript\">document.getElementById(\"autosubmit\").click();</script>");
			
			buf.append("</body></html>");
			
			PrintWriter wrt = response.getWriter();
			wrt.write(buf.toString());
			wrt.flush();
		}
		else if (fromHttps==true && redirectExc.isSecureSocket()==false && webChannel)
		{
			String url = UrlGenerator.getPageURL(false, null, redirectExc.getCommand(), redirectExc.getParameters());

			// To prevent a HTTPS to HTTP warning in the user's browser, we use JavaScript redirection
			// but only for the web channel (a browser). The voice channel for example, is not run by a browser, so normal redirection will do.
			response.setContentType("text/html; charset=UTF-8");
			
			StringBuffer buf = new StringBuffer();
			buf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
			buf.append("<html><head>");
			buf.append("<meta name=robots content=\"noindex,nofollow,noarchive\">");
			buf.append("<script type=\"text/javascript\">document.location=\"");
//				buf.append(Util.htmlEncode(url));
			buf.append(url);
			buf.append("\";</script>");
			
			buf.append("</head><body>");
			buf.append("<noscript>");
			buf.append("<a href=\"");
//				buf.append(Util.htmlEncode(url));
			buf.append(url);
			buf.append("\">Continue</a>");
			buf.append("</noscript>");
			buf.append("</body></html>");
			
			PrintWriter wrt = response.getWriter();
			wrt.write(buf.toString());
			wrt.flush();
		}
		else
		{
			String url = UrlGenerator.getPageURL(redirectExc.isSecureSocket(), null, redirectExc.getCommand(), redirectExc.getParameters());

			response.setStatus(redirectExc.getHttpCode());
			response.setHeader("Location", url);
		}
	}

	private int outputBytes(byte[] bytes, String mime, boolean gzipCompress, HttpServletResponse response)
	throws IOException
	{		
		InputStream in = new ByteArrayInputStream(bytes);
		try
		{
			return outputBytes(in, bytes.length, mime, gzipCompress, response);
		}
		finally
		{
			in.close();
		}
	}

	private int outputString(String string, String mime, boolean gzipCompress, HttpServletResponse response) throws IOException
	{
		byte[] bytes = string.getBytes("UTF-8");
		return outputBytes(new ByteArrayInputStream(bytes), bytes.length, mime, gzipCompress, response);
	}
	
	private int outputBytes(InputStream in, int size, String mime, boolean gzipCompress, HttpServletResponse response) throws IOException
	{		
		byte[] buffer = new byte[8192];
		int len;

		if (gzipCompress)
		{
			ByteArrayOutputStream compressed = new ByteArrayOutputStream(Math.max(size/4, 1024));
			GZIPOutputStream zipper = new GZIPOutputStream(compressed);
			
			try
			{
				while ((len = in.read(buffer)) > 0)
				{
					zipper.write(buffer, 0, len);
				}
			}
			finally
			{
				zipper.close();
			}
			
			// Override the given input stream with the new compressed stream
			byte[] bytes = compressed.toByteArray();
			in = new ByteArrayInputStream(bytes);
			size = bytes.length;

			response.setHeader("Content-Encoding", "gzip");
		}

		// Set response headers
		if (mime.startsWith("text/") && mime.indexOf(";")<0)
		{
			mime += "; charset=UTF-8";
		}
		if (mime!=null) response.setContentType(mime);
		if (size>0) response.setContentLength(size);
		
		// Output the stream
		int totalLen = 0;
		OutputStream out = response.getOutputStream();
		try
		{
			while ((len = in.read(buffer)) > 0)
			{
				totalLen += len;
				out.write(buffer, 0, len);
			}
			out.flush();
		}
		catch (Exception exc)
		{
			// Ignore. Mostly connection aborts.
		}
//		finally
//		{
//			out.close();
//		}
		
		return totalLen;
	}

	private void outputException(HttpServletRequest request, HttpServletResponse response, int httpCode, String httpTitle, Throwable exc) throws IOException
	{
		// Print exception
		response.setStatus(httpCode);
		response.setContentType("text/plain");

		PrintWriter wrt = response.getWriter();
		wrt.write(String.valueOf(httpCode) + " " + httpTitle);
		if (exc!=null && Setup.isDebug())
		{
			wrt.write("\r\n\r\n");
			wrt.write(Util.exceptionDesc(exc));
		}
		wrt.flush();
	}
	
	private void serviceInternal(RequestContext ctx, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long startTime = System.currentTimeMillis();

		// Safety check
		String cmd = ctx.getCommand();
		String cmdUpper = ctx.getCommand().toUpperCase(Locale.US);
		for (int i=0; i<cmdUpper.length(); i++)
		{
			char c = cmdUpper.charAt(i);
			if (c!=' ' && c!='-' && c!='.' && c!='/' && c!='_' &&
				(c<'A' || c>'Z') && (c<'0' || c>'9'))
			{
				throw new PageNotFoundException();
			}
		}
		if (cmd.indexOf("..")>=0 ||
			cmdUpper.indexOf("WEB-INF")>=0 ||
			cmdUpper.indexOf("META-INF")>=0)
		{
			throw new PageNotFoundException();
		}

		// Redirect to main domain from alternative domain names
		if (serveAltDomains(ctx, request, response)) return;

		// Serve HTML pages
		if (serveHtml(ctx, request, response)) return;

		// Compile LESS files
		if (serveLESS(ctx, request, response)) return;

		// Serve images
		if (serveImage(ctx, request, response)) return;

		// Request for file
		if (serveFile(ctx, request, response)) return;
		
		// Log the unrecognized request
		LogEntryStore.log(new RenderNotFoundLogEntry(System.currentTimeMillis() - startTime));
		
		throw new PageNotFoundException();
	}
	
	private boolean serveAltDomains(RequestContext ctx, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		// Main domain name
		String mainHost = Setup.getHost();
		String host = ctx.getHost();
		if (host.equalsIgnoreCase(mainHost))
		{
			return false;
		}
		
		// Redirect to official domain name
		throw new RedirectException(ctx.isSecureSocket(), ctx.getMethod(), ctx.getCommand(), ctx.getParameters());
	}

	private boolean serveHtml(RequestContext ctx, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long startTime = System.currentTimeMillis();
		long renderDuration = 0;
		
		WebPage page = Dispatcher.lookup(ctx);;
		if (page==null)
		{
			return false;
		}

		try
		{
			Dispatcher.execute(page, ctx);
			
			renderDuration = System.currentTimeMillis() - startTime;
		}
		catch (HttpException httpExc)
		{
			// Log the event
			if (page.isLog())
			{
				LogEntryStore.log(new RenderHTMLLogEntry(httpExc.getHttpCode(), renderDuration, 0, 0));
			}
//			if (Setup.isDebug()==false &&
//				(httpExc instanceof PageNotFoundException || httpExc instanceof UnavailableException))
//			{
//				// Redirect to a user-friendly error page
//				throw new RedirectException(ErrorPage.COMMAND + "/" + httpExc.getHttpCode(), null);
//			}
//			else
			{
				throw httpExc;
			}
		}
//		catch (Throwable genErr)
//		{
//			// Log the event
//			if (Setup.isDebug()==false)
//			{
//				LogEntryStore.log(genErr);
//
//				// Redirect to a user-friendly error page
//				throw new RedirectException(ErrorPage.COMMAND + "/" + HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
//			}
//			else
//			{
//				throw genErr;
//			}
//		}
		finally
		{
			setCacheHeaders(request, response, page.isCacheable());
			
			// Cookies
			Map<String, String> pageCookies = page.getCookies();
//			if (pageCookies!=null ||
//				(ctx.getUserID()==null && !Util.isEmpty(ctx.getCookie(RequestContext.COOKIE_AUTH))) ||
//				(ctx.getUserID()!=null && Util.isEmpty(ctx.getCookie(RequestContext.COOKIE_AUTH))) ||
//				Util.isEmpty(ctx.getCookie(RequestContext.COOKIE_SESSION)) ||
//				(ctx.getTimeZone()!=null && !Util.objectsEqual(ctx.getTimeZone().getID(), ctx.getCookie(RequestContext.COOKIE_TIMEZONE))))
			{
				Map<String, String> cookies = new HashMap<String, String>();
				if (!Util.isEmpty(ctx.getCookie(RequestContext.COOKIE_AUTH)))
				{
					cookies.put(RequestContext.COOKIE_AUTH, ctx.getCookie(RequestContext.COOKIE_AUTH));
				}
				if (ctx.getUserID()==null)
				{
					cookies.put(RequestContext.COOKIE_AUTH, "");
				}
				if (Util.isEmpty(ctx.getCookie(RequestContext.COOKIE_SESSION)))
				{
					cookies.put(RequestContext.COOKIE_SESSION, ctx.getSessionID().toString());
				}
				if (ctx.getTimeZone()!=null && !Util.objectsEqual(ctx.getTimeZone().getID(), ctx.getCookie(RequestContext.COOKIE_TIMEZONE)))
				{
					cookies.put(RequestContext.COOKIE_TIMEZONE, ctx.getTimeZone().getID());
				}
				if (pageCookies!=null)
				{
					cookies.putAll(pageCookies);
				}
//				cookies.remove(RequestContext.COOKIE_TIMEZONE_OFFSET);
//				cookies.remove(RequestContext.COOKIE_SCREEN);
				
				// Send cookie in response		
				Iterator<String> cookieIter = cookies.keySet().iterator();
				String domainPart = Util.domainPart(ctx.getHost());
				if (domainPart.equals(ctx.getHost())==false)
				{
					domainPart = "." + domainPart;
				}
				else
				{
					domainPart = null;
				}
				while (cookieIter.hasNext())
				{
					String key = cookieIter.next();
					String val = cookies.get(key);
		
					Cookie cookie = new Cookie(key, val);
					if (domainPart!=null)
					{
						cookie.setDomain(domainPart);
					}
					cookie.setPath("/");
					if (Util.isEmpty(val))
					{
						cookie.setMaxAge(0); // Expire the cookie
					}
					else
					{
						cookie.setMaxAge((int) (Setup.getCookieExpires()));
					}
					response.addCookie(cookie);
				}
			}
		}
		
		// No index, no follow, no archive
		int xRobot = page.getXRobotFlags();
		if (xRobot!=0)
		{
			String spiderHeader = "";
			if ((xRobot & WebPage.NO_INDEX)!=0)
			{
				spiderHeader += ", noindex";
			}
			if ((xRobot & WebPage.NO_ARCHIVE)!=0)
			{
				spiderHeader += ", noarchive";
			}
			if ((xRobot & WebPage.NO_FOLLOW)!=0)
			{
				spiderHeader += ", nofollow";
			}
			if ((xRobot & WebPage.NO_SNIPPET)!=0)
			{
				spiderHeader += ", nosnippet";
			}
			response.setHeader("X-Robots-Tag", spiderHeader.substring(2));
		}
		
		// Output result
		response.setStatus(page.getStatusCode()); // Generally 200
		
		String mimeType = page.getMimeType();
		byte[] content = null;
		
		if (mimeType.equalsIgnoreCase("text/less"))
		{
			// JIT compile LESS
			String cssText = compileLESS(ctx, page.getContentAsString());
			content = cssText.getBytes("UTF-8");
			mimeType = "text/css";
		}
		else
		{
			content = page.getContent();
		}
		
//// Debug
//if (Setup.isDebug() && ctx.getChannel().equalsIgnoreCase(Channel.VOICE))
//{
//	String vxml = page.getContentAsString();
//	Debug.logln(vxml);
//}

		boolean gzipCompress = isGzipCompress(ctx, mimeType);
		int len = outputBytes(	content,
								mimeType,
								gzipCompress,
								response);
		
		long deliverDuration = System.currentTimeMillis() - startTime - renderDuration;

		// Log the event
		if (page.isLog())
		{
			LogEntryStore.log(new RenderHTMLLogEntry(HttpServletResponse.SC_OK, renderDuration, deliverDuration, len));
		}
		
		pageHitCount++;
		return true;
	}
	
	private void setCacheHeaders(HttpServletRequest request, HttpServletResponse response, boolean cacheable)
	{
		if (!cacheable)
		{
			// Never cache
			if (request.getProtocol().equals("HTTP/1.0"))
			{
				response.setHeader("Pragma", "no-cache");
				response.setDateHeader("Expires", 0);
			}
			else
			{
				response.setHeader("Cache-Control", "private, no-cache, no-store");
			}
		}
		else
		{
			// Cache for specified seconds
			if (request.getProtocol().equals("HTTP/1.0"))
			{
				response.setDateHeader("Expires", System.currentTimeMillis() + clientCacheExpires);
			}
			else
			{
				response.setHeader("Cache-Control", "max-age=" + String.valueOf(clientCacheExpires/1000L));
			}
		}
	}

	private boolean serveLESS(RequestContext ctx, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		// Get URI
		String path = Util.urlDecode(ctx.getCommand());
		if (path.toLowerCase(Locale.US).endsWith(".less")==false)
		{
			return false;
		}
		InputStream stm = instance.getServletContext().getResourceAsStream(path);
		if (stm==null)
		{
			return false;
		}

		setCacheHeaders(request, response, true);
		
		// JIT compile LESS
		String cssText = compileLESS(ctx, Util.inputStreamToString(stm, "UTF-8"));

		// Return result
		String mimeType = "text/css";
		outputString(cssText, mimeType, isGzipCompress(ctx, mimeType), response);
		return true;
	}
	
	private boolean serveFile(RequestContext ctx, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long startTime = System.currentTimeMillis();
		long renderDuration = 0;

		String path = ctx.getCommand();
		String mimeType = instance.getServletContext().getMimeType(path);
		int ratio = Math.round(ctx.getUserAgent().getPixelRatio());

		// Obtain InputStream to the resource file.
		// Must not access the file system directly because webapp can be running inside a WAR.
		InputStream stm = null;
		if (mimeType!=null && mimeType.startsWith("image/") && ratio>1)
		{
			// Look for the high-resolution image by getting the file name with X2 appended,
			// i.e. helloX2.jpg is the Retina version of hello.jpg
			int p = path.lastIndexOf(".");
			if (p<0) p = path.length();						
			stm = instance.getServletContext().getResourceAsStream(path.substring(0, p) + "X" + ratio + path.substring(p));
			
			if (stm==null)
			{
				// Look for the a high-resolution image by changing the numerical size suffix,
				// i.e. for "image-32.png", look for "image-64.png"
				int q = p;
				while (q>0 && Character.isDigit(path.charAt(q-1)))
				{
					q--;
				}
				if (q<p)
				{
					int size = Integer.parseInt(path.substring(q, p));
					stm = instance.getServletContext().getResourceAsStream(path.substring(0, q) + (size*ratio) + path.substring(p));
				}
			}
		}
		if (stm==null)
		{
			stm = instance.getServletContext().getResourceAsStream(path);
		}
		if (stm==null)
		{
			return false;
		}
		
		renderDuration = System.currentTimeMillis() - startTime;
		
		// Deliver
		setCacheHeaders(request, response, true);

		// Not need to cache. Caching implemented by Tomcat.
		int len = outputBytes(stm, 0, mimeType, isGzipCompress(ctx, mimeType), response);
		
		long deliverDuration = System.currentTimeMillis() - startTime - renderDuration;

		// Log
		LogEntryStore.log(new RenderFileLogEntry(renderDuration, deliverDuration, len));
		
		return true;
	}
		
	private boolean serveImage(RequestContext ctx, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long startTime = System.currentTimeMillis();
		long renderDuration = 0;

		String cmd = ctx.getCommand(1);
		if (cmd.equalsIgnoreCase(UrlGenerator.COMMAND_IMAGE)==false)
		{
			return false;
		}
		
		// Fetch/render image
		String uuidStr = ctx.getCommand(2);
		if (!Util.isUUID(uuidStr))
		{
			throw new PageNotFoundException();
		}
		UUID imgID = UUID.fromString(uuidStr);
		String size = ctx.getCommand(3);
		float ratio = ctx.getUserAgent().getPixelRatio();
		Image img = ImageStore.getInstance().loadAndResize(imgID, size, ratio);
		if (img==null)
		{
			throw new PageNotFoundException();
		}
		
		renderDuration = System.currentTimeMillis() - startTime;
		
		// Deliver
		setCacheHeaders(request, response, true);
		outputBytes(img.getBytes(), img.getMimeType(), false, response);

		long deliverDuration = System.currentTimeMillis() - startTime - renderDuration;

		// Log
		LogEntryStore.log(new RenderImageLogEntry(renderDuration, deliverDuration, img.getBytes().length));

		return true;
	}
	
	private static boolean isGzipCompress(RequestContext ctx, String mimeType)
	{
		if (ctx.isAcceptGzip()==false)
		{
			// Client does not support gzip
			return false;
		}
		
		if (mimeType.startsWith("image/"))
		{
			// Do not compress images
			return false;
		}
		
		if (mimeType.equals("text/css")==true)
		{
			UserAgent ua = ctx.getUserAgent();
			if (ua.isMSIE() && ua.getVersionMSIE()<7)
			{
				// IE does not handle gzipped CSS files before version 7
				return false;
			}
		}
		return true;
	}
	
	private static String compileLESS(RequestContext ctx, String lessText) throws LessException, UnsupportedEncodingException, NoSuchAlgorithmException
	{
		// Compile LESS
		if (lessEngine==null)
		{
			synchronized(lessEngineLock)
			{
				if (lessEngine==null)
				{
					lessEngine = new LessEngine();
				}
			}
		}
		
		// Calc hash of input to use for cache lookup
		String lessHash = Util.hashSHA256(lessText);
		
		// Look up in cache, and compile if needed
		String cacheKey = "less:" + lessHash;
		String cssText = (String) Cache.get(cacheKey);
		if (cssText==null)
		{
			cssText = lessEngine.compile(lessText, Setup.isDebug()==false, null);
			Cache.insert(cacheKey, cssText);
		}
		return cssText;
	}
}
