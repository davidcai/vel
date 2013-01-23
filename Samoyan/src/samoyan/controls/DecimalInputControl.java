package samoyan.controls;

import samoyan.servlet.WebPage;

public class DecimalInputControl extends TextInputControl
{
	public DecimalInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);

		setAttribute("type", "text");
	}

	public DecimalInputControl setMinValue(Float minVal)
	{
		setAttribute("min", minVal == null ? "" : String.valueOf(minVal));
		return this;
	}

	public Float getMinValue()
	{
		String str = getAttribute("min");
		return str == null ? null : Float.parseFloat(str);
	}

	public DecimalInputControl setMaxValue(Float maxVal)
	{
		setAttribute("max", maxVal == null ? "" : String.valueOf(maxVal));
		return this;
	}

	public Float getMaxValue()
	{
		String str = getAttribute("max");
		return str == null ? null : Float.parseFloat(str);
	}
	
	@Override
	public String getCurrentValue()
	{
		String value = super.getCurrentValue();
		if (value != null)
		{
			try
			{
				Float f = Float.valueOf(value);
				if (((float) f.longValue()) == f)
				{
					value = String.valueOf(f.longValue());
				}
//				boolean hasDecimals = (Math.round(f * 1000) % 1000) != 0;
//				if (hasDecimals == false)
//				{
//					value = String.valueOf(f.intValue());
//				}
			}
			catch (Exception e)
			{
				// Do nothing
			}
		}
		
		return value;
	}
}
