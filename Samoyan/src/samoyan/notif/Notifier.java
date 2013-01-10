package samoyan.notif;

import java.util.BitSet;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.LogEntryStore;
import samoyan.database.Notification;
import samoyan.database.NotificationStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.email.EmailListener;
import samoyan.email.EmailMessage;
import samoyan.email.EmailServer;
import samoyan.servlet.Channel;
import samoyan.sms.SmsListener;
import samoyan.sms.SmsMessage;
import samoyan.sms.SmsServer;
import samoyan.twitter.TwitterListener;
import samoyan.twitter.TwitterMessage;
import samoyan.twitter.TwitterServer;

public class Notifier implements EmailListener, SmsListener, TwitterListener
{
	private ExecutorService executor = Executors.newCachedThreadPool();
	private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
	private ScheduledFuture<?> future;
	
	private static Notifier instance = new Notifier();
	private Notifier()
	{
	}
	
	public static void init()
	{
//		if (Setup.isDebug())
//		{
//			// Will prevent unlimited tasks from being created when debugging
//			instance.executor = Executors.newFixedThreadPool(4);
//		}
		
		EmailServer.addListener(instance);
		SmsServer.addListener(instance);
		TwitterServer.addListener(instance);
		
		instance.future = instance.scheduledExecutor.scheduleWithFixedDelay(new ScheduleFutureNotifs(), 0, ScheduleFutureNotifs.INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	public static void terminate()
	{
		TwitterServer.removeListener(instance);
		SmsServer.removeListener(instance);
		EmailServer.removeListener(instance);
		
		instance.future.cancel(true);
		Util.shutdownNowAndAwaitTermination(instance.scheduledExecutor);
		Util.shutdownNowAndAwaitTermination(instance.executor);
	}
	
	private void doSend(final UUID notifID, final Date when)
	{
		long now = System.currentTimeMillis();
//		Debug.logln("Notifier.doSend: when=" + (when==null?"null":when.getTime()) + " now=" + now);
		
		if (when==null || when.getTime()<=now)
		{
			// Run now
			instance.executor.execute(new NotifRunner(notifID));
//			Debug.logln("Notifier.doSend: run now");
		}
		else
		{
			// Run later
			instance.scheduledExecutor.schedule(
				new Runnable()
				{
					@Override
					public void run()
					{
						instance.executor.execute(new NotifRunner(notifID));
					}
				},
				when.getTime()-now,
				TimeUnit.MILLISECONDS);
			
//			Debug.logln("Notifier.doSend: run later");
		}
	}
	
	private void doAction(final UUID notifID, final String action)
	{
		instance.executor.execute(new NotifRunner(notifID, action));
	}
	
	/**
	 * Sends a notification to the user according to their alert timeline. 
	 * @param recipientUserID The ID of the user to address the notification to.
	 * @param eventID An ID to attach this notification, and follow-up notifications, to. Can be <code>null</code>.
	 * @param command The command of the notification.
	 * @param params The parameters to pass to the notification page.
	 * @throws Exception
	 */
	public static void send(UUID recipientUserID, UUID eventID, String command, Map<String, String> params) throws Exception
	{
		User user = UserStore.getInstance().load(recipientUserID);
		if (user==null)
		{
			throw new NullPointerException();
		}
	
		Date now = new Date();
		if (eventID==null)
		{
			eventID = UUID.randomUUID();
		}
		
		// We need to verify that any delay (stop) is sanctioned by the admin
		Server fed = ServerStore.getInstance().loadFederation();
		BitSet timelineStops = fed.getTimelineStops();

		// Send on each channel according to timeline
		for (String channel : Channel.getPush())
		{
			if (fed.isChannelEnabled(channel)==false || user.isChannelActive(channel)==false)
			{
				continue;
			}

			BitSet timeline = user.getTimeline(channel);
			if (timeline==null)
			{
				// Use the default timeline for this channel
				timeline = fed.getTimeline(channel);
			}
			if (timeline!=null)
			{
				for (int m=0; m<timeline.size(); m++)
				{
					if (timeline.get(m) && timelineStops.get(m))
					{
						Date when = new Date(now.getTime()+m*60L*1000L);
						
						Notification notif = new Notification();
						notif.setEventID(eventID);
						notif.setUserID(recipientUserID);
						notif.setDateCreated(now);
						notif.setStatusCode(Notification.STATUS_UNSENT);
						notif.setDateStatus(when);
						notif.setChannel(channel);
						notif.setCommand(command);
						notif.setParameters(params);
						NotificationStore.getInstance().save(notif);

						// Schedule execution
						instance.doSend(notif.getID(), when);
					}
				}
			}
		}
	}
	
	/**
	 * Sends a notification to the user on a given date and channel.
	 * @param channel One of the constants defined in {@link Channel}. The channel must be enabled by the admin. The channel must be a push channel.
	 * @param when A future date to send the notification, or <code>null</code> to send now.
	 * @param recipientUserID The ID of the user to address the notification to.
	 * @param eventID An ID to attach this notification, and follow-up notifications, to. Can be <code>null</code>.
	 * @param command The command of the notification.
	 * @param params The parameters to pass to the notification page.
	 * @throws Exception
	 */
	public static void send(String channel, Date when, UUID recipientUserID, UUID eventID, String command, Map<String, String> params) throws Exception
	{
		if (Channel.isPush(channel)==false)
		{
			return;
		}
		
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isChannelEnabled(channel)==false)
		{
			return;
		}
				
		User user = UserStore.getInstance().load(recipientUserID);
		if (user==null)
		{
			throw new NullPointerException();
		}
		
		// Check that channel is active for the user
		if (user.isChannelActive(channel)==false)
		{
			return;
		}
		
		// Create the new notification
		Date now = new Date();
		if (when==null || when.before(now))
		{
			when = now;
		}

		if (eventID==null)
		{
			eventID = UUID.randomUUID();
		}
		
		Notification notif = new Notification();
		notif.setEventID(eventID);
		notif.setUserID(recipientUserID);
		notif.setDateCreated(now);
		notif.setStatusCode(Notification.STATUS_UNSENT);
		notif.setDateStatus(when);
		notif.setChannel(channel);
		notif.setCommand(command);
		notif.setParameters(params);
		NotificationStore.getInstance().save(notif);

		// Schedule execution
		instance.doSend(notif.getID(), when);
	}

	/**
	 * Sends an unsent notification.
	 * @param notifID
	 * @throws Exception
	 */
	public static void send(UUID notifID) throws Exception
	{
		Notification notif = NotificationStore.getInstance().load(notifID);
		if (notif!=null && notif.getStatusCode()==Notification.STATUS_UNSENT)
		{
			instance.doSend(notifID, notif.getDateStatus());
		}
	}
	
	// - - - - -
	// EMAIL listener
	
	@Override
	public void onEmailDeliveryFailure(EmailMessage msg, String trackback, String failedAddress, String diagnostic)
	{
		// Failure date cannot be in the future
		Date date = msg.getDate();
		if (date.getTime() > System.currentTimeMillis())
		{
			date = new Date();
		}
		
		// Mark the notification as failed
		if (!Util.isEmpty(trackback))
		{
    		try
    		{
				Notification notif = NotificationStore.getInstance().openByExternalID(trackback);
				if (notif!=null)
				{
					notif.setStatusCode(Notification.STATUS_FAILED);
					notif.setDateStatus(date);
					NotificationStore.getInstance().save(notif);
				}
    		}
    		catch (Exception e)
    		{
    			// Log the exception
    			LogEntryStore.log(e);
    		}
       }
	}
	
	@Override
	public void onEmailReceived(EmailMessage msg, String trackback)
	{
		// Load notif based on trackback info
		Notification notif = null;
		if (Util.isEmpty(trackback))
		{
			return;
		}
		
		try
		{
			notif = NotificationStore.getInstance().loadByExternalID(trackback);
		}
		catch (Exception e)
		{
			// Log the exception
			LogEntryStore.log(e);
		}
		if (notif==null)
		{
			return;
		}		
		Debug.logln("onEmailReceived NotifID: " + notif.getID());

		// Get the textual response
		String text = msg.getContent("text/plain");
		if (text==null)
		{
			text = Util.htmlToText(msg.getContent("text/html"));
		}
		Debug.logln("Content:\r\n" + text);
		if (text==null)
		{
			return;
		}
		
		// Get FROM and TO
		String fromAddress = msg.getSenderAddress();
		String toAddress = msg.getRecipientAddress();

		// Find the first line with any text in it
		StringTokenizer lines = new StringTokenizer(text, "\n");
		while (lines.hasMoreTokens())
		{
			String line = lines.nextToken().trim();
			if (line.length()==0)
			{
				// Ignore empty lines
				continue;
			}
//			if (line.startsWith(">"))
//			{
//				// Ignore quote lines
//				continue;
//			}
			
			if (line.indexOf(toAddress)>=0)
			{
				// The original TO address typically indicates we're in the quote block
				return;
			}
			if (fromAddress.endsWith("@hotmail.com") && line.startsWith("Date:"))
			{
				// Detect Hotmail quote block (English only)
				
				// Date: Fri, 8 Jun 2012 17:42:07 -0700
				// From: notifications@example.com
				// To: example@example.com
				// Subject: Hello

				return;
			}
			if (line.startsWith("-") && line.endsWith("-") && line.indexOf("Original Message")>=0)
			{
				return;
			}
			// !$! Add more custom code to detect quote blocks specific to the common email providers
			
			// Found the reply
			Debug.logln("Notifier: received '" + line + "' for notif " + notif.getID().toString());
			
			doAction(notif.getID(), line);
			return;
		}
	}

	@Override
	public void onEmailOpened(String externalID, Date date)
	{
		try
		{
			Notification notif = NotificationStore.getInstance().openByExternalID(externalID);
			if (notif!=null)
			{
				notif.setStatusCode(Notification.STATUS_DELIVERED);
				notif.setDateStatus(date);
				NotificationStore.getInstance().save(notif);
			}
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}

	@Override
	public void onEmailSent(EmailMessage msg)
	{
		// Do nothing
	}

	// - - - - -
	// SMS listener
	
	@Override
	public void onSmsSent(SmsMessage msg)
	{
		// Do nothing
	}

	@Override
	public void onSmsReceived(SmsMessage msg, String trackback)
	{
		try
		{
			Notification notif = NotificationStore.getInstance().loadByExternalID(trackback);
			if (notif!=null)
			{
				Debug.logln("onSmsReceived NotifID: " + notif.getID());
				doAction(notif.getID(), msg.getText());
			}
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}

	@Override
	public void onSmsDeliveryFailed(String externalID, Date date, String diagnostic)
	{
		try
		{
			Notification notif = NotificationStore.getInstance().openByExternalID(externalID);
			if (notif!=null)
			{
				notif.setStatusCode(Notification.STATUS_FAILED);
				notif.setDateStatus(date);
				NotificationStore.getInstance().save(notif);
			}
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}
	
	@Override
	public void onSmsDeliveryConfirmed(String externalID, Date date)
	{
		try
		{
			Notification notif = NotificationStore.getInstance().openByExternalID(externalID);
			if (notif!=null)
			{
				notif.setStatusCode(Notification.STATUS_DELIVERED);
				notif.setDateStatus(date);
				NotificationStore.getInstance().save(notif);
			}
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}

	// - - - - -
	// SMS listener
	
	@Override
	public void onTwitterSent(TwitterMessage tweetSent)
	{
		// Do nothing
	}

	@Override
	public void onTwitterReceived(TwitterMessage tweetReceived, String trackback)
	{
		try
		{
			Notification notif = NotificationStore.getInstance().loadByExternalID(trackback);
			if (notif!=null)
			{
				Debug.logln("onTwitterReceived NotifID: " + notif.getID());
				doAction(notif.getID(), tweetReceived.getText());
			}
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}

	@Override
	public void onTwitterDeliveryFailed(String externalID, Date date, String diagnostic)
	{
		try
		{
			Notification notif = NotificationStore.getInstance().openByExternalID(externalID);
			if (notif!=null)
			{
				notif.setStatusCode(Notification.STATUS_FAILED);
				notif.setDateStatus(date);
				NotificationStore.getInstance().save(notif);
			}
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
	}
}
