package samoyan.email;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;
import javax.mail.Message.RecipientType;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import com.sun.mail.dsn.DeliveryStatus;
import com.sun.mail.dsn.MultipartReport;
import com.sun.mail.imap.IMAPFolder;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.LogEntryStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.Trackback;
import samoyan.database.TrackbackStore;
import samoyan.servlet.Channel;

public class ImapReader implements Runnable, MessageCountListener
{
	private Session session = null;
	private Store store = null;
	private IMAPFolder inbox = null;
	private boolean idleSupported = true;
	private ScheduledExecutorService executor = null;
	private long interval = 0;
	private long lastPoll = 0; 
	private long lastReconnect = 0; 
	private EmailListener listener = null; 
	
	public ImapReader(EmailListener listener)
	{
		this.listener = listener;
	}
	
	public void connect() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isIMAPActive()==false)
		{
			return;
		}
		
		String host = fed.getIMAPHost();
		int port = fed.getIMAPPort();
		String user = fed.getIMAPUser();
		String password = fed.getIMAPPassword();
		
		Debug.logln("IMAP connecting to " + user + " at " + host + ":" + port);

		// Create the Session
		Properties props = new Properties();
		props.put("mail.imap.host", host);
		props.put("mail.imap.port", String.valueOf(port));
		props.put("mail.imap.timeout", String.valueOf(60000));
		props.put("mail.imap.connectiontimeout", String.valueOf(20000));
		
		this.session = Session.getInstance(props, null);
		this.session.setDebug(false);
		
		// Connect to store
		this.store = this.session.getStore("imaps");
		this.store.connect(host, user, password);
		
		// Get inbox
		this.inbox = (IMAPFolder) this.store.getDefaultFolder().getFolder("INBOX");
		if (this.inbox==null || this.inbox.exists()==false)
		{
			disconnect();
			throw new MessagingException("Inbox folder does not exist");
		}
		this.inbox.addMessageCountListener(this);
		this.inbox.open(Folder.READ_WRITE);			
		this.idleSupported = true; // Assume IMAP IDLE is supported, we'll detect it later if it's not

		// Schedule thread
		this.interval = fed.getIMAPPollingInterval();
		this.lastPoll = 0;
		this.executor = Executors.newSingleThreadScheduledExecutor();
		this.executor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.SECONDS);		
	}
	
	public void disconnect() throws Exception
	{
		if (this.session!=null)
		{
			Debug.logln("IMAP disconnecting from " + this.session.getProperty("mail.imap.host") + ":" + this.session.getProperty("mail.imap.port"));
		}
		
		if (this.executor!=null)
		{
			Util.shutdownAndAwaitTermination(this.executor);
			this.executor = null;
		}
		
		if (this.inbox!=null)
		{
			this.inbox.removeMessageCountListener(this);
			this.inbox.close(true);
			this.inbox = null;
		}
		
		if (this.store!=null)
		{
			this.store.close();
			this.store = null;
		}

		this.session = null;
	}
	
	public void poll()
	{
		this.lastPoll = System.currentTimeMillis();
		try
		{
			Debug.logln("IMAP polling INBOX...");
			processFolder(this.inbox);
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}
	
	/**
	 * Internal method. Do not call this method externally despite being public.
	 */
	@Override
	public void run()
	{
		// Reconnect every 2min if the connection dropped
		if (this.store==null || this.store.isConnected()==false || 
			this.inbox==null || this.inbox.isOpen()==false)
		{
			if (this.lastReconnect + 120L*1000L < System.currentTimeMillis())
			{
				try
				{
					disconnect();
				}
				catch (Exception e)
				{
					LogEntryStore.log(e);
				}
				try
				{
					connect();
				}
				catch (Exception e)
				{
					LogEntryStore.log(e);
				}
			}
			this.lastReconnect = System.currentTimeMillis();
		}
		
		// Do the initial poll on system start
		if (this.lastPoll==0)
		{
			poll();
		}
		
		// Try IMAP IDLE connection
		if (this.idleSupported)
		{
			try
			{
				Debug.logln("IMAP before IDLE");
				this.inbox.idle(true); // Wait IDLE until one event occurs, or timeout (5 minutes on gmail)
				Debug.logln("IMAP after IDLE");
				
				// Process the inbox
				poll();
			}
			catch (MessagingException me)
			{
				// No support for IDLE
				this.idleSupported = false;
				
				Debug.logStackTrace(me);
			}
		}
		
		// Default to polling
		if (!this.idleSupported && this.lastPoll + this.interval < System.currentTimeMillis())
		{
			poll();
		}
	}

	/**
	 * This callback will be called if the store supports IMAP IDLE when a new message comes in.
	 * Internal method. Do not call this method externally despite being public.
	 */
	@Override
	public void messagesAdded(MessageCountEvent mce)
	{
// Do nothing here since we poll the inbox immediately after the .idle() call
		
//		try
//		{
//			Debug.logln("IMAP processing INBOX...");
//			processFolder(this.inbox);
//		}
//		catch (Exception e)
//		{
//			LogEntryStore.log(e);
//		}
	}

	/**
	 * Internal method. Do not call this method externally despite being public.
	 */
	@Override
	public void messagesRemoved(MessageCountEvent mce)
	{
		// Do nothing
	}

	// - - - - - - - - - -
	
	protected void processFolder(Folder inbox) throws Exception
	{
		int count = inbox.getUnreadMessageCount();
		Debug.logln("IMAP " + count + " unread messages");
		if (count>0)
		{
			FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
			Message[] msgs = inbox.search(ft);
//				Message[] msgs = inbox.getMessages();
			
//			Debug.logln("IMAP " + msgs.length + " total messages");
			for (Message msg : msgs)
			{
				if (Thread.currentThread().isInterrupted()) break;

				if (msg.getFlags().contains(Flag.SEEN)==false)
				{
					processMessage(msg);
					msg.setFlag(Flag.SEEN, true);

					count--;
					if (count==0)
					{
						break;
					}
				}
			}
		}		
	}
	
	protected void printFolder(Folder inbox) throws MessagingException
	{
		StringBuffer buf = new StringBuffer();
		inbox.open(Folder.READ_ONLY);
		try
		{
			Message[] msgs = inbox.getMessages();
			buf.append("IMAP " + msgs.length + " messages\r\n");

	        for (int i = msgs.length-1; i>=0; i--)
			{
				Message msg = msgs[i];
	
				buf.append("  ");
				buf.append(i + 1);
				buf.append("\t");
	            
	            Date date = msg.getSentDate();
	            if (date!=null)
	            {
	            	buf.append(date.toString());
	            	buf.append("\t");
	            }
	
	            String from = getFromAddress(msg);
	            buf.append(from);              
	            buf.append("\t");
	
	            buf.append(msg.getSubject());              
	            buf.append("\t");
	            
	            String mime = msg.getContentType();
	            buf.append(mime);              
	            buf.append("\t");
	            
	            buf.append("\r\n");
			}
		}
		finally
		{
			inbox.close(false);
			Debug.logln(buf.toString());
		}
	}

	/**
	 * 
	 * @param msg
	 * @return <code>true</code> if this message was deleted.
	 * @throws MessagingException
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ParseException 
	 */
	protected void processMessage(Message msg) throws Exception
	{
        String mime = msg.getContentType();
        String from = getFromAddress(msg);
        String to = getToAddress(msg);
        
        // Delivery status report
        if (mime!=null && mime.startsWith("multipart/report") && mime.indexOf("delivery-status")>0)
        {
        	processReport(msg);
        	return;
        }
         
        // Gmail
		if (from!=null && from.equalsIgnoreCase("mailer-daemon@googlemail.com"))
		{
			processGmail(msg);
			return;
		}
		
		// PostCast
		if (from!=null && from.startsWith("Mailer-Daemon@"))
		{
			String[] xmailer = msg.getHeader("X-Mailer");
			if (xmailer!=null && xmailer.length>0 && xmailer[0].startsWith("PostCast"))
			{
				processPostCast(msg);
				return;
			}
		}

		// Trackback
		String trackback = null;
		int plus = to.indexOf("+");
		int at = to.indexOf("@");
		if (plus>=0 && plus<at)
		{
			// Extract the trackback from the recipient address
			trackback = to.substring(plus+1, at);
		}
		else
		{
			// If not present in the recipient address, look it up in the trackback table using the pre-@ part of the sender address
	        String text = getText(msg);
			Trackback tb = TrackbackStore.getInstance().loadByIncomingText(Channel.EMAIL, from.substring(0, from.indexOf("@")), text);
			if (tb!=null)
			{
				trackback = tb.getExternalID();
			}
		}
		
		if (trackback==null)
		{
			// !$! Send error message back to sender?
			return;
		}
		
		
		if (this.listener!=null)
		{
			this.listener.onEmailReceived(composeMessage(msg), trackback);
		}
	}
	
	protected void processReport(Message msg) throws Exception
	{
		Debug.logln("IMAP " + getSendDate(msg).toString() + " " + msg.getSubject() + " REPORT");

		MultipartReport mpReport = (MultipartReport) msg.getContent();
		DeliveryStatus deliverStatus = (DeliveryStatus) mpReport.getReport();

		int count = deliverStatus.getRecipientDSNCount();
		for (int i=0; i<count; i++)
		{
			InternetHeaders hdrs = deliverStatus.getRecipientDSN(i);
			
			String action = hdrs.getHeader("Action", "; ");
			if (action==null || action.trim().equalsIgnoreCase("failed")==false) continue;
			
			String email = hdrs.getHeader("Final-Recipient", "; ");
			if (email==null) continue;
			email = email.toLowerCase(Locale.US);
			if (email.startsWith("rfc822;"))
			{
				email = email.substring(7);
			}
			email = email.trim();
			if (email.startsWith("<")) email = email.substring(1);
			if (email.endsWith(">")) email = email.substring(0, email.length()-1);
			email = email.trim();
			if (Util.isValidEmailAddress(email)==false) continue;
			
			String diagnostic = hdrs.getHeader("Diagnostic-Code", "; ");
			if (Util.isEmpty(diagnostic))
			{
				diagnostic = hdrs.getHeader("Status", "; ");
			}
			
			processDeliveryFailure(msg, email, diagnostic);
		}
	}
	
	protected void processGmail(Message msg) throws Exception
	{
		Debug.logln("IMAP " + getSendDate(msg).toString() + " " + msg.getSubject() + " GMAIL");
		
		String subject = (String) msg.getSubject();
		if (subject==null) return;
		if (subject.equalsIgnoreCase("Delivery Status Notification (Failure)")==false) return; // Probably a delay message
		
		String content = (String) msg.getContent();
		
//		if (content.indexOf("PERM_FAILURE")<0)
//		{
//			// Probably a delay message
//			return;
//		}

		int at = content.indexOf("@");
		int nlp = content.lastIndexOf("\n", at);
		int nlq = content.indexOf("\n", at);
		String email = content.substring(nlp, nlq);
		email = email.trim();
		if (email.startsWith("<")) email = email.substring(1);
		if (email.endsWith(">")) email = email.substring(0, email.length()-1);
		email = email.trim();
		if (Util.isValidEmailAddress(email)==false) return;
				
		String diagnostic = null;
		int p = content.indexOf("was:");
		if (p<0) p = content.indexOf("FAILURE:");
		if (p>=0)
		{
			p = content.indexOf(":", p) + 1;
			int q = content.indexOf("\n", p);
			if (q>p)
			{
				diagnostic = content.substring(p, q);
				diagnostic = diagnostic.trim();
				System.out.println("    " + diagnostic);
			}
		}
		
		processDeliveryFailure(msg, email, diagnostic);
	}

	protected void processPostCast(Message msg) throws Exception
	{
		Debug.logln("IMAP " + getSendDate(msg).toString() + " " + msg.getSubject() + " POSTCAST");
		
		String subject = (String) msg.getSubject();
		if (subject==null) return;
		if (subject.startsWith("Returned mail:")==false) return;
		
		String content = (String) msg.getContent();
		
//		if (content.indexOf("PERM_FAILURE")<0)
//		{
//			// Probably a delay message
//			return;
//		}

		int at = content.indexOf("@");
		int nlp = content.lastIndexOf("\n", at);
		int nlq = content.indexOf("\n", at);
		String email = content.substring(nlp, nlq);
		email = email.trim();
		if (email.startsWith("<")) email = email.substring(1);
		if (email.endsWith(">")) email = email.substring(0, email.length()-1);
		email = email.trim();
		if (Util.isValidEmailAddress(email)==false) return;
				
		String diagnostic = null;
		int p = content.indexOf("Error:");
		if (p>=0)
		{
			p += 6;
			int q = content.indexOf("\n", p);
			if (q>p)
			{
				diagnostic = content.substring(p, q);
				diagnostic = diagnostic.trim();
				System.out.println("    " + diagnostic);
			}
		}
				
		processDeliveryFailure(msg, email, diagnostic);
	}

	protected void processDeliveryFailure(Message msg, String failedAddress, String diagnostic) throws Exception
	{
		Debug.logln("IMAP " + failedAddress + " " + (diagnostic!=null?diagnostic:""));

		// Extract the trackback from the recipient address
		String to = getToAddress(msg);
		String trackback = null;
		int plus = to.indexOf("+");
		int at = to.indexOf("@");
		if (plus>=0 && plus<at)
		{
			trackback = to.substring(plus+1, at);
		}

		if (this.listener!=null)
		{
			this.listener.onEmailDeliveryFailure(composeMessage(msg), trackback, failedAddress, diagnostic);
		}
	}
	
	protected String getFromAddress(Message msg)
	{
		try
		{
			Address[] fromArr = msg.getFrom();
			if (fromArr!=null && fromArr.length>0)
			{
				InternetAddress from = (InternetAddress) fromArr[0];
				return from.getAddress();
			}
		}
		catch (Exception exc)
		{
			// Error in address
		}
		
		return null;
	}
	
	protected String getToAddress(Message msg)
	{
		try
		{
			Address[] toArr = msg.getRecipients(RecipientType.TO);
			if (toArr!=null && toArr.length>0)
			{
				InternetAddress to = (InternetAddress) toArr[0];
				return to.getAddress();
			}
		}
		catch (Exception exc)
		{
			// Error in address
		}
		
		return null;
	}

	protected Date getSendDate(Message msg)
	{
		Date result = new Date();
		try
		{
			Date date = msg.getSentDate();
			if (date!=null && date.getTime()<System.currentTimeMillis()) // Do not allow dates in the future
			{
				result = date;
			}
		}
		catch (Exception exc)
		{
		}
		
		return result;
	}	
	
//	private String getText(Message msg) throws IOException, MessagingException
//	{
//		String html = null;
//		String plain = null;
//		
//		// Read textual content
//		Object content = msg.getContent();
//        if (content instanceof MimeMultipart)
//        {
//        	MimeMultipart mmp = (MimeMultipart) content;
//        	for (int p=0; p<mmp.getCount(); p++)
//        	{
//        		BodyPart part = mmp.getBodyPart(p);
//        		String mimeType = part.getContentType();
//        		if (mimeType.startsWith("text/html"))
//        		{
//        			html = (String) part.getContent();
//        		}
//        		else if (mimeType.startsWith("text/plain"))
//        		{
//        			plain = (String) part.getContent();
//        		}
//        	}
//        }        	
//        else
//        {
//    		String mimeType = msg.getContentType();
//    		if (mimeType.startsWith("text/html"))
//    		{
//    			html = (String) msg.getContent();
//    		}
//    		else if (mimeType.startsWith("text/plain"))
//    		{
//    			plain = (String) msg.getContent();
//    		}
//        }
//        
//        if (plain!=null)
//        {
//        	return plain.trim();
//        }
//        else if (html!=null)
//        {
//        	return Util.htmlToText(html).trim();
//        }
//        else
//        {
//        	return "";
//        }
//	}

	protected String getText(Message msg) throws IOException, MessagingException
	{			
		// Read textual content
		Object content = msg.getContent();
        if (content instanceof MimeMultipart)
        {
    		String from = getFromAddress(msg);
    		if (from.endsWith("@pm.sprint.com"))
    		{
        		// Hack: Sprint sends the content of SMS messages in an attachment
    			return getSprintText((MimeMultipart) content);
    		}
    		else
    		{
    			return getText((MimeMultipart) content);
    		}
        }

		String mimeType = msg.getContentType().toLowerCase(Locale.US);
		if (mimeType.startsWith("text/html"))
		{
			String html = (String) msg.getContent();
			return Util.htmlToText(html).trim();
		}
		else if (mimeType.startsWith("text/plain"))
		{
			String plain = (String) msg.getContent();
        	return plain.trim();
		}
		else
		{
			return "";
		}
	}

	private String getText(MimeMultipart mmp) throws MessagingException, IOException
	{
		String html = null;
		String plain = null;
		
    	for (int p=0; p<mmp.getCount(); p++)
    	{
    		BodyPart part = mmp.getBodyPart(p);
    		String mimeType = part.getContentType().toLowerCase(Locale.US);
    		if (mimeType.startsWith("text/html"))
    		{
    			html = (String) part.getContent();
    		}
    		else if (mimeType.startsWith("text/plain"))
    		{
    			plain = (String) part.getContent();
    		}
    		else if (part.getContent() instanceof MimeMultipart)
    		{
    			plain = getText((MimeMultipart) part.getContent());
    		}
    	}

        if (plain!=null)
        {
        	return plain.trim();
        }
        else if (html!=null)
        {
        	return Util.htmlToText(html).trim();
        }
        else
        {
        	return "";
        }
	}

	private String getSprintText(MimeMultipart mmp) throws MessagingException, IOException
	{
    	for (int p=0; p<mmp.getCount(); p++)
    	{
    		BodyPart part = mmp.getBodyPart(p);
    		String mimeType = part.getContentType().toLowerCase(Locale.US);
    		if (mimeType.startsWith("application/octet-stream"))
    		{
    			InputStream stm = (InputStream) part.getContent();
    			return Util.inputStreamToString(stm, "UTF-8");
    		}
    	}
    	return "";
	}
	
	protected EmailMessage composeMessage(Message msg) throws Exception
	{
		EmailMessage em = new EmailMessage();
		em.setSubject(msg.getSubject());
		
		if (msg.getFrom().length>0)
		{
			InternetAddress from = (InternetAddress) msg.getFrom()[0];
			em.setSender(from.getAddress(), from.getPersonal());
		}
		if (msg.getRecipients(RecipientType.TO).length>0)
		{
			InternetAddress to = (InternetAddress) msg.getRecipients(RecipientType.TO)[0];
			em.setRecipient(to.getAddress(), to.getPersonal());
		}
		em.setDate(msg.getSentDate());
		
		String text = getText(msg);
		em.write(text, "text/plain");
		
		return em;
	}

}
