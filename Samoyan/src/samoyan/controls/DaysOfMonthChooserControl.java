package samoyan.controls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;

import samoyan.servlet.WebPage;

/**
 * A control for letting the user select multiple days in a month. Sample usage:
 * <pre>
 * new DaysOfMonthChooserControl(this)
 * 	.setName("fieldName")
 * 	.setMonth(2012, Calendar.JULY)
 * 	.selectAll()
 * 	.disableBefore(new Date())
 * 	.render();
 * </pre>
 * After submittal, the parameter with the given name will hold a <code>String</code> of 1's and 0's for each day of the month. 
 * @author brian
 */
public class DaysOfMonthChooserControl
{
	private WebPage outputPage;
	private String name;
	private int yyyy;
	private int mm;
	private BitSet disabled = new BitSet();
	private BitSet selected = new BitSet();
	private Date disableBefore = null;
	private Date disableAfter = null;
	private Calendar cal = null;
	private boolean readOnly = false;
	
	public DaysOfMonthChooserControl(WebPage outputPage)
	{
		this.outputPage = outputPage;
		this.cal = Calendar.getInstance(outputPage.getTimeZone(), outputPage.getLocale());
	}
	
	public DaysOfMonthChooserControl setName(String name)
	{
		this.name = name;
		return this;
	}

	/**
	 * 
	 * @param yyyy The year, e.g. 2012
	 * @param mm The month (1-based), i.e. January is 1, February is 2, etc.
	 */
	public DaysOfMonthChooserControl setMonth(int yyyy, int mm)
	{
		this.yyyy = yyyy;
		this.mm = mm-1;
		return this;
	}
	
	/**
	 * Disable the particular day of the month.
	 * @param dd
	 */
	public DaysOfMonthChooserControl disable(int dd)
	{
		disabled.set(dd);
		return this;
	}
	
	/**
	 * Disable a range of days of the month.
	 * @param dd
	 */
	public DaysOfMonthChooserControl disableRange(int ddFrom, int ddTo)
	{
		for (int dd=ddFrom; dd<=ddTo; dd++)
		{
			disabled.set(dd);
		}
		return this;
	}

	/**
	 * Disable all days before the given date.
	 * @param date
	 */
	public DaysOfMonthChooserControl disableBefore(Date date)
	{
		this.cal.setTime(date);
		this.cal.set(Calendar.HOUR_OF_DAY, 0);
		this.cal.set(Calendar.MINUTE, 0);
		this.cal.set(Calendar.SECOND, 0);
		this.cal.set(Calendar.MILLISECOND, 0);
		this.disableBefore = this.cal.getTime();
		return this;
	}

	/**
	 * Disable all days before the given date.
	 * @param date
	 */
	public DaysOfMonthChooserControl disableAfter(Date date)
	{
		this.cal.setTime(date);
		this.cal.set(Calendar.HOUR_OF_DAY, 0);
		this.cal.set(Calendar.MINUTE, 0);
		this.cal.set(Calendar.SECOND, 0);
		this.cal.set(Calendar.MILLISECOND, 0);
		this.cal.add(Calendar.DATE, 1);
		this.cal.add(Calendar.MILLISECOND, -1);
		this.disableAfter = date;
		return this;
	}
	
	/**
	 * Select the particular day of the month.
	 * @param dd
	 */
	public DaysOfMonthChooserControl select(int dd)
	{
		selected.set(dd);
		return this;
	}

	/**
	 * Unselect the particular day of the month.
	 * @param dd
	 */
	public DaysOfMonthChooserControl unselect(int dd)
	{
		selected.clear(dd);
		return this;
	}

	public DaysOfMonthChooserControl selectRange(int ddFrom, int ddTo)
	{
		for (int dd=ddFrom; dd<=ddTo; dd++)
		{
			selected.set(dd);
		}
		return this;
	}

	public DaysOfMonthChooserControl unselectRange(int ddFrom, int ddTo)
	{
		for (int dd=ddFrom; dd<=ddTo; dd++)
		{
			selected.clear(dd);
		}
		return this;
	}

	/**
	 * Select all the days of the month.
	 * @param dd
	 */
	public DaysOfMonthChooserControl selectAll()
	{
		for (int i=1; i<=this.cal.getMaximum(Calendar.DAY_OF_MONTH); i++)
		{
			selected.set(i);
		}
		return this;
	}

	/**
	 * Unselect all the days of the month.
	 * @param dd
	 */
	public DaysOfMonthChooserControl unselectAll()
	{
		selected.clear();
		return this;
	}

	public DaysOfMonthChooserControl readOnly()
	{
		readOnly = true;
		return this;
	}
	
	public void render()
	{
		this.cal.clear();
		this.cal.set(this.yyyy, this.mm, 1);

		// Check reposted values
		String postedVal = outputPage.getParameterString(this.name);
		if (postedVal!=null)
		{
			for (int i=0; i<postedVal.length(); i++)
			{
				if (postedVal.charAt(i)=='1')
				{
					this.selected.set(i+1);
				}
				else
				{
					this.selected.clear(i+1);
				}
			}
		}
		else
		{
			postedVal = "";
			for (int i=0; i<this.cal.getActualMaximum(Calendar.DAY_OF_MONTH); i++)
			{
				postedVal += (this.selected.get(i+1)? "1" : "0");
			}
		}
		
		
		// The input box
		outputPage.writeHiddenInput(this.name, postedVal);
		
		// The frame
		outputPage.write("<table class=\"DaysOfMonthChooser");
		if (outputPage.isFormException(this.name))
		{
			outputPage.write(" Error");
		}
		if (this.readOnly==false)
		{
			outputPage.write(" Enabled");
		}
		outputPage.write("\"");
		outputPage.write(">");

		// Month header
		outputPage.write("<tr><td class=Month colspan=7>");
		DateFormat df = new SimpleDateFormat("MMMM yyyy");
		df.setTimeZone(cal.getTimeZone());
		outputPage.writeEncode(df.format(this.cal.getTime()));
		outputPage.write("</td></tr>");
		
		// Days of week
		int firstDow = this.cal.getFirstDayOfWeek(); // Typically Sunday or Monday
		cal.set(Calendar.DAY_OF_WEEK, firstDow);
		
		df = new SimpleDateFormat("EEE");
		outputPage.write("<tr>");
		for (int i=0; i<7; i++)
		{
			outputPage.write("<td class=DOW>");
			outputPage.writeEncode(df.format(this.cal.getTime()).substring(0, 1));
			outputPage.write("</td>");
			cal.add(Calendar.DATE, 1);
		}
		outputPage.write("</tr>");
		
		// Days
		this.cal.set(Calendar.DAY_OF_MONTH, 1);
		if (firstDow!=this.cal.get(Calendar.DAY_OF_WEEK))
		{
			this.cal.set(Calendar.DAY_OF_WEEK, firstDow);
		}
		else
		{
			this.cal.add(Calendar.DATE, -7);
		}
		
		// Always print 6 weeks
		for (int week=0; week<6; week++)
		{
			outputPage.write("<tr>");
			
			for (int day=0; day<7; day++)
			{
				int m = this.cal.get(Calendar.MONTH);
				int d = this.cal.get(Calendar.DAY_OF_MONTH);
				outputPage.write("<td class=\"");
				boolean disabled = (m!=this.mm || this.disabled.get(d));
				disabled = disabled || (this.disableAfter!=null && this.cal.getTime().after(this.disableAfter));
				disabled = disabled || (this.disableBefore!=null && this.cal.getTime().before(this.disableBefore));
				boolean selected = (m==this.mm && this.selected.get(d));
				boolean ignore = (m!=this.mm);
				if (!disabled && !ignore)
				{
					outputPage.write("Enabled ");
				}
				else
				{
					outputPage.write("Disabled ");
				}
				if (selected)
				{
					outputPage.write("Selected ");
				}
				if (!ignore)
				{
					outputPage.write("Day ");
				}
				outputPage.write("\">");
				outputPage.write(d);
				outputPage.write("</td>");
				
				this.cal.add(Calendar.DATE, 1);
			}
			
			outputPage.write("</tr>");
		}
		
		// Close the frame
		outputPage.write("</table>");
	}
}
