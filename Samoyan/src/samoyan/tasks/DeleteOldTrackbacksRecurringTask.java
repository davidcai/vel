package samoyan.tasks;

import java.util.Date;

import samoyan.database.TrackbackStore;
import samoyan.servlet.Setup;

public class DeleteOldTrackbacksRecurringTask implements RecurringTask
{
	@Override
	public void work() throws Exception
	{
		TrackbackStore.getInstance().removeOlder(new Date(System.currentTimeMillis() - Setup.getCookieExpires()));
	}
	
	@Override
	public long getInterval()
	{
		return 24L*60L*60L*1000L; // 24 hrs
	}

}
