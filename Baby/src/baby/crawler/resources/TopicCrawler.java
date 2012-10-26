package baby.crawler.resources;

import java.util.concurrent.Callable;

import baby.crawler.CrawlExecutor;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;

public class TopicCrawler implements Callable<Void>
{
	private WebBrowser wb;
	private String url;
	private String region;
	private String topic;
	
	public TopicCrawler(WebBrowser wb, String url, String region, String topic)
	{
		this.wb = wb;
		this.url = url;
		this.region = region;
		this.topic = topic;
	}

	@Override
	public Void call() throws Exception
	{
		Debug.logln("Crawl: " + this.region + " :: " + this.topic);

		// Fetch the page
		wb.get("https://healthy.kaiserpermanente.org" + this.url);
		String html = wb.getContent();

		// Get list of cities
		int start = html.indexOf("<h6>City</h6>");
		int end = html.indexOf("<h6>Program type</h6>", start);
		
		int p = start;
		while (true)
		{
			p = html.indexOf("<a href='", p);
			if (p>=end || p<0) break;
			
			p += 9;
			int q = html.indexOf("'", p);
			
			String url = html.substring(p, q).trim();
			while (url.startsWith("_"))
			{
				url = url.substring(1);
			}
			
			p = html.indexOf(">", q);
			p ++;
			q = html.indexOf("&nbsp;", p);
			
			String city = Util.htmlDecode(html.substring(p, q));
						
			// Crawl each city page
			CrawlExecutor.submit(new CityCrawler((WebBrowser) wb.clone(), url, this.region, this.topic, city));
		}
		
		return null;
	}
}
