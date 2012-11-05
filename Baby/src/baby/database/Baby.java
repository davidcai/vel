package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public class Baby extends DataBean
{
	public static final int MAXSIZE_NAME = 64;
	
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

	public boolean isMale()
	{
		return (Boolean) get("Male", false);
	}

	public void setMale(boolean male)
	{
		set("Male", male);
	}
}
