package samoyan.sms;

import java.util.Random;

import samoyan.core.Debug;
import samoyan.core.ParameterMap;
import samoyan.core.WebBrowser;
import samoyan.database.Country;
import samoyan.database.Server;
import samoyan.database.ServerStore;

public class BulkSMS
{
	/**
	 * 
	 * @param sender
	 * @param destination
	 * @param text
	 * @return
	 * @throws Exception
	 * @see http://bulksms.com.es/docs/eapi/submission/send_sms/
	 */
	public static String send(String sender, String destination, String text) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isBulkSMSActive()==false)
		{
			throw new IllegalStateException("BulkSMS is inactive");
		}
		
		// Generate unique source ID for message
		String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuilder sourceID = new StringBuilder(16);
		for (int i=0; i<16; i++)
		{
			sourceID.append(letters.charAt(random.nextInt(letters.length())));
		}
		
		// Prepare the params
		ParameterMap params = new ParameterMap()
			.plus("username", fed.getBulkSMSUser())
			.plus("password", fed.getBulkSMSPassword())
			.plus("message", text)
			.plus("msisdn", destination)
			// .plus("sender", sender) // Sender ID cannot be set when repliable is set to 1
			.plus("dca", "7bit") // !$! support unicode (16bit)
			.plus("want_report", "1")
			.plus("source_id", sourceID.toString())
			.plus("repliable", "1")
			.plus("allow_concat_text_sms", "1")
			.plus("concat_text_sms_max_parts", "3");
		
		// Select data center URL
		String url;
		String region = fed.getBulkSMSRegion();
		if (region.equalsIgnoreCase(Country.UNITED_STATES))
		{
			url = "http://usa.bulksms.com:5567/eapi/submission/send_sms/2/2.0";
		}
		else if (region.equalsIgnoreCase(Country.UNITED_KINGDOM))
		{
			url = "http://www.bulksms.co.uk:5567/eapi/submission/send_sms/2/2.0";
		}
		else if (region.equalsIgnoreCase(Country.GERMANY))
		{
			url = "http://bulksms.de:5567/eapi/submission/send_sms/2/2.0";
		}
		else if (region.equalsIgnoreCase(Country.SPAIN))
		{
			url = "http://bulksms.com.es:5567/eapi/submission/send_sms/2/2.0";
		}
		else if (region.equalsIgnoreCase(Country.SOUTH_AFRICA))
		{
			url = "http://bulksms.2way.co.za:5567/eapi/submission/send_sms/2/2.0";
		}
		else
		{
			url = "http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0";
		}
				
		// Post
		WebBrowser wb = new WebBrowser();
		wb.setUserAgent(WebBrowser.AGENT_FIREFOX);
		wb.postForm(url, params);
		String response = wb.getContent(); // status_code|status_description|batch_id (where batch_id is optional, depending on the error)

Debug.logln(response);

		// Parse the response
		int p = response.indexOf("|");
		int q = response.indexOf("|", p+1);
		if (q<0)
		{
			q = response.length();
		}
		
		String errorCode = response.substring(0, p);
		String errorMsg = response.substring(p+1, q);

		if (errorCode.equals("0")) // Success
		{
			return sourceID.toString();
		}
		else
		{
			throw new SmsException(errorCode + ", " + errorMsg);
		}
	}	
}
