package baby.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public final class ChecklistStore extends DataBeanStore<Checklist>
{
	private static ChecklistStore instance = new ChecklistStore();

	protected ChecklistStore()
	{
	}

	public final static ChecklistStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Checklist> getBeanClass()
	{
		return Checklist.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Checklists", this);
		
		td.defineCol("Title", String.class).size(0, Checklist.MAXSIZE_TITLE);
		td.defineCol("Description", String.class).size(0, Checklist.MAXSIZE_DESCRIPTION);
		td.defineCol("Section", String.class).size(0, Checklist.MAXSIZE_SECTION);
		td.defineCol("TimelineFrom", Integer.class);
		td.defineCol("TimelineTo", Integer.class);
		td.defineCol("UserID", UUID.class).ownedBy("Users");

		return td;
	}

	// - - -

	/**
	 * Returns the IDs of checklists that are defined by the content manager, i.e. excluding checklists created by users.
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getAllStandard() throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Checklists WHERE UserID IS NULL ORDER BY TimelineFrom ASC, TITLE ASC", null);
	}

	public List<UUID> queryBySectionAndTimeline(String section, int lowStage, int highStage) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Checklists WHERE Section=? AND " +
				"((TimelineFrom>=? AND TimelineFrom<=?) OR (TimelineTo>=? AND TimelineTo<=?) OR (TimelineFrom<? AND TimelineTo>?))" +
				"ORDER BY TimelineFrom DESC, Title ASC",
				new ParameterList(section).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage));
	}
	
	public List<UUID> queryByTimeline(int lowStage, int highStage) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Checklists WHERE " +
				"(TimelineFrom>=? AND TimelineFrom<=?) OR (TimelineTo>=? AND TimelineTo<=?) OR (TimelineFrom<? AND TimelineTo>?)" +
				"ORDER BY TimelineFrom DESC, Title ASC",
				new ParameterList().plus(lowStage).plus(highStage).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage));
	}
	
	public Checklist loadPersonalChecklist(UUID userID) throws Exception
	{
		if (userID==null) return null;
		
		Checklist cl = loadByColumn("UserID", userID);
		if (cl==null)
		{
			cl = new Checklist();
			cl.setTitle(userID.toString());
			cl.setUserID(userID);
			save(cl);
		}
		return cl;
	}

	public List<UUID> getAll() throws Exception
	{
		return super.getAllBeanIDs();
	}
}
