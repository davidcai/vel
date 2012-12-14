package baby.tasks;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import baby.app.BabyConsts;
import baby.crawler.CrawlExecutor;
import baby.database.ArticleStore;
import samoyan.tasks.RecurringTask;

public final class CrawlResourcesRecurringTask implements RecurringTask
{
	private Date lastRun = null;
	
	@Override
	public void work() throws Exception
	{
		if (lastRun==null)
		{
			lastRun = ArticleStore.getInstance().queryLastUpdated(BabyConsts.SECTION_RESOURCE);
		}
		
		// Calculate last Sunday at 11pm
		Calendar cal = Calendar.getInstance(TimeZone.getDefault(), Locale.US);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (cal.getTimeInMillis() > System.currentTimeMillis())
		{
			cal.add(Calendar.DATE, -7);
		}
		
		if (lastRun==null || lastRun.before(cal.getTime()))
		{
			// Run now
			lastRun = new Date();
			CrawlExecutor.crawlResources();
		}
	}

	@Override
	public long getInterval()
	{
		return 60L*60L*1000L; // Run hourly
	}
}
