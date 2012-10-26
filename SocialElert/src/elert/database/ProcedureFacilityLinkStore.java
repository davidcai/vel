package elert.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public class ProcedureFacilityLinkStore extends LinkStore
{
	private static ProcedureFacilityLinkStore instance = new ProcedureFacilityLinkStore();

	protected ProcedureFacilityLinkStore()
	{
	}
	public final static ProcedureFacilityLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("ProcedureFacilityLink");

		td.setKey1("ProcedureID", "Procedures");
		td.setKey2("FacilityID", "Facilities");
		td.setWeightColumn("Active");
		
		return td;
	}

	// - - -

	public void assignProcedureToFacility(UUID procedureID, UUID facilityID) throws SQLException
	{
		link(procedureID, facilityID, 1);
	}

	/**
	 * Don't remove the relationship, but rather mark it as inactive
	 * @param procedureID
	 * @param facilityID
	 * @throws SQLException
	 */
	public void unassignProcedureFromFacility(UUID procedureID, UUID facilityID) throws SQLException
	{
		link(procedureID, facilityID, 0);
	}
	
	public void removeProcedureFromFacility(UUID procedureID, UUID facilityID) throws SQLException
	{
		unlink(procedureID, facilityID);
	}
	
	/**
	 * Returns the list of procedures with a link to the facility, regardless of the weight of this link.
	 * To get the list of facilities with link weight 1 (assigned), call {@link #getProceduresAssignedToFacility(UUID)}.
	 * @param procedureID
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getProceduresByFacility(UUID facilityID) throws SQLException
	{
		return getByKey2(facilityID);
	}

	public List<UUID> getProceduresAssignedToFacility(UUID facilityID) throws SQLException
	{
		List<UUID> procIDs = new ArrayList<UUID>(getProceduresByFacility(facilityID));
		Iterator<UUID> iter = procIDs.iterator();
		while (iter.hasNext())
		{
			UUID id = iter.next();
			if (isProcedureAssignedToFacility(id, facilityID)==false)
			{
				iter.remove();
			}
		}
		return procIDs;
	}

	public boolean isProcedureAssignedToFacility(UUID procedureID, UUID facilityID) throws SQLException
	{
		Integer i = getWeight(procedureID, facilityID);
		return i!=null && i==1;
	}
	
	/**
	 * Returns the list of facilities with a link to the procedure, regardless of the weight of this link.
	 * To get the list of facilities with link weight 1 (assigned), call {@link #getFacilitiesWithAssignedProcedure(UUID)}.
	 * @param procedureID
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getFacilitiesWithProcedure(UUID procedureID) throws SQLException
	{
		return getByKey1(procedureID);
	}

	public List<UUID> getFacilitiesWithAssignedProcedure(UUID procedureID) throws SQLException
	{
		List<UUID> facilityIDs = new ArrayList<UUID>(getFacilitiesWithProcedure(procedureID));
		Iterator<UUID> iter = facilityIDs.iterator();
		while (iter.hasNext())
		{
			UUID id = iter.next();
			if (isProcedureAssignedToFacility(procedureID, id)==false)
			{
				iter.remove();
			}
		}
		return facilityIDs;
	}
}
