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
		TableDef td = createTableDef("Measures");

		td.defineCol("Label", String.class).size(0, Measure.MAXSIZE_LABEL);
		td.defineCol("ForMother", Boolean.class);
		td.defineCol("ForPreconception", Boolean.class);
		td.defineCol("ForPregnancy", Boolean.class);
		td.defineCol("ForInfancy", Boolean.class);
		td.defineCol("MetricUnit", String.class).size(0, Measure.MAXSIZE_UNIT);
		td.defineCol("ImperialUnit", String.class).size(0, Measure.MAXSIZE_UNIT);
		td.defineCol("MetricMin", Float.class);
		td.defineCol("MetricMax", Float.class);
		td.defineCol("ImperialMin", Float.class);
		td.defineCol("ImperialMax", Float.class);
		td.defineCol("MetricToImperialAlpha", Float.class);
		td.defineCol("MetricToImperialBeta", Float.class);

		return td;
	}
	
	public List<UUID> getAll() throws Exception
	{
		return queryAll("Label", true);
	}
	
	public List<UUID> getAll(boolean forMother) throws Exception
	{
		return queryByColumn("ForMother", forMother, "Label", true);
	}
}
