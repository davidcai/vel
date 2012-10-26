package mind.database;

import java.util.*;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class PatientStore extends DataBeanStore<Patient>
{
	private static PatientStore instance = new PatientStore();

	protected PatientStore()
	{
	}
	public final static PatientStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Patient> getBeanClass()
	{
		return Patient.class;
	}	
	
	@Override
	public TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Patients", this);
		
		td.defineCol("LoginID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("MRN", Long.class);
		
		return td;
	}
	
	// - - -
	
	/**
	 * Returns the <code>Patient</code> for the give login ID.
	 * @param loginID
	 * @return
	 * @throws Exception 
	 */
	public Patient loadByUserID(UUID loginID) throws Exception
	{
		return getInstance().loadByColumn("LoginID", loginID);
	}

	public Patient openByUserID(UUID loginID) throws Exception
	{
		return getInstance().openByColumn("LoginID", loginID);
	}
}
