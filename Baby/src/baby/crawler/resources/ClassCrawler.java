package baby.crawler.resources;

import java.util.Date;
import java.util.concurrent.Callable;

import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.core.WebBrowser;

public class ClassCrawler implements Callable<Void>
{
	private WebBrowser wb;
	private String url;
	private String title;
	private String region;
	private String city;
	private String topic;
	
	public ClassCrawler(WebBrowser wb, String url, String region, String topic, String city, String title)
	{
		this.wb = wb;
		this.url = url;
		this.title = title;
		this.region = region;
		this.city = city;
		this.topic = topic;
	}

	@Override
	public Void call() throws Exception
	{
		Debug.logln("Crawl: " + this.region + " :: " + this.topic + " :: " + this.city + " :: " + this.title);

		// Fetch the page
		wb.get("https://healthy.kaiserpermanente.org" + this.url);
		String html = wb.getContent();

		int p = html.indexOf("wppHeaderFragment");
		int term = html.indexOf("cmsArticleDisclaimer", p);
		
		p = html.indexOf("<p>", p);
		int q = html.indexOf("</div>", p);
		String content = html.substring(p, q);
		content += "<p><strong>Location and times: </strong></p>";
		
		while (true)
		{
			p = html.indexOf("<div dojoType=\"kpdj.Fold\" heading=\"", q);
			if (p<0 || p>term) break;
			p += 35;
			q = html.indexOf("\"", p);
			if (q<0 || q>term) break;
			
			content += "<p>";
			content += html.substring(p, q);

			p = html.indexOf("Directions and maps</a>", q);
			if (p<0 || p>term) break;
			p += 23;
			q = html.indexOf("</div>", p);
			if (q<0 || q>term) break;
			
			content += html.substring(p, q);
		}
		
		// Extract the summary
		q = content.indexOf("</p>");
		String summary = Util.getTextAbstract(Util.htmlToText(Util.htmlDecode(content.substring(3, q))), Article.MAXSIZE_SUMMARY);
		
		// Extract the section
		p = content.indexOf("<strong>Program type: </strong>");
		p += 31;
		q = content.indexOf("</p>", p);
		String section = Util.htmlDecode(content.substring(p, q));
		
		// Write the article
		Article article = ArticleStore.getInstance().openBySourceURL(this.url);
		if (article==null)
		{
			article = new Article();
		}
		article.setHTML(content);
		article.setRegion(this.region);
		article.setMedicalCenter(this.city);
		article.setSection(BabyConsts.SECTION_RESOURCE);
		article.setSubSection(section);
		article.setSourceURL(this.url);
		article.setSummary(summary);
		article.setTitle(this.title.substring(0, Math.min(Article.MAXSIZE_TITLE, this.title.length())));
		article.setUpdatedDate(new Date());
		article.setByCrawler(true);
		ArticleStore.getInstance().save(article);
		
		return null;
	}
}
