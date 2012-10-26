package samoyan.syslog;

import samoyan.database.LogEntry;

public class SystemStartLogEntry extends LogEntry
{
	/**
	 * 
	 * @param duration Time it took for the system to start, in millisecs.
	 */
	public SystemStartLogEntry(long duration)
	{
		super("System start", INFO);
		setMeasure(1, "Time (ms)", duration);
	}
}
