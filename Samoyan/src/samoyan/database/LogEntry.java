package samoyan.database;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import samoyan.core.Util;
import samoyan.servlet.RequestContext;

public class LogEntry extends DataBean
{
	private static String machineName = null;
	
	public final static int INFO = 0;
	public final static int WARNING = 1;
	public final static int ERROR = 2;
	
	public static final int MAXSIZE_SERVER = 16; // Must be same as Server.MAXSIZE_NAME
	public static final int MAXSIZE_NAME = 32;
	public static final int MAXSIZE_STRING = 256;
	
	public final static int NUM_MEASURES = 4;
	public final static int NUM_STRINGS = 4;
	public final static int NUM_TEXTS = 2;

	private Map<String, String> labels = new HashMap<String, String>();
	
	LogEntry()
	{
	}
	
	public LogEntry(String name, int severity)
	{
		if (machineName==null)
		{
			String mn = Util.getLocalHostName();
			if (mn.length()>MAXSIZE_SERVER)
			{
				mn = mn.substring(0, MAXSIZE_SERVER);
			}
			machineName = mn;
		}
		
		setName(name);
		setSeverity(severity);
		setServer(machineName);
		setTime(new Date());

		RequestContext ctx = RequestContext.getCurrent();
		if (ctx!=null)
		{			
			setRequestContext(ctx);
		}
	}
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}
	public UUID getSessionID()
	{
		return (UUID) get("SessionID");
	}
	public void setSessionID(UUID sessionID)
	{
		set("SessionID", sessionID);
	}
	public InetAddress getIPAddress()
	{
		return (InetAddress) get("IP");
	}
	public void setIPAddress(InetAddress ipAddress)
	{
		set("IP", ipAddress);
	}
	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String name)
	{
		if (name!=null && name.length()>MAXSIZE_NAME)
		{
			name = name.substring(0, MAXSIZE_NAME);
		}
		set("Name", name);
	}
	public String getServer()
	{
		return (String) get("Server");
	}
	public void setServer(String server)
	{
		if (server!=null && server.length()>MAXSIZE_SERVER)
		{
			server = server.substring(0, MAXSIZE_SERVER);
		}
		set("Server", server);
	}
	public Date getTime()
	{
		return (Date) get("Time");
	}
	public void setTime(Date time)
	{
		set("Time", time);
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
	 * Sets the value of the indicated measure.
	 * @param index The index of the measure to set, 1 to NUM_MEASURES.
	 * @param name The label to set for this measure.
	 * @param val The value of the measure.
	 */
	public void setMeasure(int index, String name, double val)
	{
		this.labels.put("M"+index+"Label", name);
		set("M"+index, val);
	}
	/**
	 * Sets the value of the indicated string.
	 * @param index The index of the string to set, 1 to NUM_STRINGS.
	 * @param name The label to set for this string.
	 * @param val The value of the string. Must be shorter than <code>MAXSIZE_STRING</code>.
	 */
	public void setString(int index, String name, String val)
	{
		if (val!=null && val.length()>MAXSIZE_STRING)
		{
			val = val.substring(0, MAXSIZE_STRING);
		}
		this.labels.put("S"+index+"Label", name);
		set("S"+index, val);
	}
	/**
	 * Sets the value of the indicated text.
	 * @param index The index of the string to set, 1 to NUM_TEXTS.
	 * @param name The label to set for this text.
	 * @param val The value of the text. No length limit.
	 */
	public void setText(int index, String name, String val)
	{
		this.labels.put("T"+index+"Label", name);
		set("T"+index, val);
	}
	
	public Double getMeasure(int index)
	{
		return (Double) get("M"+index);
	}
	public String getString(int index)
	{
		return (String) get("S"+index);
	}
	public String getText(int index)
	{
		return (String) get("T"+index);
	}

	public String getMeasureLabel(int index)
	{
		return this.labels.get("M"+index+"Label");
	}
	public String getStringLabel(int index)
	{
		return this.labels.get("S"+index+"Label");
	}
	public String getTextLabel(int index)
	{
		return this.labels.get("T"+index+"Label");
	}
	
	public void setMeasureLabel(int index, String label)
	{
		this.labels.put("M"+index+"Label", label);
	}
	public void setStringLabel(int index, String label)
	{
		this.labels.put("S"+index+"Label", label);
	}
	public void setTextLabel(int index, String label)
	{
		this.labels.put("T"+index+"Label", label);
	}
	
	public String getRequestContext()
	{
		return (String) get("ReqCtx");
	}
	public void setRequestContext(RequestContext ctx)
	{
		try
		{
			setIPAddress(InetAddress.getByName(ctx.getIPAddress()));
		}
		catch (UnknownHostException e)
		{
			// Ignore error
		}
		setUserID(ctx.getUserID());
		setSessionID(ctx.getSessionID());
		setTime(new Date(ctx.getTime()));		

		set("ReqCtx", ctx.toString());
	}
}
