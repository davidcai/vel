package samoyan.tasks;

import java.util.concurrent.atomic.AtomicInteger;

import samoyan.core.Debug;
import samoyan.database.LogEntryStore;
import samoyan.syslog.TaskErrorLogEntry;
import samoyan.tasks.RecurringTask;
import samoyan.tasks.RecurringTaskStats;

class RecurringTaskWrapper implements Runnable, RecurringTaskStats
{
	private RecurringTask recurringTask;
	
	// Stats
	private AtomicInteger running = new AtomicInteger(0);
	private AtomicInteger maxRunning = new AtomicInteger(0);
	private long lastRun = 0;
	private long lastDuration = 0;

	RecurringTaskWrapper(RecurringTask t)
	{
		recurringTask = t;
	}
	
	@Override
	public void run()
	{
		long startTime = System.currentTimeMillis();
		
		int count = running.incrementAndGet();
		maxRunning.compareAndSet(count-1, count);
		lastRun = startTime;
		
		try
		{
			Debug.logln("TaskManager: Now running " + getName());
			recurringTask.work();
		}
		catch (Exception e)
		{
			// Log the exception
			LogEntryStore.log(new TaskErrorLogEntry(getName(), e));
		}
		finally
		{
			lastDuration = System.currentTimeMillis() - startTime;
			running.decrementAndGet();
		}
	}
	
	// - - -
	
	public int getCountRunning()
	{
		return running.intValue();
	}

	public int getMaxRunning()
	{
		return maxRunning.intValue();
	}

	public long getLastRun()
	{
		return lastRun;
	}

	public long getLastDuration()
	{
		return lastDuration;
	}
	
	public String getName()
	{
		return recurringTask.getClass().getName();
	}
}
