package samoyan.servlet;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

import samoyan.core.Util;

public final class RequestContext implements Cloneable
{	
	public final static String COOKIE_AUTH = "Auth";
	public final static String COOKIE_SESSION = "Session";
	public final static String COOKIE_TIMEZONE_OFFSET = "TZOffset";
	public final static String COOKIE_TIMEZONE = "TZ";
	public final static String COOKIE_SCREEN = "Screen";
	public final static String COOKIE_OVERRIDE_SCREEN = "OverrideScreen";
	public final static String COOKIE_OVERRIDE_USER_AGENT = "OverrideUserAgent";
	
	public final static String PARAM_CHARSET = "_charset_"; // Used by Firefox and Safari to send charset. Must not be changed.
	public final static String PARAM_SESSION = "_sessionid_";
	public final static String PARAM_CHANNEL = "_channel_";
	public final static String PARAM_ACTION = "_action_";
	public final static String PARAM_COMMAND = "_command_";
	public final static String PARAM_AUTH = "_auth_";
	public final static String PARAM_SAVED = "_saved_";

	private String channel = "";
	private String ip = "";
	private UserAgent userAgent = null;
	private String host = "";
	private int port = 80;
	private String command = "";
	private HashMap<String,String> headers = null;
	private HashMap<String,String> parameters = null;
	private HashMap<String, String> cookies = null;
	private HashMap<String, File> postedFiles = null;
	private String httpMethod = "";
	private boolean ssl = false;
	private boolean acceptGzip = false;
	private long time = System.currentTimeMillis();	
	private TimeZone tz = null;
	private ArrayList<Locale> locales = null;
	private UUID requestID = null;
	private UUID userID = null;
//	private UUID authTokenID = null;
	private UUID sessionID = null;
	private byte[] payload = null;
//	private int screenWidth = 0;
//	private int screenHeight = 0;
	private String action = null;
	
	private static final ThreadLocal <RequestContext> threadLocal = new ThreadLocal<RequestContext>();

	public RequestContext()
	{
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		RequestContext clone = (RequestContext) super.clone();
		if (this.headers!=null)
		{
			clone.headers = (HashMap<String, String>) this.headers.clone();
		}
		if (this.parameters!=null)
		{
			clone.parameters = (HashMap<String, String>) this.parameters.clone();
		}
		if (this.cookies!=null)
		{
			clone.cookies = (HashMap<String, String>) this.cookies.clone();
		}
		if (this.postedFiles!=null)
		{
			clone.postedFiles = (HashMap<String, File>) this.postedFiles.clone();
		}
		if (this.locales!=null)
		{
			clone.locales = (ArrayList<Locale>) this.locales.clone();
		}
		if (this.userAgent!=null)
		{
			clone.userAgent = (UserAgent) this.userAgent.clone();
		}
		return clone;
	}

	/**
	 * Attached a <code>RequestContext</code> to the current thread. 
	 * @param ctx
	 * @return The previously set request context.
	 */
	public static RequestContext setCurrent(RequestContext ctx)
	{
		RequestContext current = threadLocal.get();
		if (ctx==null)
		{
			threadLocal.remove();
		}
		else
		{
			threadLocal.set(ctx);
		}
		return current;
	}
	/**
	 * Gets the <code>RequestContext</code> attached to the current thread.
	 * @return
	 */
	public static RequestContext getCurrent()
	{
		return threadLocal.get();
	}
	

	public byte[] getPayload()
	{
		return this.payload;
	}
	public void setPayload(byte[] payload)
	{
		this.payload = payload;
	}
	
	/**
	 * 
	 * @param charsetName Such as "UTF-8".
	 * @return
	 */
	public String getPayloadAsString(String charsetName)
	{
		if (this.payload==null)
		{
			return null;
		}
		try
		{
			return new String(this.payload, charsetName);
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}

	// - - - HEADERS - - -
	
	/**
	 * Returns the headers of the request.
	 * @return A <code>Map</code> of all headers received as part of this request.
	 */
	public Map<String,String> getHeaders()
	{
		if (this.headers==null)
		{
			this.headers =  new HashMap<String,String>();
		}
		return headers;
	}

	/**
	 * Returns the value of a specific header.
	 * @return The string value of the request header or <code>null</code> if
	 * not available.
	 */
	public String getHeader(String hdrName)
	{
		if (this.headers==null)
		{
			return null;
		}
		else
		{
			return this.headers.get(hdrName);
		}
	}
	/**
	 * Returns the value of a specific header. This method is less efficient than {@link #getHeader(String)}.
	 * @param hdrName The case-insensitive name of the header to find.
	 * @return
	 */
	public String getHeaderIgnoreCase(String hdrName)
	{
		if (this.cookies==null)
		{
			return null;
		}
		else
		{
			// First attempt to find exact case
			String result = this.headers.get(hdrName);
			if (result!=null)
			{
				return result;
			}
			
			// Iterate over keys to find case-insensitive match
			String lcHdrName = hdrName.toLowerCase(Locale.US);
			
			Iterator<String> iter = this.headers.keySet().iterator();
			while (iter.hasNext())
			{
				String n = iter.next();
				if (n.toLowerCase(Locale.US).equals(lcHdrName))
				{
					return this.headers.get(n);
				}
			}
			
			return null;
		}
	}
	public void setHeader(String hdrName, String hdrValue)
	{
		getHeaders().put(hdrName, hdrValue);
	}

	// - - - COOKIES - - -
	
	/**
	 * Returns the cookies of the request.
	 * @return A <code>Map</code> of all cookies received as part of this request.
	 */
	public Map<String,String> getCookies()
	{
		if (this.cookies==null)
		{
			this.cookies =  new HashMap<String,String>();
		}
		return cookies;
	}
	/**
	 * Returns an iterator on the names of the cookies of the request. The names are in the same case (upper or lower) as received from the HTTP request.
	 * @return An <code>Iterator</code> on the names of all cookies received as part
	 * of this request.
	 */
	public Iterator<String> getCookiesNamesIterator()
	{
		return getCookies().keySet().iterator();
	}
	/**
	 * Returns the value of a specific cookie.
	 * @param cookieName The case-sensitive name of the cookie to find.
	 * @return The string value of the request cookie or <code>null</code> if
	 * not available.
	 */
	public String getCookie(String cookieName)
	{
		if (this.cookies==null)
		{
			return null;
		}
		else
		{
			return this.cookies.get(cookieName);
		}
	}
	/**
	 * Returns the value of a specific cookie. This method is less efficient than {@link #getCookie(String)}.
	 * @param cookieName The case-insensitive name of the cookie to find.
	 * @return
	 */
	public String getCookieIgnoreCase(String cookieName)
	{
		if (this.cookies==null)
		{
			return null;
		}
		else
		{
			// First attempt to find exact case
			String result = this.cookies.get(cookieName);
			if (result!=null)
			{
				return result;
			}
			
			// Iterate over keys to find case-insensitive match
			String lcCookieName = cookieName.toLowerCase(Locale.US);
			
			Iterator<String> iter = this.cookies.keySet().iterator();
			while (iter.hasNext())
			{
				String n = iter.next();
				if (n.toLowerCase(Locale.US).equals(lcCookieName))
				{
					return this.cookies.get(n);
				}
			}
			
			return null;
		}
	}
	public void setCookie(String cookieName, String cookieValue)
	{
		getCookies().put(cookieName, cookieValue);
	}

	// - - - LOCALES - - -

	/**
	 * Returns the locales of the request.
	 * This is the internal structure held by the <code>RequestContext</code> so any changes will be visible by others accessing it.
	 * @return The locales associated with this request. May be an empty list.
	 */
	public List<Locale> getLocales()
	{
		if (this.locales==null)
		{
			this.locales =  new ArrayList<Locale>();
		}
		return this.locales;
	}
	
	// - - - PARAMETERS - - -

	/**
	 * Returns the parameters of the request.
	 * This is the internal structure held by the <code>RequestContext</code> so any changes will be visible by others accessing it.
	 * @return A <code>Map</code> of all parameters received as part of this request.
	 */
	public Map<String,String> getParameters()
	{
		if (this.parameters==null)
		{
			this.parameters =  new HashMap<String,String>();
		}
		return parameters;
	}
	
	public List<String> getParameterNamesThatStartWith(String prefix)
	{
		List<String> result = new ArrayList<String>();
		for (String p : this.parameters.keySet())
		{
			if (p.startsWith(prefix))
			{
				result.add(p);
			}
		}
		return result;
	}
	
	public List<String> getParameterNames()
	{
		return new ArrayList<String>(this.parameters.keySet());
	}
	
	/**
	 * Returns the value of a specific parameter.
	 * @return The string value of the request parameter or <code>null</code> if
	 * not available.
	 */
	public String getParameter(String paramName)
	{
		if (this.parameters==null)
		{
			return null;
		}
		else
		{
			return this.parameters.get(paramName);
		}
	}
	public void setParameter(String paramName, String paramValue)
	{
		getParameters().put(paramName, paramValue);
	}

	// - - - POSTED FILES - - -
	
	/**
	 * Returns the posted files of the request.
	 * This is the internal structure held by the <code>RequestContext</code> so any changes will be visible by others accessing it.
	 * @return A <code>Map</code> of all files received as part
	 * of this request.
	 */
	public Map<String, File> getPostedFiles()
	{
		if (this.postedFiles==null)
		{
			this.postedFiles = new HashMap<String, File>();
		}
		return this.postedFiles;
	}
	
	/**
	 * Returns an iterator on the names of the posted files of the request.
	 * @return An <code>Iterator</code> on the names of all posted files received as part
	 * of this request.
	 */
	public Iterator<String> getPostedFileNamesIterator()
	{
		return getPostedFiles().keySet().iterator();
	}

	/**
	 * Returns the value of a specific parameter.
	 * @return The temporary <code>File</code> where the data of the request parameter is stored,
	 * or <code>null</code> if not available.
	 */
	public File getPostedFile(String paramName)
	{
		if (this.postedFiles==null)
		{
			return null;
		}
		return this.postedFiles.get(paramName);
	}
	public void setPostedFile(String paramName, File file)
	{
		getPostedFiles().put(paramName, file);
	}
	
	// - - -
	
	/**
	 * Returns the IPv4 or IPv6 address of the request originator, if available.
	 * @return The IP address in <code>String</code> form, e.g. "127.0.0.1" or "25:1:0:0:0:0:abcd:0"
	 */
	public String getIPAddress()
	{
		return this.ip;
	}
	public void setIPAddress(String ipAddress)
	{
		this.ip = ipAddress;
	}
	
	public UserAgent getUserAgent()
	{
		if (this.userAgent==null)
		{
			this.userAgent = UserAgent.createInstance("", 0, 0, 1);
		}
		return this.userAgent;
	}
	public void setUserAgent(String userAgentString, int screenWidth, int screenHeight, int pixelRatio)
	{
		this.userAgent = UserAgent.createInstance(userAgentString, screenWidth, screenHeight, pixelRatio);
	}
	
	public String getHost()
	{
		return this.host;
	}
	public void setHost(String host)
	{
		this.host = host;
	}
	
	public int getPort()
	{
		return this.port;
	}
	public void setPort(int port)
	{
		this.port = port;
	}

	/**
	 * The system time when the request was received.
	 * @return
	 */
	public long getTime()
	{
		return this.time;
	}
	public void setTime(long time)
	{
		this.time = time;
	}

	/**
	 * A unique ID of this request.
	 * @return A <code>UUID</code>.
	 */
	public UUID getID()
	{
		if (this.requestID==null)
		{
			this.requestID = UUID.randomUUID();
		}
		return this.requestID;
	}

	/**
	 * The user ID authenticated with this request, or <code>null</code>.
	 * @return
	 */
	public UUID getUserID()
	{
		return this.userID;
	}
	public void setUserID(UUID userID)
	{
		this.userID = userID;
	}
	
//	/**
//	 * The auth token Id of this request, or <code>null</code>.
//	 * @return
//	 */
//	public UUID getAuthTokenID()
//	{
//		return this.authTokenID;
//	}
//	public void setAuthTokenID(UUID authTokenID)
//	{
//		this.authTokenID = authTokenID;
//	}

	public UUID getSessionID()
	{
		return this.sessionID;
	}
	public void setSessionID(UUID sessionID)
	{
		this.sessionID = sessionID;
	}

	public String getMethod()
	{
		return this.httpMethod;
	}
	public void setMethod(String httpMethod)
	{
		this.httpMethod = httpMethod;
	}

	public String getChannel()
	{
		return this.channel;
	}
	public void setChannel(String channel)
	{
		this.channel = channel;
	}
	
	public boolean isSecureSocket()
	{
		return this.ssl;
	}
	public void setSecureSocket(boolean ssl)
	{
		this.ssl = ssl;
	}
	
	public String getCommand()
	{
		return this.command;
	}
	public void setCommand(String cmd)
	{
		this.command = cmd;
	}
	/**
	 * Returns the n-th part of the URI (1-based). For example, in "example/folder/test.html", <code>getCommand(2)</code> will be "folder" and
	 * <code>getCommand(4)</code> will be <code>null</code>.
	 * @param count
	 * @return
	 */
	public String getCommand(int count)
	{
		if (this.command==null)
		{
			// Shouldn't happen
			return null;
		}
		
		int q = 0;
		int p = -1;
		for (int i=1; i<=count; i++)
		{
			q = p+1;
			p = this.command.indexOf("/", q);
			if (p<0)
			{
				if (i==count)
				{
					return this.command.substring(q);
				}
				else
				{
					return null;
				}
			}
		}
		
		return this.command.substring(q, p);
	}
			
	/**
	 * Gets the time zone of the request.
	 * @return The <code>TimeZone<code> of the request.
	 * May be <code>null</code> if not indicated by the request.
	 */
	public TimeZone getTimeZone()
	{
		return this.tz;
	}
	public void setTimeZone(TimeZone tz)
	{
		this.tz = tz;
	}
	
//	/**
//	 * Returns the user's screen width, or 0 if cannot be determined.
//	 * @return
//	 */
//	public int getScreenWidth()
//	{
//		return this.screenWidth;
//	}
//	public void setScreenWidth(int width)
//	{
//		this.screenWidth = width;
//	}
//	
//	/**
//	 * Returns the user's screen height, or 0 if cannot be determined.
//	 * @return
//	 */
//	public int getScreenHeight()
//	{
//		return this.screenHeight;
//	}
//	public void setScreenHeight(int height)
//	{
//		this.screenHeight = height;
//	}

	/**
	 * Returns <code>true</code> if the caller accepts gZip compression.
	 * @return
	 */
	public boolean isAcceptGzip()
	{
		return this.acceptGzip;
	}
	public void setAcceptGzip(boolean gzip)
	{
		this.acceptGzip = gzip;
	}
	
	/**
	 * Calculates a unique hash code for this context based on identifying request headers (such as user agent, accept, accept-encoding, etc.)
	 * @return
	 */
	public String getIdentifyingHeadersSHA256()
	{
		StringBuilder str = new StringBuilder();
		if (this.headers!=null)
		{
			for (String h : this.headers.keySet())
			{
				if (h.equalsIgnoreCase("user-agent") ||
					h.equalsIgnoreCase("ua-cpu") ||	
					h.toLowerCase(Locale.US).startsWith("accept"))
				{
					str.append(h);
					str.append("=");
					str.append(this.headers.get(h));
					str.append("\r\n");
				}
			}
		}
		return Util.hashSHA256(str.toString());
	}
	
	public String toString()
	{
		StringBuffer debug = new StringBuffer();
		debug.append(this.getMethod().toUpperCase(Locale.US));
		debug.append(" ");
		debug.append(this.isSecureSocket()?"https:":"http:");
		debug.append("//");
		debug.append(this.getHost());
		if (this.getPort()!=80 && this.getPort()!=443)
		{
			debug.append(":");
			debug.append(this.getPort());
		}
		debug.append(Controller.getServletPath());
		debug.append("/");
		debug.append(this.command);
		debug.append("\r\n");
	
		debug.append(" IP: " + this.ip + "\r\n");

		if (this.userID!=null)
		{
			debug.append(" User: " + this.userID + "\r\n");
		}
		
		if (this.headers!=null && this.headers.size()>0)
		{
			debug.append(" Headers:\r\n");
			Iterator<String> iter = this.headers.keySet().iterator();
			while (iter.hasNext())
			{
				String hdr = (String) iter.next();
				String val = this.getHeader(hdr);
				if (val.length()>20480)
				{
					val = val.substring(0, 20480) + "...";
				}
				debug.append("  " + hdr + "=" + val + "\r\n");
			}
		}
		
		if (this.parameters!=null && this.parameters.size()>0)
		{
			debug.append(" Parameters:\r\n");
			Iterator<String> iter = this.parameters.keySet().iterator();
			while (iter.hasNext())
			{
				String prm = (String) iter.next();
				String val = this.getParameter(prm);
				if (val!=null && val.length()>20480)
				{
					val = val.substring(0, 20480) + "...";
				}
				debug.append("  " + prm + "=" + val + "\r\n");
			}
		}
		
		return debug.toString();
	}
}
