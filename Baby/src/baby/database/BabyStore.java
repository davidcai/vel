package baby.database;

import java.util.ArrayList;
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
		TableDef td = createTableDef("Babies");

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("Name", String.class);
		td.defineCol("Gender", String.class);
		
		return td;
	}
	
	public List<UUID> getByUser(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "Name", true);
	}
	
	public List<UUID> getAtLeastOneBaby(UUID userID) throws Exception
	{
		List<UUID> babies = queryByColumn("UserID", userID, "Name", true);
		if (babies.size()==0)
		{
			// Auto create the first baby
			Baby baby = new Baby();
			baby.setUserID(userID);
			save(baby);
			
			babies = new ArrayList<UUID>();
			babies.add(baby.getID());
		}
		return babies;
	}
}
