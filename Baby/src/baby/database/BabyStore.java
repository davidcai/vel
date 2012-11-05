package baby.database;

import java.util.List;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class BabyStore extends DataBeanStore<Baby>
{
	private static BabyStore instance = new BabyStore();

	protected BabyStore()
	{
	}

	public final static BabyStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Baby> getBeanClass()
	{
		return Baby.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Babies", this);

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("Name", String.class);
		td.defineCol("Male", Boolean.class);
		
		return td;
	}
	
	public List<UUID> getByUser(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "Name", true);
	}
}
