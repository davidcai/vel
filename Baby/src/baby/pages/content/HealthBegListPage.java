package baby.pages.content;

import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;

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
			.addLink(	getString("content:HealthBegList.NewArticle"), getPageURL(EditHealthBegPage.COMMAND), "icons/basic1/pencil_16.png")
			.render();

		List<UUID> articleIDs = ArticleStore.getInstance().queryBySection(Article.SECTION_HEALTHY_BEGINNINGS);
		// !$! Need better sorting/grouping; scrolling
		
		new DataTableControl<UUID>(this, "articles", articleIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column(getString("content:HealthBegList.ArticleTitle"));
				column(getString("content:HealthBegList.From"));
				column(getString("content:HealthBegList.To"));
			}

			@Override
			protected void renderRow(UUID articleID) throws Exception
			{
				Article article = ArticleStore.getInstance().load(articleID);
				
				cell();
				writeLink(article.getTitle(), getPageURL(EditHealthBegPage.COMMAND, new ParameterMap(EditHealthBegPage.PARAM_ID, article.getID().toString())));
				
				cell();
				writeTimelineLabel(article.getTimelineFrom());
				
				cell();
				writeTimelineLabel(article.getTimelineTo());
			}			
		}.render();
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
