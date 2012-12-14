package elert.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.QueryIterator;
import samoyan.database.TableDef;

public class OpeningStore extends DataBeanStore<Opening>
{
	private static OpeningStore instance = new OpeningStore();

	protected OpeningStore()
	{
	}	
	public final static OpeningStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Opening> getBeanClass()
	{
		return Opening.class;
	}	

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Openings");

		td.defineCol("RegionID", UUID.class).refersTo("Regions").invariant();	
		td.defineCol("ServiceAreaID", UUID.class).refersTo("ServiceAreas").invariant();	
		td.defineCol("FacilityID", UUID.class).refersTo("Facilities").invariant();	
		td.defineCol("Room", String.class).invariant();
		td.defineCol("DateTime", Date.class).invariant();
		td.defineCol("OriginalDuration", Integer.class).invariant();	
		td.defineCol("Duration", Integer.class);	
		td.defineCol("Closed", Boolean.class);	
		td.defineCol("SchedulerID", UUID.class).refersTo("Users").invariant();	
		
		return td;
	}
	
	// - - -
	
//	/**
//	 * Queries for the list of openings that are linked to the given list of facilities.
//	 * @param facilities
//	 * @throws SQLException 
//	 */
//	public List<UUID> queryByFacilities(List<UUID> facilities) throws SQLException
//	{
//		List<UUID> result = new ArrayList<UUID>();
//		for (UUID facilityID : facilities)
//		{
//			result.addAll(queryByColumn("FacilityID", facilityID));
//		}
//		return result;
//	}
	
	/**
	 * Returns the list of unresolved openings linked to the given list of facilities.
	 * @param facilityIDs
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> queryUnresolvedOpenings(List<UUID> facilityIDs) throws SQLException
	{
		if (facilityIDs.size()==0)
		{
			return new ArrayList<UUID>();
		}

		ParameterList params = new ParameterList();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID FROM Openings WHERE FacilityID IN (");
		for (int f=0; f<facilityIDs.size(); f++)
		{
			if (f>0)
			{
				sql.append(",");
			}
			sql.append("?");
			params.add(facilityIDs.get(f));
		}
		sql.append(") AND Closed=0 AND DateTime>=?");
		params.add(new Date());

		return Query.queryListUUID(sql.toString(), params);
	}
	
	/**
	 * Returns the list of openings linked to the given list of facilities.
	 * @param facilityIDs
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> queryOpenings(Date from, Date to, List<UUID> facilityIDs) throws SQLException
	{
		if (facilityIDs.size()==0)
		{
			return new ArrayList<UUID>();
		}

		ParameterList params = new ParameterList();
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ID FROM Openings WHERE FacilityID IN (");
		for (int f=0; f<facilityIDs.size(); f++)
		{
			if (f>0)
			{
				sql.append(",");
			}
			sql.append("?");
			params.add(facilityIDs.get(f));
		}
		sql.append(") AND DateTime>=? AND DateTime<? ORDER BY DateTime ASC");
		params.add(from);
		params.add(to);

		return Query.queryListUUID(sql.toString(), params);
	}

	/**
	 * Calculates the match percentage between an opening a subscription.
	 * @param opening
	 * @param sub
	 * @return
	 * @throws SQLException
	 */
	public Integer matchPercentage(Opening opening, Subscription sub) throws SQLException
	{
		// Get unique resource IDs of the subscription
		Set<UUID> subResIDs = new HashSet<UUID>();
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(sub.getID());
		for (UUID procID : procIDs)
		{
			subResIDs.addAll(ResourceProcedureLinkStore.getInstance().getResourcesForProcedure(procID));
		}
		
		// Run over resources of the opening and see which ones will be utilized
		int total = 0;
		int utilized = 0;
		procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(opening.getID());
		for (UUID procID : procIDs)
		{
			List<UUID> resIDs = ResourceProcedureLinkStore.getInstance().getResourcesForProcedure(procID);
			for (UUID resID : resIDs)
			{
				int rank = ResourceProcedureLinkStore.getInstance().getResourceRank(procID, resID);
				total += rank;
				if (subResIDs.contains(resID))
				{
					utilized += rank;
				}
			}
		}
		
		if (total==0)
		{
			return null;
		}
		else
		{
			return 100 * utilized / total;
		}
	}
	
	/**
	 * Calculates the best match between this opening and all the accepted, non-finalized eLerts.
	 * @param opening
	 * @return
	 * @throws Exception 
	 */
	public Integer bestMatchPercentage(Opening opening) throws Exception
	{
		Integer bestMatch = null;
		boolean bestMatchUrgent = false;

		List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
		for (int e=0; e<elertIDs.size(); e++)
		{
			Elert eLert = ElertStore.getInstance().load(elertIDs.get(e));
			if (eLert.getReply()==Elert.REPLY_ACCEPTED)
			{
				Subscription sub = SubscriptionStore.getInstance().load(eLert.getSubscriptionID());
				if (!sub.isFinalized())
				{
					Integer match = OpeningStore.getInstance().matchPercentage(opening, sub);
					if (match!=null)
					{
						if (bestMatch==null ||
							bestMatch < match ||
							(bestMatch==match && bestMatchUrgent==false))
						{
							bestMatch = match;
							bestMatchUrgent = sub.isUrgent();
						}
					}
				}
			}
		}

		return bestMatch;
	}
	
	/**
	 * 
	 * @param opening
	 * @return <code>true</code>, if this opening has an urgent match; <code>false</code> if it does not;
	 * <code>null</code> if the opening has no matches at all.
	 * @throws Exception
	 */
	public Boolean hasUrgentMatch(Opening opening) throws Exception
	{
		List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
		if (elertIDs.size()==0)
		{
			return null;
		}
		
		for (int e=0; e<elertIDs.size(); e++)
		{
			Elert eLert = ElertStore.getInstance().load(elertIDs.get(e));
			if (eLert.getReply()==Elert.REPLY_ACCEPTED)
			{
				Subscription sub = SubscriptionStore.getInstance().load(eLert.getSubscriptionID());
				if (!sub.isFinalized() && sub.isUrgent())
				{
					return true;
				}
			}
		}
		return false;
	}
		
	/**
	 * 
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
	public QueryIterator<Opening> queryGhost(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws SQLException
	{
		StringBuilder sql = new StringBuilder();
		ParameterList params = new ParameterList();
		
		sql.append("SELECT * FROM Openings ");
		if (physicianID!=null)
		{
			sql.append(", PhysicianOpeningLink ");
		}
		if (procedureID!=null)
		{
			sql.append(", ProcedureOpeningLink ");
		}
		
		sql.append("WHERE 1=1 ");
		
		// Dates
		if (from!=null)
		{
			sql.append("AND Openings.DateTime>=? ");
			params.plus(from);
		}
		if (to!=null)
		{
			sql.append("AND Openings.DateTime<? ");
			params.plus(to);
		}
		
		// Geography
		if (facilityID!=null)
		{
			sql.append("AND Openings.FacilityID=? ");
			params.plus(facilityID);
		}
		else if (serviceAreaID!=null)
		{
			sql.append("AND Openings.ServiceAreaID=? ");
			params.plus(serviceAreaID);
		}
		else if (regionID!=null)
		{
			sql.append("AND Openings.RegionID=? ");
			params.plus(regionID);
		}
		
		// Scheduler
		if (schedulerID!=null)
		{
			sql.append("AND Openings.SchedulerID=? ");
			params.plus(schedulerID);
		}

		// Physician
		if (physicianID!=null)
		{
			sql.append("AND Openings.ID=PhysicianOpeningLink.OpeningID AND PhysicianOpeningLink.PhysicianID=? ");
			params.plus(physicianID);
		}
		
		// Procedure
		if (procedureID!=null)
		{
			sql.append("AND Openings.ID=ProcedureOpeningLink.OpeningID AND ProcedureOpeningLink.ProcedureID=? ");
			params.plus(procedureID);
		}
		
		sql.append("ORDER BY DateTime DESC");
		
		return this.createQueryIterator(sql.toString(), params);
	}
}
