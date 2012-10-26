package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
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
		boolean smartPhone = getContext().getUserAgent().isSmartPhone();
		if (stage.isPreconception())
		{
			writeEncode(getString("information:HealthyBeginnings.FoundResourcesPreconception", articleIDs.size()));
			write("<br><br>");
		}
		else if (stage.isPregnancy())
		{
			// 40 weeks
			writeEncode(getString("information:HealthyBeginnings.FoundResourcesPregnancy", articleIDs.size(), stage.getPregnancyWeek()));
			write("<br><br>");
			write("<div class=TimelineBar>");
			for (int i=1; i<=Stage.MAX_WEEKS; i++)
			{
				write("<a href=\"");
				write(getPageURL(getContext().getCommand(), new ParameterMap(PARAM_STAGE, String.valueOf(Stage.pregnancy(i).toInteger()))));
				write("\"");
				if (i==stage.getPregnancyWeek())
				{
					write(" class=Current");
				}
				write(">");
				writeEncodeLong(i);
				write("</a>");
				if (smartPhone && (i==13 || i==26))
				{
					write("<br>");
				}
			}
			write("</div>");
			write("<br>");
		}
		else if (stage.isInfancy())
		{
			// 12 months
			writeEncode(getString("information:HealthyBeginnings.FoundResourcesInfancy", articleIDs.size(), stage.getInfancyMonth()));
			write("<br><br>");
			write("<div class=TimelineBar>");
			for (int i=1; i<=Stage.MAX_MONTHS; i++)
			{
				write("<a href=\"");
				write(getPageURL(getContext().getCommand(), new ParameterMap(PARAM_STAGE, String.valueOf(Stage.infancy(i).toInteger()))));
				write("\"");
				if (i==stage.getInfancyMonth())
				{
					write(" class=Current");
				}
				write(">");
				writeEncodeLong(i);
				write("</a>");
			}
			write("</div>");
			write("<br>");
		}
		
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
