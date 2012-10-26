package samoyan.tasks;

import samoyan.tasks.RecurringTask;

public class HeartBeatRecurringTask implements RecurringTask
{
	@Override
	public void work()
	{
		System.out.println("<3");
	}

	@Override
	public long getInterval()
	{
		return 2000L; // 2 secs
	}
}
