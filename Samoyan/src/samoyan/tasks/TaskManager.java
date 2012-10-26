package samoyan.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import samoyan.core.Util;

public class TaskManager
{
	private static class TaskSlot
	{
		RecurringTaskWrapper wrapper;
		ExecutorService es;
	}
	private static List<TaskSlot> taskSlots = new ArrayList<TaskSlot>(); 
	
	public static void addRecurring(RecurringTask w)
	{
		RecurringTaskWrapper wrapper = new RecurringTaskWrapper(w);
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);		
		ses.scheduleAtFixedRate(wrapper, 0, w.getInterval(), TimeUnit.MILLISECONDS);
		
		TaskSlot slot = new TaskSlot();
		slot.wrapper = wrapper;
		slot.es = ses;
		
		taskSlots.add(slot);
	}
	
	public static void runOnce(RecurringTask w)
	{
		RecurringTaskWrapper wrapper = new RecurringTaskWrapper(w);
		ExecutorService es = Executors.newSingleThreadExecutor();
		es.execute(wrapper);
	}
	
	public static List<RecurringTaskStats> getStats()
	{
		List<RecurringTaskStats> result = new ArrayList<RecurringTaskStats>();
		for (TaskSlot slot : taskSlots)
		{
			result.add(slot.wrapper);
		}
		return result;
	}
	
	public static void terminateAll()
	{
		// Initiate shutdown of all tasks
		for (TaskSlot slot : taskSlots)
		{
			Util.shutdownAndAwaitTermination(slot.es);
		}
	}
}
