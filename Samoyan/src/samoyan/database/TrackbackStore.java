package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;

public final class TrackbackStore extends DataBeanStore<Trackback>
{
	private static TrackbackStore instance = new TrackbackStore();

	protected TrackbackStore()
	{
	}
	public final static TrackbackStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Trackback> getBeanClass()
	{
		return Trackback.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Trackbacks");
		
		td.defineCol("Addressee", String.class).size(0, Trackback.MAXSIZE_ADDRESSEE).invariant();
		td.defineCol("Channel", String.class).size(0, Trackback.MAXSIZE_CHANNEL).invariant();
		td.defineCol("RoundRobin", Integer.class).invariant();
		td.defineCol("Created", Date.class).invariant();
		td.defineCol("ExternalID", String.class).size(0, Trackback.MAXSIZE_EXTERNALID);
		
		return td;
	}

	// - - -
	
	public void save(Trackback trackback) throws Exception
	{
		if (trackback.isSaved())
		{
			super.save(trackback);
			return;
		}
		
		// We do our own insertion here, because we need to generate unique round robin number
		
		// Insert a new trackback
		Query q = new Query();
		try
		{
			String sql = "INSERT INTO Trackbacks (ID, Channel, Addressee, Created, ExternalID, RoundRobin) VALUES(?,?,?,?,?," + 
						"((ISNULL((SELECT TOP 1 RoundRobin FROM Trackbacks WHERE Addressee=? ORDER BY Created DESC), 0)+1)%?))";
			ParameterList params = new ParameterList();
			params.plus(trackback.getID());
			params.plus(trackback.getChannel());
			params.plus(trackback.getAddressee());
			params.plus(trackback.getCreated().getTime());
			params.plus(trackback.getExternalID());
			params.plus(trackback.getAddressee());
			params.plus(Trackback.MAX_ROUNDROBIN);
			
			int result = q.update(sql, params);
			if (result!=1)
			{
				throw new SQLException("Failed to insert Trackback");
			}
		}
		finally
		{
			q.close();
		}
		
		// Get the roundrobin number and update the bean
		try
		{
			ResultSet rs = q.select("SELECT RoundRobin FROM Trackbacks WHERE ID=?", new ParameterList(trackback.getID()));
			if (!rs.next())
			{
				throw new SQLException("Failed to query RoundRobin of Trackback");
			}
			trackback.setRoundRobin(rs.getInt(1));
			trackback.setSaved(true);
			trackback.clearDirty();
		}
		finally
		{
			q.close();
		}
	}
	
	public Trackback loadByExternalID(String externalID) throws Exception
	{
		return loadByColumn("ExternalID", externalID);
	}

	public Trackback loadByIncomingText(String channel, String addressee, String text) throws Exception
	{
		if (text!=null)
		{
			int digits = (int) Math.log10(Trackback.MAX_ROUNDROBIN);
			text = text.trim();
			if (text.length()>=1+digits &&
				text.substring(0, 1).equals(Trackback.PREFIX) &&
				text.substring(1, 1+digits).matches("[0-9]*")) // #1234 at start of line
			{
				// Find exact trackback by code
				String trackbackStr = text.substring(1, digits+1);
				
				List<UUID> trackbackIDs = Query.queryListUUID(
						"SELECT TOP 1 ID FROM Trackbacks WHERE Channel=? AND Addressee=? AND RoundRobin=? ORDER BY Created DESC",
						new ParameterList(channel).plus(addressee).plus(Integer.parseInt(trackbackStr)));
				if (trackbackIDs.size()==1)
				{
					return load(trackbackIDs.get(0));
				}
			}
		}
		
		// Find latest within last session
//		Date after = new Date(System.currentTimeMillis() - Setup.getSessionLength());
//		List<UUID> trackbackIDs = Query.queryListUUID(
//				"SELECT ID FROM Trackbacks WHERE Channel=? AND Addressee=? AND Created>? ORDER BY Created DESC",
//				new ParameterList(channel).plus(addressee).plus(after));
		List<UUID> trackbackIDs = Query.queryListUUID(
				"SELECT ID FROM Trackbacks WHERE Channel=? AND Addressee=? ORDER BY Created DESC",
				new ParameterList(channel).plus(addressee));
		if (trackbackIDs.size()>0)
		{
			return load(trackbackIDs.get(0));
		}

		return null;
	}

	public String cleanIncomingText(String text)
	{
		int digits = (int) Math.log10(Trackback.MAX_ROUNDROBIN);
		if (text.matches("^\\x23[0-9]{" + digits + "}\\b.*")) // #1234 at start of line
		{
			return text.substring(digits+1).trim();
		}
		else
		{
			return text.trim();
		}
	}
		
	public void removeOlder(Date before) throws Exception
	{
		removeMany(Query.queryListUUID("SELECT ID From Trackbacks WHERE Created<?", new ParameterList(before)));
	}
}
