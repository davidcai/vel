package samoyan.controls;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import samoyan.core.DateFormatEx;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.servlet.UserAgent;
import samoyan.servlet.WebPage;

public class TimeOfDayInputControl extends TextInputControl
{
	private boolean timeInputSupported;

	public TimeOfDayInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		UserAgent ua = outputPage.getContext().getUserAgent();
		this.timeInputSupported =
			((ua.isAppleTouch() && ua.isIOS() && ua.getVersionIOS() >= 5F) ||
//			(ua.isSafari() && ua.getVersionSafari() >= 5.1F) ||
//			(ua.isChrome() && ua.getVersionChrome() >= 10F) ||
			(ua.isOpera() && ua.getVersionOpera() >= 9F));
		
		if (this.timeInputSupported == false)
		{
			DateFormat df = DateFormatEx.getY4DateTimeInstance(outputPage.getLocale(), TimeZoneEx.GMT);
			String pattern = DateFormatEx.getPattern(df);
			if (pattern.indexOf("MM")<0)
			{
				pattern = Util.strReplace(pattern, "M", "MM");
			}
			if (pattern.indexOf("dd")<0)
			{
				pattern = Util.strReplace(pattern, "d", "dd");
			}
			setPlaceholder(pattern.toLowerCase(Locale.US));
		}
		
		setAttribute("size", "22");
		setAttribute("maxlength", "22");
	}

	public TimeOfDayInputControl setPlaceholder(String placeholder)
	{
		setAttribute("placeholder", placeholder);
		return this;
	}
	public String getPlaceholder()
	{
		return getAttribute("placeholder");
	}

	@Override
	public void render()
	{
		WebPage out = this.getOutputPage();
		
		DateFormat df;

		if (this.timeInputSupported)
		{
			df = DateFormatEx.getSimpleInstance("yyyy-MM-dd'T'HH:mm", out.getLocale(), out.getTimeZone());
			setAttribute("type", "datetime-local");
		}
		else
		{
			df = DateFormatEx.getY4DateTimeInstance(out.getLocale(), out.getTimeZone());
			setAttribute("type", "text");
		}
		
		String value = out.getContext().getParameter(this.getName());
		if (value==null)
		{
			Object initialValue = this.getInitialValue();
			if (initialValue!=null)
			{
				value = df.format((Date) initialValue);
			}
		}

		if (value!=null)
		{
			setAttribute("value", value);
		}
		
		writeTag("input");
		
		HiddenInputControl hiddenFormat = new HiddenInputControl(out, "_df_" + this.getName());
		hiddenFormat.setInitialValue(DateFormatEx.getPattern(df));
		hiddenFormat.render();
		
		HiddenInputControl hiddenTimeZone = new HiddenInputControl(out, "_tz_" + this.getName());
		hiddenTimeZone.setInitialValue(out.getTimeZone().getID());
		hiddenTimeZone.render();
	}
}
