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
	public void render()
	{
		String v = getCurrentValue();
		if (v != null)
		{
			try
			{
				Float f = Float.valueOf(v);
				boolean hasDecimals = (Math.round(f * 1000) % 1000) != 0;
				if (hasDecimals == false)
				{
					v = String.valueOf(f.intValue());
				}
			}
			catch (Exception e)
			{
				// Do nothing
			}
			
			setAttribute("value", v);
		}
		else
		{
			// To prevent certain IE9 problem when no initial value is set
			// (field is cleared when pressing Submit)
			setAttribute("value", "");
		}
		
		// Autofocus
		if (this.isAutoFocus())
		{
			WebPage out = this.getOutputPage();
			if (out.getEphemeral("autofocus") == null)
			{
				setAttribute("autofocus", "");
				out.setEphemeral("autofocus", "1");
			}
		}
		
		writeTag("input");
	}
}
