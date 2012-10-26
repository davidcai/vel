package samoyan.tasks;

public interface RecurringTaskStats
{
	/**
	 * Number of threads currently running for this task.
	 * @return
	 */
	public int getCountRunning();
	/**
	 * Maximum number of threads that have run at the same time for this task.
	 * @return
	 */
	public int getMaxRunning();
	/**
	 * The time when execution last started, in millisecs since 1970.
	 * @return
	 */
	public long getLastRun();
	/**
	 * The duration of the last execution, in millisecs.
	 * @return
	 */
	public long getLastDuration();
	/**
	 * The name of the task.
	 * @return
	 */
	public String getName();
}
