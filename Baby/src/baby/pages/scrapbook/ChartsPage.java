package baby.pages.scrapbook;

import java.text.Collator;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.GoogleGraph;
import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.database.UserStore;
import samoyan.servlet.exc.RedirectException;
import baby.database.Baby;
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

	private class GraphData
	{
		private String title;
		private boolean forMother;
		private Map<String, Float> rows;
		
		public String getTitle()
		{
			return title;
		}
		public void setTitle(String title)
		{
			this.title = title;
		}

		public boolean isForMother()
		{
			return forMother;
		}
		public void setForMother(boolean forMother)
		{
			this.forMother = forMother;
		}

		public Map<String, Float> getRows()
		{
			if (rows == null)
			{
				rows = new LinkedHashMap<String, Float>();
			}
			
			return rows;
		}
		public void setRows(Map<String, Float> rows)
		{
			this.rows = rows;
		}
	}
	
	private DateFormat dateParamFormat = DateFormatEx.getSimpleInstance("MM-dd-yyyy", getLocale(), TimeZoneEx.GMT);
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
			this.date = Calendar.getInstance(TimeZoneEx.GMT).getTime();
		}
		
		Stage stage = this.mom.getEstimatedPregnancyStage(this.date);
		
		//
		// Prepare a list of measure records for new/saved records
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
		// Pre-populate measure records with saved data of a specified date
		// 

		Calendar cal = Calendar.getInstance(TimeZoneEx.GMT);
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
				
				// For duplicate records with same measure IDs and baby IDs, 
				// we use "rec.isSaved() == false" to guarantee that we only pre-populate new record objects. 
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
			
			if (rec.getValue() == null)
			{
				// If the record value is null, delete the record from DB.
				MeasureRecordStore.getInstance().remove(rec.getID());
			}
			else
			{
				MeasureRecordStore.getInstance().save(rec);
			}
		}
		
		throw new RedirectException(COMMAND, new ParameterMap(PARAM_SAVE, "").plus(PARAM_DATE, dateParamFormat.format(this.date)));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeHorizontalNav(ChartsPage.COMMAND);
		
		writeEncode(getString("scrapbook:Charts.Help"));
		write("<br>");

		DateFormat df = DateFormatEx.getSimpleInstance("MMMMM d, yyyy", getLocale(), TimeZoneEx.GMT);
		write("<p>");
		writeEncode(df.format(this.date));
		write("</p>");
		
		//
		// Input form
		//
		
		if (this.records.isEmpty())
		{
			writeEncode(getString("scrapbook:Charts.NoRecords"));
		}
		else
		{
			writeFormOpen();
			writeMeasureRecords(this.records);
			writeHiddenInput(PARAM_DATE, dateParamFormat.format(this.date)); // Date post back
			write("<br>");
			writeSaveButton(PARAM_SAVE, null);
			writeFormClose();
		}
		
		//
		// Graphs
		//
		
		write("<br>");
		List<UUID> recIDs = MeasureRecordStore.getInstance().getByUserID(getContext().getUserID());
		
		// Earliest records come first
		Collections.reverse(recIDs);
		
		List<GraphData> lstGraphData = getGraphDataList(recIDs);
		for (GraphData data : lstGraphData)
		{
			// Display graph only when historical data has more than two records.
			if (data.getRows().size() > 1)
			{
				GoogleGraph graph = new GoogleGraph(this);
				graph.setChartType(GoogleGraph.LINE_CHART);
				graph.setLegend(GoogleGraph.NONE);
				graph.setHeight(300);
				graph.getChartArea().setTop(30);
				graph.getChartArea().setBottom(50);
				graph.addColumn(GoogleGraph.STRING, "");
				graph.addColumn(GoogleGraph.NUMBER, "");
				
				for (String date : data.getRows().keySet())
				{
					Float val = data.getRows().get(date);
					graph.addRow(date, new Number[] { val });
				}
				
				write("<h2>");
				writeEncode(data.getTitle());
				write("</h2>");
				graph.render();
				write("<br>");
			}
		}
	}
	
	/**
	 * Gets a list of GraphData objects representing a graph of a measure for a person.
	 * 
	 * @param sortedRecIDs Measure record IDs. The IDs should be already sorted by CreateDate in an ascending order.
	 * @return
	 * @throws Exception
	 */
	private List<GraphData> getGraphDataList(List<UUID> sortedRecIDs) throws Exception
	{
		Map<String, GraphData> mapGraphs = new LinkedHashMap<String, GraphData>();
		UUID userID = getContext().getUserID();
		String momName = UserStore.getInstance().load(userID).getDisplayName();
		DateFormat df = DateFormatEx.getMiniDateInstance(getLocale(), TimeZoneEx.GMT);
		
		for (UUID recID : sortedRecIDs)
		{
			MeasureRecord rec = MeasureRecordStore.getInstance().load(recID);

			String name = momName;
			if (rec.getBabyID() != null)
			{
				Baby baby = BabyStore.getInstance().load(rec.getBabyID());
				if (baby == null)
				{
					// Ignore those baby IDs that link to non-existing baby.
					continue;
				}
				
				// Use baby's name since this record is a baby record.
				name = baby.getName();
			}
			
			Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
			if (measure == null)
			{
				// Ignore records whose measures don't exist anymore
				continue;
			}
			
			// key = person ID + measure ID
			String key = (rec.getBabyID() == null ? userID : rec.getBabyID()) + "|" + measure.getID();
			
			GraphData graph = mapGraphs.get(key);
			if (graph == null)
			{
				String unit = this.mom.isMetric() ? measure.getMetricUnit() : measure.getImperialUnit();
				
				graph = new GraphData();
				graph.setTitle(getString("scrapbook:Charts.GraphTitle", name, measure.getLabel(), unit));
				graph.setForMother(rec.getBabyID() == null);
				
				mapGraphs.put(key, graph);
			}
			
			// Don't override existing value for the same date
			String date = df.format(rec.getCreatedDate());
			if (graph.getRows().containsKey(date) == false)
			{
				Float val = getMeasureRecordValue(rec);
				if (val != null)
				{
					graph.getRows().put(date, val);
				}
			}
		}
		
		// Sort by mother and graph title
		List<GraphData> graphs = new ArrayList<GraphData>(mapGraphs.values());
		Collections.sort(graphs, new Comparator<GraphData>() {

			@Override
			public int compare(GraphData gd1, GraphData gd2)
			{
				if (gd1.isForMother() != gd2.isForMother())
				{
					// Always list mother first
					return gd1.isForMother() ? -1 : 1;
				}
				
				// Sort by title
				return Collator.getInstance(getLocale()).compare(gd1.getTitle(), gd2.getTitle());
			}
		});
		
		return graphs;
	}
	
	/**
	 * Gets measure record value that is normalized by mother's preferred unit system.
	 * 
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private Float getMeasureRecordValue(MeasureRecord rec) throws Exception
	{
		Float val = rec.getValue();
		if (val != null)
		{
			Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
			
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
		
		return val;
	}
	
	/**
	 * Returns measures that are suitable for the specified pregnancy stage.
	 * 
	 * @param measureIDs
	 * @param stage
	 * @return
	 * @throws Exception
	 */
	private List<UUID> filterByPregnancyStage(List<UUID> measureIDs, Stage stage) throws Exception
	{
		List<UUID> filteredMeasureIDs = new ArrayList<UUID>();
		
		for (UUID measureID : measureIDs)
		{
			Measure m = MeasureStore.getInstance().load(measureID);
			
			if ((m.isForPreconception() && stage.isPreconception()) || 
				(m.isForPregnancy() && stage.isPregnancy()) || 
				(m.isForInfancy() && stage.isInfancy())) 
			{
				filteredMeasureIDs.add(measureID);
			}
		}
		
		return filteredMeasureIDs;
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
				Baby baby = BabyStore.getInstance().load(rec.getBabyID());
				if (baby != null)
				{
					String babyName = baby.getName();
					if (babyName.equals(name) == false)
					{
						name = babyName;
						twoCol.writeSubtitleRow(name);
					}
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
		Float val = getMeasureRecordValue(rec);
		
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
