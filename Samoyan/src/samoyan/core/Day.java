package samoyan.core;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Represents a day of the year for when the exact time of day is unknown or immaterial.
 * @author brianwillis
 *
 */
public class Day implements Comparable<Day>
{
	private int year;
	private int month;
	private int day;
	
	/**
	 * Creates a new <code>Day</code> initialized to today in the system's default time zone.
	 */
	public Day()
	{
		this(TimeZone.getDefault(), new Date());
	}
	
	/**
	 * Creates a new <code>Day</code> initialized to the given date in the given time zone.
	 * @param date
	 */
	public Day(TimeZone tz, Date date)
	{
		Calendar cal = Calendar.getInstance(tz, Locale.US);
		cal.setTime(date);
		this.year = cal.get(Calendar.YEAR);
		this.month = cal.get(Calendar.MONTH)+1;
		this.day = cal.get(Calendar.DAY_OF_MONTH);
	}
	
	/**
	 * 
	 * @param yyyy
	 * @param mm
	 * @param dd
	 */
	public Day(int yyyy, int mm, int dd)
	{
		this.year = yyyy;
		this.month = mm;
		this.day = dd;
	}
	
	/**
	 * 
	 * @param yyyymmdd A string in the format "YYYY/MM/DD" (as returned by {@link #toString()}) representing the day.
	 */
	public Day(String yyyymmdd)
	{
		int p = yyyymmdd.indexOf("/");
		int q = yyyymmdd.lastIndexOf("/");
		this.year = Integer.parseInt(yyyymmdd.substring(0, p));
		this.month = Integer.parseInt(yyyymmdd.substring(p+1, q));
		this.day = Integer.parseInt(yyyymmdd.substring(q+1));
	}
	
	/**
	 * @return A string in the format "YYYY/MM/DD" representing the day.
	 */
	@Override
	public String toString()
	{
		String yyyy = String.valueOf(this.year);
		while (yyyy.length()<4)
		{
			yyyy = "0" + yyyy;
		}
		String mm = String.valueOf(this.month);
		while (mm.length()<2)
		{
			mm = "0" + mm;
		}
		String dd = String.valueOf(this.day);
		while (dd.length()<2)
		{
			dd = "0" + dd;
		}
		return yyyy + "/" + mm + "/" + dd;
	}
	
	@Override
	public int compareTo(Day day)
	{
		if (this.year>day.year)
		{
			return 1;
		}
		else if (this.year<day.year)
		{
			return -1;
		}

		if (this.month>day.month)
		{
			return 1;
		}
		else if (this.month<day.month)
		{
			return -1;
		}

		if (this.day>day.day)
		{
			return 1;
		}
		else if (this.day<day.day)
		{
			return -1;
		}
		else
		{
			return 0;
		}
	}

	public boolean after(Day day)
	{
		if (this.year>day.year)
		{
			return true;
		}
		else if (this.year<day.year)
		{
			return false;
		}

		if (this.month>day.month)
		{
			return true;
		}
		else if (this.month<day.month)
		{
			return false;
		}

		if (this.day>day.day)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean before(Day day)
	{
		if (this.year<day.year)
		{
			return true;
		}
		else if (this.year>day.year)
		{
			return false;
		}

		if (this.month<day.month)
		{
			return true;
		}
		else if (this.month>day.month)
		{
			return false;
		}

		if (this.day<day.day)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj!=null && obj instanceof Day)
		{
			Day day = (Day) obj;
			return this.year==day.year && this.month==day.month && this.day==day.day;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Return number of days between two days.
	 * The method always returns a positive number of days.
	 * @param day
	 * @return
	 */
	public int daysBetween(Day day)
	{
		long millisBetween = Math.abs(this.getDayStart(TimeZoneEx.GMT).getTime() - day.getDayStart(TimeZoneEx.GMT).getTime());
		return (int) Math.round((float) millisBetween / (1000F * 60F * 60F * 24F));
	}
	  
	/**
	 * Returns a <code>Date</code> representing the midnight hour at the beginning of the day, for the specified time zone.
	 * @param tz
	 * @return
	 */
	public Date getDayStart(TimeZone tz)
	{
		Calendar cal = Calendar.getInstance(tz, Locale.US);
		cal.set(this.year, this.month-1, this.day, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
		
	/**
	 * Returns a <code>Date</code> representing the midnight hour at the end of the day (i.e. beginning of the next day), for the specified time zone.
	 * @param tz
	 * @return
	 */
	public Date getDayEnd(TimeZone tz)
	{
		Calendar cal = Calendar.getInstance(tz, Locale.US);
		cal.set(this.year, this.month-1, this.day, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, 1);
		return cal.getTime();
	}
	
	/**
	 * Returns a <code>Date</code> representing the indicated hour, for the specified time zone.
	 * @param tz
	 * @return
	 */
	public Date getMidDay(TimeZone tz, int hr, int min, int sec)
	{
		Calendar cal = Calendar.getInstance(tz, Locale.US);
		cal.set(this.year, this.month-1, this.day, hr, min, sec);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public int getYear()
	{
		return this.year;
	}
	public void setYear(int year)
	{
		this.year = year;
	}
	/**
	 * The month, 1-12.
	 * @return
	 */
	public int getMonth()
	{
		return this.month;
	}
	/**
	 * 
	 * @param month The month, 1-12.
	 */
	public void setMonth(int month)
	{
		this.month = month;
	}
	public int getDay()
	{
		return this.day;
	}
	/**
	 * 
	 * @param day The day of the month.
	 */
	public void setDay(int day)
	{
		this.day = day;
	}
}
