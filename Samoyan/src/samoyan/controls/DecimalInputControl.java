package samoyan.controls;

import samoyan.servlet.WebPage;

public class DecimalInputControl extends TextInputControl
{

	public DecimalInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);

		setAttribute("type", "number");
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

	public DecimalInputControl setStep(Float step)
	{
		setAttribute("step", (step == null || step == 0f) ? "any" : String.valueOf(step));
		return this;
	}

	public Float getStep()
	{
		String str = getAttribute("step");
		return str == null ? null : Float.parseFloat(str);
	}
}
