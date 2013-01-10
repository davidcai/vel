package samoyan.controls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class BigCalendarControl extends WebPage
{
	private int yyyy;
	private int mm;
	private int dd;
	private boolean dropdowns = true;
	private boolean links = true;
	private String command;
	private Map<String, String> params;
	private boolean highlightSelectedDay = true;
	
	public BigCalendarControl(WebPage outputPage)
	{
		setContainer(outputPage);

		this.command = getContext().getCommand();
		this.params = getContext().getParameters();
		
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		this.yyyy = cal.get(Calendar.YEAR);
		this.mm = cal.get(Calendar.MONTH);
		this.dd = cal.get(Calendar.DAY_OF_MONTH);
		
		Integer y = getParameterInteger("y");
		if (y!=null)
		{
			this.yyyy = y;
		}
		Integer m = getParameterInteger("m");
		if (m!=null)
		{
			this.mm = m-1; // 1-based to 0-based
		}
		Integer d = getParameterInteger("d");
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
	
	public BigCalendarControl highlightSelectedDay(boolean b)
	{
		this.highlightSelectedDay = b;
		return this;
	}
	
	public void render() throws Exception
	{
		DateFormat df;
		
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.clear();
		cal.set(yyyy, mm, 1, 12, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		// The frame
		write("<table class=BigCalendar>");

		// Month header
		ParameterMap params = new ParameterMap();
		write("<tr valign=middle>");
		write("<td class=Month>");
			if (this.links)
			{
				cal.set(yyyy, mm, 1, 12, 0, 0);
				cal.add(Calendar.MONTH, -1);
				df = new SimpleDateFormat("MMM");
				df.setTimeZone(cal.getTimeZone());
				params.plus("y", String.valueOf(cal.get(Calendar.YEAR)))
					.plus("m", String.valueOf(cal.get(Calendar.MONTH)+1))
					.plus("d", String.valueOf(Math.min(dd, cal.getActualMaximum(Calendar.DAY_OF_MONTH))));
				write("<a href=\"");
				writeEncode(getPageURL(getContext().getCommand(), params));
				write("\">");
				writeEncode("< ");
				writeEncode(df.format(cal.getTime()));
				write("</a>");
			}
			else
			{
				write("&nbsp;");
			}
		write("</td><td class=Month colspan=5>");
//			df = new SimpleDateFormat("MMMM yyyy");
//			df.setTimeZone(cal.getTimeZone());
//			writeEncode(df.format(cal.getTime()));

			// Nav form
			df = new SimpleDateFormat("MMMM");
			df.setTimeZone(cal.getTimeZone());
				
			if (this.dropdowns)
			{
				writeFormOpen("GET", getContext().getCommand());
				SelectInputControl combo = new SelectInputControl(this, "m");
				combo.setInitialValue(mm+1);
				for (int i=1; i<=12; i++)
				{
					cal.set(yyyy, i-1, 1);
					combo.addOption(df.format(cal.getTime()), i);
				}
				combo.setAutoSubmit(true);
				combo.render();
				write(" ");
				combo = new SelectInputControl(this, "y");
				combo.setInitialValue(yyyy);
				for (int i=cal.get(Calendar.YEAR)+1; i>cal.get(Calendar.YEAR)+1-10; i--)
				{
					combo.addOption(String.valueOf(i), i);
				}
				combo.setAutoSubmit(true);
				combo.render();
//				write(" ");
//				writeButton(getString("controls:Button.Go"));
				writeHiddenInput("d", dd);
				
				writeFormClose();
			}
			else
			{
				cal.set(yyyy, mm, 1);
				writeEncode(df.format(cal.getTime()));
				write(" ");
				write(String.valueOf(yyyy));
			}
			
		write("</td><td class=Month>");
			if (this.links)
			{
				cal.set(yyyy, mm, 1, 12, 0, 0);
				cal.add(Calendar.MONTH, 1);
				df = new SimpleDateFormat("MMM");
				df.setTimeZone(cal.getTimeZone());
				params.plus("y", String.valueOf(cal.get(Calendar.YEAR)))
					.plus("m", String.valueOf(cal.get(Calendar.MONTH)+1))
					.plus("d", String.valueOf(Math.min(dd, cal.getActualMaximum(Calendar.DAY_OF_MONTH))));
				write("<a href=\"");
				writeEncode(getPageURL(getContext().getCommand(), params));
				write("\">");
				writeEncode(df.format(cal.getTime()));
				writeEncode(" >");
				write("</a>");
			}
			else
			{
				write("&nbsp;");
			}
		write("</td></tr>");
		
		// Days of week
		cal.set(yyyy, mm, 1, 12, 0, 0);
		int firstDow = cal.getFirstDayOfWeek(); // Typically Sunday or Monday
		cal.set(Calendar.DAY_OF_WEEK, firstDow);
		
		df = new SimpleDateFormat("EEE");
		write("<tr>");
		for (int i=0; i<7; i++)
		{
			write("<td class=DOW>");
			writeEncode(df.format(cal.getTime()));
			write("</td>");
			cal.add(Calendar.DATE, 1);
		}
		write("</tr>");
		
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
			write("<tr>");
			
			for (int day=0; day<7; day++)
			{
				int m = cal.get(Calendar.MONTH);
				int d = cal.get(Calendar.DAY_OF_MONTH);
				int y = cal.get(Calendar.YEAR);
				
				boolean enabled = (m==mm) && isCellEnabled(y, m+1, d); // Call subclass for enable/disable flag
				
				write("<td class=\"Day");
				if (m!=mm)
				{
					write(" Outside");
				}
				else if (d==dd && highlightSelectedDay)
				{
					write(" Today");
				}
				String addlClasses = getCellCSSClass(y, m+1, d); // Call subclass for additional CSS classes
				if (!Util.isEmpty(addlClasses))
				{
					write(" ");
					writeEncode(addlClasses);
				}
				write("\">");
				if (enabled)
				{
					params.plus("y", String.valueOf(y));
					params.plus("m", String.valueOf(m+1));
					params.plus("d", String.valueOf(d));
					write("<a href=\"");
					writeEncode(getPageURL(this.command, params));
					write("\">");
				}
				else
				{
					write("<div>");
				}
				write("<div class=Number>");
				write(d);
				write("</div>");

				// Call subclass to render the cell's content
				renderCell(y, m+1, d);

				if (enabled)
				{
					write("</a>");
				}
				else
				{
					write("</div>");
				}
				write("</td>");
				
				cal.add(Calendar.DATE, 1);
			}
			
			write("</tr>");
		}
		
		// Close the frame
		write("</table>");
	}
	
	/**
	 * To be overridden by implementors to render the content of the cell.
	 * @param yyyy
	 * @param mm
	 * @param dd
	 * @throws Exception 
	 */
	protected void renderCell(int yyyy, int mm, int dd) throws Exception
	{
		// Default to nothing
	}
	
	/**
	 * To be overridden by implementors to return the CSS classes to attach to the cell.
	 * @param yyyy
	 * @param mm
	 * @param dd
	 * @return A space separated list of classes, or <code>null</code>.
	 */
	protected String getCellCSSClass(int yyyy, int mm, int dd) throws Exception
	{
		return null;
	}
	
	/**
	 * To be overridden by implementors to disable cells. Disabled cells are not clickable.
	 * @param yyyy
	 * @param mm
	 * @param dd
	 * @return <code>false</code> to disable the cell.
	 */
	protected boolean isCellEnabled(int yyyy, int mm, int dd) throws Exception
	{
		return true;
	}
}
