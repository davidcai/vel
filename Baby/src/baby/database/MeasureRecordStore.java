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
		TableDef td = createTableDef("MeasureRecords");

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("BabyID", UUID.class).invariant().ownedBy("Babies");
		td.defineCol("MeasureID", UUID.class).invariant().ownedBy("Measures");
		td.defineCol("JournalEntryID", UUID.class).invariant().ownedBy("JournalEntries");
		td.defineCol("Value", Float.class);
		td.defineCol("Metric", Boolean.class);
		td.defineCol("CreatedDate", Date.class);

		return td;
	}

	/**
	 * Gets all measure records for the specified user sorted by descending CreateDate.
	 * 
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public List<UUID> getByUserID(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "CreatedDate", false);
	}

	/**
	 * Gets all measure records for the specified user within the date range (inclusive) sorted by descending
	 * CreatedDate.
	 * 
	 * @param userID
	 * @param from
	 * @param to
	 * @return
	 * @throws Exception
	 */
	public List<UUID> getByDate(UUID userID, Date from, Date to) throws Exception
	{
		return Query.queryListUUID(
			"SELECT ID FROM MeasureRecords WHERE UserID=? AND CreatedDate>=? AND CreatedDate<? ORDER BY CreatedDate DESC",
			new ParameterList().plus(userID).plus(from.getTime()).plus(to.getTime()));
	}
	
	/**
	 * Gets measure records associated with the journal entry. Records are ordered by descending CreateDate.
	 * 
	 * @param journalEntryID
	 * @return
	 * @throws Exception
	 */
	public List<UUID> getByJournalEntryID(UUID journalEntryID) throws Exception
	{
		return Query.queryListUUID("SELECT ID FROM MeasureRecords WHERE JournalEntryID=? ORDER BY CreateDate DESC",
			new ParameterList(journalEntryID));
	}
}
