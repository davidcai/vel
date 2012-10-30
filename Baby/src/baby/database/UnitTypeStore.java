package baby.database;

import java.util.List;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class UnitTypeStore extends DataBeanStore<UnitType>
{
	private static UnitTypeStore instance = new UnitTypeStore();

	protected UnitTypeStore()
	{
	}

	public final static UnitTypeStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<UnitType> getBeanClass()
	{
		return UnitType.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("UnitTypes", this);

		td.defineCol("MetricLabel", String.class).size(0, UnitType.MAXSIZE_METRIC_LABEL);
		td.defineCol("ImperialLabel", String.class).size(0, UnitType.MAXSIZE_IMPERIAL_LABEL);

		return td;
	}
	
	public List<UUID> getAll() throws Exception
	{
		return getAllBeanIDs("ImperialLabel", true);
	}
}
