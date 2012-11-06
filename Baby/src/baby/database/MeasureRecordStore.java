package baby.database;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class MeasureRecordStore extends DataBeanStore<MeasureRecord>
{
	private static MeasureRecordStore instance = new MeasureRecordStore();

	protected MeasureRecordStore()
	{
	}

	public final static MeasureRecordStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<MeasureRecord> getBeanClass()
	{
		return MeasureRecord.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("MeasureRecords", this);

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("BabyID", UUID.class).invariant().ownedBy("Babies");
		td.defineCol("MeasureID", UUID.class).invariant().ownedBy("Measures");
		td.defineCol("MetricValue", Integer.class);
		td.defineCol("Created", Date.class);

		return td;
	}

	public List<UUID> getByUserID(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "Created", false);
	}
}
