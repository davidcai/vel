package samoyan.core;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Represents a time of day for when the exact date is unknown or immaterial
 * @author brianwillis
 *
 */
public class TimeOfDay
{
	private int hour;
	private int minute;
	private int second;
	
	/**
	 * 
	 * @param seconds Number of seconds since midnight.
	 */
	public TimeOfDay(int seconds)
	{
		this.hour = seconds / 3600;
		this.minute = (seconds / 60)  % 60;
		this.second = seconds % 60;
	}

	/**
	 * Creates a new <code>TimeOfDay</code> initialized to midnight.
	 */
	public TimeOfDay()
	{
		this.hour = 0;
		this.minute = 0;
		this.second = 0;
	}
	
	public TimeOfDay(int hr, int min, int sec)
	{
		this.hour = hr;
		this.minute = min;
		this.second = sec;
	}

	/**
	 * 
	 * @param hhmmss A string in the format "HH:MM:SS" representing the time in 24-hr clock.
	 */
	public TimeOfDay(String hhmmss)
	{
		int p = hhmmss.indexOf(":");
		int q = hhmmss.lastIndexOf(":");
		setHour(Integer.parseInt(hhmmss.substring(0, p)));
		setMinute(Integer.parseInt(hhmmss.substring(p+1, q)));
		setSecond(Integer.parseInt(hhmmss.substring(q+1)));
	}
	
	/**
	 * @return A string in the format "HH:MM:SS" representing the time in 24-hr clock.
	 */
	@Override
	public String toString()
	{
		String hh = String.valueOf(getHour());
		while (hh.length()<2)
		{
			hh = "0" + hh;
		}
		String mm = String.valueOf(getMinute());
		while (mm.length()<2)
		{
			mm = "0" + mm;
		}
		String ss = String.valueOf(getSecond());
		while (ss.length()<2)
		{
			ss = "0" + ss;
		}
		return hh + ":" + mm + ":" + ss;
	}

	/**
	 * Return the number of seconds since midnight for the given time.
	 * @return
	 */
	public int getSeconds()
	{
		return this.second + this.minute*60 + this.hour*3600;
	}
	
	/**
	 * Calculates the <code>Date</code> at the given time zone and day.
	 * @param tz
	 * @return
	 */
	public Date getTime(TimeZone tz, int yr, int mo, int day)
	{
		Calendar cal = Calendar.getInstance(tz, Locale.US);
		cal.set(yr, mo, day, this.getHour(), this.getMinute(), this.getSecond());
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
	
	public int getHour()
	{
		return hour;
	}

	public void setHour(int hour)
	{
		this.hour = hour;
	}

	public int getMinute()
	{
		return minute;
	}

	public void setMinute(int minute)
	{
		this.minute = minute;
	}

	public int getSecond()
	{
		return second;
	}

	public void setSecond(int second)
	{
		this.second = second;
	}

}
