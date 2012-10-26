package samoyan.syslog;

import samoyan.database.LogEntry;
import samoyan.servlet.RequestContext;

public class RenderHTMLLogEntry extends LogEntry
{
	public RenderHTMLLogEntry(long renderDuration, long deliverDuration, int sizeBytes)
	{
		super("Render HTML", INFO);
		
		setMeasure(1, "Render (ms)", renderDuration);
		setMeasure(2, "Deliver (ms)", deliverDuration);
		setMeasure(3, "Size (bytes)", sizeBytes);
		
		RequestContext ctx = RequestContext.getCurrent();
		setString(1, "Command", "/" + ctx.getCommand());
	}
}
