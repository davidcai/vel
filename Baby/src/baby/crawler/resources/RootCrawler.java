package baby.crawler.resources;

import java.util.concurrent.Callable;

import baby.crawler.CrawlExecutor;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;

public class RootCrawler implements Callable<Void>
{
	@Override
	public Void call() throws Exception
	{
		Debug.logln("Crawl: /");

		WebBrowser wb = new WebBrowser();
		wb.setUserAgent(WebBrowser.AGENT_FIREFOX);
		
		wb.get("https://healthy.kaiserpermanente.org/health/care/consumer/health-wellness/programs-classes/classes");

		String html = wb.getContent();
		
//		// Get the URL to post to
//		int a = html.indexOf("action='/health/care/consumer/health-wellness/programs-classes/classes");
//		a += 8;
//		int b = html.indexOf("'", a);
//		
//		String url = html.substring(a, b);
		
		// Get list of regions
		int start = html.indexOf("<option value='Select an area'>");
		start += 31;
		int end = html.indexOf("</select>", start);
		
		int p = start;
		while (true)
		{
			p = html.indexOf("<option value='", p);
			if (p>=end || p<0) break;
			
			p += 15;
			int q = html.indexOf("'", p);
			
			String key = html.substring(p, q);
			
			p = html.indexOf(">", q);
			p ++;
			q = html.indexOf("<", p);
			
			String desc = Util.htmlDecode(html.substring(p, q));
			
			// Crawl each region page
			CrawlExecutor.submit(new RegionCrawler(key, desc));
		}
		
		return null;
	}
}
