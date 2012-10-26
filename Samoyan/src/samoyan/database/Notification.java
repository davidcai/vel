package samoyan.database;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Notification extends DataBean
{
	public final static int MAXSIZE_CHANNEL = 8;
	public final static int MAXSIZE_EXTERNAL_ID = 64;

	public final static int STATUS_FAILED = -1;
	public final static int STATUS_UNSENT = 0;
	public final static int STATUS_SENT = 1;
	public final static int STATUS_DELIVERED = 2;
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	public UUID getEventID()
	{
		return (UUID) get("EventID");
	}
	public void setEventID(UUID eventID)
	{
		set("EventID", eventID);
	}

	public String getChannel()
	{
		return (String) get("Channel");
	}
	public void setChannel(String channel)
	{
		set("Channel", channel);
	}
	
	/**
	 * The command to set for the <code>RequestContext</code> when executing this event.
	 * @return
	 */
	public String getCommand()
	{
		return (String) get("Command");
	}
	public void setCommand(String command)
	{
		set("Command", command);
	}
	
	/**
	 * The parameters to pass to the <code>RequestContext</code> when executing this event.
	 * @param name
	 * @return
	 */
	public String getParameter(String name)
	{
		return (String) get("Prm_" + name);
	}
	public void setParameter(String name, String value)
	{
		set("Prm_" + name, value);
	}
	
	/**
	 * Returns a new <code>HashMap</code> representing the parameters to pass to the <code>RequestContext</code> when executing this event.
	 * This is not the internal structure and modifications to it will not affect the bean.
	 * @return
	 */
	public Map<String, String> getParameters()
	{
		Map<String, String> params = new HashMap<String, String>();
		for (String n : super.names())
		{
			if (n.startsWith("Prm_"))
			{
				params.put(n.substring(4), (String) get(n));
			}
		}
		
		return params;
	}
	
	public void setParameters(Map<String, String> params)
	{
		if (params!=null)
		{
			for (String n : params.keySet())
			{
				setParameter(n, params.get(n));
			}
		}
	}

	/**
	 * The date when the notification was created.
	 * @return
	 */
	public Date getDateCreated()
	{
		return (Date) get("Created");
	}
	public void setDateCreated(Date date)
	{
		set("Created", date);
	}
	
	/**
	 * The date when the status of the notification was changed.
	 * @return
	 */
	public Date getDateStatus()
	{
		return (Date) get("StatusDate");
	}
	public void setDateStatus(Date date)
	{
		set("StatusDate", date);
	}

	/**
	 * The status of this notification: unsent, sent, delivered, or failed.
	 * @return
	 */
	public int getStatusCode()
	{
		return (Integer) get("StatusCode", 0);
	}
	public void setStatusCode(int code)
	{
		set("StatusCode", code);
	}
	
	public int getFailCount()
	{
		return (Integer) get("FailCount", 0);
	}
	public void setFailCount(int count)
	{
		set("FailCount", count);
	}
	
	/**
	 * An ID provided by the service provider (e.g. the SMS provider) when sending out hte notification.
	 * Allows to locate the notification on any callback from the provider.
	 * @return
	 */
	public String getExternalID()
	{
		return (String) get("ExternalID");
	}
	public void setExternalID(String extID)
	{
		set("ExternalID", extID);
	}
}
