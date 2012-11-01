package baby.pages.content;

import samoyan.controls.LinkToolbarControl;
import baby.pages.BabyPage;

public class MeasureListPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure-list";
	
	public static final String PARAM_IDS = "ids";
	public static final String PARAM_ID_PREFIX = "id_";
	public static final String PARAM_LABEL_PREFIX = "label_";
	public static final String PARAM_METRIC_UNIT_PREFIX = "metricUnit_";
	public static final String PARAM_IMPERIAL_UNIT_PREFIX = "imperialUnit_";
	public static final String PARAM_FOR_MOM_PREFIX = "forMom_";
	public static final String PARAM_PRECONCEPTION_PREFIX = "preconception_";
	public static final String PARAM_PREGNANCY_PREFIX = "pregnancy_";
	public static final String PARAM_INFANCY_PREFIX = "infancy_";
	public static final String PARAM_METRIC_MIN_PREFIX = "metricMin_";
	public static final String PARAM_METRIC_MAX_PREFIX = "metricMax_";
	public static final String PARAM_METRIC_TO_IMPERIAL_ALPHA_PREFIX = "metricToImperialAlpha_";
	public static final String PARAM_METRIC_TO_IMPERIAL_BETA_PREFIX = "metricToImperialBeta_";
	public static final String PARAM_SAVE = "save";
	
	@Override
	public void validate() throws Exception
	{
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		new LinkToolbarControl(this)
			.addLink(getString("content:MeasureList.NewMeasure"), getPageURL(MeasurePage.COMMAND), "icons/basic1/pencil_16.png")
			.render();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:MeasureList.Title");
	}
}
