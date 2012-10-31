package baby.database;

import samoyan.database.DataBean;

public class Measure extends DataBean
{
	public static final int MAXSIZE_LABEL = 64;
	public static final int MAXSIZE_UNIT = 16;
	public static final int MAXSIZE_MINMAX = 7;
	public static final int MAXSIZE_METRIC_TO_IMPERIAL = 9;

	public String getLabel()
	{
		return (String) get("Label");
	}

	public void setLabel(String label)
	{
		set("Label", label);
	}

	public boolean isForMother()
	{
		return (Boolean) get("ForMother");
	}

	public void setForMother(boolean forMother)
	{
		set("ForMother", forMother);
	}

	public Integer getMetricMin()
	{
		return (Integer) get("MetricMin");
	}

	public void setMetricMin(Integer metricMin)
	{
		set("MetricMin", metricMin);
	}

	public Integer getMetricMax()
	{
		return (Integer) get("MetricMax");
	}

	public void setMetricMax(Integer metricMax)
	{
		set("MetricMax", metricMax);
	}

	public String getMetricUnit()
	{
		return (String) get("MetricUnit");
	}

	public void setMetricUnit(String metricUnit)
	{
		set("MetricUnit", metricUnit);
	}

	public String getImperialUnit()
	{
		return (String) get("ImperialUnit");
	}

	public void setImperialUnit(String imperialUnit)
	{
		set("ImperialUnit", imperialUnit);
	}

	public Float getMetricToImperialAlpha()
	{
		return (Float) get("MetricToImperialAlpha");
	}

	public void setMetricToImperialAlpha(Float metricToImperialAlpha)
	{
		set("MetricToImperialAlpha", metricToImperialAlpha);
	}

	public Float getMetricToImperialBeta()
	{
		return (Float) get("MetricToImperialBeta");
	}

	public void setMetricToImperialBeta(Float metricToImperialBeta)
	{
		set("MetricToImperialBeta", metricToImperialBeta);
	}

	public boolean isForPreconception()
	{
		return (Boolean) get("ForPreconception");
	}

	public void setForPreconception(boolean forPreconception)
	{
		set("ForPreconception", forPreconception);
	}

	public boolean isForPregnancy()
	{
		return (Boolean) get("ForPregnancy");
	}

	public void setForPregnancy(boolean forPregnancy)
	{
		set("ForPregnancy", forPregnancy);
	}

	public boolean isForInfancy()
	{
		return (Boolean) get("ForInfancy");
	}

	public void setForInfancy(boolean forInfancy)
	{
		set("ForInfancy", forInfancy);
	}
}
