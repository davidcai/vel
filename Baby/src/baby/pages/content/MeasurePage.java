package baby.pages.content;

import samoyan.controls.TwoColFormControl;
import baby.database.Measure;
import baby.database.MeasureStore;
import baby.pages.BabyPage;

public class MeasurePage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure";
	public static final String PARAM_ID = "id";
	public static final String PARAM_LABEL = "label";
	public static final String PARAM_FOR_MOM = "forMom";
	public static final String PARAM_PRECONCEPTION = "preconception";
	public static final String PARAM_PREGNANCY = "pregnancy";
	public static final String PARAM_INFANCY = "infancy";
	public static final String PARAM_METRIC_UNIT = "metricUnit";
	public static final String PARAM_IMPERIAL_UNIT = "imperialUnit";
	public static final String PARAM_METRIC_MIN = "metricMin";
	public static final String PARAM_METRIC_MAX = "metricMax";
	public static final String PARAM_METRIC_TO_IMPERIAL_ALPHA = "metricToImperialAlpha";
	public static final String PARAM_METRIC_TO_IMPERIAL_BETA = "metricToImperialBeta";
	public static final String PARAM_SAVE = "save";
	
	private Measure measure;
	
	@Override
	public void init() throws Exception
	{
		if (isParameter(PARAM_ID)) 
		{
			this.measure = MeasureStore.getInstance().open(getParameterUUID(PARAM_ID));
		}
		
		if (this.measure == null)
		{
			this.measure = new Measure();
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		getParameterUUID(PARAM_ID);
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("content:Measure.Label"));
		twoCol.writeTextInput(PARAM_LABEL, this.measure == null ? null : this.measure.getLabel(), 32, Measure.MAXSIZE_LABEL);
		twoCol.write("<br>");
		twoCol.writeCheckbox(PARAM_FOR_MOM, getString("content:Measure.ForMom"), this.measure == null ? false : this.measure.isForMother());
		
		twoCol.writeRow(getString("content:Measure.Stages"));
		twoCol.writeCheckbox(PARAM_PRECONCEPTION, getString("content:Measure.Preconception"), this.measure == null ? false : this.measure.isForPreconception());
		twoCol.writeCheckbox(PARAM_PREGNANCY, getString("content:Measure.Pregnancy"), this.measure == null ? false : this.measure.isForPregnancy());
		twoCol.writeCheckbox(PARAM_INFANCY, getString("content:Measure.Infancy"), this.measure == null ? false : this.measure.isForInfancy());
		
		twoCol.writeRow(getString("content:Measure.MetricUnit"));
		twoCol.writeTextInput(PARAM_METRIC_UNIT, this.measure == null ? null : this.measure.getMetricUnit(), 16, Measure.MAXSIZE_UNIT);
		
		twoCol.writeRow(getString("content:Measure.ImperialUnit"));
		twoCol.writeTextInput(PARAM_IMPERIAL_UNIT, this.measure == null ? null : this.measure.getImperialUnit(), 16, Measure.MAXSIZE_UNIT);
		
		twoCol.render();
		
		writeSaveButton(PARAM_SAVE, this.measure);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:Measure.Title");
	}
}
