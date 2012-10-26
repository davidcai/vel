package elert.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;

public class Opening extends DataBean
{	
	public static final int MAXSIZE_ROOM = 16;
	public static final int MAX_DURATION = 1440; // 24 hours, in minutes

	public UUID getRegionID()
	{
		return (UUID) get("RegionID");
	}
	
	public void setRegionID(UUID regionID)
	{
		set("RegionID", regionID);
	}
	
	public UUID getServiceAreaID()
	{
		return (UUID) get("ServiceAreaID");
	}
	
	public void setServiceAreaID(UUID serviceAreaID)
	{
		set("ServiceAreaID", serviceAreaID);
	}
	
	public UUID getSchedulerID()
	{
		return (UUID) get("SchedulerID");
	}
	
	public void setSchedulerID(UUID schedulerID)
	{
		set("SchedulerID", schedulerID);
	}
	
	public UUID getFacilityID()
	{
		return (UUID) get("FacilityID");
	}
	
	public void setFacilityID(UUID facilityID)
	{
		set("FacilityID", facilityID);
	}
	
	public String getRoom()
	{
		return (String)get("Room");
	}
	
	public void setRoom(String room)
	{
		set("Room", room);
	}
	
	public Date getDateTime()
	{
		return (Date) get("DateTime");
	}
	
	public void setDateTime(Date dateTime)
	{
		set("DateTime", dateTime);
	}
	
	public int getDuration()
	{
		return (Integer)get("Duration", 0);
	}
	
	public void setDuration(int duration)
	{
		set("Duration", duration);
	}

	public int getOriginalDuration()
	{
		return (Integer)get("OriginalDuration", 0);
	}
	
	public void setOriginalDuration(int duration)
	{
		set("OriginalDuration", duration);
	}
	
	public boolean isClosed()
	{
		return (Boolean) get("Closed", false);
	}
	public void setClosed(boolean b)
	{
		set("Closed", b);
	}
}
