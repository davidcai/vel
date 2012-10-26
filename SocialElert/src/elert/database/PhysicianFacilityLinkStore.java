package elert.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;
import samoyan.database.Query;

public class PhysicianFacilityLinkStore extends LinkStore
{
	private static PhysicianFacilityLinkStore instance = new PhysicianFacilityLinkStore();

	protected PhysicianFacilityLinkStore()
	{
	}
	public final static PhysicianFacilityLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("PhysicianFacilityLink");

		td.setKey1("PhysicianID", "Users");
		td.setKey2("FacilityID", "Facilities");
		td.setWeightColumn("Active");
		
		return td;
	}

	// - - -
	
	public List<UUID> getPhysiciansByFacility(UUID facilityID) throws SQLException
	{
		return getInstance().getByKey2(facilityID);
	}
	
	public List<UUID> getPhysiciansAssignedToFacility(UUID facilityID) throws SQLException
	{
		List<UUID> physicianIDs = new ArrayList<UUID>(getPhysiciansByFacility(facilityID));
		Iterator<UUID> iter = physicianIDs.iterator();
		while (iter.hasNext())
		{
			UUID id = iter.next();
			if (isPhysicianAssignedToFacility(id, facilityID)==false)
			{
				iter.remove();
			}
		}
		return physicianIDs;
	}

	public boolean isPhysicianAssignedToFacility(UUID physicianID, UUID facilityID) throws SQLException
	{
		Integer i = getInstance().getWeight(physicianID, facilityID);
		return i!=null && i==1;
	}
	
	public void assignPhysicianToFacility(UUID physicianID, UUID facilityID) throws SQLException
	{
		getInstance().link(physicianID, facilityID, 1);
	}

	/**
	 * Don't remove the relationship, but rather mark it as inactive
	 * @param physicianID
	 * @param facilityID
	 * @throws SQLException
	 */
	public void unassignPhysicianFromFacility(UUID physicianID, UUID facilityID) throws SQLException
	{
		getInstance().link(physicianID, facilityID, 0);
	}
	
	public void removePhysicianFromFacility(UUID physicianID, UUID facilityID) throws SQLException
	{
		getInstance().unlink(physicianID, facilityID);
	}
	
	/**
	 * Returns the list of physicians that are assigned to a facility in the given service area.
	 * @param serviceArea
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> queryByServiceArea(UUID areaID) throws SQLException
	{
		String sql = "SELECT DISTINCT Users.ID, Users.Name FROM Users, PhysicianFacilityLink, Facilities WHERE " +
				"Facilities.ServiceAreaID=? AND Facilities.ID=PhysicianFacilityLink.FacilityID AND " +
				"PhysicianFacilityLink.PhysicianID=Users.ID " +
				"ORDER BY Users.Name ASC";
		return Query.queryListUUID(sql, new ParameterList(areaID));
	}
}
