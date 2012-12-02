package samoyan.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimeBucketing<T>
{
	private Date base;
	private Date from;
	private Date to;
	private int granularity;
	private Calendar cal;
	private TimeZone timeZone;
	private Locale locale;
	private List<T> buckets;
	private Class<T> bucketClass;
	private DateFormat df;
	
	/**
	 * 
	 * @param from The earliest date to create a bucket for (inclusive)
	 * @param to The latest date to create a bucket for (exclusive)
	 * @param granularity Calendar.YEAR, Calendar.MONTH, Calendar.DATE, Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND,
	 * or <code>0</code> to automatically select.
	 */
	public TimeBucketing(Date from, Date to, Locale loc, TimeZone tz, Class<T> cls, int granularity)
	{
		int[] granularities = {Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY, Calendar.DATE, Calendar.MONTH, Calendar.YEAR};

		this.from = from;
		this.to = to;
		this.granularity = granularity;
		this.bucketClass = cls;
		
		this.locale = loc;
		this.timeZone = tz;
		this.cal = Calendar.getInstance(tz, loc);
		this.df = null;
		
		// Automatic granularity
		if (this.granularity==0)
		{
			this.granularity = Calendar.YEAR;
			for (int g=1; g<granularities.length; g++)
			{
				if (unitDiff(this.from, this.to, granularities[g])<=1)
				{
					this.granularity = granularities[g-1];
					break;
				}
			}
		}
		
		// Set base to beginning of period
		this.cal.setTime(this.from);
		this.cal.set(Calendar.MILLISECOND, 0);
		for (int g=0; g<granularities.length; g++)
		{
			if (this.granularity==granularities[g]) break;
			this.cal.set(granularities[g], this.cal.getMinimum(granularities[g]));
			this.base = this.cal.getTime();
		}
				
		// Allocate the buckets
		int n = unitDiff(this.base, this.to, this.granularity) + 1;
		this.buckets = new ArrayList<T>(n);
		for (int i=0; i<n; i++)
		{
			this.buckets.add(null);
		}
	}
	
	private int unitDiff(Date base, Date date, int granularity)
	{
		if (granularity==Calendar.SECOND)
		{
			return (int) ((date.getTime() - 1L - base.getTime()) / 1000L);
		}
		else if (granularity==Calendar.MINUTE)
		{
			return (int) ((date.getTime() - 1L - base.getTime()) / (60L*1000L));
		}
		else if (granularity==Calendar.HOUR_OF_DAY)
		{
			return (int) ((date.getTime() - 1L - base.getTime()) / (60L*60L*1000L));
		}
		else if (granularity==Calendar.DATE)
		{
			this.cal.setTime(date);
			long d = date.getTime() - 1L + this.cal.get(Calendar.DST_OFFSET);
			this.cal.setTime(base);
			long b = base.getTime() + this.cal.get(Calendar.DST_OFFSET);
			return (int) ((d-b) / (24L*60L*60L*1000L));
		}
		else if (granularity==Calendar.MONTH)
		{
			this.cal.setTimeInMillis(date.getTime() - 1L);
			int d = this.cal.get(Calendar.YEAR) * 12 + this.cal.get(Calendar.MONTH);
			this.cal.setTime(base);
			int b = this.cal.get(Calendar.YEAR) * 12 + this.cal.get(Calendar.MONTH);
			return d-b;
		}
		else if (granularity==Calendar.YEAR)
		{
			this.cal.setTimeInMillis(date.getTime() - 1L);
			int d = this.cal.get(Calendar.YEAR);
			this.cal.setTime(base);
			int b = this.cal.get(Calendar.YEAR);
			return d-b;
		}
		else
		{
			return -1;
		}
	}
	
	public T getBucket(Date date) throws InstantiationException, IllegalAccessException
	{
		if (date.before(this.from) || !date.before(this.to))
		{
			return null;
		}
		
		int index = unitDiff(this.from, date, this.granularity);
		T bucket = this.buckets.get(index);
		if (bucket==null)
		{
			bucket = this.bucketClass.newInstance();
			this.buckets.set(index,  bucket);
		}
		return bucket;
	}
	
	public T getBucket(int bucketIndex) throws InstantiationException, IllegalAccessException
	{
		T bucket = this.buckets.get(bucketIndex);
		if (bucket==null)
		{
			bucket = this.bucketClass.newInstance();
			this.buckets.set(bucketIndex,  bucket);
		}
		return bucket;
	}

	public int length()
	{
		return this.buckets.size();
	}
	
	public Date getBaseDate(int bucketIndex)
	{
		this.cal.setTime(this.base);
		this.cal.add(this.granularity, bucketIndex);
		return this.cal.getTime();
	}
	
	public DateFormat getDateFormat()
	{
		if (this.df!=null)
		{
			return this.df;
		}
		
		if (this.granularity==Calendar.SECOND)
		{
			this.df = new SimpleDateFormat(":ss");
		}
		else if (this.granularity==Calendar.MINUTE)
		{
			this.df = new SimpleDateFormat(":mm");
		}
		else if (this.granularity==Calendar.HOUR_OF_DAY)
		{
			this.df = DateFormatEx.getMiniTimeInstance(this.locale, this.timeZone);
		}
		else if (this.granularity==Calendar.DATE)
		{
			this.df = DateFormatEx.getMiniDateInstance(this.locale, this.timeZone);
		}
		else if (this.granularity==Calendar.MONTH)
		{
			this.df = new SimpleDateFormat("MMM yyyy");
		}
		else if (this.granularity==Calendar.YEAR)
		{
			this.df = new SimpleDateFormat("yyyy");
		}
		
		return this.df;
	}
	
	public String getLabel(int bucketIndex)
	{
		return getDateFormat().format(getBaseDate(bucketIndex));
	}	
}
