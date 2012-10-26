package samoyan.sms;

import java.io.IOException;
import java.util.Map;

import samoyan.core.Debug;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.core.WebBrowser;
import samoyan.database.Server;
import samoyan.database.ServerStore;

public class Clickatell
{
	private static String sessionID = null;
	private static long lastActivity = 0;

	public static String send(String sender, String destination, String text) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isClickatellActive()==false)
		{
			throw new IllegalStateException("Clickatell is inactive");
		}
		
		// Send SMS
		if (sender==null)
		{
			sender = fed.getClickatellSenderID();
		}
		
		if (sessionID==null || lastActivity + 10L*60L*1000L < System.currentTimeMillis())
		{
			synchronized (sessionID)
			{
				if (sessionID==null || lastActivity + 10L*60L*1000L < System.currentTimeMillis())
				{
					Map<String, String> params = new ParameterMap()
						.plus("api_id", fed.getClickatellAPIID())
						.plus("user", fed.getClickatellUser())
						.plus("password", fed.getClickatellPassword());
					
					sessionID = postHttp("auth", params);
					lastActivity = System.currentTimeMillis();
				}
			}
		}
		
		ParameterMap params = new ParameterMap()
			.plus("text", text)
			.plus("to", Util.stripCountryCodeFromPhoneNumber(destination))
			.plus("mo", "1")
			.plus("callback", "7")
			.plus("from", Util.stripCountryCodeFromPhoneNumber(sender))
			.plus("session_id", sessionID);
		
		if (text.length()>160)
		{
			params.plus("concat", "3"); // Max parts
		}
		
		// support Unicode encoding... !$!
				
		String result = postHttp("sendmsg", params);
		lastActivity = System.currentTimeMillis();
		return result;
	}
	
	private static String postHttp(String cmd, Map<String, String> params) throws IOException, SmsException
	{		
		// Prepare web browser and params
		WebBrowser wb = new WebBrowser();
		wb.setUserAgent(WebBrowser.AGENT_FIREFOX);

		// Post data
		wb.postForm("https://api.clickatell.com/http/"+cmd, params);
		String response = wb.getContent();
		Debug.logln(response);
				
		// Handle OK
		if (response.startsWith("ID:") || response.startsWith("OK:"))
		{
			return response.substring(3).trim();
		}

		// Handle exceptions
		if (response.startsWith("ERR:"))
		{
//			if (response.indexOf("003, Session ID expired")>=0)
//			{
//				sessionID = null;
//			}
			
			throw new SmsException(response.substring(4).trim());
		}
		else
		{
			throw new SmsException(response);
		}
	}
}
