package samoyan.twitter;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.Trackback;
import samoyan.database.TrackbackStore;
import samoyan.servlet.Channel;
import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterServer implements TwitterListener
{
	private long countReceived = 0;
	private long countFailed = 0;
	private long countSent = 0;

	private TwitterListenerPool listeners;
	
	private Twitter pusher = null;
	private TwitterStream streamer = null;
	
	private ScheduledExecutorService executor = null;
	private String settings = null;
	
	private static TwitterServer instance = new TwitterServer();
	private TwitterServer()
	{
		this.listeners = new TwitterListenerPool(false);
	}
	
	public static void init() throws Exception
	{
		// Add self as listener
		instance.listeners.addListener(instance);

		// Connect
		instance.connect();
		
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
						.append(fed.isTwitterActive()).append("\r\n")
						.append(fed.isTwitterDebug()).append("\r\n")
						.append(fed.getTwitterOAuthAccessToken()).append("\r\n")
						.append(fed.getTwitterOAuthAccessTokenSecret()).append("\r\n")
						.append(fed.getTwitterOAuthConsumerKey()).append("\r\n")
						.append(fed.getTwitterOAuthConsumerSecret());
					String currentSettings = strBuilder.toString();
					
					if (instance.settings!=null && instance.settings.equalsIgnoreCase(currentSettings)==false)
					{
						Debug.logln("Twitter settings changed, reconnecting...");
						
						// Reconnect
						instance.disconnect();
						instance.connect();
					}
	
					instance.settings = currentSettings;
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
		Util.shutdownAndAwaitTermination(instance.executor);
		instance.executor = null;
		
		instance.disconnect();

		// Clear listeners
		instance.listeners.removeAllListeners();
	}

	public void connect() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isTwitterActive()==false)
		{
			return;
		}
		
		// Build config
		ConfigurationBuilder confBuilder = new ConfigurationBuilder();
		confBuilder.setOAuthConsumerKey(fed.getTwitterOAuthConsumerKey());
		confBuilder.setOAuthConsumerSecret(fed.getTwitterOAuthConsumerSecret());
		confBuilder.setOAuthAccessToken(fed.getTwitterOAuthAccessToken());
		confBuilder.setOAuthAccessTokenSecret(fed.getTwitterOAuthAccessTokenSecret());
		confBuilder.setDebugEnabled(fed.isTwitterDebug());
		Configuration conf = confBuilder.build(); 

		// Create Twitter instance
		TwitterFactory twitterFactory = new TwitterFactory(conf);
		this.pusher = twitterFactory.getInstance();
		
		// Create streamer
		TwitterStreamFactory streamFactory = new TwitterStreamFactory(conf);
		TwitterStream twitterStream = streamFactory.getInstance();
		
		twitterStream.addListener(new AccountStreamListener(twitterStream.getId(), instance.listeners));
		twitterStream.user(); //start tracking the owner account(e.g.: veloxicom) activity

		this.streamer = twitterStream;
	}
	
	public void disconnect()
	{
		if (this.streamer!=null)
		{
			this.streamer.cleanUp();
			this.streamer.shutdown();
			this.streamer = null;
		}
		
		if (this.pusher!=null)
		{
			this.pusher = null;
		}
	}
	
	/**
	 * Adds a listener to intercept Twitter events.
	 * @param listener This listener is invoked in the same thread as the <code>TwitterServer</code> so implementations should return quickly or otherwise
	 * create their own sub-thread. 
	 */
	public static void addListener(TwitterListener listener)
	{
		instance.listeners.addListener(listener);
	}
	
	public static void removeListener(TwitterListener listener)
	{
		instance.listeners.removeListener(listener);
	}

	/**
	 *  Send private message to the user identified with parameter <code>twitterUserName</code>
	 * @param content the message content to send
	 * @param twitterUserName the screen name of the user the message is sent to 
	 * @return the unique number to identify the message
	 * @throws TwitterInvocationException on unsuccessful call
	 */
	public static String sendPrivateMessage(TwitterMessage msg) throws Exception
	{
		if (instance.pusher==null)
		{
			return null;
		}
		
		// Trackback
		Trackback trackback = new Trackback();
		trackback.setChannel(Channel.TWITTER);
		trackback.setAddressee(msg.getDestination());
		TrackbackStore.getInstance().save(trackback); // Must save here in order to get correct roundrobin number
		String trackbackStr = " " + Trackback.PREFIX + trackback.getRoundRobinAsString();
		
		// Send Twitter private message
		try
		{
			// Make sure message doesn't go over 140 characters
			String msgText = msg.getText();
			if (msgText.length() + trackbackStr.length() > 140)
			{
				msgText = msgText.substring(0, 140 - trackbackStr.length());
			}
			msgText += trackbackStr;
			
			DirectMessage dirMsg = instance.pusher.sendDirectMessage(msg.getDestination(), msgText);
			String id = String.valueOf(dirMsg.getId());
			
			trackback.setExternalID(id);
			TrackbackStore.getInstance().save(trackback);

			instance.listeners.onTwitterSent(msg);
			return id;
		}
		catch (Exception e)
		{
			TrackbackStore.getInstance().remove(trackback.getID());
			throw e;
		}
		
// Possible alternative way to send message
//		//send as mention update (mentioning a specific user) so the status update will appear on the target user's 'mentions' board
//		StatusUpdate status = new StatusUpdate("@" + twitterUserName + " " + content);
//		status.setPossiblySensitive(true); //reserved for future use when twitter gives special handling for sensitive data
//		twitter.updateStatus(status);		
	}

	public static void follow(String userName) throws TwitterException
	{
		if (instance.pusher!=null)
		{
			try
			{
				instance.pusher.createFriendship(userName);
			}
			catch (TwitterException te)
			{
				if (te.getStatusCode() != 403) //indicates that you already follow this user
				{
					Debug.logStackTrace(te);
					throw te;
				}
			}
		}
	}

	public static boolean validateUser(String userName)
	{
		try
		{
			follow(userName);
			return true;
		}
		catch (TwitterException te)
		{
			return false;
		}
	}

	@Override
	public void onTwitterSent(TwitterMessage tweetSent)
	{
		this.countSent ++;
	}

	@Override
	public void onTwitterReceived(TwitterMessage tweetReceived, String trackback)
	{
		this.countReceived ++;
	}

	@Override
	public void onTwitterDeliveryFailed(String externalID, Date date, String diagnostic)
	{
		this.countFailed ++;
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
