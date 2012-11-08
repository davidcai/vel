package baby.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;

public class MeasureRecord extends DataBean
{
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}

	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}
	
	public UUID getBabyID()
	{
		return (UUID) get("BabyID");
	}

	public void setBabyID(UUID babyID)
	{
		set("BabyID", babyID);
	}

	public UUID getMeasureID()
	{
		return (UUID) get("MeasureID");
	}

	public void setMeasureID(UUID measureID)
	{
		set("MeasureID", measureID);
	}

	public Float getValue()
	{
		return (Float) get("Value");
	}

	public void setValue(Float val)
	{
		set("Value", val);
	}
	
	public boolean isMetric()
	{
		return (Boolean) get("Metric", false);
	}
	
	public void setMetric(boolean metric)
	{
		set("Metric", metric);
	}

	public Date getCreatedDate()
	{
		return (Date) get("CreatedDate");
	}

	public void setCreatedDate(Date createdDate)
	{
		set("CreatedDate", createdDate);
	}
}
