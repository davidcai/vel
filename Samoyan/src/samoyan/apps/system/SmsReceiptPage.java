package samoyan.apps.system;

import java.text.DateFormat;
import java.util.Date;

import samoyan.core.DateFormatEx;
import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.sms.SmsServer;

public class SmsReceiptPage extends WebPage
{
	public final static String COMMAND = "sms-receipt";

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		if (ctx.getUserAgent().getString().startsWith("OpenMarket"))
		{
			String xml = ctx.getPayloadAsString("UTF-8");
			if (Util.isEmpty(xml))
			{
				xml = getParameterString("xml");
			}
			Debug.logln(xml);

			String ticketID = extractXmlAttr(xml, "deliveryReceipt", "ticketId");
			
			String state = extractXmlAttr(xml, "state", "id");
			// 1. Created
			// 2. Submitted to carrier
			// 3. Delivered to carrier
			//    Note: State ID 3 represents the final successful state of SMS message delivery
			//    for carriers which do not support handset delivery status receipts.
			// 5. Delivered to destination address by carrier
			//    Note: State ID 5 represents the final successful state of SMS message delivery
			//    for carriers which do support handset delivery status receipts.
			// 6. Delivery to carrier not attempted
			// 7. Delivery to carrier failed
			// 8. Delivery to destination address failed
			
			String dateStr = extractXmlAttr(xml, "message", "deliveryDate");
			DateFormat df = DateFormatEx.getISO8601MillisInstance();
			Date date = df.parse(dateStr);
			
			if (state.equals("3") || state.equals("5"))
			{
				// Dispatch the event
				SmsServer.deliveryConfirmed(ticketID, date);
			}
			else if (state.equals("6") || state.equals("7") || state.equals("8"))
			{
				// Dispatch the event
				SmsServer.deliveryFailed(ticketID, date, extractXmlAttr(xml, "response", "description"));
			}
		}
		else if (isParameter("msisdn")) // BulkSMS
		{
			// See http://usa.bulksms.com/docs/eapi/status_reports/http_push/
			String msgID = getParameterString("source_id");
			if (Util.isEmpty(msgID))
			{
				return;
			}
			
			String status = getParameterString("status");
			String statusString = "";
			final String[] statusDesc = {
					"11", "Delivered to mobile",
					"22", "Internal fatal error",
					"23", "Authentication failure",
					"24", "Data validation failed",
					"25", "You do not have sufficient credits",
					"26", "Upstream credits not available",
					"27", "You have exceeded your daily quota",
					"28", "Upstream quota exceeded",
					"29", "Message sending cancelled",
					"31", "Unroutable",
					"32", "Blocked (probably because of a recipient's complaint against you)",
					"33", "Failed: censored",
					"50", "Delivery failed - generic failure",
					"51", "Delivery to phone failed",
					"52", "Delivery to network failed",
					"53", "Message expired",
					"54", "Failed on remote network",
					"56", "Failed: remotely censored",
					"57", "Failed due to fault on handset (e.g. SIM full)",
					"64", "Queued for retry after temporary failure delivering, due to fault on handset (transient)",
					"70", "Unknown upstream status"
			};
			for (int i=0; i<statusDesc.length; i+=2)
			{
				if (statusDesc[i].equalsIgnoreCase(status))
				{
					statusString = statusDesc[i+1];
					break;
				}
			}

			Date date = new Date();
			String completed = getParameterString("completed_time");
			if (!Util.isEmpty(completed))
			{
				// time that message delivery was completed (only available for some networks).
				// If delivery was successful, this is the time that the message reached the mobile.
				// If not available, the parameter will not be sent at all. The format is yy-MM-dd HH:mm:ss (24 hour clock).
				
				// !$! We ignore this date because it lacks timezone information
			}
			
			// Dispatch the event
			if (status.equals("11"))
			{
				// Dispatch the event
				SmsServer.deliveryConfirmed(msgID, date);
			}
			else
			{
				// Dispatch the event
				SmsServer.deliveryFailed(msgID, date, statusString);
			}
		}
		else // Clickatell
		{
			String msgID = getParameterString("apiMsgId");
			
			String status = getParameterString("status");			
			String statusString = "";
			try
			{
				final String[] statusDesc = {
					"",
					"Message unknown", // 001
					"Message queued", // 002
					"Delivered to gateway", // 003
					"Received by recipient", // 004
					"Error with message", // 005
					"User cancelled message delivery", // 006
					"Error delivering message", // 007
					"OK", // 008
					"Routing error", // 009
					"Message expired", // 010
					"Message queued for later delivery", // 011
					"Out of credit", // 012
					"",
					"Maximum MT limit exceeded" // 014
				};
				statusString = statusDesc[Integer.parseInt(status)];
			}
			catch (Throwable t)
			{
				// Ignore
			}
			
			Long timeStamp = getParameterLong("timestamp");
			Date date = new Date(timeStamp);
			
			// Dispatch the event
			if (status.equals("003") || status.equals("004"))
			{
				// Dispatch the event
				SmsServer.deliveryConfirmed(msgID, date);
			}
			else if (status.equals("005") || status.equals("012") || status.equals("009") || status.equals("010"))
			{
				// Dispatch the event
				SmsServer.deliveryFailed(msgID, date, statusString);
			}
		}
	}
	
	@Override
	public boolean isAuthorized() throws Exception
	{
		return true;
	}

	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
	}

	@Override
	public String getMimeType() throws Exception
	{
		return "text/plain";
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}
	
	@Override
	public boolean isProtectXSS() throws Exception
	{
		return false;
	}

	private String extractXmlAttr(String xml, String tag, String attr)
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
