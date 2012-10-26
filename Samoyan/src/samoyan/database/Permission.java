package samoyan.database;

public final class Permission extends DataBean
{
	public static final int MAXSIZE_NAME = 256;
	
	public static final String SYSTEM_ADMINISTRATION = "System administration";

	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String name)
	{
		set("Name", name);
	}
}
