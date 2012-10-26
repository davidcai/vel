package elert.database;

import samoyan.database.DataBean;

public class Region extends DataBean
{
	public static final int MAXSIZE_NAME = 64;
	
	public String getName()
	{
		return (String) get("Name");
	}
	
	public void setName(String name)
	{
		set("Name", name);
	}
}
