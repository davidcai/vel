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

		td.defineCol("Label", String.class).size(0, Measure.MAXSIZE_LABEL);
		td.defineCol("ForMother", Boolean.class);
		td.defineCol("ForPreconception", Boolean.class);
		td.defineCol("ForPregnancy", Boolean.class);
		td.defineCol("ForInfancy", Boolean.class);
		td.defineCol("MetricUnit", String.class).size(0, Measure.MAXSIZE_UNIT);
		td.defineCol("ImperialUnit", String.class).size(0, Measure.MAXSIZE_UNIT);
		td.defineCol("MetricMin", Integer.class).size(0, Measure.MAXSIZE_MINMAX);
		td.defineCol("MetricMax", Integer.class).size(0, Measure.MAXSIZE_MINMAX);
		td.defineCol("MetricToImperialAlpha", Float.class).size(0, Measure.MAXSIZE_METRIC_TO_IMPERIAL);
		td.defineCol("MetricToImperialBeta", Float.class).size(0, Measure.MAXSIZE_METRIC_TO_IMPERIAL);

		return td;
	}
	
	public List<UUID> getAll() throws Exception
	{
		return getAllBeanIDs("Label", true);
	}
	
	public List<UUID> getAll(boolean forMother) throws Exception
	{
		return queryByColumn("ForMother", forMother, "Label", true);
	}
}
