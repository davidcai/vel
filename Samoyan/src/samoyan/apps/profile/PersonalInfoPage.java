package samoyan.apps.profile;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import samoyan.apps.system.TimeZoneTypeAhead;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.LocaleEx;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class PersonalInfoPage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/personal-info";

	protected final static String PARAM_BIRTHDAY = "x_birthday"; 
	protected final static String PARAM_TIME_ZONE = "x_tz"; 
	protected final static String PARAM_LOCALE = "x_locale"; 
	protected final static String PARAM_AVATAR = "x_avatar"; 
	protected final static String PARAM_NAME = "x_name"; 
	protected final static String PARAM_GENDER = "x_gender"; 
	
	@Override
	public final void validate() throws Exception
	{
		int disp;
		
		// Name
		disp = getFieldRequired(PARAM_NAME);
		if (disp>=0)
		{
			validateParameterString(PARAM_NAME, disp>0? User.MINSIZE_NAME : 0, User.MAXSIZE_NAME);
		}
		
		// Gender
		disp = getFieldRequired(PARAM_GENDER);
		if (disp>=0)
		{
			validateParameterString(PARAM_GENDER, User.GENDER_FEMALE.length(), User.GENDER_FEMALE.length());
		}

		// Birthday
		disp = getFieldRequired(PARAM_BIRTHDAY);
		if (disp>=0)
		{
			if (disp>0 || isParameterNotEmpty(PARAM_BIRTHDAY))
			{
				Calendar cal = Calendar.getInstance(getTimeZone());
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				cal.add(Calendar.YEAR, -13);
				Date maxDate = cal.getTime();
				cal.add(Calendar.YEAR, -107);
				Date minDate = cal.getTime();
				Date bday = validateParameterDate(PARAM_BIRTHDAY, minDate, maxDate);
			}
		}
		
		// Time zone
		disp = getFieldRequired(PARAM_TIME_ZONE);
		if (disp>=0)
		{
			if (disp>0 || isParameterNotEmpty(PARAM_TIME_ZONE))
			{
				String timeZone = getParameterTypeAhead(PARAM_TIME_ZONE).getKey();
				TimeZone tz = TimeZone.getTimeZone(timeZone);
				if (tz==null)
				{
					throw new WebFormException(PARAM_TIME_ZONE, getString("common:Errors.InvalidValue"));
				}
			}
		}
		
		// Locale
		disp = getFieldRequired(PARAM_LOCALE);
		if (disp>=0 && isParameter(PARAM_LOCALE))
		{
			if (disp>0 || isParameterNotEmpty(PARAM_LOCALE))
			{
				List<Locale> locales = ServerStore.getInstance().loadFederation().getLocales();
				Locale loc = LocaleEx.fromString(getParameterString(PARAM_LOCALE));
				if (loc==null || locales.contains(loc)==false)
				{
					throw new WebFormException(PARAM_LOCALE, getString("common:Errors.InvalidValue"));
				}
			}
		}
		
		// Call subclass
		this.validateExtra();
	}
	
	@Override
	public final void commit() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().open(ctx.getUserID());
		
		if (getFieldRequired(PARAM_GENDER)>=0)
		{
			String gender = getParameterString(PARAM_GENDER);
			if (!gender.equals(User.GENDER_FEMALE) && !gender.equals(User.GENDER_MALE))
			{
				gender = null;
			}
			user.setGender(gender);
		}
		
		if (getFieldRequired(PARAM_BIRTHDAY)>=0)
		{
			user.setBirthday(getParameterDate(PARAM_BIRTHDAY)); 
		}
		
		if (getFieldRequired(PARAM_NAME)>=0)
		{
			user.setName(getParameterString(PARAM_NAME));
		}
		
		if (getFieldRequired(PARAM_TIME_ZONE)>=0)
		{
			Pair<String, String> kvp = getParameterTypeAhead(PARAM_TIME_ZONE);
			TimeZone tz;
			if (!Util.isEmpty(kvp.getKey()))
			{
				tz = TimeZone.getTimeZone(kvp.getKey());
			}
			else
			{
				tz = getContext().getTimeZone();
			}
			user.setTimeZone(tz);
		}
		
		if (getFieldRequired(PARAM_LOCALE)>=0 && isParameter(PARAM_LOCALE))
		{
			user.setLocale(LocaleEx.fromString(getParameterString(PARAM_LOCALE)));
		}
		
		if (getFieldRequired(PARAM_AVATAR)>=0)
		{
			Image avatar = getParameterImage(PARAM_AVATAR);
			user.setAvatar(avatar);
		}
		
		UserStore.getInstance().save(user);

		// Call subclass
		this.commitExtra();

		// Support for guided setup
		progressGuidedSetup();
		
		// Redirect to self in order to clear form submission
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, "")); // getString("profile:GeneralInfo.Confirmation")));
	}

	@Override
	public final String getTitle() throws Exception
	{
		return getString("profile:GeneralInfo.Title");
	}

	@Override
	public final void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		Server fed = ServerStore.getInstance().loadFederation();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Help
		twoCol.writeTextRow(getString("profile:GeneralInfo.Help"));
		
		twoCol.writeSpaceRow();

		// Name
		if (getFieldRequired(PARAM_NAME)>=0)
		{
			twoCol.writeRow(getFieldLabel(PARAM_NAME));
			new TextInputControl(twoCol, PARAM_NAME)
				.setMaxLength(User.MAXSIZE_NAME)
				.setSize(40)
//				.setRequired(true)
				.setInitialValue(user.getName())
				.render();
			// twoCol.writeTextInput(PARAM_NAME, user.getName(), 40, User.MAXSIZE_NAME);
		}
		
		// Gender
		if (getFieldRequired(PARAM_GENDER)>=0)
		{
			String gender = user.getGender();
			if (gender==null) gender = "*";
			twoCol.writeRow(getFieldLabel(PARAM_GENDER));
			twoCol.writeRadioButton(PARAM_GENDER, getString("profile:GeneralInfo.Male"), User.GENDER_MALE, gender);
			twoCol.write(" ");
			twoCol.writeRadioButton(PARAM_GENDER, getString("profile:GeneralInfo.Female"), User.GENDER_FEMALE, gender);
			
			if (getFieldRequired(PARAM_GENDER)==0)
			{
				twoCol.write(" ");
				twoCol.writeRadioButton(PARAM_GENDER, getString("profile:GeneralInfo.Unspecified"), "*", gender);
			}
		}
		
		// Birthday
		if (getFieldRequired(PARAM_BIRTHDAY)>=0)
		{
			twoCol.writeRow(getFieldLabel(PARAM_BIRTHDAY));
			twoCol.writeDateInput(PARAM_BIRTHDAY, user.getBirthday());
		}
		
		// Avatar
		if (getFieldRequired(PARAM_AVATAR)>=0)
		{
			twoCol.writeSpaceRow();
	
			twoCol.writeRow(getFieldLabel(PARAM_AVATAR), getString("profile:GeneralInfo.AvatarHelp", Setup.getAppTitle(getLocale())));
			twoCol.writeImageInput(PARAM_AVATAR, user.getAvatar());
			
			twoCol.writeSpaceRow();
		}
		
		// Time zone
		if (getFieldRequired(PARAM_TIME_ZONE)>=0)
		{
			TimeZone tz = user.getTimeZone();
			if (tz==null)
			{
				tz = fed.getTimeZone();
			}
			twoCol.writeRow(getFieldLabel(PARAM_TIME_ZONE));
			twoCol.writeTypeAheadInput(PARAM_TIME_ZONE, tz.getID(), TimeZoneEx.getDisplayString(tz, getLocale()), 40, 64, getPageURL(TimeZoneTypeAhead.COMMAND));
		}
		
		// Locales
		if (getFieldRequired(PARAM_LOCALE)>=0)
		{
			List<Locale> locales = fed.getLocales();
			if (locales.size()>1)
			{
				twoCol.writeRow(getFieldLabel(PARAM_LOCALE));
				SelectInputControl select = new SelectInputControl(twoCol, PARAM_LOCALE);
				for (Locale loc : locales)
				{
					select.addOption(loc.getDisplayName(getLocale()), loc.toString());
				}
				if (user.getLocale()!=null)
				{
					select.setInitialValue(user.getLocale().toString());
				}
				select.render();
			}
		}
		
		// Call subclass
		this.renderExtra(twoCol);

		twoCol.render();

		write("<br>");	
		writeSaveButton(user);
		
		// Call subclass
		this.renderExtraFooter();
		
		writeFormClose();
		
//		// Neat caps script
//		write("<script type=\"text/javascript\">");
//		write("$('INPUT[name=x_name]').blur(neatCapsInput);");
//		write("</script>");
	}

	// - - -
	
	/**
	 * To be overridden by subclasses to validate any extra fields.
	 * @throws Exception
	 */
	protected void validateExtra() throws Exception
	{
	}

	/**
	 * To be overridden by subclasses to commit any extra fields.
	 * @throws Exception
	 */
	protected void commitExtra() throws Exception
	{
	}

	/**
	 * To be overridden by subclasses to render any extra fields.
	 * @param twoCol Subclass must write into this <code>TwoColFormControl</code>.
	 * @throws Exception
	 */
	protected void renderExtra(TwoColFormControl twoCol) throws Exception
	{
	}
	
	/**
	 * To be overridden by subclasses to render any extra content after the Save button.
	 * @throws Exception
	 */
	protected void renderExtraFooter() throws Exception
	{
	}

	/**
	 * To be overridden by subclasses to indicate how to render the standard fields.
	 * A return value of <code>-1</code> indicates not to show the field;
	 * A return value of <code>0</code> indicates to show the field and make it optional;
	 * A return value of <code>1</code> indicates to show the field and make it mandatory.
	 * @param name
	 * @return
	 */
	protected int getFieldRequired(String name)
	{
		if (name.equalsIgnoreCase(PARAM_NAME))
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}

	protected String getFieldLabel(String name)
	{
		if (name.equalsIgnoreCase(PARAM_AVATAR))
		{
			return getString("profile:GeneralInfo.Avatar");
		}
		else if (name.equalsIgnoreCase(PARAM_BIRTHDAY))
		{
			return getString("profile:GeneralInfo.Birthday");
		}
		else if (name.equalsIgnoreCase(PARAM_GENDER))
		{
			return getString("profile:GeneralInfo.Gender");
		}
		else if (name.equalsIgnoreCase(PARAM_LOCALE))
		{
			return getString("profile:GeneralInfo.Locale");
		}
		else if (name.equalsIgnoreCase(PARAM_NAME))
		{
			return getString("profile:GeneralInfo.Name");
		}
		else if (name.equalsIgnoreCase(PARAM_TIME_ZONE))
		{
			return getString("profile:GeneralInfo.TimeZone");
		}
		else
		{
			return null;
		}
	}
}
