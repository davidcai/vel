package samoyan.syslog;

import java.util.Locale;

import samoyan.database.LogEntry;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;

public class ActionLogEntry extends LogEntry
{
	public ActionLogEntry()
	{
		super("", INFO);

		RequestContext ctx = RequestContext.getCurrent();

		this.setName("Action " + Channel.getDescription(ctx.getChannel(), Locale.US));
		this.setString(1, "Command", "/" + ctx.getCommand());
		this.setString(2, "Action", ctx.getParameter(RequestContext.PARAM_ACTION));
	}
}
