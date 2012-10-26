package samoyan.sms;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SmsListenerPool implements SmsListener
{
	private Queue<SmsListener> listeners = new ConcurrentLinkedQueue<SmsListener>();
	private ExecutorService executor = null;
	
	public SmsListenerPool(boolean useExecutor)
	{
		if (useExecutor)
		{
			this.executor = Executors.newCachedThreadPool();
		}
	}
	
	public void addListener(SmsListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(SmsListener listener)
	{
		listeners.remove(listener);
	}

	public void removeAllListeners()
	{
		listeners.clear();
	}

	@Override
	public void onSmsReceived(final SmsMessage msg, final String trackback)
	{
		for (final SmsListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onSmsReceived(msg, trackback);
					}
				});
			}
			else
			{
				listener.onSmsReceived(msg, trackback);
			}
		}
	}

	@Override
	public void onSmsSent(final SmsMessage msg)
	{
		for (final SmsListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onSmsSent(msg);
					}
				});
			}
			else
			{
				listener.onSmsSent(msg);
			}
		}
	}

	@Override
	public void onSmsDeliveryFailed(final String externalID, final Date date, final String diagnostic)
	{
		for (final SmsListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onSmsDeliveryFailed(externalID, date, diagnostic);
					}
				});
			}
			else
			{
				listener.onSmsDeliveryFailed(externalID, date, diagnostic);
			}
		}
	}

	@Override
	public void onSmsDeliveryConfirmed(final String externalID, final Date date)
	{
		for (final SmsListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onSmsDeliveryConfirmed(externalID, date);
					}
				});
			}
			else
			{
				listener.onSmsDeliveryConfirmed(externalID, date);
			}
		}
	}
}
