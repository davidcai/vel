package baby.crawler.resources;

import java.util.concurrent.Callable;

import baby.crawler.CrawlExecutor;

import samoyan.core.Debug;
import samoyan.core.ParameterMap;
import samoyan.core.WebBrowser;

class RegionCrawler implements Callable<Void>
{
	private WebBrowser wb;
	private String regionKey;
	private String region;
	
	public RegionCrawler(String regionKey, String region)
	{
		this.regionKey = regionKey;
		this.region = region;
	}
	
	@Override
	public Void call() throws Exception
	{
		Debug.logln("Crawl: " + this.region);
		
		// Fetch the root page
		WebBrowser wb = new WebBrowser();
		wb.setUserAgent(WebBrowser.AGENT_FIREFOX);
		wb.get("https://healthy.kaiserpermanente.org/health/care/consumer/health-wellness/programs-classes/classes");
		String html = wb.getContent();
		
		// Get the URL of the form
		int a = html.indexOf("action='/health/care/consumer/health-wellness/programs-classes/classes");
		a += 8;
		int b = html.indexOf("'", a);
		String regionUrl = html.substring(a, b);

		// GET the form
		wb.get("https://healthy.kaiserpermanente.org" + regionUrl, new ParameterMap("searchRegionFilter", this.regionKey));
		html = wb.getContent();
		
		// Crawl each topic
		String[] topics = {"Parenting" , "Pregnancy", "Women&#39;s health"};
		
		for (String topic : topics)
		{
			int p = html.indexOf(topic + "</a>");
			if (p<0) continue;
			int q = html.lastIndexOf("'", p);
			p = html.lastIndexOf("'", q-1);
			p++;
			
			String url = html.substring(p, q);
			
			CrawlExecutor.submit(new TopicCrawler((WebBrowser) wb.clone(), url, this.region, topic));
		}
		
		return null;
	}
}
