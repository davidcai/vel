package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import baby.controls.TimelineControl;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class HealthyBeginningsPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/healthy-beginnings";

	private static final String PARAM_STAGE = "stage";
	
	private Mother mother;
	private Stage stage;
	
	@Override
	public void init() throws Exception
	{
		this.mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		this.stage = mother.getPregnancyStage();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:HealthyBeginnings.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		Stage stage = null;
		if (isParameter(PARAM_STAGE))
		{
			stage = Stage.fromInteger(getParameterInteger(PARAM_STAGE));
		}
		if (stage==null || stage.isValid()==false)
		{
			stage = mother.getPregnancyStage();
		}
		
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySectionAndTimeline(Article.SECTION_HEALTHY_BEGINNINGS, stage.toInteger());
				
		// Render timeline
		if (stage.isPreconception())
		{
			writeEncode(getString("information:HealthyBeginnings.FoundResourcesPreconception", articleIDs.size()));
		}
		else if (stage.isPregnancy())
		{
			writeEncode(getString("information:HealthyBeginnings.FoundResourcesPregnancy", articleIDs.size(), stage.getPregnancyWeek()));
		}
		else if (stage.isInfancy())
		{
			writeEncode(getString("information:HealthyBeginnings.FoundResourcesInfancy", articleIDs.size(), stage.getInfancyMonth()));
		}
		write("<br><br>");
		new TimelineControl(this, stage)
			.setStageParamName(PARAM_STAGE)
			.render();
		write("<br>");
		
		// Render articles
		write("<table width=\"100%\"><col width=\"1%\"><col width=\"99%\">");
		for (UUID articleID : articleIDs)
		{
			Article article = ArticleStore.getInstance().load(articleID);
			String url = getPageURL(ArticlePage.COMMAND, new ParameterMap(ArticlePage.PARAM_ID, article.getID().toString()));
			
			write("<tr><td>");
			if (article.getPhoto()!=null)
			{
				writeImage(article.getPhoto(), Image.SIZE_THUMBNAIL, article.getTitle(), url);
			}
			else
			{
				writeImage("baby/article-thumbnail.png", article.getTitle(), url);
			}
			write("</td><td>");
			writeLink(article.getTitle(), url);
//			write(" <span class=Faded>(");
//			writeEncode(article.getSection());
//			write(")</span>");
			String summary = article.getSummary();
			if (Util.isEmpty(summary))
			{
				summary = article.getPlainText();
			}
			if (!Util.isEmpty(summary))
			{
				write("<br>");
				writeEncode(Util.getTextAbstract(summary, Article.MAXSIZE_SUMMARY));
			}
			write("<br><br>");
			write("</td></tr>");
		}
		write("</table>");
	}
}
