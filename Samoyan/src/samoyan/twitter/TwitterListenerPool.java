package samoyan.twitter;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TwitterListenerPool implements TwitterListener
{
	private Queue<TwitterListener> listeners = new ConcurrentLinkedQueue<TwitterListener>();
	private ExecutorService executor = null;
	
	public TwitterListenerPool(boolean useExecutor)
	{
		if (useExecutor)
		{
			this.executor = Executors.newCachedThreadPool();
		}
	}
	
	public void addListener(TwitterListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(TwitterListener listener)
	{
		listeners.remove(listener);
	}

	public void removeAllListeners()
	{
		listeners.clear();
	}


	@Override
	public void onTwitterSent(final TwitterMessage tweetSent)
	{
		for (final TwitterListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onTwitterSent(tweetSent);
					}
				});
			}
			else
			{
				listener.onTwitterSent(tweetSent);
			}
		}
	}

	@Override
	public void onTwitterReceived(final TwitterMessage tweetReceived, final String trackback)
	{
		for (final TwitterListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onTwitterReceived(tweetReceived, trackback);
					}
				});
			}
			else
			{
				listener.onTwitterReceived(tweetReceived, trackback);
			}
		}
	}

	@Override
	public void onTwitterDeliveryFailed(final String externalID, final Date date, final String diagnostic)
	{
		for (final TwitterListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onTwitterDeliveryFailed(externalID, date, diagnostic);
					}
				});
			}
			else
			{
				listener.onTwitterDeliveryFailed(externalID, date, diagnostic);
			}
		}
	}
}
