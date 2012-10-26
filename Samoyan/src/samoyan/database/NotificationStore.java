package samoyan.database;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.core.Util;

public final class NotificationStore extends DataBeanStore<Notification>
{
	private static NotificationStore instance = new NotificationStore();

	protected NotificationStore()
	{
	}
	public final static NotificationStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Notification> getBeanClass()
	{
		return Notification.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Notifications", this);
		
		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("EventID", UUID.class).invariant();
		td.defineCol("Channel", String.class).size(0, Notification.MAXSIZE_CHANNEL).invariant();
		td.defineCol("Created", Date.class).invariant();
		td.defineCol("StatusDate", Date.class);
		td.defineCol("StatusCode", Integer.class);
		td.defineCol("FailCount", Integer.class);
		td.defineCol("ExternalID", String.class).size(0, Notification.MAXSIZE_EXTERNAL_ID);

		td.defineProp("Command", String.class).invariant();
//		td.defineProp("Markup", String.class);
		// Dynamic Prm_*
		
		return td;
	}

	// - - -
	
	public Notification loadByExternalID(String extID) throws Exception
	{
		if (Util.isEmpty(extID))
		{
			return null;
		}
		
		List<UUID> ids = Query.queryListUUID("SELECT TOP 1 ID FROM Notifications WHERE ExternalID=? ORDER BY Created DESC", new ParameterList(extID));
		if (ids.size()==0)
		{
			return null;
		}
		else
		{
			return load(ids.get(0));
		}
	}

	public Notification openByExternalID(String extID) throws Exception
	{
		Notification notif = loadByExternalID(extID);
		return (Notification) (notif==null? null : notif.clone());
	}
		
	public List<UUID> getByUserID(UUID userID) throws SQLException
	{
		return queryByColumn("UserID", userID, "StatusDate", true);
	}

	/**
	 * Gets the list of notification IDs linked to the give event ID, sorted by the SortDate of this notification.
	 * @param eventID
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getByEventID(UUID eventID) throws SQLException
	{
		return queryByColumn("EventID", eventID, "StatusDate", true);
	}
	
	public List<UUID> query(Date fromInclusive, Date toExclusive, String channel, UUID userID, UUID eventID, Integer statusCode) throws SQLException
	{
		ParameterList params = new ParameterList();
		String sql = "SELECT ID FROM Notifications WHERE 1=1";
		if (fromInclusive!=null)
		{
			sql += " AND StatusDate>=?";
			params.plus(fromInclusive);
		}
		if (toExclusive!=null)
		{
			sql += " AND StatusDate<?";
			params.plus(toExclusive);
		}
		if (!Util.isEmpty(channel))
		{
			sql += " AND Channel=?";
			params.plus(channel);
		}
		if (userID!=null)
		{
			sql += " AND UserID=?";
			params.plus(userID);
		}
		if (eventID!=null)
		{
			sql += " AND EventID=?";
			params.plus(eventID);
		}
		if (statusCode!=null)
		{
			sql += " AND StatusCode=?";
			params.plus(statusCode);
		}
		sql += " ORDER BY StatusDate ASC";
		
		return Query.queryListUUID(sql, params);
	}
	
	public void reportError(Notification notif, Date dateOfError)
	{
		int failCount = notif.getFailCount();
		notif.setFailCount(failCount+1);

//		final int minutes[] = {1,2,4,8,15,30,60,240,720,1440,2880};
		final int minutes[] = {1,2,4,8,15};
		if (failCount<minutes.length)
		{
			// Retry later in the future
			notif.setStatusCode(Notification.STATUS_UNSENT);
			notif.setDateStatus(new Date(dateOfError.getTime() + minutes[failCount] * 60L*1000L));
		}
		else
		{
			// Permanently failed
			notif.setStatusCode(Notification.STATUS_FAILED);
			notif.setDateStatus(dateOfError);
		}
	}
}
