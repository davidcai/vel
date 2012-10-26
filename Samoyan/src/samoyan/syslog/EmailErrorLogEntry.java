package samoyan.syslog;

import samoyan.core.Util;
import samoyan.database.LogEntry;

public class EmailErrorLogEntry extends LogEntry
{
	public EmailErrorLogEntry(Exception e)
	{
		super("Email error", ERROR);
		
		this.setText(1, "Exception", Util.exceptionDesc(e));
	}
}
