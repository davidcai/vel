package samoyan.tasks;

public interface RecurringTask
{
	/**
	 * Do the work of this task. Must be thread-safe (i.e. method may be called by multiple threads concurrently)
	 * @throws Exception
	 */
	public void work() throws Exception;
	
	/**
	 * Return the interval between executions, in millisecs.
	 * @return
	 */
	public long getInterval();
}
