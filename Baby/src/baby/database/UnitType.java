package baby.database;

import samoyan.database.DataBean;

public class UnitType extends DataBean
{
	public static final int MAXSIZE_LABEL = 32;
	
	public String getMetricLabel()
	{
		return (String) get("MetricLabel");
	}

	public void setMetricLabel(String metricLabel)
	{
		set("MetricLabel", metricLabel);
	}

	public String getImperialLabel()
	{
		return (String) get("ImperialLabel");
	}

	public void setImperialLabel(String imperialLabel)
	{
		set("ImperialLabel", imperialLabel);
	}
}
