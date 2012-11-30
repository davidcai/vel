package samoyan.apps.admin.log;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.usermgmt.UserPage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.LogEntry;
import samoyan.database.LogEntryStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.PageNotFoundException;

public class LogEntryPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/log-entry";
	
	public final static String PARAM_ID = "id";

	private LogEntry logEntry;
	
	@Override
	public void init() throws Exception
	{
		this.logEntry = LogEntryStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.logEntry==null)
		{
			throw new PageNotFoundException();
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);

		// Time
		twoCol.writeRow(getString("admin:LogEntry.Time"));
		twoCol.writeEncodeDateTime(this.logEntry.getTime());
		
		// Type
		twoCol.writeRow(getString("admin:LogEntry.Type"));
		twoCol.writeEncode(this.logEntry.getName());
		
		// Server
		if (!Util.isEmpty(this.logEntry.getServer()))
		{
			twoCol.writeRow(getString("admin:LogEntry.Server"));
			twoCol.writeEncode(this.logEntry.getServer());
		}
		
		// User
		if (this.logEntry.getUserID()!=null)
		{
			twoCol.writeRow(getString("admin:LogEntry.User"));

			User u = UserStore.getInstance().load(this.logEntry.getUserID());
			if (u!=null)
			{
				twoCol.writeLink(u.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, u.getID().toString())));
			}
		}
		
		// IP
		if (this.logEntry.getIPAddress()!=null)
		{
			twoCol.writeRow(getString("admin:LogEntry.IP"));
			twoCol.writeEncode(this.logEntry.getIPAddress().getHostAddress());
		}
				
		// Request
		if (this.logEntry.getRequestContext()!=null)
		{
//			twoCol.writeSpaceRow();
			twoCol.writeRow(getString("admin:LogEntry.Request"));
			twoCol.write("<pre>");
			twoCol.writeEncode(this.logEntry.getRequestContext());
			twoCol.write("</pre>");
		}

		// Measures
		for (int m=1; m<=LogEntry.NUM_MEASURES; m++)
		{
			Double measure = this.logEntry.getMeasure(m);
			if (measure!=null)
			{
				if (m==1)
				{
					twoCol.writeSpaceRow();
				}
				String label = this.logEntry.getMeasureLabel(m);
				if (label==null) label = "";
				twoCol.writeRow(label);
				if (measure==Math.round(measure))
				{
					twoCol.writeEncodeLong(measure.longValue());
				}
				else
				{
					twoCol.writeEncodeFloat((float) measure.floatValue(), 2);
				}
			}
		}
		
		// Strings
		for (int s=1; s<=LogEntry.NUM_STRINGS; s++)
		{
			String str = this.logEntry.getString(s);
			if (str!=null)
			{
				if (s==1)
				{
					twoCol.writeSpaceRow();
				}
				String label = this.logEntry.getStringLabel(s);
				if (label==null) label = "";
				twoCol.writeRow(label);
				twoCol.writeEncode(str);
			}
		}
		
		// Texts
		for (int t=1; t<=LogEntry.NUM_TEXTS; t++)
		{
			String txt = this.logEntry.getText(t);
			if (txt!=null)
			{
				if (t==1)
				{
					twoCol.writeSpaceRow();
				}
				String label = this.logEntry.getTextLabel(t);
				if (label==null) label = "";
				twoCol.writeRow(label);
				twoCol.write("<pre>");
				twoCol.writeEncode(txt);
				twoCol.write("</pre>");
			}
		}
		
		twoCol.render();
	}

	@Override
	public String getTitle() throws Exception
	{
		return this.logEntry.getName();
	}
	
	
}
