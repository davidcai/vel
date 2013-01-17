package baby.pages.journey;

import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.GoogleGraph;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TabControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Day;
import samoyan.core.Util;
import samoyan.database.UserStore;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public class ChartsPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/charts";
	
	private class GraphData
	{
		private String title;
		private boolean forMother;
		private Map<Day, MeasureRecord> rows;
		
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

		public Map<Day, MeasureRecord> getRows()
		{
			if (rows == null)
			{
				rows = new LinkedHashMap<Day, MeasureRecord>();
			}
			
			return rows;
		}
	}
	
	private Mother mom;
	
	@Override
	public void init() throws Exception
	{
		this.mom = MotherStore.getInstance().loadByUserID(getContext().getUserID());
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("journey:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("journey:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("journey:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}
		
		// Add button
		if (getContext().getUserAgent().isSmartPhone())
		{
			writeFormOpen("GET", JournalPage.COMMAND_EDIT);
			new ButtonInputControl(this, null)
				.setValue(getString("journey:Charts.AddHotButton"))
				.setMobileHotAction(true)
				.setAttribute("class", "NoShow")
				.render();
			writeFormClose();
		}
		else
		{
			new LinkToolbarControl(this)
				.addLink(getString("journey:Charts.AddLink"), getPageURL(JournalPage.COMMAND_EDIT), "icons/standard/bar-chart-16.png")
				.render();
		}
		
		//
		// Graphs
		//
		
		List<UUID> recIDs = MeasureRecordStore.getInstance().getByUserID(getContext().getUserID());
		if (recIDs.isEmpty() == false)
		{
			// Earliest records come first
			Collections.reverse(recIDs);
			
			List<GraphData> lstGraphData = getGraphDataList(recIDs);
			for (GraphData data : lstGraphData)
			{
				write("<h2>");
				writeEncode(data.getTitle());
				write("</h2>");
			
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
					
					DateFormat df = DateFormatEx.getMiniDateInstance(getLocale(), getTimeZone());
					
					for (Day day : data.getRows().keySet())
					{
						MeasureRecord rec = data.getRows().get(day);
						Float val = getMeasureRecordValue(rec);
						Date date = day.getMidDay(getTimeZone(), 0, 0, 0);
						graph.addRow(df.format(date), new Number[] { val });
					}
					
					graph.render();
				}
				else
				{
					writeEncode(getString("journey:Charts.NotEnoughData"));
					write("<br><br>");
				}
			}
		}
		else
		{
			writeEncode(getString("journey:Charts.NoRecords"));
		}
		
		write("<br><br>");
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
				name = Util.isEmpty(baby.getName()) ? getString("journey:Charts.Anonymous") : baby.getName();
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
				graph = new GraphData();
				
				String unit = this.mom.isMetric() ? measure.getMetricUnit() : measure.getImperialUnit();
				graph.setTitle(getString("journey:Charts.GraphTitle", name, measure.getLabel(), unit));
				graph.setForMother(rec.getBabyID() == null);
				
				mapGraphs.put(key, graph);
			}
			
			if (rec.getValue() != null)
			{
				// For the same date, always use the latest measure values
				Day day = new Day(getTimeZone(), rec.getCreatedDate());
				MeasureRecord prevRec = graph.getRows().get(day);
				if (prevRec == null || rec.getCreatedDate().after(prevRec.getCreatedDate()))
				{
					graph.getRows().put(day, rec);
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
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Charts.Title");
	}
}
