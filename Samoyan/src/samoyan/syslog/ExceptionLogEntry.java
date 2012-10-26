package samoyan.syslog;

import samoyan.core.Util;
import samoyan.database.LogEntry;

public class ExceptionLogEntry extends LogEntry
{
	public ExceptionLogEntry(Throwable e)
	{
		super("Exception", ERROR);
		
		this.setText(1, "Exception", Util.exceptionDesc(e));
	}
}
