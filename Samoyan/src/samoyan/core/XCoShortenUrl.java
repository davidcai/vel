package samoyan.core;

import java.io.IOException;

public class XCoShortenUrl
{
	/**
	 * Shortens the given URL to http://x.co/AB545 format. This method will issue a web request to x.co to get the shortened URL.
	 * @param apiKey The x.co API key
	 * @param url The URL to shorten
	 * @return The shortened URL
	 * @throws IOException 
	 */
	public final static String shorten(String apiKey, String url) throws IOException
	{
		if (apiKey==null)
		{
			return url;
		}
		
		WebBrowser wb = new WebBrowser();
		wb.setUserAgent(WebBrowser.AGENT_FIREFOX);
		
		wb.get("http://api.x.co/Squeeze.svc/text/" + apiKey + "?url=" + Util.urlEncode(url));
		return wb.getContent();
	}
}
