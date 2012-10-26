package samoyan.apps.profile;

import java.util.TimeZone;

import samoyan.apps.profile.ProfilePage;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class TimeZonePage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/time-zone"; 
	
	/**
	 * To be overridden by subclasses to return the list of time zones to show the user.
	 * Defaults to North American time zones only.
	 * @return
	 */
	protected String[] getTimeZones()
	{
		return TimeZoneEx.getNorthAmericaIDs();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:TimeZone.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		TimeZone userTz = user.getTimeZone();
		TimeZone ctxTz = getContext().getTimeZone();
		
		writeFormOpen();
		
		write("<table>");
		boolean foundCurrent = false;
		boolean foundContext = false;
		String[] tzIDs = this.getTimeZones(); // Call subclass
		for (String tzID : tzIDs)
		{
			TimeZone tz = TimeZone.getTimeZone(tzID);
			write("<tr><td>");
			writeRadioButton("tz", TimeZoneEx.getDisplayString(tz, getLocale()), tzID, userTz.getID());
			write("</td></tr>");
			
			if (userTz.getID().equalsIgnoreCase(tzID))
			{
				foundCurrent = true;
			}
			if (ctxTz.getID().equalsIgnoreCase(tzID))
			{
				foundContext = true;
			}
		}
		
		if (!foundCurrent)
		{
			write("<tr><td>");
			writeRadioButton("tz", TimeZoneEx.getDisplayString(userTz, getLocale()), userTz.getID(), userTz.getID());
			write("</td></tr>");
		}
		if (!foundContext)
		{
			write("<tr><td>");
			writeRadioButton("tz", TimeZoneEx.getDisplayString(ctxTz, getLocale()), ctxTz.getID(), userTz.getID());
			write("</td></tr>");
		}
		write("</table><br>");
		
		writeSaveButton(user);
		writeFormClose();
	}
		
	@Override
	public void validate() throws Exception
	{
		String tzID = getParameterString("tz");
		if (Util.isEmpty(tzID))
		{
			throw new WebFormException("tz", getString("common:Errors.MissingField"));
		}
		TimeZone tz = TimeZone.getTimeZone(tzID);
		if (tz.getID().equals(tzID)==false)
		{
			throw new WebFormException("tz", getString("common:Errors.InvalidValue"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		User user = UserStore.getInstance().open(getContext().getUserID());
		user.setTimeZone(TimeZone.getTimeZone(getParameterString("tz")));
		UserStore.getInstance().save(user);
		
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}
}
