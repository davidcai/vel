package baby.app;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.UrlGenerator;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.pages.info.ViewArticlePage;

public class BabyUtil
{
	public final static String resolveLink(String link) throws Exception
	{
		if (link==null)
		{
			return null;
		}
		
		// Static links
		if (link.startsWith("http:") || link.startsWith("https:"))
		{
			return link;
		}
		
		// Article links
		if (link.startsWith("article:"))
		{
			Article article = ArticleStore.getInstance().loadBySourceURL(link.substring(8));
			if (article==null)
			{
				return null;
			}
			else
			{
				return UrlGenerator.getPageURL(false, null, ViewArticlePage.COMMAND, new ParameterMap(ViewArticlePage.PARAM_ID, article.getID()));
			}
		}
		
		// Command+params
		String command;
		ParameterMap params = new ParameterMap();
		int p = link.indexOf("?");
		if (p<0)
		{
			command = link;
		}
		else
		{
			command = link.substring(0, p);
			for (String nv : Util.tokenize(link.substring(p+1), "&"))
			{
				int q = nv.indexOf("=");
				params.plus(Util.urlDecode(nv.substring(0, q)), Util.urlDecode(nv.substring(q+1)));
			}
		}
		return UrlGenerator.getPageURL(false, null, command, params);
	}
}
