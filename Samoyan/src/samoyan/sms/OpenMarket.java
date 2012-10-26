package samoyan.sms;

import samoyan.apps.system.SmsReceiptPage;
import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.UrlGenerator;

public class OpenMarket
{	
	public static String send(String sender, String destination, String text) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isOpenMarketActive()==false)
		{
			throw new IllegalStateException("OpenMarket is inactive");
		}
		
		// Prepare the XML
		StringBuffer xml = new StringBuffer();
		
		xml.append("<?xml version=\"1.0\" ?>");
		xml.append("<request version=\"3.0\" protocol=\"wmp\" type=\"submit\">");
		
		xml.append("<delivery receipt_requested=\"true\" url=\"");
		xml.append(UrlGenerator.getPageURL(true, null, SmsReceiptPage.COMMAND, null));
		xml.append("\"/>");
		
		xml.append("<user agent=\"Java/SMS/1.0.0\" />");
		
		xml.append("<account id=\"");
		xml.append(fed.getOpenMarketUser());
		xml.append("\" password=\"");
		xml.append(fed.getOpenMarketPassword());
		xml.append("\" />");
		
		xml.append("<option charge_type=\"0\" program_id=\"");
		xml.append(fed.getOpenMarketProgramID());
		xml.append("\" />");
		
		if (sender==null)
		{
			sender = fed.getOpenMarketSenderID();
		}
		sender = Util.stripCountryCodeFromPhoneNumber(sender);
		if (sender.length()<=6)
		{
			// Short code
			xml.append("<source ton=\"3\" address=\"");
			xml.append(sender);
			xml.append("\" />");
		}
		else
		{
			// Regular phone number
			xml.append("<source ton=\"1\" address=\"");
			xml.append(sender);
			xml.append("\" />");
		}
		
		destination = Util.stripCountryCodeFromPhoneNumber(destination);
		xml.append("<destination ton=\"1\" address=\"");
		xml.append(destination);
		xml.append("\" />");
		
		// Support 8BIT encoding for high characters !$!
		xml.append("<message text=\"");
		xml.append(text);
		xml.append("\" />");
		
		xml.append("</request>");
		
		// Post the XML
		WebBrowser wb = new WebBrowser();
		wb.setUserAgent(WebBrowser.AGENT_FIREFOX);

Debug.logln(xml.toString());
		
		wb.postXML("http://smsc-02.openmarket.com/wmp", xml.toString()); // !$! round robin between servers, considering failures
		String response = wb.getContent();

Debug.logln(response);
		
		String errorCode = extractXmlAttr(response, "error", "code");
		String errorMsg = extractXmlAttr(response, "error", "description");
		String ticketID = extractXmlAttr(response, "ticket", "id");

		if (errorCode.equals("2")) // Success
		{
			return ticketID;
		}
		else
		{
			throw new SmsException(errorCode + ", " + errorMsg);
		}
	}
	
	private static String extractXmlAttr(String xml, String tag, String attr)
	{
		int p = xml.indexOf("<" + tag);
		if (p<0) return null;
		p += tag.length() + 1;
		int q = xml.indexOf(">", p);
		if (q<0) return null;
		
		int m = xml.indexOf(attr + "=\"", p);
		if (m<0 || m>=q) return null;
		m += attr.length() + 2;
		int n = xml.indexOf("\"", m);
		if (n<0 || n>=q) return null;
		
		return xml.substring(m, n);
	}
}
