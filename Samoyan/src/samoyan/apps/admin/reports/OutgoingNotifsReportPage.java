package samoyan.apps.admin.reports;

import java.util.*;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.usermgmt.UserPage;
import samoyan.controls.DataTableControl;
import samoyan.controls.GoogleGraph;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.ReverseIterator;
import samoyan.core.TimeBucketing;
import samoyan.database.*;
import samoyan.servlet.Channel;

public final class OutgoingNotifsReportPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/outgoing-notifs";

	public static class DataPoint
	{
		public int failed = 0;
		public int succeeded = 0;
	}

	private Date defaultFrom = null;
	private Date defaultTo = null;

	@Override
	public void validate() throws Exception
	{
		validateParameterDate("datefrom");
		validateParameterDate("dateto");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:OutgoingNotifs.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("admin:OutgoingNotifs.Help"));
		write("<br><br>");

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
				
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, 1);
		defaultTo = cal.getTime();
		cal.add(Calendar.MONTH, -1);
		defaultFrom = cal.getTime();
		
		twoCol.writeRow(getString("admin:OutgoingNotifs.DateRange"));
		twoCol.writeDateTimeInput("datefrom", defaultFrom);
		twoCol.writeEncode(" ");
		twoCol.writeEncode(getString("admin:OutgoingNotifs.Through"));
		twoCol.writeEncode(" ");
		twoCol.writeDateTimeInput("dateto", defaultTo);
		
		twoCol.writeRow(getString("admin:OutgoingNotifs.View"));
		twoCol.writeRadioButton("view", getString("admin:OutgoingNotifs.Graph"), "g", "g");
		twoCol.writeEncode(" ");
		twoCol.writeRadioButton("view", getString("admin:OutgoingNotifs.Table"), "t", "g");
		
		twoCol.render();
		
		write("<br>");
		writeButton("query", getString("controls:Button.Query"));
		write("<br><br>");

		writeFormClose();
				
		if (this.isFormException())
		{
			return;
		}
		else if (isParameter("view")==false || getParameterString("view").equalsIgnoreCase("g"))
		{
			renderGraph();
		}
		else
		{
			renderTable();
		}
	}
	
	private void renderGraph() throws Exception
	{
		Date from = getParameterDate("datefrom");
		if (from==null)
		{
			from = defaultFrom;
		}
		Date to = getParameterDate("dateto");
		if (to==null)
		{
			to = defaultTo;
		}
		
		Map<String, TimeBucketing<DataPoint>> bucketsMap = new HashMap<String, TimeBucketing<DataPoint>>();
				
		// Run over notifs
		List<UUID> notifIDs = NotificationStore.getInstance().query(from, to, null, null, null, null);
		for (int i=0; i<notifIDs.size(); i++)
		{
			Notification notif = NotificationStore.getInstance().load(notifIDs.get(i));
			
			TimeBucketing<DataPoint> buckets = bucketsMap.get(notif.getChannel());
			if (buckets==null)
			{
				buckets = new TimeBucketing<OutgoingNotifsReportPage.DataPoint>(from, to, getLocale(), getTimeZone(), DataPoint.class, 0);
				bucketsMap.put(notif.getChannel(), buckets);
			}
			
			DataPoint pt = buckets.getBucket(notif.getDateStatus());
			if (pt!=null)
			{
				if (notif.getStatusCode()==Notification.STATUS_SENT || notif.getStatusCode()==Notification.STATUS_DELIVERED)
				{
					pt.succeeded++;
				}
				else if (notif.getStatusCode()==Notification.STATUS_FAILED)
				{
					pt.failed++;
				}
			}
		}
		
		if (notifIDs.size()==0)
		{
			writeEncode(getString("admin:OutgoingNotifs.NoResults"));
			return;
		}
		
		// Print graphs
		for (String channel : bucketsMap.keySet())
		{
			write("<h2>");
			writeEncode(Channel.getDescription(channel, getLocale()));
			write("</h2>");
			write("<br>");
			
			TimeBucketing<DataPoint> buckets = bucketsMap.get(channel);
						
			GoogleGraph graph = new GoogleGraph(this);
			graph.setChartType(GoogleGraph.COLUMN_CHART);
			graph.setLegend(GoogleGraph.NONE);
			graph.setHeight(150);
			graph.getChartArea().setTop(10);
			graph.getChartArea().setBottom(30);
			graph.addColumn(GoogleGraph.STRING, "");
			graph.addColumn(GoogleGraph.NUMBER, getString("admin:OutgoingNotifs.Sent"));
			graph.addColumn(GoogleGraph.NUMBER, getString("admin:OutgoingNotifs.Failed"));
			
			for (int i=0; i<buckets.length(); i++)
			{
				DataPoint pt = buckets.getBucket(i);
				graph.addRow(buckets.getLabel(i), new Number[] {pt.succeeded, pt.failed});
			}
			graph.render();
			
			write("<br>");
		}
	}
	
	private void renderTable() throws Exception
	{				
		Date from = getParameterDate("datefrom");
		Date to = getParameterDate("dateto");

		List<UUID> notifIDs = NotificationStore.getInstance().query(from, to, null, null, null, null);
		if (notifIDs.size()==0)
		{
			writeEncode(getString("admin:OutgoingNotifs.NoResults"));
			return;
		}

		new DataTableControl<UUID>(this, "notifs", new ReverseIterator<UUID>(notifIDs))
		{
			@Override
			protected void defineColumns()
			{
				column("");
				column(getString("admin:OutgoingNotifs.Created"));
				column(getString("admin:OutgoingNotifs.Channel"));
				column(getString("admin:OutgoingNotifs.Recipient"));
			}

			@Override
			protected void renderRow(UUID notifID) throws Exception
			{
				Notification notif = NotificationStore.getInstance().load(notifID);

				cell();
				if (notif.getStatusCode()==Notification.STATUS_FAILED)
				{
					writeImage("icons/standard/crossmark-16.png", "");
				}
				else if (notif.getStatusCode()==Notification.STATUS_SENT || notif.getStatusCode()==Notification.STATUS_DELIVERED)
				{
					writeImage("icons/standard/checkmark-16.png", "");
				}
				
				cell();
				writeEncodeDateTime(notif.getDateCreated());
				
				cell();
				writeEncode(Channel.getDescription(notif.getChannel(), getLocale()));

				cell();
				User user = UserStore.getInstance().load(notif.getUserID());
				if (user!=null)
				{
					writeLink(user.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString())));
				}				
			}
		}.render();		
	}
}
