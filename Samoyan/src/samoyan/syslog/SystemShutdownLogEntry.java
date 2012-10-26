package samoyan.syslog;

import samoyan.database.LogEntry;

public class SystemShutdownLogEntry extends LogEntry
{
	/**
	 * 
	 * @param duration Time it took for the system to shutdown, in millisecs.
	 */
	public SystemShutdownLogEntry(long duration)
	{
		super("System shutdown", INFO);
		setMeasure(1, "Time (ms)", duration);
	}
}
