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
import samoyan.core.Util;
import samoyan.servlet.exc.RedirectException;

import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class ArticleListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/articles";

	public final static String PARAM_SECTION = "section";
	
	@Override
	public String getTitle() throws Exception
	{
		String section = getParameterString(PARAM_SECTION);
		if (section==null)
		{
			return getString("content:Articles.Title");
		}
		else
		{
			return section;
		}

	}
	
	@Override
	public void renderHTML() throws Exception
	{
		String section = getParameterString(PARAM_SECTION);
		if (section==null)
		{
			renderSections();
		}
		else
		{
			renderSection(section);
		}
	}
	
	private void renderSections() throws Exception
	{
		// Toolbar
		new LinkToolbarControl(this)
			.addLink(getString("content:Articles.NewArticle"), getPageURL(EditArticlePage.COMMAND), "icons/standard/pencil-16.png")
			.addLink(getString("content:Articles.ImportArticles"), getPageURL(ImportArticlePage.COMMAND), "icons/standard/cardboard-box-16.png")
			.addLink(getString("content:Articles.Crawler"), getPageURL(KPOrgCrawlerPage.COMMAND), "icons/standard/lady-bug-16.png")
			.render();

		List<String> sections = ArticleStore.getInstance().getSections();
		for (String s : sections)
		{
			int count = ArticleStore.getInstance().queryBySection(s).size();
			writeLink(s, getPageURL(getContext().getCommand(), new ParameterMap(PARAM_SECTION, s)));
			write(" <span class=Faded>(");
			writeEncodeLong(count);
			write(")</span><br>");
		}
		if (sections.size()==0)
		{
			writeEncode(getString("content:Articles.NoResults"));
		}
	}
	
	private void renderSection(String section) throws Exception
	{
		// Load all articles
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySection(section);
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
					column("").width(1); // crawled?
					column("").width(1); // image?
					column(getString("content:Articles.ArticleTitle"));
					column(getString("content:Articles.SubSection"));
				}
	
				@Override
				protected void renderRow(Article article) throws Exception
				{
					cell();
					writeCheckbox("chk_" + article.getID().toString(), null, false);
					
					cell();
					if (article.getPriority()>0)
					{
						writeImage("icons/standard/pin-16.png", getString("content:Articles.Pinned"));
					}
					
					cell();
					if (article.isByCrawler())
					{
						writeImage("icons/standard/lady-bug-16.png", getString("content:Articles.Crawled"));
					}

					cell();
					if (article.getPhoto()!=null)
					{
						writeImage("icons/standard/photo-16.png", getString("content:Articles.Photo"));
					}

					cell();
					writeLink(article.getTitle(), getPageURL(EditArticlePage.COMMAND, new ParameterMap(EditArticlePage.PARAM_ID, article.getID().toString())));

					cell();
					if (!Util.isEmpty(article.getSubSection()))
					{
						writeEncode(article.getSubSection());
					}
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
