package baby.pages.scrapbook;

import java.util.ArrayList;
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
	public final static String PARAM_ID_PREFIX = "id_";
	public final static String PARAM_SAVE = "save";
	
	private Mother mom;
	
	@Override
	public void init() throws Exception
	{
		this.mom = MotherStore.getInstance().loadByUserID(getContext().getUserID());
	}
	
	@Override
	public void validate() throws Exception
	{
		int count = filterByPregnancyStage(MeasureStore.getInstance().getAll(), this.mom.getPregnancyStage()).size();
		for (int i = 0; i < count; i++)
		{
			Measure m = MeasureStore.getInstance().load(getParameterUUID(PARAM_ID_PREFIX + i));
			
			int min = this.mom.isMetric() ? m.getMetricMin() : m.toImperial(m.getMetricMin());
			int max = this.mom.isMetric() ? m.getMetricMax() : m.toMetric(m.getMetricMax());
			validateParameterInteger(PARAM_VALUE_PREFIX + i, min, max);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<h2>");
		write(getString("scrapbook:Charts.RecordMeasures"));
		write("</h2>");
		
		List<UUID> measureIDs = MeasureStore.getInstance().getAll(true);
		measureIDs.addAll(MeasureStore.getInstance().getAll(false));
		measureIDs = filterByPregnancyStage(measureIDs, this.mom.getPregnancyStage());
		
		writeFormOpen();
		
		writeMeasures(measureIDs);
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
	}
	
	private List<UUID> filterByPregnancyStage(List<UUID> source, Stage stage) throws Exception
	{
		List<UUID> ids = new ArrayList<UUID>();
		
		for (UUID id : source)
		{
			Measure m = MeasureStore.getInstance().load(id);
			
			if ((m.isForPreconception() && stage.isPreconception()) || 
				(m.isForPregnancy() && stage.isPregnancy()) || 
				(m.isForInfancy() && stage.isInfancy())) 
			{
				ids.add(id);
			}
		}
		
		return ids;
	}
	
	private void writeMeasures(List<UUID> measureIDs) throws Exception
	{
		Stage stage = this.mom.getPregnancyStage();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		for (int i = 0; i < measureIDs.size(); i++)
		{
			Measure m = MeasureStore.getInstance().load(measureIDs.get(i));
			writeMeasure(m, twoCol, this.mom.isMetric(), i);
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
		twoCol.writeHiddenInput(PARAM_ID_PREFIX + index, measure.getID().toString());
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Charts.Title");
	}
}
