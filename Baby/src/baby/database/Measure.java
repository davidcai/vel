package baby.database;

import samoyan.database.DataBean;

public class Measure extends DataBean
{
	public static final int MAXSIZE_LABEL = 64;
	public static final int MAXSIZE_UNIT = 16;
	public static final int MAXSIZE_MINMAX = 7;
	public static final int MAXSIZE_METRIC_TO_IMPERIAL = 9;
	public static final float MAXVAL_MINMAX = 9999999f;

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
		return (Boolean) get("ForMother", false);
	}

	public void setForMother(boolean forMother)
	{
		set("ForMother", forMother);
	}

	public Float getMetricMin()
	{
		return (Float) get("MetricMin", 0f);
	}

	public void setMetricMin(Float metricMin)
	{
		set("MetricMin", metricMin);
	}

	public Float getMetricMax()
	{
		return (Float) get("MetricMax", 0f);
	}

	public void setMetricMax(Float metricMax)
	{
		set("MetricMax", metricMax);
	}

	public Float getImperialMin()
	{
		return (Float) get("ImperialMin", 0f);
	}

	public void setImperialMin(Float metricMin)
	{
		set("ImperialMin", metricMin);
	}

	public Float getImperialMax()
	{
		return (Float) get("ImperialMax", 0f);
	}

	public void setImperialMax(Float metricMax)
	{
		set("ImperialMax", metricMax);
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
		return (Float) get("MetricToImperialAlpha", 0f);
	}

	public void setMetricToImperialAlpha(Float metricToImperialAlpha)
	{
		set("MetricToImperialAlpha", metricToImperialAlpha);
	}

	public Float getMetricToImperialBeta()
	{
		return (Float) get("MetricToImperialBeta", 1f);
	}

	public void setMetricToImperialBeta(Float metricToImperialBeta)
	{
		set("MetricToImperialBeta", metricToImperialBeta);
	}

	public boolean isForPreconception()
	{
		return (Boolean) get("ForPreconception", false);
	}

	public void setForPreconception(boolean forPreconception)
	{
		set("ForPreconception", forPreconception);
	}

	public boolean isForPregnancy()
	{
		return (Boolean) get("ForPregnancy", false);
	}

	public void setForPregnancy(boolean forPregnancy)
	{
		set("ForPregnancy", forPregnancy);
	}

	public boolean isForInfancy()
	{
		return (Boolean) get("ForInfancy", false);
	}

	public void setForInfancy(boolean forInfancy)
	{
		set("ForInfancy", forInfancy);
	}
	
	public Float toImperial(Float metricValue)
	{
		return this.getMetricToImperialAlpha() + this.getMetricToImperialBeta() * metricValue;
	}
	
	public Float toMetric(Float imperialValue)
	{
		return (imperialValue - this.getMetricToImperialAlpha()) / this.getMetricToImperialBeta();
	}
}
