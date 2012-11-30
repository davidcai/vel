package samoyan.apps.admin.reports;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.GoogleGraph;
import samoyan.database.QueryIterator;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;

public class AggregateTimelineReportPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/aggregate-timeline";
			
	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();

		// Get timeline stops
		BitSet stopsBitSet = fed.getTimelineStops();
		List<Integer> stops = new ArrayList<Integer>();
		for (int b=0; b<stopsBitSet.length(); b++)
		{
			if (stopsBitSet.get(b))
			{
				stops.add(b);
			}
		}
		
		// Get enabled channels
		List<String> enabledChannels = new ArrayList<String>();
		for (String channel : Channel.getPush())
		{
			if (fed.isChannelEnabled(channel)==true && fed.isChannelEnabled(channel)==true)
			{
				enabledChannels.add(channel);
			}
		}
		
		// Allocate stats structure
		Number[][] stats = new Number[stops.size()][enabledChannels.size()];
		for (int c=0; c<enabledChannels.size(); c++)
		{
			for (int s=0; s<stops.size(); s++)
			{
				stats[s][c] = 0;
			}
		}
		
		// Run over all users and collect stats
		QueryIterator<User> userGhosts = UserStore.getInstance().queryAllGhost();
		while (userGhosts.hasNext())
		{
			User ghost = userGhosts.next();
			User user = UserStore.getInstance().load(ghost.getID());
			
			for (int c=0; c<enabledChannels.size(); c++)
			{
				String channel = enabledChannels.get(c);
				BitSet timeline = user.getTimeline(channel);
				if (timeline==null)
				{
					// Use the default timeline for this channel
					timeline = fed.getTimeline(channel);
				}
				for (int s=0; s<stops.size(); s++)
				{
					if (timeline.get(stops.get(s)))
					{
						stats[s][c] = 1 + (Integer) stats[s][c];
					}
				}
			}
		}
		
		// Help string
		writeEncode(getString("admin:AggregateTimelineReport.Help"));
		write("<br><br>");
		
		// Print graph
		GoogleGraph graph = new GoogleGraph(this);
		graph.setChartType(GoogleGraph.COLUMN_CHART);
		graph.setLegend(GoogleGraph.TOP);
		graph.setHeight(300);
		graph.getChartArea().setTop(30);
		graph.getChartArea().setBottom(30);
		graph.addColumn(GoogleGraph.STRING, "");
		
		for (String channel : enabledChannels)
		{
			graph.addColumn(GoogleGraph.NUMBER, Channel.getDescription(channel, getLocale()));
		}

		for (int s=0; s<stops.size(); s++)
		{
			graph.addRow(getString("admin:AggregateTimelineReport.Label", stops.get(s)), stats[s]);
		}
		graph.render();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:AggregateTimelineReport.Title");
	}
}
