package samoyan.controls;

import samoyan.servlet.WebPage;

public class NumberInputControl extends TextInputControl
{
	public NumberInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
//		UserAgent ua = outputPage.getContext().getUserAgent();
//		if ((ua.isAppleTouch() && ua.isIOS() && ua.getVersionIOS()>=5F) ||
//			(ua.isSafari() && ua.getVersionSafari()>=5.2F) ||
//			(ua.isChrome() && ua.getVersionChrome()>=10F) ||
//			(ua.isOpera() && ua.getVersionOpera()>=11F))
//		{
			setAttribute("type", "number");
//		}
	}

	public NumberInputControl setMinValue(int minVal)
	{
		setAttribute("min", String.valueOf(minVal));
		return this;
	}
	public int getMinValue()
	{
		String str = getAttribute("min");
		return str==null? 0 : Integer.parseInt(str);
	}

	public NumberInputControl setMaxValue(int maxVal)
	{
		setAttribute("max", String.valueOf(maxVal));
		return this;
	}
	public int getMaxValue()
	{
		String str = getAttribute("max");
		return str==null? 0 : Integer.parseInt(str);
	}
}
