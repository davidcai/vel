package baby.crawler.resources;

import java.util.concurrent.Callable;

import baby.crawler.CrawlExecutor;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;

public class CityMoreCrawler implements Callable<Void>
{
	private WebBrowser wb;
	private String url;
	private String region;
	private String city;
	private String topic;
	
	public CityMoreCrawler(WebBrowser wb, String url, String region, String topic, String city)
	{
		this.wb = wb;
		this.url = url;
		this.region = region;
		this.city = city;
		this.topic = topic;
	}
	
	@Override
	public Void call() throws Exception
	{
		Debug.logln("Crawl: " + this.region + " :: " + this.topic + " :: " + this.city + " (more)");
		
		// Fetch the page
		wb.get(this.url);
		String html = wb.getContent();
		
		// Harvest the links
		int p = 0;
		while (true)
		{
			p = html.indexOf("href=\"/health/poc?uri=content:class-detail", p);
			if (p<0) break;
			p += 6;
			
			int q = html.indexOf("\"", p);
			
			String url  = html.substring(p, q);
			
			p = html.indexOf(">", q);
			p ++;
			q = html.indexOf("<", p);
			
			String title = Util.htmlDecode(html.substring(p, q));
			
			// Crawl each class
			CrawlExecutor.submit(new ClassCrawler((WebBrowser) wb.clone(), url, title, region, city, topic));
		}

		return null;
	}

}
