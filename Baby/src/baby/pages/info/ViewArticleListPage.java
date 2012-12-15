package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import baby.app.BabyConsts;
import baby.controls.TimelineControl;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class ViewArticleListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/articles";

	private static final String PARAM_STAGE = "stage";
		
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Articles.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Figure out the stage and its range (high, low)
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		int low = 0;
		int high = 0;
		Stage stage = null;
		if (isParameter(PARAM_STAGE))
		{
			String rangeStr = getParameterString(PARAM_STAGE);
			int p = rangeStr.indexOf("-");
			low = Integer.parseInt(rangeStr.substring(0, p));
			high = Integer.parseInt(rangeStr.substring(p+1));
			stage = Stage.fromInteger(low);
		}
		if (stage==null || stage.isValid()==false)
		{
			stage = mother.getPregnancyStage();
			low = TimelineControl.getLowRange(stage.toInteger());
			high = TimelineControl.getHighRange(stage.toInteger());
		}
		
		TimelineControl tlCtrl = new TimelineControl(this, stage, PARAM_STAGE);
		
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySectionAndTimeline(BabyConsts.SECTION_INFO, low, high);
		
//		writeHorizontalNav(ViewArticleListPage.COMMAND);

		// Render timeline
		write("<table><tr valign=middle><td>");
		writeEncode(getString("information:Articles.FoundResources", articleIDs.size()));
		write("</td><td>");
		tlCtrl.render();
		write("</td></tr></table><br>");
		
		// Render articles
		write("<table width=\"100%\"><col width=\"1%\"><col width=\"99%\">");
		for (UUID articleID : articleIDs)
		{
			Article article = ArticleStore.getInstance().load(articleID);
			String url = getPageURL(ViewArticlePage.COMMAND, new ParameterMap(ViewArticlePage.PARAM_ID, article.getID().toString()));
			
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
			if (!Util.isEmpty(article.getSubSection()))
			{
				write(" <span class=Faded>(");
				writeEncode(article.getSubSection());
				write(")</span>");
			}
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
			write("</td></tr>");
			write("<tr><td colspan=2>&nbsp;</td></tr>");
		}
		write("</table>");
	}
}
