package baby.pages.scrapbook;

import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import baby.database.Measure;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class ChartsPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/charts";
	
	public final static String PARAM_VALUE_PREFIX = "value_";
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<h2>");
		write(getString("scrapbook:Charts.RecordMeasures"));
		write("</h2>");
		
		Mother mom = MotherStore.getInstance().loadByUserID(getContext().getUserID());
//		List<UUID> recIDs = MeasureRecordStore.getInstance().getByUserID(getContext().getUserID());
		
		List<UUID> measureIDs = MeasureStore.getInstance().getAll(true);
		measureIDs.addAll(MeasureStore.getInstance().getAll(false));
		
		writeMeasures(measureIDs);
	}
	
	private void writeMeasures(List<UUID> measureIDs) throws Exception
	{
		Mother mom = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		Stage stage = mom.getPregnancyStage();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		int i = 0;
		for (UUID id : measureIDs)
		{
			Measure measure = MeasureStore.getInstance().load(id);
			
			if ((measure.isForPreconception() && stage.isPreconception()) || 
				(measure.isForPregnancy() && stage.isPregnancy()) || 
				(measure.isForInfancy() && stage.isInfancy())) 
			{
				writeMeasure(measure, twoCol, mom.isMetric(), i);
				i++;
			}
		}
		
		twoCol.render();
	}
	
	private void writeMeasure(Measure measure, TwoColFormControl twoCol, boolean metric, int index) throws Exception
	{
		twoCol.writeRow(measure.getLabel());

		Integer min = metric ? measure.getMetricMin() : measure.toImperial(measure.getMetricMin());
		Integer max = metric ? measure.getMetricMax() : measure.toImperial(measure.getMetricMax());
		
		twoCol.writeNumberInput(PARAM_VALUE_PREFIX + index, min, 16, min, max);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(metric ? measure.getMetricUnit() : measure.getImperialUnit());
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Charts.Title");
	}
}
