package baby.database;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Image;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class JournalEntryStore extends DataBeanStore<JournalEntry>
{
	private static JournalEntryStore instance = new JournalEntryStore();

	protected JournalEntryStore()
	{
	}

	public final static JournalEntryStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<JournalEntry> getBeanClass()
	{
		return JournalEntry.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("JournalEntries", this);

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("Text", String.class).size(0, JournalEntry.MAXSIZE_TEXT);
		td.defineCol("HasPhoto", Boolean.class);
		td.defineCol("Created", Date.class);

		td.defineProp("Photo", Image.class).dimensions(JournalEntry.MAXWIDTH_PHOTO, JournalEntry.MAXHEIGHT_PHOTO);

		return td;
	}

	// - - -
	
	public List<UUID> getByUserID(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "Created", false);
	}
	
	public List<UUID> getByDate(UUID userID, Date from, Date to) throws Exception
	{
		return Query.queryListUUID(
			"SELECT ID FROM JournalEntries WHERE UserID=? AND Created>=? AND Created<? ORDER BY Created DESC", 
			new ParameterList().plus(userID).plus(from.getTime()).plus(to.getTime()));
	}
}
