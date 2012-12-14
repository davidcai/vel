package baby.crawler;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import baby.crawler.resources.RootCrawler;

import samoyan.core.Util;

public final class CrawlExecutor
{
	private static ExecutorService executor = null;
	private static ScheduledExecutorService delayedExecutor = null;
	private final static int NUM_THREADS = 16;
	
	public static void init()
	{
		executor = Executors.newFixedThreadPool(NUM_THREADS);
		delayedExecutor = Executors.newSingleThreadScheduledExecutor();
	}
	
	public static void term()
	{
		Util.shutdownAndAwaitTermination(delayedExecutor);
		Util.shutdownAndAwaitTermination(executor);
	}
	
	public static void submit(Callable<Void> crawler)
	{
		executor.submit(crawler);
	}
	public static void submit(final Callable<Void> crawler, long delayMillisecs)
	{
		delayedExecutor.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				executor.submit(crawler);
			}
		},
		delayMillisecs, TimeUnit.MILLISECONDS);
	}
	
	public static void crawlResources()
	{
		submit(new RootCrawler());
	}
}
