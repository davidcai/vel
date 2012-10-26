package samoyan.syslog;

import java.util.Locale;

import samoyan.database.LogEntry;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;

public final class NotifLogEntry extends LogEntry
{
	public NotifLogEntry()
	{
		super("", INFO);

		RequestContext ctx = RequestContext.getCurrent();

		this.setName("Notif " + Channel.getDescription(ctx.getChannel(), Locale.US));
		this.setString(1, "Command", "/" + ctx.getCommand());
	}
}
