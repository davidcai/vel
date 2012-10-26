package samoyan.apps.admin.reports;

import java.util.Calendar;
import java.util.Date;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.GoogleGraph;
import samoyan.core.TimeBucketing;
import samoyan.database.QueryIterator;
import samoyan.database.User;
import samoyan.database.UserStore;

public class CumulativeActivityReportPage extends AdminPage
{
	public static class DataPoint
	{
		public int count = 0;
	}

	public final static String COMMAND = AdminPage.COMMAND + "/cumulative-activity";

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:ActiveReport.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("admin:ActiveReport.Help"));
		write("<br><br>");

		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, 1);
		Date tomorrow = cal.getTime();
		cal.add(Calendar.YEAR, -1);
		Date aYearAgo = cal.getTime();
		cal.add(Calendar.YEAR, 1);
		cal.add(Calendar.DATE, -15);
		Date fifteenDaysAgo = cal.getTime();
		
		// Create buckets for last 15 days, and last 12 months
		TimeBucketing<DataPoint> dailyBuckets = new TimeBucketing<DataPoint>(fifteenDaysAgo, tomorrow, getLocale(), getTimeZone(), DataPoint.class, Calendar.DATE);
		TimeBucketing<DataPoint> monthlyBuckets = new TimeBucketing<DataPoint>(aYearAgo, tomorrow, getLocale(), getTimeZone(), DataPoint.class, Calendar.MONTH);

		// Run over user slices
		QueryIterator<User> iter = UserStore.getInstance().queryActiveGhost(aYearAgo, tomorrow);
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("admin:ActiveReport.NoResults"));
				return;
			}
			while (iter.hasNext())
			{
				User u = iter.next();
				DataPoint pt = dailyBuckets.getBucket(u.getLastActive());
				if (pt!=null)
				{
					pt.count ++;
				}
				pt = monthlyBuckets.getBucket(u.getLastActive());
				if (pt!=null)
				{
					pt.count ++;
				}
			}
			iter.close();
			
			write("<h2>");
			writeEncode(getString("admin:ActiveReport.DailyTitle"));
			write("</h2>");
			
			// Print daily graph
			GoogleGraph graph = new GoogleGraph(this);
			graph.setChartType(GoogleGraph.BAR_CHART);
			graph.setLegend(GoogleGraph.NONE);
			graph.setHeight(400);
			graph.getChartArea().setTop(10);
			graph.getChartArea().setBottom(30);
			graph.getChartArea().setLeft(80);
			graph.addColumn(GoogleGraph.STRING, "");
			graph.addColumn(GoogleGraph.NUMBER, getString("admin:ActiveReport.LastActive"));
			
			int totalCount = 0;
			for (int i=dailyBuckets.length()-1; i>=0; i--)
			{
				DataPoint pt = dailyBuckets.getBucket(i);
				totalCount += pt.count;
				graph.addRow(getString("admin:ActiveReport.Days", dailyBuckets.length()-i), new Number[] {totalCount});
			}
			graph.render();

			write("<br>");
			
			write("<h2>");
			writeEncode(getString("admin:ActiveReport.MonthlyTitle"));
			write("</h2>");

			// Print monthly graph
			graph = new GoogleGraph(this);
			graph.setChartType(GoogleGraph.BAR_CHART);
			graph.setLegend(GoogleGraph.NONE);
			graph.setHeight(400);
			graph.getChartArea().setTop(10);
			graph.getChartArea().setBottom(30);
			graph.getChartArea().setLeft(80);
			graph.addColumn(GoogleGraph.STRING, "");
			graph.addColumn(GoogleGraph.NUMBER, getString("admin:ActiveReport.LastActive"));
			
			totalCount = 0;
			for (int i=monthlyBuckets.length()-1; i>=0; i--)
			{
				DataPoint pt = monthlyBuckets.getBucket(i);
				totalCount += pt.count;
				graph.addRow(monthlyBuckets.getLabel(i), new Number[] {totalCount});
			}
			graph.render();
		}
		finally
		{
			iter.close();
		}
	}
}
