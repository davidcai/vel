package samoyan.email;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class EmailListenerPool implements EmailListener
{
	private Queue<EmailListener> listeners = new ConcurrentLinkedQueue<EmailListener>();
	private ExecutorService executor = null;
	
	public EmailListenerPool(boolean useExecutor)
	{
		if (useExecutor)
		{
			this.executor = Executors.newCachedThreadPool();
		}
	}
	
	public void addListener(EmailListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeListener(EmailListener listener)
	{
		listeners.remove(listener);
	}

	public void removeAllListeners()
	{
		listeners.clear();
	}

	@Override
	public void onEmailReceived(final EmailMessage msg, final String trackback)
	{
		for (final EmailListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onEmailReceived(msg, trackback);
					}
				});
			}
			else
			{
				listener.onEmailReceived(msg, trackback);
			}
		}
	}

	@Override
	public void onEmailDeliveryFailure(final EmailMessage msg, final String trackback, final String failedAddress, final String diagnostic)
	{
		for (final EmailListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onEmailDeliveryFailure(msg, trackback, failedAddress, diagnostic);
					}
				});
			}
			else
			{
				listener.onEmailDeliveryFailure(msg, trackback, failedAddress, diagnostic);
			}
		}
	}

	@Override
	public void onEmailSent(final EmailMessage msg)
	{
		for (final EmailListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onEmailSent(msg);
					}
				});
			}
			else
			{
				listener.onEmailSent(msg);
			}
		}
	}

	@Override
	public void onEmailOpened(final String externalID, final Date date)
	{
		for (final EmailListener listener : listeners)
		{
			if (executor!=null)
			{
				executor.execute(new Runnable()
				{
					@Override
					public void run()
					{
						listener.onEmailOpened(externalID, date);
					}
				});
			}
			else
			{
				listener.onEmailOpened(externalID, date);
			}
		}
	}
}
