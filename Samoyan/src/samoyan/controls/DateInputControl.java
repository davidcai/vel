package samoyan.controls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import samoyan.core.DateFormatEx;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.servlet.UserAgent;
import samoyan.servlet.WebPage;

public class DateInputControl extends TextInputControl
{
	private boolean dateInputSupported;
	
	public DateInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		UserAgent ua = outputPage.getContext().getUserAgent();
		this.dateInputSupported =
			((ua.isAppleTouch() && ua.isIOS() && ua.getVersionIOS()>=5F) ||
//			(ua.isSafari() && ua.getVersionSafari()>=5.1F) ||
//			(ua.isChrome() && ua.getVersionChrome()>=10F) ||
			(ua.isOpera() && ua.getVersionOpera()>=9F));
		
		if (!this.dateInputSupported)
		{
			DateFormat df = DateFormatEx.getY4DateInstance(outputPage.getLocale(), TimeZoneEx.GMT);
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

		setSize(this.dateInputSupported?12:10);
		setMaxLength(10);
	}

	@Override
	public void render()
	{
		WebPage out = this.getOutputPage();
		
		DateFormat df;
		String inputType;

		UserAgent ua = out.getContext().getUserAgent();
		if (this.dateInputSupported)
		{
			df = new SimpleDateFormat("yyyy-MM-dd");
			df.setTimeZone(TimeZoneEx.GMT);
			setAttribute("type", "date");
		}
		else
		{
			df = DateFormatEx.getY4DateInstance(out.getLocale(), TimeZoneEx.GMT);
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
		else
		{
			// To prevent certain IE9 problem when no initial value is set
			setAttribute("value", "");
		}
//		setRegExp(DateFormatEx.getPattern(df)
//					.replaceAll("yyyy", "[0-9]{4}")
//					.replaceAll("MM", "[0-9]{1,2}")
//					.replaceAll("M", "[0-9]{1,2}")
//					.replaceAll("dd", "[0-9]{1,2}")
//					.replaceAll("d", "[0-9]{1,2}"));
		
		// Autofocus
		if (this.isAutoFocus())
		{
			if (out.getEphemeral("autofocus")==null)
			{
				setAttribute("autofocus", "");
				out.setEphemeral("autofocus", "1");
			}
		}

		writeTag("input");
		
		HiddenInputControl hiddenFormat = new HiddenInputControl(out, "_df_" + this.getName());
		hiddenFormat.setInitialValue(DateFormatEx.getPattern(df));
		hiddenFormat.render();
		
		HiddenInputControl hiddenTimeZone = new HiddenInputControl(out, "_tz_" + this.getName());
		hiddenTimeZone.setInitialValue(TimeZoneEx.GMT.getID());
		hiddenTimeZone.render();
	}
}
