package samoyan.controls;

import samoyan.servlet.WebPage;

public class HiddenInputControl extends TextInputControl
{
	public HiddenInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "hidden");
	}

	@Override
	public boolean isAutoFocus()
	{
		return false;
	}
	
	@Override
	public void render()
	{
		// Do not render the control if there's no value to post
		Object v = getCurrentValue();
		if (v!=null)
		{
			super.render();
		}
	}
}
