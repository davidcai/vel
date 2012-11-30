package samoyan.apps.admin.reports;

import java.util.Calendar;
import java.util.Date;
import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.usermgmt.UserPage;
import samoyan.controls.DataTableControl;
import samoyan.controls.GoogleGraph;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.TimeBucketing;
import samoyan.core.Util;
import samoyan.database.QueryIterator;
import samoyan.database.User;

public abstract class UsersOverTimeReportPage extends AdminPage
{
	public static class DataPoint
	{
		public int count = 0;
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
	public void renderHTML() throws Exception
	{
		String help = this.getHelpString(); // call subclass
		if (!Util.isEmpty(help))
		{
			writeEncode(help);
			write("<br><br>");
		}
		
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
		
		twoCol.writeRow(getString("admin:UsersOverTime.DateRange"));
		twoCol.writeDateTimeInput("datefrom", defaultFrom);
		twoCol.writeEncode(" ");
		twoCol.writeEncode(getString("admin:UsersOverTime.Through"));
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
		
		TimeBucketing<DataPoint> buckets = new TimeBucketing<DataPoint>(from, to, getLocale(), getTimeZone(), DataPoint.class, 0);
		
		// Run over user slices
		QueryIterator<User> iter = query(from, to);
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("admin:UsersOverTime.NoResults"));
				return;
			}
			while (iter.hasNext())
			{
				User u = iter.next();
				DataPoint pt = buckets.getBucket(getDateField(u));
				if (pt!=null)
				{
					pt.count ++;
				}
			}			
		}
		finally
		{
			iter.close();
		}
		
		// Print graph
		GoogleGraph graph = new GoogleGraph(this);
		graph.setChartType(GoogleGraph.COLUMN_CHART);
		graph.setLegend(GoogleGraph.NONE);
		graph.setHeight(300);
		graph.getChartArea().setTop(10);
		graph.getChartArea().setBottom(30);
		graph.addColumn(GoogleGraph.STRING, "");
		graph.addColumn(GoogleGraph.NUMBER, getDateFieldLabel());
		
		for (int i=0; i<buckets.length(); i++)
		{
			DataPoint pt = buckets.getBucket(i);
			graph.addRow(buckets.getLabel(i), new Number[] {pt.count});
		}
		graph.render();
	}

	private void renderTable() throws Exception
	{				
		Date from = getParameterDate("datefrom");
		Date to = getParameterDate("dateto");
		
		QueryIterator<User> iter = query(from, to);
		try
		{
			if (iter.hasNext()==false)
			{
				writeEncode(getString("admin:UsersOverTime.NoResults"));
				return;
			}

			new DataTableControl<User>(this, "users", iter)
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column(getDateFieldLabel());
					column(getString("admin:UsersOverTime.LoginName"));
					column(getString("admin:UsersOverTime.Name"));
				}

				@Override
				protected void renderRow(User user) throws Exception
				{
					cell();
					writeEncodeDateTime(getDateField(user));
								
					cell();
					writeLink(user.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, user.getID().toString())));
					
					cell();
					writeEncode(user.getDisplayName());
				}
				
			}.render();
		}
		finally
		{
			iter.close();
		}
	}
	
	/**
	 * To be overridden by subclass to return the query. 
	 */
	protected abstract QueryIterator<User> query(Date from, Date to) throws Exception;
	
	/**
	 * To be overridden by subclass to return the label of the significant date column.
	 * @return
	 * @throws Exception
	 */
	protected abstract String getDateFieldLabel() throws Exception;

	protected abstract Date getDateField(User user) throws Exception;

	protected String getHelpString()
	{
		return null;
	}
}
