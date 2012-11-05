package baby.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;

public class MeasureRecord extends DataBean
{
	public UUID getMeasureID()
	{
		return (UUID) get("MeasureID");
	}

	public void setMeasureID(UUID measureID)
	{
		set("MeasureID", measureID);
	}

	public Integer getMetricValue()
	{
		return (Integer) get("MetricValue");
	}

	public void setMetricValue(Integer metricValue)
	{
		set("MetricValue", metricValue);
	}

	public Date getCreated()
	{
		return (Date) get("Created");
	}

	public void setCreated(Date created)
	{
		set("Created", created);
	}
}
