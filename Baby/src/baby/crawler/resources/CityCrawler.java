package baby.crawler.resources;

import java.util.concurrent.Callable;

import baby.crawler.CrawlExecutor;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;

public class CityCrawler implements Callable<Void>
{
	private WebBrowser wb;
	private String url;
	private String region;
	private String topic;
	private String city;
	
	public CityCrawler(WebBrowser wb, String url, String region, String topic, String city)
	{
		this.wb = wb;
		this.url = url;
		this.region = region;
		this.topic = topic;
		this.city = city;
	}

	@Override
	public Void call() throws Exception
	{
		Debug.logln("Crawl: " + this.region + " :: " + this.topic + " :: " + this.city);

		// Fetch the page
		wb.get("https://healthy.kaiserpermanente.org" + this.url);
		String html = wb.getContent();

//		// find the region
//		int p = html.indexOf("<h6>Region</h6>");
//		p = html.indexOf("<div>", p);
//		p += 5;
//		int q = html.indexOf("</div>", p);
//		
//		String region = html.substring(p, q);
//		
//		// find the city
//		p = html.indexOf(">City</span></strong>", q);
//		p = html.indexOf("<div>", p);
//		p += 5;
//		q = html.indexOf("<a", p);
//		
//		String city = html.substring(p, q).trim();
//
//		// find the topic
//		p = html.indexOf(">Health topic</span></strong>", q);
//		p = html.indexOf("<div>", p);
//		p += 5;
//		q = html.indexOf("<a", p);
//		
//		String topic = html.substring(p, q).trim();
		
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
			
			String title  = Util.htmlDecode(html.substring(p, q));
			
			// Crawl each class
			CrawlExecutor.submit(new ClassCrawler((WebBrowser) wb.clone(), url, region, topic, city, title));
		}

		// Harvest more links
		p = 0;
		while (true)
		{
			p = html.indexOf("_viewmore_link\" class=", p);
			if (p<0) break;
			
			p = html.indexOf("makeResourceCall(\"", p);
			p += 18;
			
			int q = html.indexOf("\"", p);
			
			String url  = html.substring(p, q);
			
			// Crawl additional pages
			CrawlExecutor.submit(new CityMoreCrawler((WebBrowser) wb.clone(), url, region, topic, city));
		}		
		
		return null;
	}
}
