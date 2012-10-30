package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public class Measure extends DataBean
{
	public static final int MAXSIZE_LABEL = 64;
	
	public UUID getUnitTypeID()
	{
		return (UUID) get("UnitTypeID");
	}

	public void setUnitTypeID(UUID unitTypeID)
	{
		set("UnitTypeID", unitTypeID);
	}

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

	public Integer getMinValue()
	{
		return (Integer) get("MinValue");
	}

	public void setMinValue(Integer minValue)
	{
		set("MinValue", minValue);
	}

	public Integer getMaxValue()
	{
		return (Integer) get("MaxValue");
	}

	public void setMaxValue(Integer maxValue)
	{
		set("MaxValue", maxValue);
	}

	public Integer getDefValue()
	{
		return (Integer) get("DefValue");
	}

	public void setDefValue(Integer defValue)
	{
		set("DefValue", defValue);
	}
}