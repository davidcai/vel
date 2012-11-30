package samoyan.apps.admin.log;

import java.util.Iterator;
import java.util.List;

import samoyan.apps.admin.AdminPage;
import samoyan.core.Debug;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

public class ConsolePage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/console";
	
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		if (isParameter("clear"))
		{
			Debug.clearConsole();
		}
		
		// Redirect to self in order to clear form submission
		throw new RedirectException(ctx.getCommand(), null);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<pre class=Console id=console>");
		List<String> console = Debug.getConsole();
		Iterator<String> iter = console.iterator();
		while (iter.hasNext())
		{
			String line = (String) iter.next();
			writeEncode(line);
		}
		write("</pre>");
		
		// Buttons
		writeFormOpen();
		write("<br>");
		super.writeButton("refresh", getString("admin:Console.Refresh"));
		write("&nbsp;");
		super.writeButtonRed("clear", getString("admin:Console.Clear"));
		writeFormClose();
		
		// Scroll to bottom of console
		write("<script type=\"text/javascript\">$('#console').scrollTop($('#console')[0].scrollHeight);</script>");
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:Console.Title");
	}
}
