package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class LogTypeStore extends DataBeanStore<LogType>
{
	private static LogTypeStore instance = new LogTypeStore();
	
	protected LogTypeStore()
	{
	}
	public final static LogTypeStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<LogType> getBeanClass()
	{
		return LogType.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("LogTypes", this);
		
		td.defineCol("Name", String.class).size(0, LogEntry.MAXSIZE_NAME).invariant();
		td.defineCol("Severity", Integer.class);
		td.defineCol("Life", Long.class);

		td.defineCol("M1Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("M2Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("M3Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("M4Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		
		td.defineCol("S1Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("S2Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("S3Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("S4Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		
		td.defineCol("T1Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		td.defineCol("T2Label", String.class).size(0, LogType.MAXSIZE_LABEL);
		
		return td;
	}

	// - - -

	public LogType loadByName(String name) throws Exception
	{
		return getInstance().loadByColumn("Name", name);
	}
	
	public LogType openByName(String name) throws Exception
	{
		return getInstance().openByColumn("Name", name);
	}

	public List<String> getNames() throws SQLException
	{
		List<String> result = new ArrayList<String>();
		Query q = new Query();
		try
		{
			ResultSet rs = q.select("SELECT DISTINCT Name, Severity FROM LogTypes ORDER BY Severity DESC, Name ASC");
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
			return result;
		}
		finally
		{
			q.close();
		}
	}
}
