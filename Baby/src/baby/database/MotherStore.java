package baby.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class MotherStore extends DataBeanStore<Mother>
{
	private static MotherStore instance = new MotherStore();

	protected MotherStore()
	{
	}

	public final static MotherStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Mother> getBeanClass()
	{
		return Mother.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Mothers", this);

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("DueDate", Date.class);
		td.defineCol("BirthDate", Date.class);
		td.defineCol("Metric", Boolean.class);
		td.defineCol("MedicalCenter", String.class);
		td.defineCol("Region", String.class);

		return td;
	}
	
	// - - -
	
	/**
	 * Returns the Mother object associated with the user.
	 * If a record does not exist, it will be created.
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public Mother loadByUserID(UUID userID) throws Exception
	{
		if (userID==null) return null;
		
		Mother mother = loadByColumn("UserID", userID);
		if (mother==null)
		{
			mother = new Mother();
			mother.setUserID(userID);
			save(mother);
		}
		return mother;
	}
	
	/**
	 * Returns the Mother object associated with the user.
	 * If a record does not exist, it will be created.
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public Mother openByUserID(UUID userID) throws Exception
	{
		if (userID==null) return null;

		Mother mother = openByColumn("UserID", userID);
		if (mother==null)
		{
			mother = new Mother();
			mother.setUserID(userID);
			save(mother);
		}
		return mother;
	}	
}
