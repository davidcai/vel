package samoyan.email;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import samoyan.apps.system.EmailBeaconPngPage;
import samoyan.core.Debug;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.LogEntryStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.email.EmailMessage;
import samoyan.servlet.UrlGenerator;

public final class EmailServer implements EmailListener
{
	private long countReceived = 0;
	private long countFailed = 0;
	private long countSent = 0;

	private EmailListenerPool listeners;
	
	private ImapReader imapReader;
	private SmtpConnectionPool smtpPool;
	
	private ScheduledExecutorService executor = null;
	private String smtpSettings = null;

	private static EmailServer instance = new EmailServer();
	private EmailServer()
	{
		this.listeners = new EmailListenerPool(false);
		this.imapReader = new ImapReader(this.listeners);
		
		this.smtpPool = new SmtpConnectionPool();
	}
	
	public static void init() throws Exception
	{
		// Add self as listener
		instance.listeners.addListener(instance);
		
		// Start the IMAP reader
		instance.imapReader.connect();
		
		// Monitor connection settings
		instance.executor = Executors.newSingleThreadScheduledExecutor();
		instance.executor.scheduleWithFixedDelay(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Server fed = ServerStore.getInstance().loadFederation();

					StringBuilder strBuilder = new StringBuilder();
					strBuilder
						.append(fed.isSMTPActive()).append("\r\n")
						.append(fed.getSMTPHost()).append("\r\n")
						.append(fed.getSMTPPort()).append("\r\n")
						.append(fed.getSMTPUser()).append("\r\n")
						.append(fed.getSMTPPassword());
					String currentSettings = strBuilder.toString();
					
					if (instance.smtpSettings!=null && instance.smtpSettings.equalsIgnoreCase(currentSettings)==false)
					{
						Debug.logln("SMTP settings changed");
						
						// Clear SMTP connection pool
						instance.smtpPool.clear();
						
						// Reconnect to IMAP
						instance.imapReader.disconnect();
						instance.imapReader.connect();
					}
	
					instance.smtpSettings = currentSettings;
				}
				catch (Exception e)
				{
					Debug.logStackTrace(e);
				}
			}
		}, 0, 2, TimeUnit.SECONDS);
	}
	
	public static void terminate()
	{
		Util.shutdownNowAndAwaitTermination(instance.executor);
		instance.executor = null;

		try
		{
			instance.smtpPool.close();
		}
		catch (Exception e)
		{
			// Do not throw back message since app is exiting and connections will be closed anyways
			LogEntryStore.log(e);
		}
		
		try
		{
			instance.imapReader.disconnect();
		}
		catch (Exception e)
		{
			// Do not throw back message since app is exiting and connections will be closed anyways
			LogEntryStore.log(e);
		}

		// Clear listeners
		instance.listeners.removeAllListeners();
	}

	public static void poll() throws Exception
	{
		instance.imapReader.poll();
	}
	
	/**
	 * Adds a listener to intercept email events.
	 * @param listener This listener is invoked in the same thread as the <code>EmailServer</code> so implementations should return quickly or otherwise
	 * create their own sub-thread. 
	 */
	public static void addListener(EmailListener listener)
	{
		instance.listeners.addListener(listener);
	}
	
	public static void removeListener(EmailListener listener)
	{
		instance.listeners.removeListener(listener);
	}

	/**
	 * This method is called when a web beacon request is detected by the gateway.
	 * It should not be called by others.
	 * @param sms
	 */
	public static void beaconDetected(String externalID, Date date)
	{
		instance.listeners.onEmailOpened(externalID, date);
	}

	/**
	 * Sends an email synchronously (the call will return after the email was delivered to the remote email server). 
	 * @param msg
	 * @return <code>true</code> if the message was delivered; <code>false</code> otherwise.
	 * @throws InterruptedException
	 */
	public static String send(EmailMessage msg) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isSMTPActive()==false)
		{
			return null;
		}
		
		// Sender
		if (Util.isEmpty(msg.getSenderAddress()))
		{
			msg.setSenderAddress(fed.getSMTPUser());
			
			// !$! Should set user friendly name of application
//			if (Util.isEmpty(msg.getSenderName()))
//			{
//				email.setSenderName(???);
//			}
		}

		if (Util.isEmpty(msg.getSenderName()))
		{
			String senderAddress = msg.getSenderAddress();
			int p = senderAddress.indexOf("@");
			msg.setSenderName(senderAddress.substring(0, p));
		}

		// ReplyTo
		if (Util.isEmpty(msg.getReplyToAddress()))
		{
			if (!Util.isEmpty(fed.getIMAPUser()))
			{
				msg.setReplyToAddress(fed.getIMAPUser());
			}
			else
			{
				msg.setReplyToAddress(fed.getSMTPUser());
			}
		}
				
		if (Util.isEmpty(msg.getReplyToName()))
		{
			String replyToAddress = msg.getReplyToAddress();
			int p = replyToAddress.indexOf("@");
			msg.setReplyToName(replyToAddress.substring(0, p));
		}
		
		// Recipient
		if (Util.isEmpty(msg.getRecipientName()))
		{
			String recipientAddress = msg.getRecipientAddress();
			int p = recipientAddress.indexOf("@");
			msg.setRecipientName(recipientAddress.substring(0, p));
		}

		String trackbackID = UUID.randomUUID().toString();

		// Attach the trackback after a "+" in the ReplyTo field, i.e. recipient+trackback@example.com
		String replyTo = msg.getReplyToAddress();
		int p = replyTo.indexOf("@");
		replyTo = replyTo.substring(0, p) + "+" + trackbackID + replyTo.substring(p);
		msg.setReplyToAddress(replyTo);

// Can't change the FROM field, at least not while using gmail as email server
//		// Attach the trackback after a "+" also in the From field, i.e. recipient+trackback@example.com
//		// Necessary because some SMS providers such as Verizon ignore the ReplyTo field.
//		String sender = msg.getSenderAddress();
//		p = sender.indexOf("@");
//		sender = sender.substring(0, p) + "+" + trackbackID + sender.substring(p);
//		msg.setSenderAddress(sender);

		// Beacon
		if (fed.isUseEmailBeacon())
		{
			String html = msg.getContent("text/html");
			if (!Util.isEmpty(html))
			{
				ParameterMap beaconParams =  new ParameterMap(EmailBeaconPngPage.PARAM_EXTERNAL_ID, trackbackID);				
				html += 	"<img src=\"" +
							UrlGenerator.getPageURL(false, null, EmailBeaconPngPage.COMMAND, beaconParams) +
							"\" alt=\"\">";
				msg.setContent("text/html", html);
			}
		}
		
		// Prepare transport with listener
		TransportSession ts = instance.smtpPool.borrowObject();
		class StatusListener implements TransportListener
		{
			private int status = 0;
			public int getStatus()
			{
				return status;
			}

			@Override
			public void messageDelivered(TransportEvent arg0)
			{
				Debug.logln("SMTP Delivered");
				status = 1;
				synchronized(this)
				{
					this.notify();
				}
			}

			@Override
			public void messageNotDelivered(TransportEvent arg0)
			{
				Debug.logln("SMTP NotDelivered");
				status = -1;
				synchronized(this)
				{
					this.notify();
				}
			}

			@Override
			public void messagePartiallyDelivered(TransportEvent arg0)
			{
				Debug.logln("SMTP PartiallyDelivered");
				status = -1;
				synchronized(this)
				{
					this.notify();
				}
			}
		}
		StatusListener statusListener = new StatusListener();
		ts.getTransport().addTransportListener(statusListener);
		
		try
		{
			// Create Java mail message
			javax.mail.Message javaMsg = new MimeMessage(ts.getSession());
			msg.compose(javaMsg);
			
			Address[] recs = new Address[1];
			recs[0] = new InternetAddress(msg.getRecipientAddress(), msg.getRecipientName());
			
			// Send it
			ts.getTransport().sendMessage(javaMsg, recs);

			// Wait for callback
			if (statusListener.getStatus()==0)
			{
				synchronized (statusListener)
				{
					if (statusListener.getStatus()==0)
					{
						Debug.logln("SMTP Waiting...");
						statusListener.wait(20000L); // 20 secs
					}
				}
			}
			
			if (statusListener.getStatus()!=1)
			{
				throw new MessagingException("Failed to deliver message");
			}
			
			instance.listeners.onEmailSent(msg);
			
			return trackbackID;
		}
		finally
		{
			ts.getTransport().removeTransportListener(statusListener);
			instance.smtpPool.returnObject(ts);
		}
	}
	
	@Override
	public void onEmailReceived(EmailMessage msg, String trackback)
	{
		this.countReceived ++;
	}

	@Override
	public void onEmailDeliveryFailure(EmailMessage msg, String trackback, String failedAddress, String diagnostic)
	{
		this.countFailed ++;
	}

	@Override
	public void onEmailSent(EmailMessage msg)
	{
		this.countSent ++;
	}

	@Override
	public void onEmailOpened(String externalID, Date date)
	{
		// Do nothing
	}

	public static long getCountMessagesReceived()
	{
		return instance.countReceived;
	}

	public static long getCountDeliveryFailures()
	{
		return instance.countFailed;
	}

	public static long getCountMessagesSent()
	{
		return instance.countSent;
	}
}
