package elert.pages.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.GoogleGraph;
import samoyan.core.TimeBucketing;
import samoyan.database.QueryIterator;
import samoyan.servlet.Channel;
import samoyan.servlet.exc.PageNotFoundException;
import elert.database.ElertStore;
import elert.database.ElertStore.ChannelStat;

public class CommonIncomingReportPage extends CommonTimeReportPage
{	
	public static class DataPoint
	{
		public Map<String, Integer> channelCount = new HashMap<String, Integer>();
		public int elertCount = 0;
	}

	@Override
	protected void renderGraph(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws Exception
	{
		TimeBucketing<DataPoint> buckets = new TimeBucketing<DataPoint>(from, to, getLocale(), getTimeZone(), DataPoint.class, 0);
		
		QueryIterator<ChannelStat> iter = ElertStore.getInstance().queryChannelStats(	true,
																						from,
																						to,
																						regionID,
																						serviceAreaID,
																						facilityID,
																						schedulerID,
																						physicianID,
																						procedureID);
		
		Set<String> usedChannels = new HashSet<String>();
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("elert:IncomingReport.NoResults"));
				return;
			}
			
			while (iter.hasNext())
			{
				ChannelStat stat = iter.next();
				usedChannels.add(stat.channel);
				DataPoint pt = buckets.getBucket(new Date(stat.date));
				if (pt!=null)
				{
					Integer ci = pt.channelCount.get(stat.channel);
					if (ci==null) ci = 0;
					pt.channelCount.put(stat.channel, new Integer(ci+1));
				}
			}
		}
		finally
		{
			iter.close();
		}

		// Print graph
		GoogleGraph graph = new GoogleGraph(this);
		graph.setChartType(GoogleGraph.LINE_CHART);
		graph.setLegend(GoogleGraph.TOP);
		graph.setHeight(300);
		graph.getChartArea().setTop(30);
		graph.getChartArea().setBottom(50);
		graph.addColumn(GoogleGraph.STRING, "");
		
		List<String> channels = new ArrayList<String>();
		for (String c : Channel.getAll())
		{
			if (usedChannels.contains(c))
			{
				channels.add(c);
				graph.addColumn(GoogleGraph.NUMBER, Channel.getDescription(c, getLocale()));
			}
		}
		
		for (int i=0; i<buckets.length(); i++)
		{
			DataPoint pt = buckets.getBucket(i);
			Number[] measures = new Number[Channel.getAll().length];
			int j = 0;
			for (String c : channels)
			{
				measures[j] = pt.channelCount.get(c);
				if (measures[j]==null)
				{
					measures[j] = 0;
				}
				j++;
			}
			
			graph.addRow(buckets.getLabel(i), measures);
		}
		graph.render();
	}

	@Override
	protected void renderTable(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws Exception
	{
		throw new PageNotFoundException();
	}

	@Override
	protected boolean isTableView()
	{
		return false;
	}

	@Override
	protected String getHelpString()
	{
		return getString("elert:IncomingReport.Help");
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("elert:IncomingReport.Title");
	}
}
