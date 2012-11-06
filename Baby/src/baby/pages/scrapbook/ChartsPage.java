package baby.pages.scrapbook;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.database.UserStore;
import baby.database.BabyStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
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
	private List<MeasureRecord> records;
	
	@Override
	public void init() throws Exception
	{
		UUID userID = getContext().getUserID();
		this.mom = MotherStore.getInstance().loadByUserID(userID);
		Stage stage = this.mom.getPregnancyStage();
		
		records = new ArrayList<MeasureRecord>();
		
		List<UUID> momMeasureIDs = filterByPregnancyStage(MeasureStore.getInstance().getAll(true), stage);
		for (UUID momMeasureID : momMeasureIDs)
		{
			MeasureRecord rec = new MeasureRecord();
			rec.setUserID(userID);
			rec.setMeasureID(momMeasureID);
			records.add(rec);
		}
		
		List<UUID> babyMeasureIDs = filterByPregnancyStage(MeasureStore.getInstance().getAll(false), stage);
		List<UUID> babyIDs = BabyStore.getInstance().getByUser(userID);
		for (UUID babyID : babyIDs)
		{
			for (UUID babyMeasureID : babyMeasureIDs)
			{
				MeasureRecord rec = new MeasureRecord();
				rec.setUserID(userID);
				rec.setBabyID(babyID);
				rec.setMeasureID(babyMeasureID);
				records.add(rec);
			}
		}
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
		UUID userID = getContext().getUserID();
		
//		MeasureRecord rec = null;
		
//		List<UUID> recIDs = MeasureRecordStore.getInstance().getByUserID(userID);
//		if (recIDs.isEmpty() == false)
//		{
//			MeasureRecord recLatest = MeasureRecordStore.getInstance().load(recIDs.get(0));
//			
//			Calendar calToday = Calendar.getInstance(getTimeZone(), getLocale());
//			Calendar calLatest = Calendar.getInstance(getTimeZone(), getLocale());
//			calLatest.setTimeInMillis(recLatest.getCreated().getTime());
//			
//			if (calToday.get(Calendar.YEAR) == calLatest.get(Calendar.YEAR) && 
//				calToday.get(Calendar.DAY_OF_YEAR) == calLatest.get(Calendar.DAY_OF_YEAR)) 
//			{
//				// Use the same-day record
//				rec = recLatest;
//			}
//		}
//		if (rec == null)
//		{
//			rec = new MeasureRecord();
//		}
//		
//		int count = filterByPregnancyStage(MeasureStore.getInstance().getAll(), this.mom.getPregnancyStage()).size();
//		for (int i = 0; i < count; i++)
//		{
//			UUID mID = getParameterUUID(PARAM_ID_PREFIX + i);
//			Measure m = MeasureStore.getInstance().load(mID);
//			
//			rec.setUserID(userID);
//			rec.setMeasureID(mID);
//			int value = getParameterInteger(PARAM_VALUE_PREFIX + i);
//			if (this.mom.isMetric() == false)
//			{
//				value = m.toMetric(value);
//			}
//			rec.setMetricValue(value);
//			rec.setCreated(new Date());
//			
//			// TODO: Delete duplicate same-day measure records
//			
//		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<h2>");
		write(getString("scrapbook:Charts.RecordMeasures"));
		write("</h2>");
		
		
		
		writeFormOpen();
		
		writeMeasureRecords(records);
		
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
	
	private void writeMeasureRecords(List<MeasureRecord> records) throws Exception
	{
		Stage stage = this.mom.getPregnancyStage();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		String name = null;
		for (int i = 0; i < records.size(); i++)
		{
			MeasureRecord rec = records.get(i);
			Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
			if (measure.isForMother()) 
			{
				String momName = UserStore.getInstance().load(this.mom.getUserID()).getDisplayName(); 
				if (momName.equals(name) == false)
				{
					name = momName;
					twoCol.writeRow(name);
				}
			}
			else
			{
				String babyName = BabyStore.getInstance().load(rec.getBabyID()).getName();
				if (babyName.equals(name) == false)
				{
					name = babyName;
					twoCol.writeRow(name);
				}
			}
			
			writeMeasureRecord(rec, twoCol, this.mom.isMetric(), i);
		}
		
		twoCol.render();
	}
	
	private void writeMeasureRecord(MeasureRecord rec, TwoColFormControl twoCol, boolean metric, int index) throws Exception
	{
		Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
		
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
