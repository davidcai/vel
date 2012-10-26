package samoyan.servlet;

import java.util.Map;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.core.XCoShortenUrl;
import samoyan.database.Image;
import samoyan.database.Server;
import samoyan.database.ServerStore;

public class UrlGenerator
{
	public final static String COMMAND_IMAGE = "image";
	public final static String COMMAND_RESOURCE = "res";
	public final static String COMMAND_SETUP = "setup";
	
	public final static String getImageURL(boolean ssl, String host, Image img, String sizeSpec, String imgTitle)
	{
		if (img==null || img.getID()==null)
		{
			return null;
		}
		
		StringBuilder cmd = new StringBuilder();
		cmd.append(COMMAND_IMAGE);
		cmd.append("/");
		cmd.append(img.getID().toString());
		cmd.append("/");
		cmd.append(sizeSpec);
		cmd.append("/");
		if (!Util.isEmpty(imgTitle))
		{
			cmd.append(Util.urlSafe(imgTitle));
		}
		else
		{
			cmd.append("image");
		}
		if (img.getMimeType().equalsIgnoreCase("image/png"))
		{
			cmd.append(".png");
		}
		else if (img.getMimeType().equalsIgnoreCase("image/jpeg"))
		{
			cmd.append(".jpg");
		}
		
		return getPageURL(ssl, host, cmd.toString(), new ParameterMap("v", String.valueOf(img.getVersion())));
	}

	public final static String getResourceURL(boolean ssl, String host, String resourceFileName)
	{
		return getPageURL(ssl, host, COMMAND_RESOURCE + "/" + resourceFileName, null);
	}
	
	public final static String getPageURL(boolean ssl, String host, String command, Map<String, String> params)
	{
		if (Setup.isSSL()==false)
		{
			ssl = false;
		}
		if (host==null)
		{
			host = Setup.getHost();
		}
		int port = (ssl? Setup.getPortSSL() : Setup.getPort());
		
		StringBuffer buf = new StringBuffer(256);

		buf.append(ssl?"https":"http");
		buf.append("://");
		buf.append(host);
		if (port!=(ssl? 443 : 80))
		{
			buf.append(":");
			buf.append(port);
		}
		buf.append(Controller.getServletPath());
		buf.append("/");
		buf.append(command);

		if (params!=null)
		{
			boolean first = true;
			for (String k : params.keySet())
			{
				String v = params.get(k);
				if (v==null) continue;
				
				if (first)
				{
					buf.append("?");
					first = false;
				}
				else
				{
					buf.append("&");
				}
				buf.append(Util.urlEncode(k));
				buf.append("=");
				buf.append(Util.urlEncode(v));
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Shortens the URL using the x.co service.
	 * @param url The fully qualified URL, including any parameters.
	 * @return The shortened URL (e.g. http://x.com/1FG6), or the incoming URL on any problem.
	 */
	public static String shortenURL(String url)
	{
		try
		{
			Server fed = ServerStore.getInstance().loadFederation();
			if (!Util.isEmpty(fed.getXCoAPIKey()))
			{
				url = XCoShortenUrl.shorten(fed.getXCoAPIKey(), url);
			}
		}
		catch (Exception e)
		{
			// Ignore, just return the incoming URL
		}
		return url;
	}
}
