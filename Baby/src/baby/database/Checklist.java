package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public final class Checklist extends DataBean
{
	public static final int MAXSIZE_DESCRIPTION = 1024;
	public static final int MAXSIZE_TITLE = 256;
	public static final int MAXSIZE_SECTION = 64;
	
	public String getTitle()
	{
		return (String) get("Title");
	}
	public void setTitle(String title)
	{
		set("Title", title);
	}
	
	public String getDescription()
	{
		return (String) get("Description");
	}
	public void setDescription(String desc)
	{
		set("Description", desc);
	}
	
	public String getSection()
	{
		return (String) get("Section");
	}
	public void setSection(String section)
	{
		set("Section", section);
	}

	public int getTimelineFrom()
	{
		return (Integer) get("TimelineFrom", 0);
	}
	public void setTimelineFrom(int phase)
	{
		set("TimelineFrom", phase);
	}
	
	public int getTimelineTo()
	{
		return (Integer) get("TimelineTo", 0);
	}
	public void setTimelineTo(int phase)
	{
		set("TimelineTo", phase);
	}
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}
}
