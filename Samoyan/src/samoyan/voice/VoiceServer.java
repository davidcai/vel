package samoyan.voice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;
import samoyan.database.AuthTokenStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;

public class VoiceServer
{
	/**
	 * 
	 * @param userID
	 * @param command
	 * @param params
	 * @return
	 * @throws Exception
	 * @see http://www.vxml.org/t_15.htm#start
	 * @see http://www.vxml.org/tokencalls.htm
	 */
	public static String startOutboundCall(UUID userID, String phoneNumber, String command, Map<String, String> params) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (!fed.isVoxeoActive())
		{
			throw new IllegalStateException("Voxeo is inactive");
		}
		
		phoneNumber = Util.stripCountryCodeFromPhoneNumber(phoneNumber);
		
		String url = null;
		String region = fed.getVoxeoRegion();
		if (region==null)
		{
			throw new NullPointerException("Voxeo region not configured");
		}
		else if (region.equalsIgnoreCase("US"))
		{
			url = "http://api.voxeo.net/SessionControl/4.5.41/VoiceXML.start";
		}
		else if (region.equalsIgnoreCase("EU"))
		{
			url = "http://api.eu.voxeo.net/SessionControl/4.5.41/VoiceXML.start";
		}
		else if (region.equalsIgnoreCase("APAC"))
		{
			url = "http://api.apac.voxeo.net/SessionControl/4.5.41/VoiceXML.start";
		}
		else
		{
			throw new NullPointerException("Unknown Voxeo region configured");
		}
		
		if (region.equalsIgnoreCase("US")==false)
		{
			// Non-US Voxeo data centers require the tel:+ prefix
			phoneNumber = "tel:+" + phoneNumber;
		}
		
		Map<String, String> webParams = new HashMap<String, String>(2);
		webParams.put("tokenid", fed.getVoxeoDialingToken());
		webParams.put("numbertodial", phoneNumber);
		if (!Util.isEmpty(fed.getVoxeoCallerID()))
		{
			webParams.put("callerid", Util.stripCountryCodeFromPhoneNumber(fed.getVoxeoCallerID()));
		}
		webParams.put(RequestContext.PARAM_COMMAND, command);
		if (params!=null)
		{
			webParams.putAll(params);
		}
		webParams.put(RequestContext.PARAM_AUTH, AuthTokenStore.getInstance().createAuthToken(userID, null, false).toString());
		webParams.put("xmloutput", "true");

		WebBrowser web = new WebBrowser();
		web.get(url, webParams);
		
		int response = web.getResponseCode();		
		if (response!=HttpServletResponse.SC_OK)
		{
			throw new IOException(web.getResponseMessage());
		}
		
		// Extract the session ID from the result
		// <?xml version="1.0"?><response><result>success</result><tokenID>...</tokenID><sessionID>...</sessionID></response>
		String xml = web.getContent();
		Debug.logln("VoiceServer: " + xml);
		String result = extractXML(xml, "result").trim();
		if (result.equalsIgnoreCase("success"))
		{
			return extractXML(xml, "sessionID");
		}
		else if (result.equalsIgnoreCase("failure: No answer") || result.equalsIgnoreCase("failure: Busy"))
		{
			return null;
		}
		else
		{
			throw new IOException(result);
		}
	}
	
	private static String extractXML(String xml, String tag)
	{
		int p = xml.indexOf("<" + tag + ">");
		if (p<0) return null;
		p += 2 + tag.length();
		int q = xml.indexOf("<", p);
		if (q<0) return null;
		return xml.substring(p, q);
	}
}
