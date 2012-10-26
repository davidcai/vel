package elert.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class FacilityStore extends DataBeanStore<Facility>
{
	private static FacilityStore instance = new FacilityStore();

	protected FacilityStore()
	{
	}

	public final static FacilityStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Facility> getBeanClass()
	{
		return Facility.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Facilities", this);

		td.defineCol("Code", String.class).size(Facility.MAXSIZE_CODE, Facility.MAXSIZE_CODE);
		td.defineCol("Name", String.class).size(0, Facility.MAXSIZE_NAME);	
		td.defineCol("ServiceAreaID", UUID.class).refersTo("ServiceAreas").invariant();
		td.defineCol("Address", String.class).size(0, Facility.MAXSIZE_ADDRESS);	
		td.defineCol("City", String.class).size(0, Facility.MAXSIZE_CITY);	
		td.defineCol("State", String.class).size(0, Facility.MAXSIZE_STATE);
		td.defineCol("Zip", String.class).size(0, Facility.MAXSIZE_ZIP);
		td.defineCol("Phone", String.class).size(0, Facility.MAXSIZE_PHONE);
		
		return td;
	}

	// - - -

	public List<UUID> getAllIDs() throws Exception
	{
		return getInstance().getAllBeanIDs("Code", true);
	}

	public Facility loadByCode(String code) throws Exception
	{
		return getInstance().loadByColumn("Code", code);
	}

	public List<UUID> queryByRegion(UUID regionID) throws Exception
	{
		return Query.queryListUUID(
				"SELECT Facilities.ID FROM Facilities, ServiceAreas WHERE ServiceAreas.ID=Facilities.ServiceAreaID AND ServiceAreas.RegionID=?",
				new ParameterList(regionID));
	}

	public List<UUID> queryByServiceArea(UUID areaID) throws Exception
	{
		return getInstance().queryByColumn("ServiceAreaID", areaID, "Name", true);
	}

	/**
	 * Returns the IDs of facilities located within the (home) service area of the user (scheduler).
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public List<UUID> queryByUser(UUID userID) throws Exception
	{
		List<UUID> serviceAreas = ServiceAreaUserLinkStore.getInstance().getHomeSerivceAreasForUser(userID);
		if(serviceAreas == null || serviceAreas.size() == 0)
			return Collections.emptyList();

		final StringBuilder sql = new StringBuilder("SELECT ID FROM Facilities WHERE ServiceAreaID IN(");
		ParameterList params = new ParameterList();
		int length = serviceAreas.size();
		UUID areaId;
		for(int i = 0; i < length; i++)
		{
			areaId = serviceAreas.get(i);
			params.plus(areaId);
			sql.append("?");
			if(i < length - 1)
				sql.append(",");
		}
		sql.append(")");

		return Query.queryListUUID(sql.toString(), params);
	}	

	public List<UUID> searchByText(String text) throws SQLException
	{
		if(text.length() >= 3)
		{
			text = "%" + text;
		}
		text += "%";

		return Query.queryListUUID("SELECT ID FROM "  + getInstance().getTableDef().getName() + " WHERE (Name LIKE ?)", new ParameterList(text));
	}
}
