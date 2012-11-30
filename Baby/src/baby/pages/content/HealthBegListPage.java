package baby.pages.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.CollectionsEx;
import samoyan.core.ParameterMap;

import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class HealthBegListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/hb-list";

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:HealthBegList.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Toolbar
		new LinkToolbarControl(this)
			.addLink(getString("content:HealthBegList.NewArticle"), getPageURL(EditHealthBegPage.COMMAND), "icons/basic1/pencil_16.png")
			.addLink(getString("content:HealthBegList.ImportArticles"), getPageURL(ImportHealthBegPage.COMMAND), "icons/basic2/box_16.png")
			.render();

		// Load all articles
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySection(BabyConsts.SECTION_HEALTHY_BEGINNINGS);
		List<Article> articles = new ArrayList<Article>(articleIDs.size());
		for (UUID id : articleIDs)
		{
			articles.add(ArticleStore.getInstance().load(id));
		}

		// Group and sort
		class GroupByTimeline implements Comparator<Article>
		{
			@Override
			public int compare(Article o1, Article o2)
			{
				return (o1.getTimelineFrom() * 10000 + o1.getTimelineTo()) - (o2.getTimelineFrom() * 10000 + o2.getTimelineTo());
			}
		};
		class SortByPriorityAndTitle implements Comparator<Article>
		{
			@Override
			public int compare(Article o1, Article o2)
			{
				if (o1.getPriority()==o2.getPriority())
				{
					return o1.getTitle().compareTo(o2.getTitle());
				}
				else
				{
					return o2.getPriority() - o1.getPriority();
				}
			}
		}
		Collection<Collection<Article>> groups = CollectionsEx.group(articles, new GroupByTimeline(), new SortByPriorityAndTitle());
		
		// Print each group
		for (Collection<Article> group : groups)
		{
			Article firstArticle = group.iterator().next();
			write("<h2>");
			writeTimelineLabel(firstArticle.getTimelineFrom());
			if (firstArticle.getTimelineFrom()!=firstArticle.getTimelineTo())
			{
				write(" - ");
				writeTimelineLabel(firstArticle.getTimelineTo());
			}
			write("</h2>");
			
//			write("<table>");
//			for (Article article : group)
//			{
//				write("<tr><td>");
//				writeLink(article.getTitle(), getPageURL(EditHealthBegPage.COMMAND, new ParameterMap(EditHealthBegPage.PARAM_ID, article.getID().toString())));
//				write("</td></tr>");
//			}
//			write("</table>");
			
			new DataTableControl<Article>(this, "articles", group.iterator())
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1); // pinned?
					column("").width(1); // image?
					column(getString("content:HealthBegList.ArticleTitle"));
				}
	
				@Override
				protected void renderRow(Article article) throws Exception
				{
					cell();
					if (article.getPriority()>0)
					{
						writeImage("icons/basic1/label_16.png", getString("content:HealthBegList.Pinned"));
					}
					
					cell();
					if (article.getPhoto()!=null)
					{
						writeImage("icons/basic2/photo_16.png", getString("content:HealthBegList.Photo"));
					}

					cell();
					writeLink(article.getTitle(), getPageURL(EditHealthBegPage.COMMAND, new ParameterMap(EditHealthBegPage.PARAM_ID, article.getID().toString())));
				}			
			}
			.setPageSize(group.size()) // No paging
			.render();
		}
	}

	private void writeTimelineLabel(int stageKey)
	{
		Stage stage = Stage.fromInteger(stageKey);
		if (stage.isPreconception())
		{
			writeEncode(getString("content:HealthBegList.Preconception"));
		}
		else if (stage.isPregnancy())
		{
			writeEncode(getString("content:HealthBegList.Pregnancy", stage.getPregnancyWeek()));
		}
		else if (stage.isInfancy())
		{
			writeEncode(getString("content:HealthBegList.Infancy", stage.getInfancyMonth()));
		}
	}
}
