package baby.database;

import java.util.List;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class MeasureStore extends DataBeanStore<Measure>
{
	private static MeasureStore instance = new MeasureStore();

	protected MeasureStore()
	{
	}

	public final static MeasureStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Measure> getBeanClass()
	{
		return Measure.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Measures", this);

		td.defineCol("UnitTypeID", UUID.class).invariant().ownedBy("UnitTypes");
		td.defineCol("Label", String.class).size(0, Measure.MAXSIZE_LABEL);
		td.defineCol("ForMother", Boolean.class);
		td.defineCol("MinValue", Integer.class);
		td.defineCol("MaxValue", Integer.class);
		td.defineCol("DefValue", Integer.class);

		return td;
	}
	
	public List<UUID> getMeasures(boolean forMother) throws Exception
	{
		return queryByColumn("ForMother", forMother, "Label", true);
	}
}
