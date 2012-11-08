package baby.pages.scrapbook;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
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
	public final static String PARAM_DATE = "date";
	public final static String PARAM_SAVE = "save";
	
	private DateFormat dateParamFormat = DateFormatEx.getSimpleInstance("MM-dd-yyyy", getLocale(), getTimeZone());
	private Mother mom;
	private Date date;
	private List<MeasureRecord> records;
	
	@Override
	public void init() throws Exception
	{
		UUID userID = getContext().getUserID();
		this.mom = MotherStore.getInstance().loadByUserID(userID);

		// Get date
		if (isParameter(PARAM_DATE))
		{
			try
			{
				this.date = dateParamFormat.parse(getParameterString(PARAM_DATE));
			}
			catch (ParseException e)
			{
				this.date = null;
			}
		}
		if (this.date == null)
		{
			this.date = Calendar.getInstance(getTimeZone()).getTime();
		}
		
		Stage stage = this.mom.getPregnancyStage(this.date);
		
		//
		// Prepare a list of measure records for both new and saved records
		//
		
		this.records = new ArrayList<MeasureRecord>();
		
		List<UUID> momMeasureIDs = filterByPregnancyStage(MeasureStore.getInstance().getAll(true), stage);
		for (UUID momMeasureID : momMeasureIDs)
		{
			MeasureRecord rec = new MeasureRecord();
			rec.setUserID(userID);
			rec.setMeasureID(momMeasureID);
			rec.setCreatedDate(this.date);
			
			// By default, use unit system defined in mother's profile
			rec.setMetric(this.mom.isMetric());
			
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
				rec.setCreatedDate(this.date);

				// By default, use unit system defined in mother's profile
				rec.setMetric(this.mom.isMetric());
				
				this.records.add(rec);
			}
		}
		
		//
		// Populate measure records with saved data of a specified date
		// 
		
		Calendar cal = Calendar.getInstance(getTimeZone());
		cal.setTime(this.date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		cal.set(Calendar.HOUR_OF_DAY, 24);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
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
				Float min = this.mom.isMetric() ? m.getMetricMin() : m.getImperialMin();
				Float max = this.mom.isMetric() ? m.getMetricMax() : m.getImperialMax();
				validateParameterDecimal(PARAM_VALUE_PREFIX + i, min, max);
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
			
			// Unit system defined in mother's profile always triumph over record's unit system.
			rec.setMetric(this.mom.isMetric());
			
			MeasureRecordStore.getInstance().save(rec);
		}
		
		throw new RedirectException(COMMAND, new ParameterMap(PARAM_SAVE, "").plus(PARAM_DATE, dateParamFormat.format(this.date)));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("scrapbook:Charts.Help"));
		write("<br>");

		DateFormat df = DateFormatEx.getSimpleInstance("MMMMM d, yyyy", getLocale(), getTimeZone());
		write("<p>");
		writeEncode(df.format(this.date));
		write("</p>");
		
		writeFormOpen();
		
		writeMeasureRecords(this.records);
		
		// Date post back
		writeHiddenInput(PARAM_DATE, dateParamFormat.format(this.date));
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
		
//		// TODO: Charts
//		List<UUID> allRecIDs = MeasureRecordStore.getInstance().getByUserID(getContext().getUserID());
//		if (allRecIDs.isEmpty() == false)
//		{
//			MeasureRecord latestRec = MeasureRecordStore.getInstance().load(allRecIDs.get(0));
//			MeasureRecord earliestRec = MeasureRecordStore.getInstance().load(allRecIDs.get(allRecIDs.size() - 1));
//			
//			latestRec.getCreatedDate();
//			
//		}
//		
//	
//		TimeBucketing<Date> buckets = new TimeBucketing<Date>(from, to, getLocale(), getTimeZone(), Date.class, Calendar.DATE);
//		
//		GoogleGraph graph = new GoogleGraph(this);
//		graph.setChartType(GoogleGraph.LINE_CHART);
//		graph.setLegend(GoogleGraph.TOP);
//		graph.setHeight(300);
//		graph.getChartArea().setTop(30);
//		graph.getChartArea().setBottom(50);
//		graph.addColumn(GoogleGraph.STRING, "");
		
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
					twoCol.writeSubtitleRow(momName);
				}
			}
			else
			{
				String babyName = BabyStore.getInstance().load(rec.getBabyID()).getName();
				if (babyName.equals(name) == false)
				{
					name = babyName;
					twoCol.writeSubtitleRow(name);
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

		Float min = this.mom.isMetric() ? measure.getMetricMin() : measure.getImperialMin();
		Float max = this.mom.isMetric() ? measure.getMetricMax() : measure.getImperialMax();
		Float val = rec.getValue();
		if (val != null)
		{
			// Convert value from record's current unit system to mother's unit system
			if (this.mom.isMetric() && rec.isMetric() == false)
			{
				val = measure.toMetric(val);
			}
			else if (this.mom.isMetric() == false && rec.isMetric())
			{
				val = measure.toImperial(val);
			}
		}
		
		twoCol.writeDecimalInput(PARAM_VALUE_PREFIX + index, val, 16, min, max);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(this.mom.isMetric() ? measure.getMetricUnit() : measure.getImperialUnit());
		twoCol.writeHiddenInput(PARAM_ID_PREFIX + index, rec.getID().toString());
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Charts.Title");
	}
}
