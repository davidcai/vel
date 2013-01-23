package baby.controls;

import java.util.List;
import java.util.UUID;

import baby.database.Article;
import baby.database.ArticleStore;
import baby.pages.info.ViewArticlePage;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.servlet.WebPage;

public class ArticleListControl
{
	private WebPage out;
	private List<UUID> articleIDs;
	private boolean showImages;
	private boolean showSummary;
	private boolean showSubSection;
	private boolean showRegion;
	
	public ArticleListControl(WebPage out, List<UUID> articleIDs)
	{
		this.out = out;
		this.articleIDs = articleIDs;
		this.showImages = true;
		this.showSummary = true;
		this.showSubSection = true;
		this.showRegion = true;
	}
	
	public ArticleListControl showImages(boolean b)
	{
		this.showImages = b;
		return this;
	}
	public ArticleListControl showSummary(boolean b)
	{
		this.showSummary = b;
		return this;
	}
	public ArticleListControl showSubSection(boolean b)
	{
		this.showSubSection = b;
		return this;
	}
	public ArticleListControl showRegion(boolean b)
	{
		this.showRegion = b;
		return this;
	}
	
	public void render() throws Exception
	{
		out.write("<table width=\"100%\" class=ArticleList>");
		if (this.showImages)
		{
			out.write("<col width=\"1%\"><col width=\"99%\">");
		}
		for (int i=0; i<articleIDs.size(); i++)
		{
			Article article = ArticleStore.getInstance().load(articleIDs.get(i));
			String url = out.getPageURL(ViewArticlePage.COMMAND, new ParameterMap(ViewArticlePage.PARAM_ID, article.getID().toString()));
			
			out.write("<tr>");
			if (this.showImages)
			{
				out.write("<td>");
				if (article.getPhoto()!=null)
				{
					out.writeImage(article.getPhoto(), Image.SIZE_THUMBNAIL, article.getTitle(), url);
				}
				else
				{
					out.writeImage("baby/article-thumbnail.png", article.getTitle(), url);
				}
				out.write("</td>");
			}
			out.write("<td>");
			out.writeLink(article.getTitle(), url);
			if (this.showSubSection && !Util.isEmpty(article.getSubSection()))
			{
				out.write("<br>");
				if (this.showSummary)
				{
					out.write("<span class=Faded>");
				}
				out.writeEncode(article.getSubSection());
				if (this.showSummary)
				{
					out.write("</span>");
				}
			}
			if (this.showRegion && (!Util.isEmpty(article.getRegion()) || !Util.isEmpty(article.getMedicalCenter())))
			{
				out.write("<br>");
				if (this.showSummary)
				{
					out.write("<span class=Faded>");
				}
				if (!Util.isEmpty(article.getMedicalCenter()))
				{
					out.writeEncode(article.getMedicalCenter());
				}
				else
				{
					out.writeEncode(article.getRegion());
				}
				if (this.showSummary)
				{
					out.write("</span>");
				}
			}
			if (this.showSummary)
			{
				String summary = article.getSummary();
				if (!Util.isEmpty(summary))
				{
					out.write("<br>");
					out.writeEncode(summary);
					out.write(" <a class=MoreLink href=\"");
					out.writeEncode(url);
					out.write("\">");
					out.writeEncode(out.getString("baby:ArticlesCtrl.MoreLink"));
					out.write("</a>");
				}
			}
			out.write("</td></tr>");
			
			if (i<articleIDs.size()-1)
			{
				out.write("<tr><td colspan=");
				out.write(this.showImages?2:1);
				out.write("><hr>");
				out.write("</td></tr>");
			}
		}
		out.write("</table>");
	}
}
