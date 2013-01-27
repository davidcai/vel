package baby.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public final class CheckItemStore extends DataBeanStore<CheckItem>
{
	private static CheckItemStore instance = new CheckItemStore();

	protected CheckItemStore()
	{
	}

	public final static CheckItemStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<CheckItem> getBeanClass()
	{
		return CheckItem.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = createTableDef("CheckItems");
		
		td.defineCol("Text", String.class).size(0, CheckItem.MAXSIZE_TEXT);
		td.defineCol("ChecklistID", UUID.class).ownedBy("Checklists");
		td.defineCol("OrderSeq", Integer.class);
		td.defineCol("Link", String.class).size(0, CheckItem.MAXSIZE_LINK);

		return td;
	}

	// - - -
	
	public List<UUID> getByChecklistID(UUID id) throws SQLException
	{
		return queryByColumn("ChecklistID", id, "OrderSeq", true);
	}
}
