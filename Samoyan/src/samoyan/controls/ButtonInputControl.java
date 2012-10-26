package samoyan.controls;

import samoyan.servlet.WebPage;

public class ButtonInputControl extends InputControl
{
	private Object value = null;

	public ButtonInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "submit");
	}

	/**
	 * Will show the button is a subdued color (e.g. grey).
	 * @param b
	 * @return
	 */
	public ButtonInputControl setSubdued(boolean b)
	{
		addCssClass("Subdued");
		return this;
	}
	public boolean isSubdued()
	{
		return isCssClass("Subdued");
	}
	
	/**
	 * Will show the button is a strong color (e.g. red).
	 * @param b
	 * @return
	 */
	public ButtonInputControl setStrong(boolean b)
	{
		addCssClass("Strong");
		return this;
	}
	public boolean isStrong()
	{
		return isCssClass("Strong");
	}
	
	public ButtonInputControl setValue(Object value)
	{
		this.value = value;
		if (value!=null)
		{
			setAttribute("value", value.toString());
		}
		else
		{
			setAttribute("value", null);
		}
		return this;
	}
	public Object getValue()
	{
		return this.value;
	}

	@Override
	public void render()
	{
		writeTag("input");
	}
}
