package samoyan.syslog;

import samoyan.database.LogEntry;
import samoyan.servlet.RequestContext;

public class RenderNotFoundLogEntry extends LogEntry
{
	public RenderNotFoundLogEntry(long renderDuration)
	{
		super("Render not found", INFO);
		
		setMeasure(1, "Render (ms)", renderDuration);
		
		RequestContext ctx = RequestContext.getCurrent();
		setString(1, "Command", "/" + ctx.getCommand());
	}
}
