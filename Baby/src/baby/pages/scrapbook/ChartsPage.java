package baby.pages.scrapbook;

import java.text.Collator;
import java.text.DateFormat;
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
import samoyan.controls.TabControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
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
	public final static String PARAM_YYYY = "yyyy";
	public final static String PARAM_M = "m";
	public final static String PARAM_D = "d";
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
	}
	
	private Mother mom;
	private Date date;
	
	/**
	 *  Measure records sorted by be mother and baby names
	 */
	private List<MeasureRecord> sortedRecords;
	
	@Override
	public void init() throws Exception
	{
		UUID userID = getContext().getUserID();
		this.mom = MotherStore.getInstance().loadByUserID(userID);

		// Get date
		if (isParameterNotEmpty(PARAM_YYYY) && isParameterNotEmpty(PARAM_M) && isParameterNotEmpty(PARAM_D))
		{
			Integer yyyy = getParameterInteger(PARAM_YYYY);
			Integer m = getParameterInteger(PARAM_M);
			Integer d = getParameterInteger(PARAM_D);
			
			if (yyyy != null && m != null & d != null)
			{
				Calendar cal = Calendar.getInstance(getTimeZone());
				cal.set(yyyy, m - 1, d, 0, 0, 0);
				cal.set(Calendar.MILLISECOND, 0);
				
				this.date = cal.getTime();
			}
		}
		if (this.date == null)
		{
			this.date = Calendar.getInstance(getTimeZone()).getTime();
		}
		
		Stage stage = this.mom.getEstimatedPregnancyStage(this.date, getTimeZone());
		
		//
		// Prepare a list of measure records for new or saved records
		//
		
		this.sortedRecords = new ArrayList<MeasureRecord>();
		
		List<UUID> momMeasureIDs = filterByPregnancyStage(MeasureStore.getInstance().getAll(true), stage);
		for (UUID momMeasureID : momMeasureIDs)
		{
			MeasureRecord rec = new MeasureRecord();
			rec.setUserID(userID);
			rec.setMeasureID(momMeasureID);
			rec.setCreatedDate(this.date);
			
			// By default, use the unit system defined in mother's profile
			rec.setMetric(this.mom.isMetric());
			
			this.sortedRecords.add(rec);
		}
		
		// Duplicate measure records for each baby
		List<UUID> babyMeasureIDs = filterByPregnancyStage(MeasureStore.getInstance().getAll(false), stage);
		List<UUID> babyIDs = BabyStore.getInstance().getByUser(userID); // Sorted by baby names
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
				
				this.sortedRecords.add(rec);
			}
		}
		
		//
		// Pre-populate measure records with saved data of a specified date
		// 

		Calendar cal = Calendar.getInstance(getTimeZone());
		cal.setTime(this.date);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		cal.add(Calendar.DATE, 1);
		Date to = cal.getTime();
		
		List<UUID> savedRecIDs = MeasureRecordStore.getInstance().getByDate(userID, from, to);
		for (UUID savedRecID : savedRecIDs)
		{
			MeasureRecord savedRec = MeasureRecordStore.getInstance().open(savedRecID);
			
			for (int i = 0; i < this.sortedRecords.size(); i++)
			{
				MeasureRecord rec = this.sortedRecords.get(i);
				
				// For duplicate records with same measure IDs and baby IDs, 
				// we use "rec.isSaved() == false" to guarantee that we only pre-populate new record objects. 
				if (rec.isSaved() == false && rec.getMeasureID().equals(savedRec.getMeasureID()) && (
					( rec.getBabyID() == null && savedRec.getBabyID() == null ) || // Mom
					( rec.getBabyID() != null && rec.getBabyID().equals(savedRec.getBabyID()) ) // Baby
				))
				{
					// Replace with saved record
					this.sortedRecords.set(i, savedRec);
				}
			}
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		for (MeasureRecord rec : this.sortedRecords)
		{
			if (isParameterNotEmpty(getFieldKey(PARAM_VALUE_PREFIX, rec)))
			{
				Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
				Float min = this.mom.isMetric() ? m.getMetricMin() : m.getImperialMin();
				Float max = this.mom.isMetric() ? m.getMetricMax() : m.getImperialMax();
				validateParameterDecimal(getFieldKey(PARAM_VALUE_PREFIX, rec), min, max);
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		for (MeasureRecord rec : this.sortedRecords)
		{
			rec.setValue(getParameterDecimal(getFieldKey(PARAM_VALUE_PREFIX, rec)));
			
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
		
		Calendar cal = Calendar.getInstance(getTimeZone());
		cal.setTime(this.date);
		int yyyy = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH) + 1;
		int d = cal.get(Calendar.DAY_OF_MONTH);
		
		throw new RedirectException(COMMAND, new ParameterMap(PARAM_SAVE, "").plus(PARAM_YYYY, yyyy).plus(PARAM_M, m).plus(PARAM_D, d));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("scrapbook:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("scrapbook:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("scrapbook:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}
		
		writeEncode(getString("scrapbook:Charts.Help"));
		write("<br>");

		DateFormat df = DateFormatEx.getDateInstance(getLocale(), getTimeZone());
		write("<p>");
		writeEncode(df.format(this.date));
		write("</p>");
		
		//
		// Input form
		//
		
		if (this.sortedRecords.isEmpty())
		{
			writeEncode(getString("scrapbook:Charts.NoRecords"));
		}
		else
		{
			writeFormOpen();
			writeMeasureRecords(this.sortedRecords);
			
			// Date post back
			Calendar cal = Calendar.getInstance(getTimeZone());
			cal.setTime(this.date);
			int yyyy = cal.get(Calendar.YEAR);
			int m = cal.get(Calendar.MONTH) + 1;
			int d = cal.get(Calendar.DAY_OF_MONTH);
			writeHiddenInput(PARAM_YYYY, yyyy); 
			writeHiddenInput(PARAM_M, m); 
			writeHiddenInput(PARAM_D, d); 
			
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
		DateFormat df = DateFormatEx.getMiniDateInstance(getLocale(), getTimeZone());
		
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
		Map<UUID, List<MeasureRecord>> grouped = new LinkedHashMap<UUID, List<MeasureRecord>>();
		grouped.put(this.mom.getUserID(), new ArrayList<MeasureRecord>());

		// Group records by mother and baby IDs
		for (MeasureRecord rec : records)
		{
			Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
			
			UUID id = measure.isForMother() ? this.mom.getUserID() : rec.getBabyID();
			List<MeasureRecord> rs = grouped.get(id);
			if (rs == null)
			{
				rs = new ArrayList<MeasureRecord>();
				grouped.put(id, rs);
			}
			
			rs.add(rec);
		}
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		for (UUID id : grouped.keySet())
		{
			List<MeasureRecord> rs = grouped.get(id);
			
			// Sort record by measure labels
			Collections.sort(rs, new Comparator<MeasureRecord>()
			{
				@Override
				public int compare(MeasureRecord r1, MeasureRecord r2)
				{
					try
					{
						Measure m1 = MeasureStore.getInstance().load(r1.getMeasureID());
						Measure m2 = MeasureStore.getInstance().load(r2.getMeasureID());
						
						return Collator.getInstance(getLocale()).compare(m1.getLabel(), m2.getLabel());
					}
					catch (Exception e)
					{
						return r1.getID().compareTo(r2.getID());
					}
				}
			});
			
			// Section title
			String title = null;
			if (id.equals(this.mom.getUserID()))
			{
				title = UserStore.getInstance().load(this.mom.getUserID()).getDisplayName();
			}
			else
			{
				Baby baby = BabyStore.getInstance().load(id);
				if (baby != null)
				{
					title = baby.getName();
				}
			}
			
			// Fields
			if (title != null)
			{
				twoCol.writeSubtitleRow(title);
				for (MeasureRecord rec : rs)
				{
					writeMeasureRecord(rec, twoCol);
				}
			}
		}
		twoCol.render();
	}
	
	/**
	 * Field key = prefix + user ID + measure ID.
	 * 
	 * @param prefix
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private String getFieldKey(String prefix, MeasureRecord rec) throws Exception
	{
		StringBuilder sb = new StringBuilder(prefix);
		
		Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
		if (measure.isForMother())
		{
			sb.append(this.mom.getUserID().toString());
		}
		else
		{
			sb.append(rec.getBabyID().toString());
		}
		
		sb.append(rec.getMeasureID());
		
		return sb.toString();
	}
	
	private void writeMeasureRecord(MeasureRecord rec, TwoColFormControl twoCol) throws Exception
	{
		Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
		
		twoCol.writeRow(measure.getLabel());

		Float min = this.mom.isMetric() ? measure.getMetricMin() : measure.getImperialMin();
		Float max = this.mom.isMetric() ? measure.getMetricMax() : measure.getImperialMax();
		Float val = getMeasureRecordValue(rec);
		
		if (measure.isForMother())
		{
			rec.getBabyID();
		}
		
		twoCol.writeDecimalInput(getFieldKey(PARAM_VALUE_PREFIX, rec), val, 16, min, max);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(this.mom.isMetric() ? measure.getMetricUnit() : measure.getImperialUnit());
		twoCol.writeHiddenInput(getFieldKey(PARAM_ID_PREFIX, rec), rec.getID().toString());
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Charts.Title");
	}
}
