package baby.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;
import baby.app.BabyConsts;

public class Appointment extends DataBean
{
	public static final int MAXSIZE_DESCRIPTION = 64;
	public static final int MAXSIZE_TYPE = 64;

	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}

	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	public String getDescription()
	{
		return (String) get("Description");
	}

	public void setDescription(String description)
	{
		set("Description", description);
	}

	public String getType()
	{
		return (String) get("Type", BabyConsts.SECTION_CHECKUP);
	}

	public void setType(String type)
	{
		set("Type", type);
	}

	public Date getDateTime()
	{
		return (Date) get("DateTime");
	}

	public void setDateTime(Date dateTime)
	{
		set("DateTime", dateTime);
	}

	public boolean isRemindMeOneDayBefore()
	{
		return (Boolean) get("RemindMeOneDayBefore", false);
	}

	public void setRemindMeOneDayBefore(boolean remindMeOneDayBefore)
	{
		set("RemindMeOneDayBefore", remindMeOneDayBefore);
	}

	public boolean isRemindMeFourHoursBefore()
	{
		return (Boolean) get("RemindMeFourHoursBefore", false);
	}

	public void setRemindMeFourHoursBefore(boolean remindMeFourHoursBefore)
	{
		set("RemindMeFourHoursBefore", remindMeFourHoursBefore);
	}

	public boolean isRemindMeTwoHoursBefore()
	{
		return (Boolean) get("RemindMeTwoHoursBefore", false);
	}

	public void setRemindMeTwoHoursBefore(boolean remindMeTwoHoursBefore)
	{
		set("RemindMeTwoHoursBefore", remindMeTwoHoursBefore);
	}

	public boolean isRemindMeOneHourBefore()
	{
		return (Boolean) get("RemindMeOneHourBefore", false);
	}

	public void setRemindMeOneHourBefore(boolean remindMeOneHourBefore)
	{
		set("RemindMeOneHourBefore", remindMeOneHourBefore);
	}

	public String getAskMyDoctor()
	{
		return (String) get("AskMyDoctor");
	}

	public void setAskMyDoctor(String askMyDoctor)
	{
		set("AskMyDoctor", askMyDoctor);
	}
}
