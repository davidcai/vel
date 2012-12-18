package samoyan.apps.admin.log;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.admin.typeahead.UserTypeAhead;
import samoyan.apps.admin.usermgmt.UserPage;
import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.LogEntry;
import samoyan.database.LogEntryStore;
import samoyan.database.LogType;
import samoyan.database.LogTypeStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.WebFormException;

public class QueryLogPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/system-log";

	public static final String PARAM_LOGINNAME = "loginname";

	private boolean runQuery = false;
	
	@Override
	public void validate() throws Exception
	{
		// Dates
		validateParameterDate("datefrom");
		validateParameterDate("dateto");
		
		// IP address
		String ip = validateParameterString("ip", 0, 15);
		if (!Util.isEmpty(ip))
		{
			try
			{
				InetAddress.getByName(ip);
			}
			catch (Exception e)
			{
				throw new WebFormException("ip", getString("common:Errors.InvalidValue"));
			}
		}
		
		// User
		Pair<String, String> userKvp = getParameterTypeAhead("user");
		if (!Util.isEmpty(userKvp.getValue()))
		{
			User user = UserStore.getInstance().loadByLoginName(userKvp.getKey());
			if (user==null)
			{
				throw new WebFormException("user", getString("common:Errors.InvalidValue"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{		
		this.runQuery = true;
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:QueryLog.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (UserStore.getInstance().loadByLoginName(getParameterString(PARAM_LOGINNAME))!=null)
		{
			this.runQuery = true;
		}

		writeFormOpen();
		
		renderForm();
		
		if (this.runQuery)
		{
			write("<br><br>");
			renderQuery();
		}
		
		writeFormClose();
	}

	private void renderForm() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);

		// Dates
		twoCol.writeRow(getString("admin:QueryLog.Time"));
		twoCol.writeDateTimeInput("datefrom", getDefaultDate(0));
		twoCol.write(" ");
		twoCol.writeEncode(getString("admin:QueryLog.Through"));
		twoCol.write(" ");
		twoCol.writeDateTimeInput("dateto", getDefaultDate(1));

		// Server
		twoCol.writeRow(getString("admin:QueryLog.Server"));
		twoCol.writeTextInput("server", null, 20, LogEntry.MAXSIZE_SERVER);
		
		// IP
		twoCol.writeRow(getString("admin:QueryLog.IP"));
		twoCol.writeTextInput("ip", null, 20, 15);

		// User
		User u = UserStore.getInstance().loadByLoginName(getParameterString(PARAM_LOGINNAME));;
		twoCol.writeRow(getString("admin:QueryLog.User"));
		twoCol.writeTypeAheadInput("user", u!=null? u.getLoginName() : null, u!=null? u.getDisplayName() : null, 40, User.MAXSIZE_LOGINNAME, getPageURL(UserTypeAhead.COMMAND));
		
		// Types
		twoCol.writeRow(getString("admin:QueryLog.Type"));
		new CheckboxInputControl(twoCol, "typeall").affectAll("type_").setLabel(getString("admin:QueryLog.All")).setInitialValue(true).render();
		twoCol.write("<table><tr><td>");
		for (String t : LogTypeStore.getInstance().getNames())
		{
			LogType type = LogTypeStore.getInstance().loadByName(t);
			if (type.getSeverity()==LogEntry.ERROR)
			{
				twoCol.writeCheckbox("type_" + t, t, true);
				twoCol.write("<br>");
			}
		}
		twoCol.write("</td><td>");
		for (String t : LogTypeStore.getInstance().getNames())
		{
			LogType type = LogTypeStore.getInstance().loadByName(t);
			if (type.getSeverity()==LogEntry.WARNING)
			{
				twoCol.writeCheckbox("type_" + t, t, true);
				twoCol.write("<br>");
			}
		}
		twoCol.write("</td><td>");
		for (String t : LogTypeStore.getInstance().getNames())
		{
			LogType type = LogTypeStore.getInstance().loadByName(t);
			if (type.getSeverity()==LogEntry.INFO)
			{
				twoCol.writeCheckbox("type_" + t, t, true);
				twoCol.write("<br>");
			}
		}
		twoCol.write("</td></tr></table>");
		
		twoCol.render();
		
		write("<br>");
		writeButton("query", getString("controls:Button.Query"));
		
		writeHiddenInput("at", "0");
	}
	
	private void renderQuery() throws Exception
	{
		// Dates
		Date from = getParameterDate("datefrom");
		if (from==null)
		{
			from = getDefaultDate(0);
		}
		Date to = getParameterDate("dateto");
		if (to==null)
		{
			to = getDefaultDate(1);
		}
		
		// Types
		Set<String> types = new HashSet<String>();
		for (String x : getContext().getParameterNamesThatStartWith("type_"))
		{
			types.add(x.substring(5));
		}
		
		// Server
		String server = getParameterString("server");
		if (Util.isEmpty(server))
		{
			server = null;
		}
		
		// User ID
		User user = null;
		if (isParameter(PARAM_LOGINNAME))
		{
			user = UserStore.getInstance().loadByLoginName(getParameterString(PARAM_LOGINNAME));
		}
		else if (isParameter("user"))
		{
			user = UserStore.getInstance().loadByLoginName(getParameterTypeAhead("user").getKey());
		}
		UUID userID = (user==null? null : user.getID());
				
		// IP
		String ip = getParameterString("ip");
		if (Util.isEmpty(ip))
		{
			ip = null;
		}
		
		if (types.size()==0)
		{
			writeEncode(getString("admin:QueryLog.NoResults"));
			return;
		}

		List<UUID> entryIDs = LogEntryStore.getInstance().queryLog(from, to, server, userID, ip, null, types);
		if (entryIDs.size()==0)
		{
			writeEncode(getString("admin:QueryLog.NoResults"));
			return;
		}

		new DataTableControl<UUID>(this, "logs", entryIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column(getString("admin:QueryLog.Time"));
				column(getString("admin:QueryLog.Server"));
				column(getString("admin:QueryLog.User"));
				column(getString("admin:QueryLog.IP"));
				column(getString("admin:QueryLog.Type"));
				column(getString("admin:QueryLog.Measures"));
				column(getString("admin:QueryLog.Data"));
			}

			@Override
			protected void renderRow(UUID entryID) throws Exception
			{
				LogEntry log = LogEntryStore.getInstance().load(entryID);
				
				cell();
				writeEncodeDateTime(log.getTime());
				
				cell();
				writeEncode(log.getServer());

				cell();
				if (log.getUserID()!=null)
				{
					User u = UserStore.getInstance().load(log.getUserID());
					if (u!=null)
					{
						writeLink(u.getLoginName(), getPageURL(UserPage.COMMAND, new ParameterMap(UserPage.PARAM_ID, u.getID().toString())));
					}
				}

				cell();
				if (log.getIPAddress()!=null)
				{
					writeEncode(log.getIPAddress().getHostAddress());
				}
				
				cell();
				writeLink(log.getName(), getPageURL(LogEntryPage.COMMAND, new ParameterMap(LogEntryPage.PARAM_ID, log.getID().toString())));
				
				cell();
				boolean comma = false;
				for (int m=1; m<=LogEntry.NUM_MEASURES; m++)
				{
					Double measure = log.getMeasure(m);
					if (measure!=null)
					{
						String label = log.getMeasureLabel(m);
						if (comma)
						{
							write(", ");
						}
						if (!Util.isEmpty(label))
						{
							write("<span title=\"");
							writeEncode(label);
							write("\">");
						}
						String measureStr;
						if (measure==Math.round(measure))
						{
							measureStr = String.valueOf(Math.round(measure));
						}
						else
						{
							measureStr = String.valueOf(measure);
							int dot = measureStr.indexOf(".");
							if (dot>=0 && dot<measureStr.length()-3)
							{
								measureStr = measureStr.substring(0, dot+3);
							}
						}
						writeEncode(measureStr);
//						if (measure==Math.round(measure))
//						{
//							writeEncodeLong(measure.longValue());
//						}
//						else
//						{
//							writeEncodeFloat((float) measure.floatValue(), 2);
//						}
						if (!Util.isEmpty(label))
						{
							write("</span>");
						}
						comma = true;
					}
				}
				
				cell();
				comma = false;
				for (int s=1; s<=LogEntry.NUM_STRINGS; s++)
				{
					String str = log.getString(s);
					if (str!=null)
					{
						String label = log.getStringLabel(s);
						if (comma)
						{
							write(", ");
						}
						if (!Util.isEmpty(label))
						{
							write("<span title=\"");
							writeEncode(log.getStringLabel(s));
							write("\">");
						}
						writeEncode(str);
						if (!Util.isEmpty(label))
						{
							write("</span>");
						}
						comma = true;
					}
				}
			}
		}.render();
	}
	
	private Date getDefaultDate(int x)
	{
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, x);
		return cal.getTime();
	}
}
