package samoyan.syslog;

import samoyan.database.LogEntry;

public class LoginOKLogEntry extends LogEntry
{
	public LoginOKLogEntry()
	{
		super("Login OK", INFO);
	}
}
