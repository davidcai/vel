package samoyan.syslog;

import java.util.UUID;

import samoyan.database.LogEntry;

public class UserTerminatedLogEntry extends LogEntry
{
	public UserTerminatedLogEntry(UUID userID)
	{
		super("User terminated", INFO);
		setUserID(userID);
	}
}
