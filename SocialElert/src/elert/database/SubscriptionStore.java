package elert.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class SubscriptionStore extends DataBeanStore<Subscription>
{
	private static SubscriptionStore instance = new SubscriptionStore();

	protected SubscriptionStore()
	{
	}

	public final static SubscriptionStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Subscription> getBeanClass()
	{
		return Subscription.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Subscriptions");

		td.defineCol("UserID", UUID.class).ownedBy("Users").invariant();
		td.defineCol("AreaID", UUID.class).refersTo("ServiceAreas");
		td.defineCol("AdvanceNotice", Integer.class);
		td.defineCol("OriginalDate", Date.class);
		td.defineCol("AcceptOtherPhysician", Boolean.class);
		td.defineCol("VerifiedBy", UUID.class);
		td.defineCol("VerifiedDate", Date.class);
		td.defineCol("CreatedDate", Date.class);
		td.defineCol("Urgent", Boolean.class);
		td.defineCol("Duration", Integer.class);
		td.defineCol("Finalized", Boolean.class);
		td.defineCol("Removed", Boolean.class);

		td.defineProp("Reason", String.class).size(0, Subscription.MAXSIZE_REASON);
		td.defineProp("AlwaysAvailable", Boolean.class);
		// Dynamic AvailableYYYYMM
		
		return td;
	}

	// - - -
	
	/**
	 * Returns the list of subscriptions of the user, sorted by creation date (latest first).
	 * @param userID
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getByUserID(UUID userID) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Subscriptions WHERE UserID=? AND Removed=0 ORDER BY CreatedDate DESC", new ParameterList(userID));
		
//		return queryByColumn("UserID", userID, "CreatedDate", false);
	}
	
	/**
	 * Returns a list of non-expired subscriptions related to the physician sorted by patient names. Only includes
	 * subscriptions that are either related to the physician or allow other physicians in facilities of the patient's
	 * service area. 
	 * 
	 * @param physicianID
	 * @return
	 * @throws Exception
	 */
	public List<UUID> queryPhysicianSubscriptions(UUID physicianID) throws Exception
	{
		List<UUID> subs = new ArrayList<UUID>();
		
		//	select * from Subscriptions as sub
		//	join SubscriptionPhysicianLink as spl
		//		on sub.id = spl.SubscriptionID
		//	join Users as u
		//		on sub.UserID = u.ID
		//	where 
		//		sub.AreaID in (
		//			select distinct ServiceAreaID from Facilities as fac
		//			join PhysicianFacilityLink as pfl
		//				on fac.ID = pfl.FacilityID
		//			where pfl.PhysicianID = 0x93631B765E91459980FC52344BA969AD
		//		)
		//		and
		//		(
		//			sub.AcceptOtherPhysician = 1 or spl.PhysicianID = 0x93631B765E91459980FC52344BA969AD
		//		) 
		//		and sub.CreatedDate >= 1347745185082
		//		and sub.Removed = 0
		//	order by u.Name
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT sub.ID FROM Subscriptions AS sub ");
		sql.append("JOIN SubscriptionPhysicianLink AS spl ");
		sql.append("  ON sub.ID = spl.SubscriptionID ");
		sql.append("JOIN Users AS u ");
		sql.append("  ON sub.UserID = u.ID ");
		sql.append("WHERE ");
		sql.append("  sub.AreaID IN (");
		sql.append("    SELECT DISTINCT ServiceAreaID FROM Facilities AS fac ");
		sql.append("	JOIN PhysicianFacilityLink AS pfl ");
		sql.append("      ON fac.ID = pfl.FacilityID ");
		sql.append("    WHERE pfl.PhysicianID = ? "); 
		sql.append("  ) "); //-- sub.AreaID IN (
		sql.append("  AND (");
		sql.append("    sub.AcceptOtherPhysician = 1 OR spl.PhysicianID = ?"); 
		sql.append("  )"); //-- AND (
		sql.append("  AND sub.Removed = 0 ");
		sql.append("  AND sub.CreatedDate >= ? "); 
		sql.append("ORDER BY u.Name");

		
		// 1st day of today's next month minus Subscription.MAX_AVAILABILITY_MONTHS
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, 1 - Subscription.MAX_AVAILABILITY_MONTHS);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		
		subs = Query.queryListUUID(sql.toString(), new ParameterList()
			.plus(physicianID)
			.plus(physicianID)
			.plus(cal.getTime())
		);
		
		return subs;
	}
	
	/**
	 * Returns a list of non-finalized subscription listed in any of the given service areas.
	 * The list is sorted by creation date, latest first.
	 * @param areaIDs
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> queryOpenSubscriptions(List<UUID> areaIDs, int maxDuration) throws SQLException
	{
		if (areaIDs.size()==0)
		{
			return new ArrayList<UUID>();
		}
		
		ParameterList params = new ParameterList();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID FROM Subscriptions WHERE AreaID IN (");
		for (int f=0; f<areaIDs.size(); f++)
		{
			if (f>0)
			{
				sql.append(",");
			}
			sql.append("?");
			params.add(areaIDs.get(f));
		}
		sql.append(") AND Finalized=0 AND Removed=0 AND Duration<=? AND CreatedDate>=? ORDER BY CreatedDate DESC");
		params.add(maxDuration);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -Subscription.MAX_AVAILABILITY_MONTHS);
		params.add(cal.getTime());

		return Query.queryListUUID(sql.toString(), params);
	}
}
