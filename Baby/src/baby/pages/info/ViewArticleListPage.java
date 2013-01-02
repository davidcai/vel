package baby.pages.info;

import java.util.List;
import java.util.UUID;

import baby.app.BabyConsts;
import baby.controls.ArticleListControl;
import baby.controls.TimelineControl;
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
		boolean phone = getContext().getUserAgent().isSmartPhone();

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
		new ArticleListControl(this, articleIDs).showSummary(!phone).render();
	}
}
