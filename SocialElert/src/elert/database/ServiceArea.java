package elert.database;
import java.util.UUID;

import samoyan.database.DataBean;

public class ServiceArea extends DataBean
{
	public static final int MAXSIZE_NAME = 64;
	public static final int MAXSIZE_REGION = 64;
	
	public String getName()
	{
		return (String) get("Name");
	}
	
	public void setName(String name)
	{
		set("Name", name);
	}
	
	public UUID getRegionID()
	{
		return (UUID) get("RegionID");
	}
	
	public void setRegionID(UUID regionID)
	{
		set("RegionID", regionID);
	}
}
