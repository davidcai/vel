package baby.database;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.database.DataBeanStore;
import samoyan.database.TableDef;

public class AppointmentStore extends DataBeanStore<Appointment>
{
	private static AppointmentStore instance = new AppointmentStore();

	protected AppointmentStore()
	{
	}

	public final static AppointmentStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Appointment> getBeanClass()
	{
		return Appointment.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Appointments", this);

		td.defineCol("UserID", UUID.class).invariant().ownedBy("Users");
		td.defineCol("Description", String.class).size(0, Appointment.MAXSIZE_DESCRIPTION);
		td.defineCol("Type", String.class).size(0, Appointment.MAXSIZE_TYPE);
		td.defineCol("DateTime", Date.class);
		
		td.defineProp("RemindMeOneDayBefore", Boolean.class);
		td.defineProp("RemindMeFourHoursBefore", Boolean.class);
		td.defineProp("RemindMeTwoHoursBefore", Boolean.class);
		td.defineProp("RemindMeOneHourBefore", Boolean.class);
		td.defineProp("AskMyDoctor", String.class);
		
		return td;
	}
	
	/**
	 * Retrieves all appointment IDs for this user sorted by DateTime in descending order. 
	 * 
	 * @param userID
	 * @return
	 * @throws Exception
	 */
	public List<UUID> getAll(UUID userID) throws Exception
	{
		return queryByColumn("UserID", userID, "DateTime", false);
	}
}
