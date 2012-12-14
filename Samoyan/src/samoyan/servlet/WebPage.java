package samoyan.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import samoyan.apps.guidedsetup.CompletePage;
import samoyan.apps.system.CaptchaImagePage;
import samoyan.controls.ButtonInputControl;
import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DateInputControl;
import samoyan.controls.DateTimeInputControl;
import samoyan.controls.DecimalInputControl;
import samoyan.controls.HiddenInputControl;
import samoyan.controls.ImageControl;
import samoyan.controls.ImageInputControl;
import samoyan.controls.NumberInputControl;
import samoyan.controls.PasswordInputControl;
import samoyan.controls.PhoneInputControl;
import samoyan.controls.RadioButtonInputControl;
import samoyan.controls.RichEditControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.controls.TextInputControl;
import samoyan.core.Captcha;
import samoyan.core.DateFormatEx;
import samoyan.core.LocaleEx;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.StringBundle;
import samoyan.core.Util;
import samoyan.core.image.JaiImage;
import samoyan.database.Country;
import samoyan.database.DataBean;
import samoyan.database.Image;
import samoyan.database.ImageStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class WebPage
{
	public final static int NO_INDEX = 0x01;
	public final static int NO_FOLLOW = 0x02;
	public final static int NO_ARCHIVE = 0x04;
	public final static int NO_SNIPPET = 0x08;
	
	private ByteArrayOutputStream canvas = null;
	private Map<String, String> cookies = null;
	private Map<String, String> ephemerals = null;
	private RequestContext cachedCtx = null;
	
	private WebPage child = null;
	private WebPage container = null;
	private WebPage delegate = null;
	private WebFormException formException = null;
	private boolean committed = false;

	// - - -
	// INIT
	
	public void setChild(WebPage child)
	{
		this.child = child;
		this.delegate = child;
		if (this.child!=null)
		{
			this.child.setCanvas(this.getCanvas());
		}
	}
	public WebPage getChild()
	{
		return this.child;
	}

	public void setContainer(WebPage container)
	{
		this.container = container;
		this.delegate = (this.child!=null? this.child : container); // Child gets precedence of container
		if (this.container!=null)
		{
			this.canvas = this.container.getCanvas();
		}
	}
	public WebPage getContainer()
	{
		return this.container;
	}

	public RequestContext getContext()
	{
		if (cachedCtx==null)
		{
			cachedCtx = RequestContext.getCurrent();
		}
		return cachedCtx;
	}

	public ByteArrayOutputStream getCanvas()
	{
		if (this.canvas==null)
		{
//			Debug.logln("Canvas allocated");
			this.canvas = new ByteArrayOutputStream(4096);
		}
		return canvas;
	}
	public void setCanvas(ByteArrayOutputStream canvas)
	{
		this.canvas = canvas;
		if (this.child!=null)
		{
			this.child.setCanvas(this.getCanvas());
		}
	}
	public byte[] getContent() throws Exception
	{
		if (this.canvas==null)
		{
			return new byte[0];
		}
		else
		{
			return this.canvas.toByteArray();
		}
	}
	public String getContentAsString() throws Exception
	{
		if (this.canvas==null)
		{
			return "";
		}
		else
		{
			return new String(getContent(), "UTF-8");
		}
	}

	// - - -
	
	public TimeZone getTimeZone()
	{
		if (this.delegate!=null)
		{
			return this.delegate.getTimeZone();
		}
		else
		{
			RequestContext ctx = getContext();
			try
			{
				User user = UserStore.getInstance().load(ctx.getUserID());
				if (user!=null && user.getTimeZone()!=null)
				{
					return user.getTimeZone();
				}
			}
			catch (Exception e)
			{
			}
			
			if (ctx.getTimeZone()!=null)
			{
				return ctx.getTimeZone();
			}

			try
			{
				return ServerStore.getInstance().loadFederation().getTimeZone();
			}
			catch (Exception e)
			{
				return TimeZone.getDefault();
			}
		}
	}
	public Locale getLocale()
	{
		if (this.delegate!=null)
		{
			return this.delegate.getLocale();
		}
		else
		{
			RequestContext ctx = getContext();
			try
			{
				Server fed = ServerStore.getInstance().loadFederation();
				List<Locale> availLocales = fed.getLocales();
				if (ctx.getChannel().equalsIgnoreCase(Channel.VOICE))
				{
					// !$! This may result in no locales at all
					// Need to do better matching and keep languages even if country does not match
					availLocales.retainAll(fed.getVoxeoLocales());
					if (availLocales.size()==0)
					{
						availLocales.add(Locale.US);
					}
				}
				
				User user = UserStore.getInstance().load(ctx.getUserID());
				if (user!=null && user.getLocale()!=null)
				{
					return LocaleEx.bestMatch(availLocales, user.getLocale());
				}
				else
				{
					return LocaleEx.bestMatch(availLocales, ctx.getLocales());
				}
			}
			catch (Exception e)
			{
				return Locale.getDefault();
			}
		}
	}
	
	public String getCookie(String name)
	{
		if (this.delegate!=null)
		{
			return this.delegate.getCookie(name);
		}
		else if (this.cookies==null)
		{
			return null;
		}
		else
		{
			return this.cookies.get(name);
		}
	}
	public void setCookie(String name, String val)
	{
		if (this.delegate!=null)
		{
			this.delegate.setCookie(name, val);
		}
		else
		{
			if (this.cookies==null)
			{
				this.cookies = new HashMap<String, String>();
			}
			this.cookies.put(name, val);
		}
	}
	public Map<String, String> getCookies()
	{
		if (this.delegate!=null)
		{
			return this.delegate.getCookies();
		}
		else
		{
			return this.cookies;
		}
	}
	
	public void setFormException(WebFormException formException)
	{
		if (this.delegate!=null)
		{
			this.delegate.setFormException(formException);
		}
		else
		{
			this.formException = formException;
		}
	}
	public WebFormException getFormException()
	{
		if (this.delegate!=null)
		{
			return this.delegate.getFormException();
		}
		else
		{
			return this.formException;
		}
	}
	public boolean isFormException()
	{
		return getFormException()!=null;
	}
	public boolean isFormException(String fieldName)
	{
		return getFormException()!=null && getFormException().getFields().contains(fieldName);
	}
	
	public boolean isCommitted()
	{
		if (this.delegate!=null)
		{
			return this.delegate.isCommitted();
		}
		else
		{
			return this.committed;
		}
	}
	public void setCommitted(boolean b)
	{
		if (this.delegate!=null)
		{
			this.delegate.setCommitted(b);
		}
		else
		{
			this.committed  = b;
		}
	}
	
	public String getEphemeral(String name)
	{
		if (this.delegate!=null)
		{
			return this.delegate.getEphemeral(name);
		}
		else if (this.ephemerals==null)
		{
			return null;
		}
		else
		{
			return this.ephemerals.get(name);
		}
	}
	public void setEphemeral(String name, String val)
	{
		if (this.delegate!=null)
		{
			this.delegate.setEphemeral(name, val);
		}
		else
		{
			if (this.ephemerals==null)
			{
				this.ephemerals = new HashMap<String, String>();
			}
			this.ephemerals.put(name, val);
		}
	}
	public Map<String, String> getEphemerals()
	{
		if (this.delegate!=null)
		{
			return this.delegate.getEphemerals();
		}
		else
		{
			return this.ephemerals;
		}
	}

	public void progressGuidedSetup() throws Exception
	{
		if (getContext().getCommand(1).equals(UrlGenerator.COMMAND_SETUP)==false)
		{
			return;
		}
		
		User user = UserStore.getInstance().load(getContext().getUserID());
		if (user!=null && user.isGuidedSetup())
		{
			int step = user.getGuidedSetupStep();
			List<String> pages = user.getGuidedSetupPages();
						
			if (step<0 || getContext().getCommand().equals(UrlGenerator.COMMAND_SETUP + "/" + pages.get(step)))
			{
				// User is on the last page, so we increment the step counter
				if (step+1<pages.size())
				{
					user = (User) user.clone();
					user.setGuidedSetupStep(step+1);
					UserStore.getInstance().save(user);
					
					throw new RedirectException(UrlGenerator.COMMAND_SETUP + "/" + pages.get(step+1), null);
				}
				else
				{
					throw new RedirectException(CompletePage.COMMAND, null);
				}
			}
			else
			{
				// User is on a previous page, so we just redirect to the following step
				for (int s=0; s<pages.size(); s++)
				{
					if (getContext().getCommand().equals(UrlGenerator.COMMAND_SETUP + "/" + pages.get(s)))
					{
						throw new RedirectException(UrlGenerator.COMMAND_SETUP + "/" + pages.get(s+1), null);
					}
				}
				
				// Just in case, redirect to the current step
				throw new RedirectException(UrlGenerator.COMMAND_SETUP + "/" + pages.get(step), null);
			}
		}
	}
	
	// - - - - -
	// WRITE
	
	public void write(Object obj)
	{
		try
		{
			writeBinary(obj.toString().getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			// Shouldn't happen
		}
	}

	public void writeEncode(Object obj)
	{
		try
		{
			writeBinary(Util.htmlEncode(obj.toString()).getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			// Shouldn't happen
		}
	}
	
	public void writeBinary(byte[] bytes)
	{
		writeBinary(bytes, 0, bytes.length);
	}
	
	public void writeBinary(byte[] bytes, int off, int len)
	{
		getCanvas().write(bytes, off, len);
	}
	
	/**
	 * Write an anchor (A) tag to the page.
	 * @param caption The caption of the link, may be <code>null</code>, in which case the URL will be shown.
	 * @param url The URL of the link.
	 */
	public void writeLink(String caption, String url)
	{
		write("<a href=\"");
		writeEncode(url);
		write("\">");
		if (!Util.isEmpty(caption))
		{
			writeEncode(caption);
		}
		else
		{
			writeEncode(url);
		}
		write("</a>");
	}
	
	public void writeImage(String resourceFileName, String altText) throws Exception
	{
		new ImageControl(this).resource(resourceFileName).altText(altText).render();
	}
	public void writeImage(String resourceFileName, String altText, String url) throws Exception
	{
		new ImageControl(this).resource(resourceFileName).altText(altText).url(url).render();
	}
	public void writeImageSized(String resourceFileName, String altText, String url, int w, int h) throws Exception
	{
		new ImageControl(this).resource(resourceFileName).altText(altText).url(url).width(w).height(h).render();
	}
	public void writeImage(Image image, String sizeSpec, String altText, String url) throws Exception
	{
		new ImageControl(this).img(image, sizeSpec).altText(altText).url(url).render();
	}
	
	public void writeIncludeCSS(String cssFileName)
	{
		write("<link type=\"text/css\" rel=stylesheet href=\"");
		write(getResourceURL(cssFileName));
		write("?v=");
		write(getContext().getUserAgent().hashCode() + Controller.getStartTime()/1000L);
// For Demo 2012-07-09
//if (isParameter("kpbg"))
//{
//	write("&kpbg=");
//	write(Util.urlEncode(getParameterString("kpbg")));
//}
//if (isParameter("kpax"))
//{
//	write("&kpax=");
//	write(Util.urlEncode(getParameterString("kpax")));
//}
		write("\">");
	}
	public void writeIncludeJS(String jsFileName)
	{
		write("<script type=\"text/javascript\" src=\"");
		write(getResourceURL(jsFileName));
		write("?v=");
		write(Controller.getStartTime()/1000L);
		write("\"></script>");
	}
	
	/**
	 * Writes the Javascript code to create the UserAgent object which allows detecting the user agent.
	 * Should be called only once.
	 */
	public void writeUserAgentJS()
	{
		Map<String, Float> tags = getContext().getUserAgent().getTags();
		write("<script type=\"text/javascript\">");
		write("var UserAgent={");
		boolean first = true;
		for (String n : tags.keySet())
		{
			Float v = tags.get(n);
			
			if (!first)
			{
				write(",");
			}
			writeEncode(n);
			write(":");
			if (v==0F)
			{
				write("true");
			}
			else
			{
				write(v);
			}
			first = false;
		}
		write("};");
		write("</script>");
	}

	public void writeEncodeFloat(float f, int digits)
	{
		NumberFormat formatter = NumberFormat.getNumberInstance(getLocale());
		formatter.setMaximumFractionDigits(digits);
		formatter.setMinimumFractionDigits(digits);
//		formatter.setGroupingUsed(true);
		writeEncode(formatter.format(f));
	}

	public void writeEncodeLong(long l)
	{
		NumberFormat formatter = NumberFormat.getNumberInstance(getLocale());
//		formatter.setGroupingUsed(true);
		writeEncode(formatter.format(l));
	}

	/**
	 * Dates that represent a particular day rather than a particular time, e.g. birthday, should be written using this method.
	 * @param date
	 */
	public void writeEncodeDay(Date date)
	{
		writeEncode(DateFormatEx.getDateInstance(getLocale(), TimeZone.getTimeZone("GMT")).format(date));
	}

	public void writeEncodeDate(Date date)
	{
		writeEncode(DateFormatEx.getDateInstance(getLocale(), getTimeZone()).format(date));
	}

	public void writeEncodeMiniDate(Date date)
	{
		writeEncode(DateFormatEx.getMiniDateInstance(getLocale(), getTimeZone()).format(date));
	}

	public void writeEncodeTime(Date date)
	{
		writeEncode(DateFormatEx.getTimeInstance(getLocale(), getTimeZone()).format(date));
	}

	public void writeEncodeMiniTime(Date date)
	{
		writeEncode(DateFormatEx.getMiniTimeInstance(getLocale(), getTimeZone()).format(date));
	}

	public void writeEncodeDateTime(Date date)
	{
		writeEncode(DateFormatEx.getDateTimeInstance(getLocale(), getTimeZone()).format(date));
	}
	
	public final void writeEncodeDateOrTime(Date dt)
	{
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long beginningOfToday = cal.getTimeInMillis();
		cal.add(Calendar.DATE, 1);
		long beginningOfTomorrow = cal.getTimeInMillis();
		
		if (dt.getTime() >= beginningOfToday && dt.getTime() < beginningOfTomorrow)
		{
			writeEncodeTime(dt);
		}
		else
		{
			writeEncodeDate(dt);
		}
	}

	public void writeFormOpen()
	{
		writeFormOpen(getContext().isSecureSocket(), null, null);
	}
	
	public void writeFormOpen(String method, String command)
	{
		writeFormOpen(getContext().isSecureSocket(), method, command);
	}
	
	public void writeFormOpen(boolean https, String method, String command)
	{
		if (Util.isEmpty(method)) method = "POST";
		if (Util.isEmpty(command))
		{
			command = getContext().getCommand();
		}

		write("<form method=");
		write(method);
		write(" action=\"");
		write(getPageURL(https, command, null));
		write("\" accept-charset=\"UTF-8\" autocomplete=off>");
		if (method.equalsIgnoreCase("POST"))
		{
			writeHiddenInput(RequestContext.PARAM_CHARSET, "");
			writeHiddenInput(RequestContext.PARAM_SESSION, getContext().getSessionID().toString());
		}
	}
	
	public void writeFormClose()
	{
		write("</form>");		
	}
	
	public void writeHiddenInput(String name, Object initialValue)
	{
		HiddenInputControl hidden = new HiddenInputControl(this, name);
		hidden.setInitialValue(initialValue);
		hidden.render();
	}
	
	public void writePasswordInput(String name, Object initialValue, int size, int maxLen)
	{
		PasswordInputControl pw = new PasswordInputControl(this, name);
		pw.setInitialValue(initialValue);
		pw.setSize(size);
		pw.setMaxLength(maxLen);
		pw.render();
	}

	public void writeTextInput(String name, Object initialValue, int size, int maxLen)
	{
		TextInputControl txt = new TextInputControl(this, name);
		txt.setInitialValue(initialValue);
		txt.setSize(size);
		txt.setMaxLength(maxLen);
		txt.render();
	}
		
	/**
	 * Embeds videos to YouTube.
	 * @param url
	 * @param width
	 * @param height
	 */
	public void writeEmbedVideo(String url, int width, int height)
	{
		if (Util.isEmpty(url)) return;
		
		if (url.indexOf("://youtu.be/")>0)
		{
			int p = url.indexOf("://youtu.be/") + "://youtu.be/".length();
			String videoID = url.substring(p);
			if (videoID.matches("^[0-9a-zA-z\\x2d_]+$")==false)
			{
				return;
			}
			
			write("<iframe width=");
			write(width);
			write(" height=");
			write(height);
			write(" src=\"http://www.youtube.com/embed/");
			writeEncode(videoID);
			write("\" frameborder=0 allowfullscreen></iframe>");
		}
		else if (url.indexOf("://www.youtube.com/watch")>0)
		{
			int p = url.indexOf("v=");
			if (p<0) return;
			int q = url.indexOf("&", p);
			if (q<0) q = url.length();
			
			String videoID = url.substring(p, q);
			if (videoID.matches("^[0-9a-zA-z\\x2d_]+$")==false)
			{
				return;
			}
			
			write("<iframe width=");
			write(width);
			write(" height=");
			write(height);
			write(" src=\"http://www.youtube.com/embed/");
			writeEncode(videoID);
			write("\" frameborder=0 allowfullscreen></iframe>");
		}
	}
	
	public void writeRichEditField(String name, String defaultValue, int cols, int rows) throws Exception
	{
		RichEditControl richEdit = new RichEditControl(this);
		richEdit.setName(name);
		richEdit.setInitialValue(defaultValue);
		richEdit.setCols(cols);
		richEdit.setRows(rows);
		richEdit.render();
	}
	
	public void writePhoneInput(String name, String defaultValue) throws Exception
	{
		new PhoneInputControl(this, name)
			.setInitialValue(defaultValue)
			.render();
		
//		String prefix = null;
//		String localNumber = null;
//		if (defaultValue!=null)
//		{
//			prefix = CountryStore.getInstance().extractPhonePrefix(defaultValue);
//			localNumber = defaultValue.substring(prefix.length());
//		}
//		
//		write("<table class=PhoneInput><tr><td>");
//		// Populate countries combo
//		List<String> prefixes = CountryStore.getInstance().getAllPhonePrefixes();
//		
//		SelectControl select = new SelectControl(this, "_prefix_" + name);
//		select.addOption("", "");
//		for (String p : prefixes)
//		{
//			select.addOption("+" + p, p);
//		}
//		select.setInitialValue(prefix);
//		select.render();
//		write("</td><td>");
//		writeTextInput(name, localNumber, 12, 20);
//		write("</td></tr></table>");
	}
	
	public final String getParameterPhone(String name)
	{
		String prefix = getContext().getParameter("_prefix_" + name);
		String local = getContext().getParameter(name).toLowerCase(Locale.US);
		
		// Remove leading zeros (except in Italy)
		while (local.startsWith("0") && prefix.startsWith(Country.ITALY + "/")==false)
		{
			local = local.substring(1);
		}
		
		String localEx = "";
		for (int i=0; i<local.length(); i++)
		{
			char ch = local.charAt(i);
			if (Character.isDigit(ch))
			{
				localEx += ch;
			}
			else if (ch>='a' && ch<='c')
			{
				localEx += "2";
			}
			else if (ch>='d' && ch<='f')
			{
				localEx += "3";
			}
			else if (ch>='g' && ch<='i')
			{
				localEx += "4";
			}
			else if (ch>='j' && ch<='l')
			{
				localEx += "5";
			}
			else if (ch>='m' && ch<='o')
			{
				localEx += "6";
			}
			else if (ch>='p' && ch<='s')
			{
				localEx += "7";
			}
			else if (ch>='t' && ch<='v')
			{
				localEx += "8";
			}
			else if (ch>='w' && ch<='z')
			{
				localEx += "9";
			}
		}
		
		if (Util.isEmpty(localEx))
		{
			return "";
		}
		else
		{
			return prefix + localEx;
		}
	}

	public final String validateParameterPhone(String name) throws WebFormException
	{
		String local = getParameterString(name);
		if (Util.isEmpty(local))
		{
			throw new WebFormException(name, getString("common:Errors.MissingField"));
		}

		String prefix = getParameterString("_prefix_" + name);
		if (Util.isEmpty(prefix))
		{
			throw new WebFormException("_prefix_" + name, getString("common:Errors.MissingField"));
		}
		
		if (prefix.matches("^[a-zA-Z]{2}/[0-9]{1,4}$")==false)
		{
			throw new WebFormException("_prefix_" + name, getString("common:Errors.InvalidValue"));
		}

		String fullNum = getParameterPhone(name);
		if (Util.isEmpty(fullNum))
		{
			// No digits in local number
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}
		
		// Verification of US/Canada numbers
		if (prefix.endsWith("/1") && local.length()!=10)
		{
			if (local.length()>10)
			{
				throw new WebFormException(name, getString("common:Errors.FieldTooLong", 10));
			}
			if (local.length()<10)
			{
				throw new WebFormException(name, getString("common:Errors.FieldTooShort", 10));
			}
		}

		return fullNum;
	}

	/**
	 * Returns the HTML posted through a rich edit control.
	 * @return The HTML code, or <code>null<code> if no parameter was posted under this name.
	 */
	public final String getParameterRichEdit(String name)
	{
		return RichEditControl.validate(getContext().getParameter(name));
	}

	/**
	 * Returns the value of a <code>String</code> parameter.
	 * @return The <code>String</code> posted, or <code>null<code> if no parameter was posted under this name.
	 */
	public final String getParameterString(String name)
	{
		String s = getContext().getParameter(name);
		if (s!=null)
		{
			s = s.trim();
		}
		return s;
	}
			
	/**
	 * Returns the value of a <code>String</code> parameter.
	 * @param name The name of the parameter.
	 * @param minLen The minimum length to be returned; <code>-1</code> to indicate no limit; <code>1</code> to mandate non-empty field.
	 * @param maxLen The maximum length to be returned; <code>-1</code> to indicate no limit. 
	 * @return The <code>String</code> posted, or <code>null<code> if no parameter was posted under this name.
	 * @throws WebFormException If the value posted exceeded <code>maxLen</code>.
	 */
	public final String validateParameterString(String name, int minLen, int maxLen) throws WebFormException
	{
		String s = getContext().getParameter(name);
		if (s!=null)
		{
			s = s.trim();
		}
		
		// Verify not empty
		if (Util.isEmpty(s) && minLen>0)
		{
			throw new WebFormException(name, getString("common:Errors.MissingField"));
		}
		
		if (s==null)
		{
			return null;
		}
		
		if (maxLen>=0 && s.length()>maxLen)
		{
			throw new WebFormException(name, getString("common:Errors.FieldTooLong", maxLen));
		}
		if (minLen>=0 && s.length()<minLen)
		{
			throw new WebFormException(name, getString("common:Errors.FieldTooShort", minLen));
		}
		
		return s;
	}

	public final int validateParameterInteger(String name, int minVal, int maxVal) throws WebFormException
	{
		String valStr = getContext().getParameter(name);
		if (Util.isEmpty(valStr))
		{
			throw new WebFormException(name, getString("common:Errors.MissingField"));
		}
		
		int val;
		try
		{
			val = Integer.parseInt(valStr);
		}
		catch (NumberFormatException nfe)
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}
		
		if (val<minVal || val>maxVal)
		{
			throw new WebFormException(name, getString("common:Errors.IntegerBetween", minVal, maxVal));
		}
		
		return val;
	}

	public final Float validateParameterDecimal(String name, Float minVal, Float maxVal) throws WebFormException
	{
		String valStr = getContext().getParameter(name);
		if (Util.isEmpty(valStr))
		{
			throw new WebFormException(name, getString("common:Errors.MissingField"));
		}
		
		Float val;
		try
		{
			val = Float.parseFloat(valStr);
		}
		catch (NumberFormatException nfe)
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}
		
		if ((minVal != null && val < minVal) || (maxVal != null && val > maxVal))
		{
			throw new WebFormException(name, getString("common:Errors.DecimalBetween", minVal, maxVal));
		}
				
		return val;
	}

	/**
	 * Returns the value of a <code>Integer</code> parameter.
	 * @param name The name of the parameter.
	 * @return The <code>Integer</code> posted, or <code>null<code> if no parameter was posted under this name.
	 */
	public final Integer getParameterInteger(String name)
	{
		String valStr = getContext().getParameter(name);
		if (Util.isEmpty(valStr))
		{
			return null;
		}
		
		try
		{
			return Integer.parseInt(valStr);
		}
		catch (NumberFormatException nfe)
		{
			return null;
		}
	}

	/**
	 * Returns the value of a <code>Decimal</code> parameter.
	 * @param name The name of the parameter.
	 * @return The <code>Decimal</code> posted, or <code>null<code> if no parameter was posted under this name.
	 */
	public final Float getParameterDecimal(String name)
	{
		String valStr = getContext().getParameter(name);
		if (Util.isEmpty(valStr))
		{
			return null;
		}
		
		try
		{
			return Float.parseFloat(valStr);
		}
		catch (NumberFormatException nfe)
		{
			return null;
		}
	}
	
	/**
	 * Returns the value of a <code>Long</code> parameter.
	 * @param name The name of the parameter.
	 * @return The <code>Long</code> posted, or <code>null<code> otherwise.
	 */
	public final Long getParameterLong(String name)
	{
		String valStr = getContext().getParameter(name);
		if (Util.isEmpty(valStr))
		{
			return null;
		}
		
		try
		{
			return Long.parseLong(valStr);
		}
		catch (NumberFormatException nfe)
		{
			return null;
		}
	}

	/**
	 * Returns the value of a <code>Date</code> parameter.
	 * @param name The name of the parameter.
	 * @return The <code>Date</code>, or <code>null</code> otherwise.
	 */
	public final Date getParameterDate(String name)
	{
		String val = getParameterString(name);
		if (Util.isEmpty(val)) return null;

		String dfStr = getParameterString("_df_" + name);
		if (Util.isEmpty(dfStr)) return null;
		
		String tzStr = getParameterString("_tz_" + name);
		if (Util.isEmpty(tzStr)) return null;

		try
		{
			DateFormat df = DateFormatEx.getSimpleInstance(dfStr, getLocale(), TimeZone.getTimeZone(tzStr));
			return df.parse(val);
		}
		catch (ParseException pe)
		{
			return null;
		}
	}
	
	/**
	 * Returns the value of a <code>Date</code> parameter.
	 * @param name The name of the parameter.
	 * @return The <code>Date</code>, or <code>null</code>, if no parameter was posted under this name.
	 * @throws WebFormException If an invalid value was posted.
	 */
	public final Date validateParameterDate(String name) throws WebFormException
	{
		String val = getParameterString(name);
		if (Util.isEmpty(val))
		{
			throw new WebFormException(name, getString("common:Errors.MissingField"));
		}

		String dfStr = getParameterString("_df_" + name);
		if (Util.isEmpty(dfStr))
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}
		
		String tzStr = getParameterString("_tz_" + name);
		if (Util.isEmpty(tzStr))
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}

		try
		{
			DateFormat df = DateFormatEx.getSimpleInstance(dfStr, getLocale(), TimeZone.getTimeZone(tzStr));
			
			Date date = df.parse(val);
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tzStr));
			cal.setTime(date);
			int yy = cal.get(Calendar.YEAR);
			if (yy<100 && yy>=0 && val.indexOf("00"+(yy<10?"0":"")+yy)<0)
			{
				throw new WebFormException(name, getString("common:Errors.NotY4Date"));
			}
			
			return date;
		}
		catch (ParseException pe)
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));		
		}
	}
	
	public final Date validateParameterDate(String name, Date minVal, Date maxVal) throws WebFormException
	{
		Date val = validateParameterDate(name);
		if (minVal!=null && val.before(minVal))
		{
			throw new WebFormException(name, getString("common:Errors.DateCannotBeEarlier", minVal));
		}
		if (maxVal!=null && val.after(maxVal))
		{
			throw new WebFormException(name, getString("common:Errors.DateCannotBeLater", maxVal));
		}
		return val;
	}
	
	/**
	 * Returns the value of a <code>UUID</code> parameter.
	 * @param name The name of the parameter.
	 * @return The <code>UUID</code>, or <code>null</code>, if no parameter was posted under this name.
	 * @throws WebFormException If an invalid value was posted.
	 */
	public final UUID validateParameterUUID(String name) throws WebFormException
	{
		String valStr = getContext().getParameter(name);
		if (Util.isEmpty(valStr))
		{
			throw new WebFormException(name, getString("common:Errors.MissingField"));
		}
		
		if (!Util.isUUID(valStr))
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}
		
		try
		{
			return UUID.fromString(valStr);
		}
		catch (NumberFormatException nfe)
		{
			throw new WebFormException(name, getString("common:Errors.InvalidValue"));
		}
	}
	
	public final UUID getParameterUUID(String name)
	{
		String valStr = getContext().getParameter(name);
		if (!Util.isUUID(valStr))
		{
			return null;
		}
		else
		{
			return UUID.fromString(valStr);
		}
	}
	
	public final Pair<String,String> getParameterTypeAhead(String name)
	{
		String k = getParameterString("_key_" + name);
		if (k==null) return null;
		String v = getParameterString(name);
		return new Pair<String, String>(k.trim(), v.trim());
	}
	
	/**
	 * Indicates whether or not a parameter was posted.
	 * @param name The name of the parameter.
	 * @return
	 */
	public final boolean isParameter(String name)
	{
		return getContext().getParameter(name)!=null;
	}
	
	/**
	 * Indicates whether or not a parameter was posted and is not empty.
	 * @param name The name of the parameter.
	 * @return
	 */
	public final boolean isParameterNotEmpty(String name)
	{
		return !Util.isEmpty(getContext().getParameter(name));
	}

	/**
	 * Writes a date picker.
	 * @param name
	 * @param initialValue If specified, must be midnight GMT.
	 */
	public void writeDateInput(String name, Date initialValue)
	{
		DateInputControl dt = new DateInputControl(this, name);
		dt.setInitialValue(initialValue);
		dt.render();
	}

	/**
	 * Writes a date+time picker.
	 * @param name
	 * @param initialValue If specified, this date will be printed in the page's current time zone.
	 */
	public void writeDateTimeInput(String name, Date initialValue)
	{
		DateTimeInputControl dt = new DateTimeInputControl(this, name);
		dt.setInitialValue(initialValue);
		dt.render();
	}

	public void writeNumberInput(String name, int initialValue, int size, int minValue, int maxValue)
	{
		NumberInputControl num = new NumberInputControl(this, name);
		num.setInitialValue(initialValue);
		num.setSize(size);
		num.setMaxLength(size);
		num.setMinValue(minValue);
		num.setMaxValue(maxValue);
		num.render();
	}
	
	public void writeTextAreaInput(String name, Object initialValue, int cols, int rows, int maxLen)
	{
		TextAreaInputControl area = new TextAreaInputControl(this, name);
		area.setInitialValue(initialValue);
		area.setCols(cols);
		area.setRows(rows);
		area.setMaxLength(maxLen);
		area.render();
	}
	
	public void writeDecimalInput(String name, Float initialValue, int size, Float minValue, Float maxValue)
	{
		DecimalInputControl dec = new DecimalInputControl(this, name);
		
		// Truncate .0 if the number appears to be an integer
		if (initialValue != null && initialValue.intValue() == initialValue)
		{
			dec.setInitialValue(initialValue.intValue());
		}
		else
		{
			dec.setInitialValue(initialValue);
		}
		
		dec.setSize(size);
		dec.setMaxLength(size);
		dec.setMinValue(minValue);
		dec.setMaxValue(maxValue);
		dec.render();
	}
		
	public void writeTypeAheadInput(String name, Object initialKey, Object initialValue, int size, int maxLen, String jsonPageUrl)
	{
		if (initialKey==null) initialKey = "";

		// Hidden input must immediately precede the visible input (Javascript relies on it)		
		HiddenInputControl hidden = new HiddenInputControl(this, "_key_" + name);
		hidden.setInitialValue(initialKey);
		hidden.render();
		
		TextInputControl txt = new TextInputControl(this, name);
		txt.setInitialValue(initialValue);
		txt.setSize(size);
		txt.setMaxLength(maxLen);
		txt.setAttribute("typeahead", jsonPageUrl);
		txt.render();
	}
		
	public void writeImageInput(String name, Image initialValue)
	{
		new ImageInputControl(this, name).setInitialValue(initialValue).render();
	}
	
	public final Image validateParameterImage(String name) throws WebFormException
	{
		RequestContext ctx = getContext();

		String state = ctx.getParameter("_state_" + name);
		if (state.equals("uploaded"))
		{
			String tempFileName = ctx.getParameter("_uploaded_"+name);
			if (!tempFileName.matches("^\\w+\\x2e?\\w*$")) // abcd12345.tmp
			{
				throw new WebFormException(name, getString("common:Errors.InvalidValue"));
			}
			File file = new File(System.getProperty("java.io.tmpdir"), tempFileName);
			if (!file.exists())
			{
				throw new WebFormException(name, getString("common:Errors.InvalidValue"));
			}
			try
			{
				JaiImage jai = new JaiImage(file);
				Image img = new Image(jai);
				return img;
			}
			catch (IOException e)
			{
				throw new WebFormException(name, getString("common:Errors.InvalidImage"));
			}
		}
		else if (state.equals("new"))
		{
			File file = getContext().getPostedFile(name);
			if (!file.exists())
			{
				throw new WebFormException(name, getString("common:Errors.InvalidValue"));
			}
			try
			{
				JaiImage jai = new JaiImage(file);
				Image img = new Image(jai);
				return img;
			}
			catch (IOException e)
			{
				throw new WebFormException(name, getString("common:Errors.InvalidImage"));
			}
		}
		else if (state.equals("current"))
		{
			String current = ctx.getParameter("_current_" + name);
			if (!Util.isUUID(current))
			{
				throw new WebFormException(name, getString("common:Errors.InvalidValue"));
			}
			
			Image image = null;
			try
			{
				image = ImageStore.getInstance().load(UUID.fromString(current));
			}
			catch (Exception e)
			{
			}
			if (image==null)
			{
				throw new WebFormException(name, getString("common:Errors.InvalidValue"));
			}
			// !$! Need to check that current user is owner of this image
			return image;
		}
//		else if (state.equals("empty"))
//		{
//			return null;
//		}
		
		return null;
	}
	
	public final Image getParameterImage(String name) throws Exception
	{
		return getParameterImage(name, Image.MAX_WIDTH, Image.MAX_HEIGHT);
	}
	
	/**
	 * Returns the image posted through an image input.
	 * @param name
	 * @param maxWidth If greater than 0, larger newly-uploaded images will be scaled down to fit this width.
	 * @param maxHeight If greater than 0, larger newly-uploaded images will be scaled down to fit this height.
	 * @return
	 * @throws Exception 
	 */
	public final Image getParameterImage(String name, int maxWidth, int maxHeight) throws Exception
	{
		RequestContext ctx = getContext();

		String state = ctx.getParameter("_state_" + name);
		if (state.equals("uploaded"))
		{
			// Previously uploaded image (error in form)
			String tempFileName = ctx.getParameter("_uploaded_"+name);
			if (tempFileName.matches("^\\w+\\x2e?\\w*$")) // abcd12345.tmp
			{
				File file = new File(System.getProperty("java.io.tmpdir"), tempFileName);
				if (file.exists())
				{
					JaiImage jai = new JaiImage(file);
					if (maxWidth>0 && maxHeight>0)
					{
						jai.resizeToFit(maxWidth, maxHeight);
					}
					Image img = new Image(jai);
					return img;
				}
			}
		}
		else if (state.equals("new"))
		{
			// Newly uploaded image
			File file = getContext().getPostedFile(name);
			if (file!=null)
			{
				JaiImage jai = new JaiImage(file);
				if (maxWidth>0 && maxHeight>0)
				{
					jai.resizeToFit(maxWidth, maxHeight);
				}
				Image img = new Image(jai);
				return img;
			}
		}
		else if (state.equals("current"))
		{
			String current = ctx.getParameter("_current_" + name);
			if (Util.isUUID(current))
			{
				Image image = ImageStore.getInstance().load(UUID.fromString(current));
				// !$! Need to check that current user is owner of this image
				return image;
			}
		}
//		else if (state.equals("empty"))
//		{
//			return null;
//		}
		
		return null;
	}
	
	public void writeRadioButton(String name, String label, Object value, Object initialValue)
	{
		new RadioButtonInputControl(this, name)
			.setValue(value)
			.setLabel(label)
			.setInitialValue(initialValue)
			.render();
	}
		
	public void writeCheckbox(String name, String label, boolean initialValue)
	{
		new CheckboxInputControl(this, name)
			.setLabel(label)
			.setInitialValue(initialValue)
			.render();
	}
	
	public void writeButton(String caption)
	{
		writeButton(null, caption);
	}

	public void writeSaveButton(DataBean bean)
	{
		writeSaveButton(null, bean);
	}
	public void writeSaveButton(String name, DataBean bean)
	{
		String label;
//		if (getContext().getCommand(1).equalsIgnoreCase(UrlGenerator.COMMAND_SETUP))
//		{
//			label = "controls:Button.Next";
//		}
//		else
		if (bean==null || bean.isSaved())
		{
			label = "controls:Button.Save";
		}
		else
		{
			label = "controls:Button.Create";
		}
		writeButton(name, getString(label));
	}
	public void writeRemoveButton()
	{
		writeButtonRed(null, getString("controls:Button.Remove"));
	}
	public void writeRemoveButton(String name)
	{
		writeButtonRed(name, getString("controls:Button.Remove"));
	}
	
	public void writeButton(String name, String caption)
	{
		new ButtonInputControl(this, name).setValue(caption).render();
//		write("<input type=submit");
//		if (name!=null)
//		{
//			write(" name=\"");
//			writeEncode(name);
//			write("\"");
//		}
//		if (caption!=null)
//		{
//			write(" value=\"");
//			writeEncode(caption);
//			write("\"");
//		}
//		write(">");
	}
	
	public void writeButtonRed(String name, String caption)
	{
		new ButtonInputControl(this, name).setValue(caption).setStrong(true).render();
//		write("<input type=submit");
//		if (name!=null)
//		{
//			write(" name=\"");
//			writeEncode(name);
//			write("\"");
//		}
//		if (caption!=null)
//		{
//			write(" value=\"");
//			writeEncode(caption);
//			write("\"");
//		}
//		write(" red>");
	}

	/**
	 * Renders a button that takes the user to the preceding page.
	 * @param caption The caption to use for the button. Can be <code>null</code>.
	 * @param pageID Any identifier that can uniquely identify the page. if <code>null</code>, defaults to the command.
	 */
	public void writeBackButton(String caption, String pageID)
	{
//		if (pageID==null)
//		{
//			pageID = getContext().getCommand();
//		}
//		
//		// Push the _back_ param into the sessionStorage stack
//		String ephem = getEphemeral("BackBtnPush");
//		if (ephem==null)
//		{
//			setEphemeral("BackBtnPush", "1");
//
//			String back = getContext().getParameter(RequestContext.PARAM_BACK);
//			if (back==null)
//			{
//				back = getContext().getHeader("referer");
//			}
//			String backCaption = getContext().getParameter(RequestContext.PARAM_BACK_CAPTION);
//			
//			if (back!=null)
//			{
//				write("<script type=\"text/javascript\">backPush('");
//				write(Util.jsonEncode(back));
//				write("','");
//				if (!Util.isEmpty(backCaption))
//				{
//					write(Util.jsonEncode(backCaption));
//				}
//				write("','");
//				write(pageID);
//				write("');</script>");
//			}			
//		}
		
		// Render the button initially hidden
		String id = UUID.randomUUID().toString();
		new ButtonInputControl(this, null)
			.setValue(Util.isEmpty(caption)? getString("controls:Button.Back") : caption)
			.setStyleAttribute("display", "none")
			.setAttribute("id", "backBtn"+id)
			.setAttribute("class", "Back")
			.render();
		
		// Show it, if the page ID is at the top of the stack
		write("<script type=\"text/javascript\">backActivateButton('backBtn");
		write(id);
		write("');</script>");
	}

	public void writeAjaxFrameOpen()
	{
		write("<span id=\"ajaxframe");
		write(Util.nextRoundRobin());
		write("\" onclick=\"ajaxFrameClick(event,this)\" onsubmit=\"ajaxFormSubmit(event,this)\">");
	}
	public void writeAjaxFrameClose()
	{
		write("</span>");
	}
	
	public void writeTooltip(String text, String helpString)
	{
		write("<span class=Tooltip onclick=\"tooltipToggle(this);\"");
		if (getContext().getUserAgent().isMSIE() && getContext().getUserAgent().getVersionMSIE()<8)
		{
			// Hack to fix overlapping bug in IE7
			int z = 1000;
			String zIndex = getEphemeral("tooltip.zindex");
			if (zIndex!=null)
			{
				z = Integer.parseInt(zIndex);
			}
			setEphemeral("tooltip.zindex", String.valueOf(z-1));
			write(" style='z-index:");
			write(z);
			write("'");
		}
		write(">");
		writeEncode(text);
		write("<span>");
		writeEncode(helpString);
		write("</span>");
		write("</span>");
	}
	public void writeTooltipRightAligned(String text, String helpString)
	{
		write("<span class=\"Tooltip Right\" onclick=\"tooltipToggle(this);\"");
		if (getContext().getUserAgent().isMSIE() && getContext().getUserAgent().getVersionMSIE()<8)
		{
			// Hack to fix overlapping bug in IE7
			int z = 1000;
			String zIndex = getEphemeral("tooltip.zindex");
			if (zIndex!=null)
			{
				z = Integer.parseInt(zIndex);
			}
			setEphemeral("tooltip.zindex", String.valueOf(z-1));
			write(" style='z-index:");
			write(z);
			write("'");
		}
		write(">");
		writeEncode(text);
		write("<span>");
		writeEncode(helpString);
		write("</span>");
		write("</span>");
	}
	
	/**
	 * Pages displaying a CAPTCHA should not allow caching by returning FLAG_CACHE_OK.
	 * @param name
	 */
	public void writeCaptcha(String name)
	{
		Captcha cap = null;
		String capKey = getContext().getParameter("_key_" + name);
		if (capKey!=null)
		{
			cap = Captcha.getByKey(UUID.fromString(capKey));
		}
		if (cap==null)
		{
			cap = Captcha.createCaptcha();
		}
		
//		write("<table cellspacing=0 cellpadding=0><tr valign=top><td>");
		writeTextInput(name, null, cap.getText().length(), cap.getText().length());
//		write("</td><td>&nbsp;</td><td>");
		
//		String script = "var i=$('#" + Util.htmlEncode(name) + "_img');i.attr('src',i.attr('src')+'&v=1');return false;";
//		write("<small> <a href=\"about:blank\" onclick=\"" + script + "\">");
//		write("refresh");
//		write("</a></small>");
		write("<br>");
		
		write("<img ");
//		write("id=\"");
//		writeEncode(name);
//		write("_img\" ");
		write("width=");
		write(cap.getWidth());
		write(" height=");
		write(cap.getHeight());
		write(" class=Captcha src=\"");
		write(getPageURL(CaptchaImagePage.COMMAND, new ParameterMap(CaptchaImagePage.PARAM_ID, cap.getKey().toString())));
		write("\">");
		
		writeHiddenInput("_key_" + name, cap.getKey().toString());

//		write("</td></tr></table>");
	}
	
	public final void validateParameterCaptcha(String name) throws Exception
	{
		RequestContext ctx = getContext();
		
		String capKey = ctx.getParameter("_key_" + name);
		if (capKey==null)
		{
			throw new WebFormException("captcha", getString("common:Errors.CaptchaMismatch"));
		}
		Captcha cap = Captcha.getByKey(UUID.fromString(capKey));
		if (cap==null)
		{
			throw new WebFormException("captcha", getString("common:Errors.CaptchaMismatch"));
		}
		String capText = ctx.getParameter(name);
		if (capText==null)
		{
			throw new WebFormException("captcha", getString("common:Errors.CaptchaMismatch"));
		}
		if (capText.equalsIgnoreCase(cap.getText())==false)
		{
			throw new WebFormException("captcha", getString("common:Errors.CaptchaMismatch"));
		}
	}

	public void writeDuration(long duration)
	{
		if (duration<0)
		{
			write("-");
			duration *= -1;
		}
		
		duration += 500L; // Rounding
		
		int secs = (int) (duration/1000L);
		int ss = secs % 60;
		int mm = (secs / 60) % 60;
		int hh = (secs / 60 / 60) % 24;
		int dd = (secs / 60 / 60 / 24);
		if (dd>0)
		{
			write(dd);
			write("d ");
			if (hh<10) write("0");
			if (hh==0)
			{
				write("0:");
				if (mm<10) write("0");
			}
		}
		if (hh>0)
		{
			write(hh);
			write(":");
			if (mm<10) write("0");
		}
		write(mm);
		write(":");
		if (ss<10) write("0");
		write(ss);
//		write(".");
//		write(ds);
	}
	
	/**
	 * Writes the BODY tag of the page, including the correct CSS classes, according to the user agent.
	 */
	public void writeBodyOpen()
	{
		RequestContext ctx = getContext();
		
		write("<body class=\"");
		Map<String, Float> uaTags = ctx.getUserAgent().getTags();
		for (String t : uaTags.keySet())
		{
			writeEncode(t);
			Float ver = uaTags.get(t);
			if (ver!=0F)
			{
				write(" ");
				writeEncode(t);
				write("_");
				write((int) Math.floor(ver));
			}
			write(" ");
		}
		write("\"");
		write(" uri=\"/");
		writeEncode(ctx.getCommand());
		write("\"");
		write(">");
		
		String overrideScreen = ctx.getCookie(RequestContext.COOKIE_OVERRIDE_SCREEN);
		if (overrideScreen!=null && overrideScreen.equals(ctx.getCookie(RequestContext.COOKIE_SCREEN))==false)
		{
			write("<table width=\"100%\" height=\"100%\"><tr valign=middle><td align=center>");
			write("<div align=left id=overridescreen style=\"width:");
			write(ctx.getUserAgent().getScreenWidth() + 16); // 16 pixels for scrollbars
			write("px !important;height:");
			write(ctx.getUserAgent().getScreenHeight() + 16); // 16 pixels for scrollbars
			write("px !important;\">");
		}
	}
	
	public void writeBodyClose()
	{
		RequestContext ctx = getContext();
		String overrideScreen = ctx.getCookie(RequestContext.COOKIE_OVERRIDE_SCREEN);
		if (overrideScreen!=null && overrideScreen.equals(ctx.getCookie(RequestContext.COOKIE_SCREEN))==false)
		{
			write("</td></tr></table>");
			write("</div>");
		}
		
		write("</body>");
	}

	public void writeYouTubeVideo(String videoID, int width, int height)
	{
		write("<iframe width=\"");
		write(width);
		write("\" height=\"");
		write(height);
		write("\" src=\"http://www.youtube.com/embed/");
		writeEncode(videoID);
		write("?rel=0\" frameborder=\"0\" allowfullscreen=\"0\"></iframe>");
	}
	
	// - - -
	// STRINGS AND URLS
	
	/**
	 * Get a static string from the resource bundle.
	 * @param id The ID of the string as it appears in the .properties file.
	 * @return The string from the ResourceBundle or <code>null</code> if not found.
	 */
	public String getString(String id)
	{
		return StringBundle.getString(getLocale(), getTimeZone(), id, null);
	}

	/**
	 * Returns a <code>Map</code> of all the properties with keys that begin with the
	 * specified prefix. The map contains new keys equivalent to the original key
	 * less the prefix. For example, if a property is defined as "db.color.0.FG1" and
	 * this method is called with "db.color.0." the result is a property named "FG1".
	 * @param idPrefix The prefix to search for and eliminate from the original keys.
	 * @return
	 */
	public Map<String, String> getStrings(String idPrefix)
	{
		return StringBundle.getStrings(getLocale(), idPrefix);
	}

	/**
	 * Get a pattern string from the resource bundle and apply a <code>MessageFormat</code> to it
	 * using the variables.
	 * @param id The ID of the string as it appears in the .properties file.
	 * @param vars The variables to apply to the pattern.
	 * @return The string from the ResourceBundle or <code>null</code> if not found.
	 */
	public String getString(String id, Object... vars)
	{
		return StringBundle.getString(getLocale(), getTimeZone(), id, vars);
	}
		
	public String getResourceURL(String path)
	{
		RequestContext ctx = getContext();
		return relativeUrl( UrlGenerator.getResourceURL(ctx.isSecureSocket(), ctx.getHost(), path) );
	}
	
	public InputStream getResourceAsStream(String path)
	{
		return Controller.getResourceAsStream(UrlGenerator.COMMAND_RESOURCE + "/" + path);
	}

	/**
	 * 
	 * @param image The image object.
	 * @param sizeSpec A constant indicating the desired size. Can be one of the standard sizes <code>Image.SIZE_*</code>,
	 * or a custom size handled by the application in an overridden <code>ImagePage</code>.
	 * @param title
	 * @return
	 */
	public String getImageURL(Image image, String sizeSpec, String title)
	{
		RequestContext ctx = getContext();
		return relativeUrl( UrlGenerator.getImageURL(ctx.isSecureSocket(), ctx.getHost(), image, sizeSpec, title) );
	}

	public String getPageURL(String command)
	{
		return getPageURL(getContext().isSecureSocket(), command, null);
	}

	public String getPageURL(String command, Map<String, String> params)
	{
		return getPageURL(getContext().isSecureSocket(), command, params);
	}
	
	/**
	 * 
	 * @param protocol "http" or "https", or <code>null</code> to use the one used for the current request context.
	 * @param command
	 * @param params
	 * @return
	 */
	public String getPageURL(boolean ssl, String command, Map<String, String> params)
	{
		RequestContext ctx = getContext();
		return relativeUrl( UrlGenerator.getPageURL(ssl, ctx.getHost(), command, params) );
	}
	
	private String baseUrl = null;
	private String relativeUrl(String qualifiedURL)
	{
		RequestContext ctx = getContext();
		
		// Web pages support relative URLs
		if (ctx.getChannel().equals(Channel.WEB))
		{
			if (this.baseUrl==null)
			{
				String base = UrlGenerator.getPageURL(ctx.isSecureSocket(), ctx.getHost(), "", null);
				int p = base.indexOf("://");
				p = base.indexOf("/", p + 3);
				this.baseUrl = base.substring(0, p+1);
			}
			
			if (qualifiedURL.startsWith(this.baseUrl))
			{
				qualifiedURL = qualifiedURL.substring(this.baseUrl.length() - 1);
			}
		}

		return qualifiedURL;		
	}

	// - - -
	// TO BE OVERRIDDEN
	
	/**
	 * To be overridden by the subclass to init this page.
	 * Called before any other method, unless otherwise indicated.
	 */
	public void init() throws Exception
	{
		if (this.child!=null)
		{
			this.child.init();
		}
	}

	/**
	 * To be overridden by the subclass to validate the form posted on this page.
	 * Called when the page is invoked with HTTP method POST, just before {@link #commit()}.
	 * @throws Should throw a {@link WebFormException} for invalid fields. 
	 */
	public void validate() throws Exception
	{
		if (this.child!=null)
		{
			this.child.validate();
		}
	}

	/**
	 * To be overridden by the subclass to commit the form posted on this page.
	 * Called when the page is invoked with HTTP method POST, after {@link #init()} and before {@link #render()}.
	 */
	public void commit() throws Exception
	{
		if (this.child!=null)
		{
			this.child.commit();
		}
	}

	/**
	 * Renders the content of this page by dispatching to the content-type specific render method, such as {@link #renderHTML()}.
	 * Subclasses should generally override the content-type specific render methods, not this one.
	 * @throws Exception
	 */
	public void render() throws Exception
	{
// Delegation is performed inside the individual render methods
//		if (this.child!=null)
//		{
//			this.child.render();
//		}
//		else
		{
			String channel = getContext().getChannel();
			if (channel.equals(Channel.WEB))
			{
//				boolean mobile = getContext().getUserAgent().isSmartPhone();
//				if (mobile)
//				{
//					this.renderHTMLMobile();
//				}
//				else
//				{
					this.renderHTML();
//				}
			}
			else if (channel.equals(Channel.EMAIL) || channel.equals(Channel.FACEBOOK_MESSSAGE))
			{
				this.renderSimpleHTML();
			}
			else if (channel.equals(Channel.SMS) || channel.equals(Channel.TWITTER))
			{
				this.renderShortText();
			}
			else if (channel.equals(Channel.INSTANT_MESSAGE))
			{
				this.renderText();
			}
			else if (channel.equals(Channel.VOICE))
			{				 
				this.renderVoiceXML();
			}
		}
	}

	/**
	 * To be overridden by subclass to return the MIME type of the page.
	 * @return The MIME type. Defaults to "text/html".
	 */
	public String getMimeType() throws Exception
	{
		if (this.child!=null)
		{
			return this.child.getMimeType();
		}
		else
		{
			String channel = getContext().getChannel();
			if (channel.equals(Channel.WEB) || channel.equals(Channel.EMAIL))
			{
				return "text/html";
			}
			else if (channel.equals(Channel.VOICE))
			{
				return "text/x-vxml";
			}
			else
			{
				return "text/plain";
			}
		}
	}
	
	/**
	 * To be overridden by subclass to return the HTTP status code returned upon a successful rendering of the page
	 * (i.e. when no exception is thrown).
	 * @return The response code. Defaults to <code>HttpServletResponse.SC_OK</code> (200).
	 */
	public int getStatusCode() throws Exception
	{
		if (this.child!=null)
		{
			return this.child.getStatusCode();
		}
		else
		{
			return HttpServletResponse.SC_OK;
		}
	}

	/**
	 * To be overridden by the subclass to render the content of the page as HTML for viewing inside a full-blown browser.
	 * The HTML can be complex, and include JavaScript, CSS, CSS includes, etc.
	 * Typically, HTML is rendered for the <code>Channel.WEB</code> channel.
	 */
	public void renderHTML() throws Exception
	{
		if (this.child!=null)
		{
			this.child.renderHTML();
		}
		else
		{
			throw new PageNotFoundException();
		}
	}

//	/**
//	 * To be overridden by the subclass to render the content of the page as HTML for viewing inside a mobile phone browser.
//	 * The HTML can be complex, and include JavaScript, CSS, CSS includes, etc.
//	 * Typically, HTML is rendered for the <code>Channel.WEB</code> channel.
//	 * By default, this method delegates to {@link #renderHTML()} so subclasses should typically override this method only
//	 * when special handling is required.
//	 */
//	public void renderHTMLSmartPhone() throws Exception
//	{
//		if (this.child!=null)
//		{
//			this.child.renderHTMLSmartPhone();
//		}
//		else
//		{
//			this.renderHTML();
//		}
//	}

	/**
	 * To be overridden by the subclass to render the content of the page as simple HTML for embedding inside other platforms.
	 * The HTML must not include a BODY tag, JavaScript, CSS, nor CSS includes.
	 * Typically, simple HTML is rendered for the <code>Channel.EMAIL</code>,
	 * and <code>Channel.FACEBOOK_MESSSAGE</code> channels.
	 */
	public void renderSimpleHTML() throws Exception
	{
		if (this.child!=null)
		{
			this.child.renderSimpleHTML();
		}
		else
		{
			throw new PageNotFoundException();
		}
	}

	/**
	 * To be overridden by the subclass to render the content of the page as plain text, no more than <code>Channel.MAXLEN_SHORT_TEXT</code> characters long.
	 * Typically, short text is rendered for the <code>Channel.SMS</code> and <code>Channel.TWITTER</code> channels.
	 */
	public void renderShortText() throws Exception
	{
		if (this.child!=null)
		{
			this.child.renderShortText();
		}
		else
		{
			throw new PageNotFoundException();
		}
	}

	/**
	 * To be overridden by the subclass to render the content of the page as plain text.
	 * Typically, text is rendered for the <code>Channel.INSTANT_MESSAGE</code> channel.
	 */
	public void renderText() throws Exception
	{
		if (this.child!=null)
		{
			this.child.renderText();
		}
		else
		{
			throw new PageNotFoundException();
		}
	}

	/**
	 * To be overridden by the subclass to render the content of the page as voice XML.
	 * Typically, text is rendered for the <code>Channel.VOICE</code> channel.
	 */
	public void renderVoiceXML() throws Exception
	{
		if (this.child!=null)
		{
			this.child.renderVoiceXML();
		}
		else
		{
			throw new PageNotFoundException();
		}
	}

	/**
	 * To be overridden by subclass to return the title of the page.
	 * @return The title of the page.
	 */
	public String getTitle() throws Exception
	{
		if (this.child!=null)
		{
			return this.child.getTitle();
		}
		else
		{
			return null;
		}
	}

	/**
	 * To be overridden by subclass to return if the page must be accessed via HTTPS.
	 * Always called prior to {@link #init()}.
	 * @return <code>true</code> to mandate SSL. Defaults to <code>false</code>.
	 */
	public boolean isSecureSocket() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isSecureSocket();
		}
		else
		{
			return false;
		}
	}

	/**
	 * To be overridden by subclass to return if the system should protect against cross site scripting (XSS)
	 * by validating the session cookie vs the session parameters.
	 * Should be set to <code>false</code> for forms that are POSTed to externally.
	 * @return <code>true</code> to protect against XSS attacks. Defaults to <code>true</code>.
	 */
	public boolean isProtectXSS() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isProtectXSS();
		}
		else
		{
			return true;
		}
	}

	/**
	 * To be overridden by subclass to return if the user is allowed to view this page.
	 * Always called prior to {@link #init()}.
	 * @return <code>false</code> to redirect the user to the login screen.
	 */
	public boolean isAuthorized() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isAuthorized();
		}
		else
		{
			return true;
		}
	}

	/**
	 * To be overridden by subclass to return if the page should be enveloped.
	 * Always called prior to {@link #init()}.
	 * @return
	 */
	public boolean isEnvelope() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isEnvelope();
		}
		else
		{
			return true;
		}
	}

	/**
	 * To be overridden by subclass to return how this page should be indexed by search engines.
	 * @return A logical "or" of one of more of NO_INDEX | NO_FOLLOW | NO_ARCHIVE | NO_SNIPPET
	 * @throws Exception
	 */
	public int getXRobotFlags() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.getXRobotFlags();
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * To be overridden by subclass to return if this page should be cached on the client.
	 * @return Defaults to <code>false</code>.
	 */
	public boolean isCacheable() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isCacheable();
		}
		else
		{
			return false;
		}
	}

	/**
	 * To be overridden by subclass to return if the viewing of the page should be logged.
	 * @return Defaults to <code>true</code>.
	 */
	public boolean isLog() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isLog();
		}
		else
		{
			return true;
		}
	}

	/**
	 * To be overridden by subclass to return if the page supports POST actions.
	 * @return Defaults to <code>true</code> for non-push channels.
	 * Should be set to <code>true</code> by push notifications that want to receive a response from the user. 
	 */
	public boolean isActionable() throws Exception
	{
		if (this.delegate!=null)
		{
			return this.delegate.isActionable();
		}
		else
		{
			return true;
		}
	}

	/**
	 * To be overridden by the subclass to indicate the command part of the URL that will invoke this page.
	 */
	public static String COMMAND = null;
	
	// - - -	
}
