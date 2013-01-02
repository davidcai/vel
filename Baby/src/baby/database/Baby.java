package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public class Baby extends DataBean
{
	public static final int MAXSIZE_NAME = 64;

	public final static String GENDER_MALE = "M";
	public final static String GENDER_FEMALE = "F";
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}

	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	public String getName()
	{
		return (String) get("Name");
	}

	public void setName(String name)
	{
		set("Name", name);
	}

	public String getGender()
	{
		return (String) get("Gender");
	}

	public void setGender(String gender)
	{
		set("Gender", gender);
	}
}
