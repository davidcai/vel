package baby.database;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
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
		td.defineCol("Value", Float.class);
		td.defineCol("Metric", Boolean.class);
		td.defineCol("CreatedDate", Date.class);

		return td;
	}

	public List<UUID> getByUserID(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "CreatedDate", false);
	}
	
	public List<UUID> getByDate(UUID userID, Date from, Date to) throws Exception
	{
		return Query.queryListUUID(
			"SELECT ID FROM MeasureRecords WHERE UserID=? AND CreatedDate>=? AND CreatedDate<? ORDER BY CreatedDate DESC", 
			new ParameterList().plus(userID).plus(from.getTime()).plus(to.getTime()));
	}
}
