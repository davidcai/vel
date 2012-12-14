package elert.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.database.DataBeanStore;
import samoyan.database.Notification;
import samoyan.database.Query;
import samoyan.database.QueryIterator;
import samoyan.database.TableDef;

public final class ElertStore extends DataBeanStore<Elert>
{
	private static ElertStore instance = new ElertStore();

	protected ElertStore()
	{
	}

	public final static ElertStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Elert> getBeanClass()
	{
		return Elert.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Elerts");

		td.defineCol("SubscriptionID", UUID.class).refersTo("Subscriptions").invariant();
		td.defineCol("OpeningID", UUID.class).refersTo("Openings").invariant();
		td.defineCol("SchedulerID", UUID.class).refersTo("Users").invariant();
		td.defineCol("PatientID", UUID.class).refersTo("Users").invariant();
		td.defineCol("RegionID", UUID.class).refersTo("Regions").invariant();
		td.defineCol("ServiceAreaID", UUID.class).refersTo("ServiceAreas").invariant();
		td.defineCol("FacilityID", UUID.class).refersTo("Facilities").invariant();
		
		td.defineCol("SentDate", Date.class).invariant();
		td.defineCol("OpeningDate", Date.class).invariant();
		
		td.defineCol("Reply", Integer.class);
		td.defineCol("ReplyChannel", String.class).size(0, Elert.MAXSIZE_CHANNEL);
		td.defineCol("ReplyDate", Date.class);

		td.defineCol("Decision", Integer.class);
		td.defineCol("DecisionDate", Date.class);
		
		td.defineCol("Hidden", Boolean.class);

		return td;
	}

	// - - -
	
	public UUID getFinalForSubscription(UUID subscriptionID) throws Exception
	{
		List<UUID> ids = Query.queryListUUID("SELECT ID FROM Elerts WHERE SubscriptionID = ? AND Decision = 1", new ParameterList(subscriptionID));
		if (ids.size() == 1)
		{
			return ids.get(0);
		}
		else
		{
			return null;
		}
	}
	
	public Elert loadByOpeningAndSubscription(UUID openingID, UUID subscriptionID) throws Exception
	{
		List<UUID> ids = Query.queryListUUID("SELECT ID FROM Elerts WHERE SubscriptionID=? AND OpeningID=?", new ParameterList(subscriptionID).plus(openingID));
		if (ids.size()==1)
		{
			return super.load(ids.get(0));
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns all eLerts sent for the opening, in reverse chronological order of their sent date.
	 * @param openingID
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> queryByOpeningID(UUID openingID) throws SQLException
	{
		return queryByColumn("OpeningID", openingID, "SentDate", false);
	}

	/**
	 * Returns all eLerts sent to the given user, in reverse chronological order of their sent date.
	 */
	public List<UUID> queryByPatientID(UUID patientID) throws SQLException
	{
		return queryByColumn("PatientID", patientID, "SentDate", false);
	}
	
	public class ChannelStat
	{
		public long date;
		public String channel;
		public UUID id;
	}
	
	/**
	 * 
	 * @param incoming When <code>true</code>, stats are on eLert replies; when <code>false</code>, stats are on outgoing eLerts.
	 * @param from Elert must have been created on or after this date
	 * @param to Elert must have been created before this date
	 * @param regionID Region in which the eLert was created.
	 * @param serviceAreaID The service area in which the eLert was created.
	 * If provided, <code>regionID</code> is ignored.
	 * @param facilityID Facility in which eLert was created.
	 * If provided, <code>serviceAreaID</code> and <code>regionID</code> are ignored.
	 * @param schedulerID The scheduler who created the eLert.
	 * @param physicianID A physician that is assigned to the opening associated with this eLErt.
	 * @param procedureID A procedure assigned to the subscription associated with this eLert.
	 * @return 
	 * @throws SQLException 
	 */
	public QueryIterator<ChannelStat> queryChannelStats(boolean incoming, Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws SQLException
	{
		StringBuilder sql = new StringBuilder();
		ParameterList params = new ParameterList();
		
		sql.append("SELECT Elerts.ID, Elerts.SentDate, ");
		if (incoming)
		{
			sql.append("Elerts.ReplyChannel ");
		}
		else
		{
			sql.append("Notifications.Channel ");
		}
		sql.append("FROM Elerts ");
		if (!incoming)
		{
			sql.append(", Notifications ");
		}
		if (physicianID!=null)
		{
			sql.append(", PhysicianOpeningLink ");
		}
		if (procedureID!=null)
		{
			sql.append(", SubscriptionProcedureLink ");
		}
		
		sql.append("WHERE 1=1 ");
		if (!incoming)
		{
			sql.append("AND Notifications.EventID=Elerts.ID AND (Notifications.StatusCode=? OR Notifications.StatusCode=?) ");
			params.plus(Notification.STATUS_SENT);
			params.plus(Notification.STATUS_DELIVERED);
		}
		
		// Dates
		if (from!=null)
		{
			sql.append("AND Elerts.SentDate>=? ");
			params.plus(from);
		}
		if (to!=null)
		{
			sql.append("AND Elerts.SentDate<? ");
			params.plus(to);
		}
		
		// Geography
		if (facilityID!=null)
		{
			sql.append("AND Elerts.FacilityID=? ");
			params.plus(facilityID);
		}
		else if (serviceAreaID!=null)
		{
			sql.append("AND Elerts.ServiceAreaID=? ");
			params.plus(serviceAreaID);
		}
		else if (regionID!=null)
		{
			sql.append("AND Elerts.RegionID=? ");
			params.plus(regionID);
		}
		
		// Scheduler
		if (schedulerID!=null)
		{
			sql.append("AND Elerts.SchedulerID=? ");
			params.plus(schedulerID);
		}

		// Physician
		if (physicianID!=null)
		{
			sql.append("AND Elerts.OpeningID=PhysicianOpeningLink.OpeningID AND PhysicianOpeningLink.PhysicianID=? ");
			params.plus(physicianID);
		}
		
		// Procedure
		if (procedureID!=null)
		{
			sql.append("AND Elerts.SubscriptionID=SubscriptionProcedureLink.SubscriptionID AND SubscriptionProcedureLink.ProcedureID=? ");
			params.plus(procedureID);
		}
		
		return new QueryIterator<ChannelStat>(sql.toString(), params)
		{
			@Override
			protected ChannelStat fromResultSet(ResultSet rs) throws Exception
			{
				ChannelStat stat = new ChannelStat();
				stat.id = Util.bytesToUUID(rs.getBytes(1));
				stat.date = rs.getLong(2);
				stat.channel = rs.getString(3);
				return stat;
			}
		};
	}
}
