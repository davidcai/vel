package mind.database;

import samoyan.database.DataBean;

public class Equipment extends DataBean
{
	public static final int MAXSIZE_NAME = 32;
	public static final int MAXSIZE_INDUSTRY = 32;
	public static final int MAXSIZE_WEIGHT = 5;
	public static final int MAXVALUE_WEIGHT = 99999;
	
	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String serverName)
	{
		set("Name", serverName);
	}
	
	public String getIndustry()
	{
		return (String) get("Industry");
	}
	public void setIndustry(String industry)
	{
		set("Industry", industry);
	}
	
	public Integer getWeight()
	{
		return (Integer) get("Weight");
	}
	public void setWeight(Integer weight)
	{
		set("Weight", weight);
	}
	
	public String getDesc()
	{
		return (String) get("Desc");
	}
	public void setDesc(String desc)
	{
		set("Desc", desc);
	}
}
