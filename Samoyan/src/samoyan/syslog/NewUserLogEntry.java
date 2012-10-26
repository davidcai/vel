package samoyan.syslog;

import java.util.UUID;

import samoyan.database.LogEntry;

public class NewUserLogEntry extends LogEntry
{
	public NewUserLogEntry(UUID userID)
	{
		super("New user", INFO);
		setUserID(userID);
	}
}
