package samoyan.syslog;

import samoyan.database.LogEntry;

public class LoginDeniedLogEntry extends LogEntry
{
	public LoginDeniedLogEntry()
	{
		super("Login denied", INFO);
	}
}
