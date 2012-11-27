package samoyan.controls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import samoyan.core.ParameterMap;
import samoyan.servlet.WebPage;

public class BigCalendarControl
{
	private int yyyy;
	private int mm;
	private int dd;
	private WebPage out;
	private boolean dropdowns = true;
	private boolean links = true;
	private String command;
	private Map<String, String> params;
	
	public BigCalendarControl(WebPage outputPage)
	{
		this.out = outputPage;

		this.command = this.out.getContext().getCommand();
		this.params = this.out.getContext().getParameters();
		
		Calendar cal = Calendar.getInstance(out.getTimeZone(), out.getLocale());
		this.yyyy = cal.get(Calendar.YEAR);
		this.mm = cal.get(Calendar.MONTH);
		this.dd = cal.get(Calendar.DAY_OF_MONTH);
		
		Integer y = out.getParameterInteger("y");
		if (y!=null)
		{
			this.yyyy = y;
		}
		Integer m = out.getParameterInteger("m");
		if (m!=null)
		{
			this.mm = m-1; // 1-based to 0-based
		}
		Integer d = out.getParameterInteger("d");
		if (d!=null)
		{
			this.dd = d;
		}
	}
	
	public BigCalendarControl setDay(int yyyy, int mm, int dd)
	{
		this.yyyy = yyyy;
		this.mm = mm-1; // 1-based to 0-based
		this.dd = dd;
		return this;
	}
	
	public BigCalendarControl enableDropdowns(boolean b)
	{
		this.dropdowns = b;
		return this;
	}
	
	public BigCalendarControl enableLinks(boolean b)
	{
		this.links = b;
		return this;
	}
	
	public BigCalendarControl setCommand(String command, Map<String, String> params)
	{
		this.command = command;
		this.params = params;
		return this;
	}
	
	public void render() throws Exception
	{
		DateFormat df;
		
		Calendar cal = Calendar.getInstance(out.getTimeZone(), out.getLocale());
		cal.clear();
		cal.set(yyyy, mm, 1, 12, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		// The frame
		out.write("<table class=BigCalendar>");

		// Month header
		ParameterMap params = new ParameterMap();
		out.write("<tr valign=middle>");
		out.write("<td class=Month>");
			if (this.links)
			{
				cal.set(yyyy, mm, 1, 12, 0, 0);
				cal.add(Calendar.MONTH, -1);
				df = new SimpleDateFormat("MMM");
				df.setTimeZone(cal.getTimeZone());
				params.plus("y", String.valueOf(cal.get(Calendar.YEAR)))
					.plus("m", String.valueOf(cal.get(Calendar.MONTH)+1))
					.plus("d", String.valueOf(Math.min(dd, cal.getActualMaximum(Calendar.DAY_OF_MONTH))));
				out.write("<a href=\"");
				out.writeEncode(out.getPageURL(out.getContext().getCommand(), params));
				out.write("\">");
				out.writeEncode("< ");
				out.writeEncode(df.format(cal.getTime()));
				out.write("</a>");
			}
			else
			{
				out.write("&nbsp;");
			}
		out.write("</td><td class=Month colspan=5>");
//			df = new SimpleDateFormat("MMMM yyyy");
//			df.setTimeZone(cal.getTimeZone());
//			out.writeEncode(df.format(cal.getTime()));

			// Nav form
			df = new SimpleDateFormat("MMMM");
			df.setTimeZone(cal.getTimeZone());
				
			if (this.dropdowns)
			{
				out.writeFormOpen("GET", out.getContext().getCommand());
				SelectInputControl combo = new SelectInputControl(out, "m");
				combo.setInitialValue(mm+1);
				for (int i=1; i<=12; i++)
				{
					cal.set(yyyy, i-1, 1);
					combo.addOption(df.format(cal.getTime()), i);
				}
				combo.setAutoSubmit(true);
				combo.render();
				out.write(" ");
				combo = new SelectInputControl(out, "y");
				combo.setInitialValue(yyyy);
				for (int i=cal.get(Calendar.YEAR)+1; i>cal.get(Calendar.YEAR)+1-10; i--)
				{
					combo.addOption(String.valueOf(i), i);
				}
				combo.setAutoSubmit(true);
				combo.render();
//				out.write(" ");
//				out.writeButton(getString("controls:Button.Go"));
				out.writeHiddenInput("d", dd);
				
				out.writeFormClose();
			}
			else
			{
				cal.set(yyyy, mm, 1);
				out.writeEncode(df.format(cal.getTime()));
				out.write(" ");
				out.write(String.valueOf(yyyy));
			}
			
		out.write("</td><td class=Month>");
			if (this.links)
			{
				cal.set(yyyy, mm, 1, 12, 0, 0);
				cal.add(Calendar.MONTH, 1);
				df = new SimpleDateFormat("MMM");
				df.setTimeZone(cal.getTimeZone());
				params.plus("y", String.valueOf(cal.get(Calendar.YEAR)))
					.plus("m", String.valueOf(cal.get(Calendar.MONTH)+1))
					.plus("d", String.valueOf(Math.min(dd, cal.getActualMaximum(Calendar.DAY_OF_MONTH))));
				out.write("<a href=\"");
				out.writeEncode(out.getPageURL(out.getContext().getCommand(), params));
				out.write("\">");
				out.writeEncode(df.format(cal.getTime()));
				out.writeEncode(" >");
				out.write("</a>");
			}
			else
			{
				out.write("&nbsp;");
			}
		out.write("</td></tr>");
		
		// Days of week
		cal.set(yyyy, mm, 1, 12, 0, 0);
		int firstDow = cal.getFirstDayOfWeek(); // Typically Sunday or Monday
		cal.set(Calendar.DAY_OF_WEEK, firstDow);
		
		df = new SimpleDateFormat("EEE");
		out.write("<tr>");
		for (int i=0; i<7; i++)
		{
			out.write("<td class=DOW>");
			out.writeEncode(df.format(cal.getTime()));
			out.write("</td>");
			cal.add(Calendar.DATE, 1);
		}
		out.write("</tr>");
		
		// Days
		cal.set(Calendar.DAY_OF_MONTH, 1);
		if (firstDow!=cal.get(Calendar.DAY_OF_WEEK))
		{
			cal.set(Calendar.DAY_OF_WEEK, firstDow);
		}
		else
		{
			cal.add(Calendar.DATE, -7);
		}
		
		// Always print 6 weeks
		params.clear();
		if (this.params!=null)
		{
			params.putAll(this.params);
		}
		for (int week=0; week<6; week++)
		{
			out.write("<tr>");
			
			for (int day=0; day<7; day++)
			{
				int m = cal.get(Calendar.MONTH);
				int d = cal.get(Calendar.DAY_OF_MONTH);
				int y = cal.get(Calendar.YEAR);
				out.write("<td class=\"Day");
				if (m!=mm)
				{
					out.write(" Disabled");
				}
				else if (d==dd)
				{
					out.write(" Selected");
				}
				out.write("\">");
				if (m==mm)
				{
					params.plus("y", String.valueOf(y));
					params.plus("m", String.valueOf(m+1));
					params.plus("d", String.valueOf(d));
					out.write("<a href=\"");
					out.writeEncode(out.getPageURL(this.command, params));
					out.write("\">");
				}
				else
				{
					out.write("<div>");
				}
				out.write("<div class=Number>");
				out.write(d);
				out.write("</div>");

				// Call subclass to render the cell's content
				renderCell(y, m, d);

				if (m==mm)
				{
					out.write("</a>");
				}
				else
				{
					out.write("</div>");
				}
				out.write("</td>");
				
				cal.add(Calendar.DATE, 1);
			}
			
			out.write("</tr>");
		}
		
		// Close the frame
		out.write("</table>");
	}
	
	/**
	 * To be overridden by implementors to render the content of the cell.
	 * @param yyyy
	 * @param mm
	 * @param dd
	 */
	protected void renderCell(int yyyy, int mm, int dd)
	{
		// Default to nothing
	}
}
