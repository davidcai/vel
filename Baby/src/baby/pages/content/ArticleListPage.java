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
import samoyan.servlet.exc.RedirectException;

import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class ArticleListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/articles";

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:Articles.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Toolbar
		new LinkToolbarControl(this)
			.addLink(getString("content:Articles.NewArticle"), getPageURL(EditArticlePage.COMMAND), "icons/basic1/pencil_16.png")
			.addLink(getString("content:Articles.ImportArticles"), getPageURL(ImportArticlePage.COMMAND), "icons/basic2/box_16.png")
			.render();

		// Load all articles
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySection(BabyConsts.SECTION_INFO);
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
			
			writeFormOpen();
			
			new DataTableControl<Article>(this, "articles", group.iterator())
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1); // checkbox
					column("").width(1); // pinned?
					column("").width(1); // image?
					column(getString("content:Articles.ArticleTitle"));
					column(getString("content:Articles.Section")).width(15);
				}
	
				@Override
				protected void renderRow(Article article) throws Exception
				{
					cell();
					writeCheckbox("chk_" + article.getID().toString(), null, false);
					
					cell();
					if (article.getPriority()>0)
					{
						writeImage("icons/basic1/label_16.png", getString("content:Articles.Pinned"));
					}
					
					cell();
					if (article.getPhoto()!=null)
					{
						writeImage("icons/basic2/photo_16.png", getString("content:Articles.Photo"));
					}

					cell();
					writeLink(article.getTitle(), getPageURL(EditArticlePage.COMMAND, new ParameterMap(EditArticlePage.PARAM_ID, article.getID().toString())));

					cell();
					writeEncode(article.getSection());
				}
			}
			.setPageSize(group.size()) // No paging
			.render();
			
			write("<br>");
			writeRemoveButton();
			
			writeFormClose();
		}
		
		if (groups.size()==0)
		{
			writeEncode(getString("content:Articles.NoResults"));
		}
	}

	private void writeTimelineLabel(int stageKey)
	{
		Stage stage = Stage.fromInteger(stageKey);
		if (stage.isPreconception())
		{
			writeEncode(getString("content:Articles.Preconception"));
		}
		else if (stage.isPregnancy())
		{
			writeEncode(getString("content:Articles.Pregnancy", stage.getPregnancyWeek()));
		}
		else if (stage.isInfancy())
		{
			writeEncode(getString("content:Articles.Infancy", stage.getInfancyMonth()));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		for (String p : getContext().getParameterNamesThatStartWith("chk_"))
		{
			ArticleStore.getInstance().remove(UUID.fromString(p.substring(4)));
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), null);
	}
}
