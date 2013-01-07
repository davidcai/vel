package baby.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;
import baby.app.BabyConsts;

public class Appointment extends DataBean
{
	public static final int MAXSIZE_DESCRIPTION = 64;
	public static final int MAXSIZE_TYPE = 64;

	public Appointment()
	{
		init("DateTime", new Date());
	}
	
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

	public boolean isReminderOneDay()
	{
		return (Boolean) get("ReminderOneDay", false);
	}

	public void setReminderOneDay(boolean b)
	{
		set("ReminderOneDay", b);
	}

	public boolean isReminderTwoDays()
	{
		return (Boolean) get("ReminderTwoDays", false);
	}

	public void setReminderTwoDays(boolean b)
	{
		set("ReminderTwoDays", b);
	}

	public boolean isReminderTwoHours()
	{
		return (Boolean) get("ReminderTwoHours", false);
	}

	public void setReminderTwoHours(boolean b)
	{
		set("ReminderTwoHours", b);
	}

	public boolean isReminderOneHour()
	{
		return (Boolean) get("ReminderOneHour", false);
	}

	public void setReminderOneHour(boolean b)
	{
		set("ReminderOneHour", b);
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
