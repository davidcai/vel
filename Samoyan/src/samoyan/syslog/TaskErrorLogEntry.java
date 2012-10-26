package samoyan.syslog;

import samoyan.core.Util;
import samoyan.database.LogEntry;

public class TaskErrorLogEntry extends LogEntry
{
	public TaskErrorLogEntry(String taskName, Exception e)
	{
		super("Task error", ERROR);
		
		this.setString(1, "Task", taskName);
		this.setText(1, "Exception", Util.exceptionDesc(e));
	}
}
