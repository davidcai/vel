package elert.pages;

import elert.app.ElertConsts;
import samoyan.database.PermissionStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public abstract class ElertPage extends WebPage
{
	public static final String COMMAND_GOVERN = "govern";
	public static final String COMMAND_SCHEDULE = "schedule";
	public static final String COMMAND_PATIENT = "patient";
	public static final String COMMAND_PHYSICIAN = "physician";

	@Override
	public boolean isAuthorized() throws Exception
	{
		RequestContext ctx = getContext();
		String perm = null;
		if (ctx.getCommand(1).equalsIgnoreCase(COMMAND_GOVERN))
		{
			perm = ElertConsts.PERMISSION_APPLICATION_GOVERNMENT;
		}
		else if (ctx.getCommand(1).equalsIgnoreCase(COMMAND_SCHEDULE))
		{
			perm = ElertConsts.PERMISSION_SCHEDULING;
		}
		else if (ctx.getCommand(1).equalsIgnoreCase(COMMAND_PHYSICIAN))
		{
			perm = ElertConsts.PERMISSION_PHYSICIAN;
		}
		if (perm==null)
		{
			return ctx.getUserID()!=null;
		}
		else
		{
			return PermissionStore.getInstance().isUserGrantedPermission(ctx.getUserID(), perm);
		}
	}
	
	protected void writeLegend() throws Exception
	{
		String[] legend = {	
				"elert/circle-v.png", "elert:Legend.Accepted",
				"elert/circle-x.png", "elert:Legend.Declined",
				"elert/circle-q.png", "elert:Legend.DidNotReply",
				"elert/finalized.png", "elert:Legend.Finalized",
				"elert/verified.png", "elert:Legend.Verified",
				"elert/urgent.png", "elert:Legend.Urgent",
				"elert/perishable.png", "elert:Legend.Perishable"
		};
		
		write("<table class=SchedulerLegend>");
		write("<tr><td colspan=2>");
		writeEncode(getString("elert:Legend.Legend"));
		write("</td></tr>");
		
		for (int i=0; i<legend.length; i+=2)
		{
			write("<tr><td>");
			writeImage(legend[i], getString(legend[i+1]));
			write("</td><td>");
			writeEncode(getString(legend[i+1]));
			write("</td></tr>");
		}
		
		write("</table>");
	}
}
