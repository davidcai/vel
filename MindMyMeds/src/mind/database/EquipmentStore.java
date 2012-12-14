package mind.database;

import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class EquipmentStore extends DataBeanStore<Equipment>
{
	private static EquipmentStore instance = new EquipmentStore();
	
	protected EquipmentStore()
	{
	}
	public final static EquipmentStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Equipment> getBeanClass()
	{
		return Equipment.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Equipments");
		
		td.defineCol("Name", String.class).size(0, Equipment.MAXSIZE_NAME);
		td.defineCol("Industry", String.class).size(0, Equipment.MAXSIZE_INDUSTRY);
		td.defineCol("Weight", Integer.class);
		
		td.defineProp("Desc", String.class);
		
		return td;
	}
	
	public List<UUID> getAllIDs() throws Exception
	{
		return queryAll("Name", true);
	}

	public Equipment loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}
	
	public List<UUID> findWithinWeightRange(Integer from, Integer to) throws Exception
	{
		return Query.queryListUUID("SELECT ID FROM Equipments WHERE Weight >= ? AND Weight <= ?", new ParameterList().plus(from).plus(to));
	}
	
	public List<String> searchIndustryByName(String queryString) throws Exception
	{
		if (queryString.length() >= 3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";
		
		return Query.queryListString("SELECT DISTINCT Industry FROM Equipments WHERE Industry LIKE ?", new ParameterList(queryString));
	}
}
