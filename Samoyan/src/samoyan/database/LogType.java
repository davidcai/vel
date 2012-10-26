package samoyan.database;

public final class LogType extends DataBean
{
	public static final int MAXSIZE_LABEL = 32;
	
	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String name)
	{
		set("Name", name);
	}

	public int getSeverity()
	{
		return (Integer) get("Severity");
	}
	public void setSeverity(int severity)
	{
		set("Severity", severity);
	}

	/**
	 * Sets how many days to store a given log entry in the database.
	 * @param days The number of days to store the log entry in the database. <code>0</code> indicates not to store at all;
	 * a negative number indicates to store forever (default).
	 */
	public void setLife(int days)
	{
		set("Life", (long) days * 24L*60L*60L*1000L);
	}
	/**
	 * Indicates how many days to store a given log entry in the database.
	 * @return The number of days to store the log entry in the database. <code>0</code> indicates not to store at all;
	 * a negative number indicates to store forever (default).
	 */
	public int getLife()
	{
		Long millis = (Long) get("Life", -1L);
		if (millis==null || millis<0L)
		{
			return -1;
		}
		else
		{
			return (int) (millis / (24L*60L*60L*1000L));
		}
	}
	
	public void setMeasureLabel(int index, String label)
	{
		set("M"+index+"Label", label);
	}
	public void setStringLabel(int index, String label)
	{
		set("S"+index+"Label", label);
	}
	public void setTextLabel(int index, String label)
	{
		set("T"+index+"Label", label);
	}

	public String getMeasureLabel(int index)
	{
		return (String) get("M"+index+"Label");
	}
	public String getStringLabel(int index)
	{
		return (String) get("S"+index+"Label");
	}
	public String getTextLabel(int index)
	{
		return (String) get("T"+index+"Label");
	}
}
