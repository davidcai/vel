package samoyan.core;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

public final class WebBrowser implements Cloneable
{
	private interface Poster
	{
		public void post(OutputStream outStm) throws IOException;
	}
	
	private class StringPoster implements Poster
	{
		private String str;
		private String charset;
		
		public StringPoster(String payload, String charset)
		{
			this.str = payload;
			this.charset = charset;
		}
		
		public void post(OutputStream outStm) throws IOException
		{
			outStm.write(this.str.getBytes(this.charset));
		}
	}
	
	private class MultipartPoster implements Poster
	{
		private String charset;
		private Map<String, String> params;
		private Map<String, NamedInputStream> parts;
		private String boundary;
		
		public MultipartPoster(Map<String, String> params, Map<String, NamedInputStream> parts, String boundary, String charset)
		{
			this.params = params;
			this.parts = parts;
			this.charset = charset;
			this.boundary = boundary;
		}

		public void post(OutputStream outStm) throws IOException
		{
			// Post form parameters 
			if (this.params!=null)
			{
				StringBuffer paramString = new StringBuffer();
				
				for (String key : this.params.keySet())
				{
					String val = this.params.get(key);
					
					paramString.append("--");
					paramString.append(boundary);
					paramString.append("\r\n");
					paramString.append("Content-Disposition: form-data; name=\"");
					paramString.append(key);
					paramString.append("\"\r\n\r\n");
					paramString.append(val);
					paramString.append("\r\n");
				}
				
				outStm.write(paramString.toString().getBytes(this.charset));
			}
			
			// Post files
			if (this.parts!=null)
			{
				byte[] buffer = new byte[8192];
				for (String key : this.parts.keySet())
				{
					NamedInputStream inStm = this.parts.get(key);
					
					StringBuffer paramString = new StringBuffer();

					paramString.append("--");
					paramString.append(boundary);
					paramString.append("\r\n");
					paramString.append("Content-Disposition: form-data; name=\"");
					paramString.append(key);
					paramString.append("\"; filename=\"");
					paramString.append(inStm.getFileName());
					paramString.append("\"\r\nContent-Type: ");
					paramString.append(inStm.getMimeType());
					paramString.append("\r\n\r\n");

					outStm.write(paramString.toString().getBytes(this.charset));

					int count = 0;
					do
					{
						count = inStm.read(buffer);
						if (count>0)
						{
							outStm.write(buffer, 0, count);
						}
					}
					while (count>0);
					inStm.close();

					outStm.write('\r');
					outStm.write('\n');
				}
			}
			
			String ender = "--" + boundary + "--\r\n";
			outStm.write(ender.getBytes(this.charset));
		}
	}
		
	// ---
	
	public final static String AGENT_IE = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.0;)";
	public final static String AGENT_FIREFOX = "Mozilla/5.0 (Windows NT 6.0; rv:10.0) Gecko/20100101 Firefox/10.0";
	public final static String AGENT_SAMOYAN = "Samoyan/1.0";
		
	private String content;
	private String userAgent;
	private String user;
	private String password;
	private int connectionTimeout;
	private int readTimeout;
	private int responseCode;
	private String responseMessage;
	private String charset;
	private byte[] contentBinary;
	private String contentType;
	private Proxy proxy;
	private boolean useCache;
	private boolean followRedirects;
	private Map<String, String> headers;
	private long lastModified;
	private Map<String, Map<String, String>> cookies;
	
	public WebBrowser()
	{
		resetDefaults();
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		WebBrowser clone = (WebBrowser) super.clone();
		if (this.headers!=null)
		{
			clone.headers = new HashMap<String, String>(this.headers);
		}
		if (this.cookies!=null)
		{
			clone.cookies = new HashMap<String, Map<String, String>>(this.cookies);
		}
		
		return clone;
	}

	public void resetDefaults()
	{
		content = null;
		userAgent = AGENT_SAMOYAN;
		user = null;
		password = null;
		connectionTimeout = 60000;
		readTimeout = 60000;
		responseCode = HttpServletResponse.SC_OK;
		responseMessage = null;
		charset = "UTF-8";
		contentBinary = null;
		contentType = null;
		proxy = null;
		useCache = false;
		followRedirects = true;
		headers = new HashMap<String, String>();
		cookies = new HashMap<String, Map<String, String>>();
	}
		
	public final void setCharset(String charset)
	{
		this.charset = charset;
	}
	
	public final void setConnectionTimeout(int timeout)
	{
		this.connectionTimeout = timeout;
	}
	
	public final void setReadTimeout(int timeout)
	{
		this.readTimeout = timeout;
	}

	public final void get(String url) throws IOException
	{
		fetch("GET", url, null, null);
	}
	
	public final void get(String url, Map<String,String> params) throws IOException
	{
		// Append parameters to the URL 
		if (params!=null)
		{
			boolean first = true;
			for (String key : params.keySet())
			{
				String val = params.get(key);
				url += (first ? "?" : "&");
				first = false;
				
				url += Util.urlEncode(key, this.charset);
				url += "=";
				url += Util.urlEncode(val, this.charset);
			}
		}

		fetch("GET", url, null, null);
	}

	public final void delete(String url) throws IOException
	{
		fetch("DELETE", url, null, null);
	}
	
	public final void delete(String url, Map<String,String> params) throws IOException
	{
		// Append parameters to the URL 
		if (params!=null)
		{
			boolean first = true;
			for (String key : params.keySet())
			{
				String val = params.get(key);
				url += (first ? "?" : "&");
				first = false;
				
				url += Util.urlEncode(key, this.charset);
				url += "=";
				url += Util.urlEncode(val, this.charset);
			}
		}
		
		fetch("DELETE", url, null, null);
	}

	public final void postForm(String url, Map<String,String> params) throws IOException
	{
		StringBuffer payload = new StringBuffer();
		
		// Post form parameters 
		if (params!=null)
		{
			boolean first = true;
			for (String key : params.keySet())
			{
				String val = params.get(key);
				if (first==false)
				{
					payload.append('&');
				}
				first = false;
				
				payload.append(Util.urlEncode(key, false, this.charset));
				payload.append('=');
				payload.append(Util.urlEncode(val, false, this.charset));
			}
		}
		
		StringPoster poster = new StringPoster(payload.toString(), this.charset);
		fetch("POST", url, "application/x-www-form-urlencoded; charset=" + this.charset, poster);
	}

	public final void postMultipart(String url, Map<String,String> params, Map<String, NamedInputStream> files) throws IOException
	{
		// Generate random boundary
		Random rnd = new Random();
		long seed = rnd.nextLong();
		if (seed<Long.MAX_VALUE/2)
		{
			seed += Long.MAX_VALUE/2;
		}
		String boundary = "-------------------------" + seed;

		MultipartPoster poster = new MultipartPoster(params, files, boundary, this.charset);
		fetch("POST", url, "multipart/form-data; boundary=" + boundary, poster);
	}

	public final void postXML(String url, String xmlPayload) throws IOException
	{
		StringPoster poster = new StringPoster(xmlPayload, this.charset);
		fetch("POST", url, "text/xml; charset=" + this.charset, poster);
	}

	public final void postText(String url, String textPayload) throws IOException
	{
		StringPoster poster = new StringPoster(textPayload, this.charset);
		fetch("POST", url, "text/plain; charset=" + this.charset, poster);
	}

	public final void setBasicAuthentication(String user, String password)
	{
		this.user = user;
		this.password = password;
	}
	
	public final void setProxy(String host, int port)
	{
		this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
	}
	
	public final void clearProxy()
	{
		this.proxy = null;
	}
	
	private final void fetch(String method, String address, String mimeType, Poster poster) throws IOException
	{
//		long start = System.currentTimeMillis();
		
		HttpURLConnection cn = null;
			
		int redirectCount = 0;
		while (address != null)
		{
//			Debug.logln("WebBrowser " + method + " " + address);
			
			// Open the connection
			URL url = new URL(address);
			if (this.proxy!=null)
			{
				cn = (HttpURLConnection) url.openConnection(this.proxy);
			}
			else
			{
				cn = (HttpURLConnection) url.openConnection();
			}
			if (this.connectionTimeout>=0)
			{
				cn.setConnectTimeout(this.connectionTimeout);
			}
			if (this.readTimeout>=0)
			{
				cn.setReadTimeout(this.readTimeout);
			}
			cn.setAllowUserInteraction(false);
			cn.setInstanceFollowRedirects(false); // We do our own following

			cn.setRequestMethod(method);

			if (this.isUseCache()==false)
			{
				cn.setUseCaches(false);
				cn.setRequestProperty("Cache-Control", "no-cache");
				cn.setRequestProperty("Pragma", "no-cache");
			}
			cn.setRequestProperty("Accept-Charset", this.charset + ";q=0.7,*;q=0.7");
			
			// User agent
			if (this.userAgent!=null)
			{
				cn.setRequestProperty("User-Agent", this.userAgent);
			}
						
			// Basic authentication
			if (this.user!=null && this.password!=null)
			{
				String creds = this.user + ":" + this.password;
				String encoded = Base64.encodeBase64String(creds.getBytes());
				encoded = encoded.substring(0, encoded.length()-2);
				cn.setRequestProperty("Authorization", "Basic " + encoded);
			}
			
			// Cookies
			writeCookies(cn);
			
			// Additional headers
			for (String h : this.headers.keySet())
			{
				String v = this.headers.get(h);
				if (v!=null)
				{
					cn.setRequestProperty(h, v);
				}
			}
			
			// Mime type
			if (mimeType!=null)
			{
				cn.setRequestProperty("Content-Type", mimeType);
			}

			if (method.equals("GET") || method.equals("DELETE"))
			{
				cn.connect();
			}
			else if (method.equals("POST"))
			{
				cn.setDoOutput(true);
				
				cn.connect();

				if (poster!=null)
				{
					// Post the payload
					OutputStream out = cn.getOutputStream();
					poster.post(out);
					out.flush();
					out.close();
				}
			}
	
			// Cookies
			readCookies(cn);
			
			// Repeat for redirections
			String oldAddress = address;
			address = null;
			if (this.followRedirects &&
				(cn.getResponseCode()==HttpURLConnection.HTTP_MOVED_PERM ||
				cn.getResponseCode()==HttpURLConnection.HTTP_MOVED_TEMP))
			{
				if (redirectCount==5)
				{
					throw new IOException("Too many redirects");
				}
				address = cn.getHeaderField("Location");
				if (address.indexOf("://")<0)
				{
					if (address.startsWith("/"))
					{
						int p = oldAddress.indexOf("://");
						p = oldAddress.indexOf("/", p+3);
						if (p<0)
						{
							p = oldAddress.length();
						}
						address = oldAddress.substring(0, p) + address;
					}
					else
					{
						int p = oldAddress.indexOf("?");
						if (p<0)
						{
							p = oldAddress.length();
						}
						p = oldAddress.lastIndexOf("/");
						address = oldAddress.substring(0, p+1) + address;
					}
				}

				method = "GET";
				redirectCount++;
				continue;
			}
		}
		
		// Get response code
		this.responseCode = cn.getResponseCode();
		this.responseMessage = cn.getResponseMessage();
		
		// Get cookie
		this.contentType = cn.getHeaderField("Content-Type");
		this.lastModified = cn.getHeaderFieldDate("Last-Modified", 0);
		
		// Get the content
		byte[] buffer = new byte[8192];
		InputStream stm = null;
		try
		{
			stm = cn.getInputStream();
		}
		catch (IOException x)
		{
			stm = cn.getErrorStream();
		}
		
		if (stm!=null)
		{
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
			while (true)
			{
				int count = stm.read(buffer);
				if (count<=0) break;
				
				bais.write(buffer, 0, count);
			}
			this.contentBinary = bais.toByteArray();
			this.content = null;
		}
		else
		{
			this.contentBinary = new byte[0];
			this.content = null;
		}
				
//		long finish = System.currentTimeMillis();
//		Debug.println("URL fetched: " + this.content.length() + " bytes in " + (finish-start) + "ms");
	}
	
	private void readCookies(HttpURLConnection cn)
	{
		List<String> setCookie = cn.getHeaderFields().get("Set-Cookie");
		if (setCookie==null) return;
		
		String host = cn.getURL().getHost().toLowerCase(Locale.US);
		for (String cookie : setCookie)
		{
			String domain = crumb(cookie, "Domain");
			if (domain==null || host.toLowerCase(Locale.US).endsWith(domain)==false)
			{
				domain = host;
			}
			domain = domain.toLowerCase(Locale.US);
			
			int eq = cookie.indexOf("=");
			if (eq<0) continue;
			int sc = cookie.indexOf(";", eq);
			if (sc<0) sc = cookie.length();
			
			String name = cookie.substring(0, eq).trim();
			String val = cookie.substring(eq+1, sc).trim();
			
			Map<String, String> domainCookies = this.cookies.get(domain);
			if (domainCookies==null)
			{
				domainCookies = new HashMap<String, String>();
				this.cookies.put(domain, domainCookies);
			}
			domainCookies.put(name, val);
		}
	}

	private void writeCookies(HttpURLConnection cn)
	{
		StringBuffer cookieStr = new StringBuffer();
		
		String host = cn.getURL().getHost().toLowerCase(Locale.US);
		for (String domain : this.cookies.keySet())
		{
			if (host.endsWith(domain))
			{
				Map<String, String> domainCookies = this.cookies.get(domain);
				for (String name : domainCookies.keySet())
				{
					if (cookieStr.length()>0)
					{
						cookieStr.append(" ");
					}
					cookieStr.append(name);
					cookieStr.append("=");
					cookieStr.append(domainCookies.get(name));
					cookieStr.append(";");
				}
			}
		}
		if (cookieStr.length()>0)
		{
			cn.addRequestProperty("Cookie", cookieStr.toString());
//			Debug.logln("cookie=" + cookieStr.toString());
		}
	}
	
	public void clearCookies()
	{
		this.cookies.clear();
	}

	private String crumb(String cookie, String crumbName)
	{
		String lcCookie = cookie.toLowerCase(Locale.US);
		
		int p = lcCookie.indexOf(crumbName.toLowerCase(Locale.US));
		if (p<0) return null;
		p = cookie.indexOf("=", p);
		if (p<0) return null;
		p++;
		
		int q = cookie.indexOf(";", p);
		if (q<0)
		{
			q = cookie.length();
		}
		
		return cookie.substring(p, q);
	}
	
	public byte[] getContentBinary()
	{
		return this.contentBinary;
	}

	/**
	 * Returns the content type of the response.
	 * @return
	 */
	public String getContentType()
	{
		return this.contentType;
	}
	
	/**
	 * Returns the last-modified header of the response.
	 * @return
	 */
	public long getLastModified()
	{
		return this.lastModified;
	}

	public String getContent() throws UnsupportedEncodingException
	{
		String charset = this.charset; // Default to input charset
		
		if (this.contentType!=null)
		{
			int p = this.contentType.indexOf("charset=");
			if (p>=0)
			{
				p += 8;
				int q = this.contentType.indexOf(";", p);
				if (q<0)
				{
					q = this.contentType.length();
				}
				charset = this.contentType.substring(p, q);				
			}
		}

		return this.getContent(charset);
	}
	
	public String getContent(String charset) throws UnsupportedEncodingException
	{
		// Verify charset validity
		try
		{
			Charset.forName(charset);
		}
		catch (Exception e)
		{
			charset = this.charset;
		}

		if (this.content==null)
		{
			this.content = new String(this.contentBinary, charset);
		}
		return this.content;
	}

	public int getResponseCode()
	{
		return this.responseCode ;
	}
	
	public String getResponseMessage()
	{
		return this.responseMessage;
	}

	public String getUserAgent()
	{
		return userAgent;
	}

	public void setUserAgent(String userAgent)
	{
		this.userAgent = userAgent;
	}
	
	public void setUseCache(boolean useCache)
	{
		this.useCache = useCache;
	}

	public boolean isUseCache()
	{
		return useCache;
	}

	public boolean isFollowRedirects()
	{
		return followRedirects;
	}

	public void setFollowRedirects(boolean followRedirects)
	{
		this.followRedirects = followRedirects;
	}

	public void setHeader(String header, String value)
	{
		this.headers.put(header, value);
	}
}
