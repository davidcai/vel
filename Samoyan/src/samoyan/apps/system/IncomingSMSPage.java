package samoyan.apps.system;

import java.io.UnsupportedEncodingException;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.Trackback;
import samoyan.database.TrackbackStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.sms.SmsMessage;
import samoyan.sms.SmsServer;

public class IncomingSMSPage extends WebPage
{
	public final static String COMMAND = "incoming-sms";
	
	/**
	 * Extracts the "from", "to" and "text" content of the SMS message from the request,
	 * based on the SMS provider (OpenMarket, Clickatell, or BulkSMS). 
	 * @param paramName
	 * @return
	 * @throws Exception
	 */
	private String getParam(String paramName) throws Exception
	{
		RequestContext ctx = getContext();
		if (ctx.getUserAgent().getString().startsWith("OpenMarket"))
		{
			String xml = ctx.getParameter("xml");
			if (Util.isEmpty(xml))
			{
				xml = ctx.getPayloadAsString("UTF-8");
			}
//			Debug.logln(xml);

			if (paramName.equals("from"))
			{
				String from = extractXmlAttr(xml, "source", "address");
				if (from.startsWith("+"))
				{
					from = from.substring(1);
				}
				return from;
			}
			else if (paramName.equals("to"))
			{
				String to =  extractXmlAttr(xml, "destination", "address");
				if (to.startsWith("+"))
				{
					to = to.substring(1);
				}
				return to;
			}
			else if (paramName.equals("text"))
			{
				String dataCoding = extractXmlAttr(xml, "option", "datacoding");
				String udhi = extractXmlAttr(xml, "message", "udhi");
			
				// Simple text
				String text = extractXmlAttr(xml, "message", "text");
				if (text!=null && udhi.equalsIgnoreCase("false") && dataCoding.equalsIgnoreCase("7BIT"))
				{
					return removeDemoPrefix(text);
				}
				
				if (text==null)
				{
					text = extractXmlAttr(xml, "message", "data");
				}

				byte[] buffer = new byte[text.length()/2];
				for (int b=0; b<text.length()/2; b++)
				{
					buffer[b] = (byte) Integer.parseInt(text.substring(b*2, b*2+2), 16);
				}
				
				int s = 0;
				if (udhi.equalsIgnoreCase("true"))
				{
					// Skip the UDHI header. First byte indicate length of header
					s = (int) buffer[0];
				}
				
				try
				{
					return removeDemoPrefix(new String(buffer, s, buffer.length-s, "utf-8"));
				}
				catch (UnsupportedEncodingException e)
				{
					return null;
				}
			}
		}
		else if (isParameter("msisdn")) // BulkSMS
		{
			// See http://usa.bulksms.com/docs/eapi/reception/http_push/
			if (paramName.equals("from"))
			{
				return ctx.getParameter("sender");
			}
			else if (paramName.equals("to"))
			{
				return ctx.getParameter("msisdn");
			}
			else if (paramName.equals("text"))
			{
				// !$! Support 16bit messages. Check "dca" param for "7bit" or "16bit". Ignore "8bit".
				return ctx.getParameter("message");
			}
			else if (paramName.equals("source"))
			{
				return ctx.getParameter("source_id");
			}
		}
		else // Clickatell
		{
			return ctx.getParameter(paramName);
		}

		return null;
	}
	
	private String removeDemoPrefix(String text) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		String prefix = fed.getOpenMarketDemoPrefix();
		if (!Util.isEmpty(prefix) && text.toLowerCase().startsWith(prefix.toLowerCase()))
		{
			text = text.substring(prefix.length()).trim();
		}
		return text;
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
	
	@Override
	public void renderHTML() throws Exception
	{
		String phoneNum = getParam("from");
		String text = getParam("text").trim();

		// Detect trackback
		String src = getParam("source");
		if (src==null)
		{
			Trackback trackback = TrackbackStore.getInstance().loadByIncomingText(Channel.SMS, phoneNum, text);
			if (trackback==null)
			{
				// !$! Send error message back to sender?
				return;
			}
			src = trackback.getExternalID();
		}
		
		// Dispatch the message to the SmsListeners
		Debug.logln("Incoming SMS for trackback " + src);
		
		SmsMessage sms = new SmsMessage();
		sms.write(TrackbackStore.getInstance().cleanIncomingText(text));
		sms.setSender(phoneNum);
		sms.setDestination(getParam("to"));
		
		SmsServer.incomingMessage(sms, src);
	}

//	private void sendError(String msgText) throws Exception
//	{
//		SmsMessage sms = new SmsMessage();
//		sms.setDestination(getParam("from"));
//		sms.write(msgText);
//		SmsServer.sendMessage(sms);
//		
//		// !$! Should have an error notification page rather than raw SMS.
//	}

	@Override
	public String getMimeType() throws Exception
	{
		return "text/plain";
	}

	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
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
}
