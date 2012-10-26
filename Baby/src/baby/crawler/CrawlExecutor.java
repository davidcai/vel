package baby.crawler;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import baby.crawler.resources.RootCrawler;

import samoyan.core.Util;

public final class CrawlExecutor
{
	private static ExecutorService executor = null;
	private final static int NUM_THREADS = 16;
	
	public static void init()
	{
		executor = Executors.newFixedThreadPool(NUM_THREADS);
	}
	
	public static void term()
	{
		Util.shutdownAndAwaitTermination(executor);
	}
	
	public static Future<Void> submit(Callable<Void> crawler)
	{
		return executor.submit(crawler);
	}
	
	public static void crawlResources()
	{
		submit(new RootCrawler());
	}
}
