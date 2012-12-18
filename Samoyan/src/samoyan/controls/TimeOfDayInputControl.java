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
	private boolean includeSeconds;

	public TimeOfDayInputControl(WebPage outputPage, String name, boolean includeSeconds)
	{
		super(outputPage, name);

		UserAgent ua = outputPage.getContext().getUserAgent();
		this.timeInputSupported =
			((ua.isAppleTouch() && ua.isIOS() && ua.getVersionIOS() >= 5F) ||
//			(ua.isSafari() && ua.getVersionSafari() >= 5.1F) ||
//			(ua.isChrome() && ua.getVersionChrome() >= 10F) ||
			(ua.isOpera() && ua.getVersionOpera() >= 9F));
		
		this.includeSeconds = includeSeconds;
		
		if (this.includeSeconds || this.timeInputSupported == false)
		{
			// Edit box
			DateFormat df = (this.includeSeconds) ? DateFormat.getTimeInstance(DateFormat.LONG, outputPage.getLocale())
					: DateFormatEx.getTimeInstance(outputPage.getLocale(), TimeZoneEx.GMT);
			
			String pattern = DateFormatEx.getPattern(df);
			if (pattern.indexOf("hh") < 0)
			{
				pattern = Util.strReplace(pattern, "h", "hh");
			}
			if (pattern.indexOf("mm") < 0)
			{
				pattern = Util.strReplace(pattern, "m", "mm");
			}
			if (pattern.indexOf("ss") < 0)
			{
				pattern = Util.strReplace(pattern, "s", "ss");
			}
			
			// TODO: Strip 'a' (AM/PM) from pattern?
			
			setPlaceholder(pattern.toLowerCase(Locale.US));
		}
		else
		{
			// Time input
		}
		
		setSize(22);
		setMaxLength(22);
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
