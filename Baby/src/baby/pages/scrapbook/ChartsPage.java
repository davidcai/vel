package baby.pages.scrapbook;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.database.UserStore;
import samoyan.servlet.exc.RedirectException;
import baby.database.BabyStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
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
		
		// TODO: Get date from date param
		Date date = new Date();
		Stage stage = this.mom.getPregnancyStage(date);
		
		//
		// Prepare the measure records
		//
		
		this.records = new ArrayList<MeasureRecord>();
		
		List<UUID> momMeasureIDs = filterByPregnancyStage(MeasureStore.getInstance().getAll(true), stage);
		for (UUID momMeasureID : momMeasureIDs)
		{
			MeasureRecord rec = new MeasureRecord();
			rec.setUserID(userID);
			rec.setMeasureID(momMeasureID);
			rec.setMetric(this.mom.isMetric());
			rec.setCreatedDate(date);
			this.records.add(rec);
		}
		
		// Duplicate measure records for each baby
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
				rec.setMetric(this.mom.isMetric());
				rec.setCreatedDate(date);
				this.records.add(rec);
			}
		}
		
		//
		// Populate measure records with saved records
		// 
		
		Calendar cal = Calendar.getInstance(getTimeZone());
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		Date from = cal.getTime();
		cal.set(Calendar.HOUR_OF_DAY, 24);
		Date to = cal.getTime();
		
		List<UUID> savedRecIDs = MeasureRecordStore.getInstance().getByDate(userID, from, to);
		for (UUID savedRecID : savedRecIDs)
		{
			MeasureRecord savedRec = MeasureRecordStore.getInstance().open(savedRecID);
			
			for (int i = 0; i < this.records.size(); i++)
			{
				MeasureRecord rec = this.records.get(i);
				
				// For duplicate records with same measure IDs and baby IDs, always choose the latest record. 
				if (rec.isSaved() == false && rec.getMeasureID().equals(savedRec.getMeasureID()) && (
					( rec.getBabyID() == null && savedRec.getBabyID() == null ) || // Mom
					( rec.getBabyID() != null && rec.getBabyID().equals(savedRec.getBabyID()) ) // Baby
				))
				{
					// Replace with saved record
					this.records.set(i, savedRec);
				}
			}
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		for (int i = 0; i < this.records.size(); i++)
		{
			Float val = getParameterDecimal(PARAM_VALUE_PREFIX + i);
			if (val != null)
			{
				MeasureRecord rec = this.records.get(i);
				Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
				Float min = rec.isMetric() ? m.getMetricMin() : m.getImperialMin();
				Float max = rec.isMetric() ? m.getMetricMax() : m.getImperialMax();
				validateParameterDecimal(PARAM_VALUE_PREFIX + i, min, max, null);
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		for (int i = 0; i < this.records.size(); i++)
		{
			MeasureRecord rec = this.records.get(i);
			rec.setValue(getParameterDecimal(PARAM_VALUE_PREFIX + i));
			
			MeasureRecordStore.getInstance().save(rec);
		}
		
		throw new RedirectException(COMMAND, new ParameterMap(PARAM_SAVE, ""));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<h2>");
		write(getString("scrapbook:Charts.RecordMeasures"));
		write("</h2>");
		
		writeFormOpen();
		
		writeMeasureRecords(this.records);
		
		// TODO: Add date post back
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
	}
	
	private List<UUID> filterByPregnancyStage(List<UUID> measureIDs, Stage stage) throws Exception
	{
		List<UUID> ids = new ArrayList<UUID>();
		
		for (UUID id : measureIDs)
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
					twoCol.writeTextRow(name);
				}
			}
			else
			{
				String babyName = BabyStore.getInstance().load(rec.getBabyID()).getName();
				if (babyName.equals(name) == false)
				{
					name = babyName;
					twoCol.writeTextRow(name);
				}
			}
			
			writeMeasureRecord(rec, twoCol, i);
		}
		
		twoCol.render();
	}
	
	private void writeMeasureRecord(MeasureRecord rec, TwoColFormControl twoCol, int index) throws Exception
	{
		Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
		
		twoCol.writeRow(measure.getLabel());

		Float min = rec.isMetric() ? measure.getMetricMin() : measure.getImperialMin();
		Float max = rec.isMetric() ? measure.getMetricMax() : measure.getImperialMax();
		
		twoCol.writeDecimalInput(PARAM_VALUE_PREFIX + index, rec.getValue() == null ? null : rec.getValue(), 16, min, max, null);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(rec.isMetric() ? measure.getMetricUnit() : measure.getImperialUnit());
		twoCol.writeHiddenInput(PARAM_ID_PREFIX + index, rec.getID().toString());
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Charts.Title");
	}
}
