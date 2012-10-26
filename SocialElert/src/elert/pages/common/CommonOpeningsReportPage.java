package elert.pages.common;

import java.util.Date;
import java.util.UUID;

import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;

import samoyan.controls.DataTableControl;
import samoyan.controls.GoogleGraph;
import samoyan.core.Pair;
import samoyan.core.TimeBucketing;
import samoyan.core.Util;
import samoyan.database.QueryIterator;
import samoyan.database.User;
import samoyan.database.UserStore;

public final class CommonOpeningsReportPage extends CommonTimeReportPage
{
	public static class DataPoint
	{
		public int count = 0;
		public int totalMinutes = 0;
		public int unresolvedMinutes = 0;
	}
		
	@Override
	public String getTitle() throws Exception
	{
		return getString("elert:OpeningsReport.Title");
	}

	@Override
	protected String getHelpString()
	{
		return getString("elert:OpeningsReport.Help");
	}

	@Override
	protected void renderGraph(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws Exception
	{
		TimeBucketing<DataPoint> buckets = new TimeBucketing<DataPoint>(from, to, getLocale(), getTimeZone(), DataPoint.class, 0);

		QueryIterator<Opening> iter = OpeningStore.getInstance().queryGhost(from,
																			to,
																			regionID,
																			serviceAreaID,
																			facilityID,
																			schedulerID,
																			physicianID,
																			procedureID);
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("elert:OpeningsReport.NoResults"));
				return;
			}
			while (iter.hasNext())
			{
				Opening o = iter.next();
				DataPoint pt = buckets.getBucket(o.getDateTime());
				if (pt!=null)
				{
					pt.count ++;
					pt.totalMinutes += o.getOriginalDuration();
					pt.unresolvedMinutes += o.getDuration();
				}
			}
		}
		finally
		{
			iter.close();
		}

		// Print graph
		GoogleGraph graph = new GoogleGraph(this);
		graph.setChartType(GoogleGraph.COMBO_CHART);
		graph.setLegend(GoogleGraph.TOP);
		graph.setHeight(300);
		graph.getChartArea().setTop(30);
		graph.getChartArea().setBottom(50);
		graph.addColumn(GoogleGraph.STRING, "");
		graph.addColumn(GoogleGraph.NUMBER, getString("elert:OpeningsReport.Count")).setType(GoogleGraph.BARS);
		graph.addColumn(GoogleGraph.NUMBER, getString("elert:OpeningsReport.TotalMinutes")).setOppositeAxis(true).setType(GoogleGraph.LINE);
		graph.addColumn(GoogleGraph.NUMBER, getString("elert:OpeningsReport.ResolvedMinutes")).setOppositeAxis(true).setType(GoogleGraph.LINE);
		
		for (int i=0; i<buckets.length(); i++)
		{
			DataPoint pt = buckets.getBucket(i);
			graph.addRow(buckets.getLabel(i), new Number[] {pt.count, pt.totalMinutes, pt.totalMinutes-pt.unresolvedMinutes});
		}
		graph.render();
	}
	
	private UUID getUUID(String paramName)
	{
		Pair<String, String> kvp = getParameterTypeAhead(paramName);
		return kvp==null || !Util.isUUID(kvp.getKey())? null : UUID.fromString(kvp.getKey());
	}

	@Override
	protected void renderTable(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws Exception
	{
		QueryIterator<Opening> iter = OpeningStore.getInstance().queryGhost(from,
				to,
				regionID,
				serviceAreaID,
				facilityID,
				schedulerID,
				physicianID,
				procedureID);
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("elert:OpeningsReport.NoResults"));
				return;
			}

			new DataTableControl<Opening>(this, "openings", iter)
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column(getString("elert:OpeningsReport.Date"));
					column(getString("elert:OpeningsReport.Facility"));
					column(getString("elert:OpeningsReport.Scheduler"));
					column(getString("elert:OpeningsReport.Duration")).align("right").alignHeader("right");
					column(getString("elert:OpeningsReport.ResolvedDuration")).align("right").alignHeader("right");
				}

				@Override
				protected void renderRow(Opening opening) throws Exception
				{
					cell();
					writeEncodeDateTime(opening.getDateTime());
								
					cell();
					Facility facility = FacilityStore.getInstance().load(opening.getFacilityID());
					writeEncode(facility.getName());
					
					cell();
					User scheduler = UserStore.getInstance().load(opening.getSchedulerID());
					writeEncode(scheduler.getDisplayName());
					
					cell();
					writeEncodeLong(opening.getOriginalDuration());
					
					cell();
					writeEncodeLong(opening.getOriginalDuration() - opening.getDuration());
				}
			}.render();
		}
		finally
		{
			iter.close();
		}
	}
}
