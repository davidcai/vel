package samoyan.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import samoyan.core.Util;

public class TaskManager
{
	private static class TaskSlot
	{
		RecurringTaskWrapper wrapper;
		ScheduledExecutorService es;
		ScheduledFuture<?> future;
	}
	private static List<TaskSlot> taskSlots = new ArrayList<TaskSlot>(); 
	
	public static void addRecurring(RecurringTask w)
	{
		TaskSlot slot = new TaskSlot();
		slot.wrapper = new RecurringTaskWrapper(w);;
		slot.es = Executors.newScheduledThreadPool(1);
		slot.future = slot.es.scheduleAtFixedRate(slot.wrapper, 0, w.getInterval(), TimeUnit.MILLISECONDS);
		
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
			slot.future.cancel(true);
			Util.shutdownNowAndAwaitTermination(slot.es);
		}
	}
}
