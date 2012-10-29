package baby.database;

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
}
