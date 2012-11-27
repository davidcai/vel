package baby.pages.content;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import baby.app.BabyConsts;
import baby.crawler.CrawlExecutor;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.pages.BabyPage;

/**
 * Lists all resource articles in the system.
 * @author brian
 *
 */
public final class ResourceListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/res-list";

	@Override
	public String getTitle() throws Exception
	{
		String region = getParameterString("region");
		String medCenter = getParameterString("center");
		if (!Util.isEmpty(medCenter) && !Util.isEmpty(region))
		{
			return medCenter;
		}
		else
		{
			return getString("content:ResourceList.Title");
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		String region = getParameterString("region");
		String medCenter = getParameterString("center");
		if (!Util.isEmpty(medCenter) && !Util.isEmpty(region))
		{
			List<UUID> articleIDs = ArticleStore.getInstance().queryByMedicalCenter(region, medCenter);
			renderList(articleIDs);
		}
		else if (ArticleStore.getInstance().getRegions().size()>0)
		{
			renderRegionTree();
		}
		else
		{
			writeEncode(getString("content:ResourceList.NoArticles"));
		}
	}
	
	private void renderRegionTree() throws Exception
	{
		if (isParameter("crawl"))
		{
			CrawlExecutor.crawlResources();
			write("<div class=InfoMessage>");
			writeEncode(getString("content:ResourceList.CrawlInitiated", new Date()));
			write("</div>");
		}
		
		// Toolbar
		new LinkToolbarControl(this)
			.addLink(	getString("content:ResourceList.InitiateCrawl"), getPageURL(getContext().getCommand(), new ParameterMap("crawl", "")), "icons/basic2/reload_16.png")
			.render();

		int COLS = 5;
		if (getContext().getUserAgent().isSmartPhone())
		{
			COLS = 2;
		}

		List<String> regions = ArticleStore.getInstance().getRegions();
		for (String region : regions)
		{
			write("<b>");
			writeEncode(region);
			write("</b><br>");
			
			List<String> centers = ArticleStore.getInstance().getMedicalCenters(region);
			write("<table width=\"100%\">");
			for (int i=0; i<COLS; i++)
			{
				write("<col width=\"");
				write(100/COLS);
				write("%\">");
			}
			for (int i=0; i<centers.size(); i++)
			{
				if (i%COLS==0)
				{
					write("<tr>");
				}
				write("<td>");
				
				String center = centers.get(i);
				writeLink(center, getPageURL(getContext().getCommand(), new ParameterMap("region", region).plus("center", center)));
				
				List<UUID> articleIDs = ArticleStore.getInstance().queryByMedicalCenter(region, center);
				write("<span class=Faded> (" );
				writeEncodeLong(articleIDs.size());
				write(")</span>");
				
				write("</td>");
				if (i%COLS==COLS-1)
				{
					write("</tr>");
				}
			}
			if (centers.size()%COLS!=0)
			{
				write("<td colspan=");
				write(COLS-centers.size()%COLS);
				write(">");
				write("&nbsp;</td></tr>");
			}
			write("</table><br>");
		}

	}
	
	private void renderList(List<UUID> articleIDs) throws Exception
	{		
		new DataTableControl<UUID>(this, "articles", articleIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column(getString("content:ResourceList.ArticleTitle"));
				column(getString("content:ResourceList.Section"));
			}

			
			@Override
			protected boolean isRenderRow(UUID articleID) throws Exception
			{
				Article article = ArticleStore.getInstance().load(articleID);
				return article.getSection().equals(BabyConsts.SECTION_HEALTHY_BEGINNINGS)==false;
			}

			@Override
			protected void renderRow(UUID articleID) throws Exception
			{
				Article article = ArticleStore.getInstance().load(articleID);
				
				cell();
				writeEncode(article.getTitle());
				
				cell();
				writeEncode(article.getSection());
			}
		}.render();
	}
}
