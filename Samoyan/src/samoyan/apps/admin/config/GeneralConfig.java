package samoyan.apps.admin.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.LocaleTypeAhead;
import samoyan.apps.system.TimeZoneTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.LocaleEx;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class GeneralConfig extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/general-config";
	
	@Override
	public void validate() throws Exception
	{
		// Time zone
		String tzStr = getParameterTypeAhead("tz").getKey();
		TimeZone tz = TimeZone.getTimeZone(tzStr);
		if (tz.getID().equals(tzStr)==false)
		{
			throw new WebFormException("tz", getString("common:Errors.InvalidValue"));
		}
		
		// Locales
		int count = getParameterInteger("locales");
		for (int i=0; i<count; i++)
		{
			if (isParameterNotEmpty("loc_" + i))
			{
				String locStr = getParameterTypeAhead("loc_" + i).getKey();
				if (LocaleEx.fromString(locStr)==null)
				{
					throw new WebFormException("loc_" + i, getString("common:Errors.InvalidValue"));
				}
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		Server fed = ServerStore.getInstance().openFederation(); 
		
		fed.setTimeZone(TimeZone.getTimeZone(getParameterTypeAhead("tz").getKey()));
		
		List<Locale> locales = new ArrayList<Locale>();
		int count = getParameterInteger("locales");
		for (int i=0; i<count; i++)
		{
			if (isParameterNotEmpty("loc_" + i))
			{
				Locale loc = LocaleEx.fromString(getParameterTypeAhead("loc_" + i).getKey());
				if (loc!=null && !locales.contains(loc))
				{
					locales.add(loc);
				}
			}
		}
		fed.setLocales(locales);
		
		fed.setOpenRegistration(getParameterString("open").equals("1"));

		ServerStore.getInstance().save(fed);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation(); 
		TimeZone tz = fed.getTimeZone();
		List<Locale> locales = fed.getLocales();
		
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);

		// Time zone
		twoCol.writeRow(getString("admin:GeneralConfig.TimeZone"));
		twoCol.writeTypeAheadInput("tz", tz.getID(), TimeZoneEx.getDisplayString(tz, getLocale()), 40, 64, getPageURL(TimeZoneTypeAhead.COMMAND));
		
		twoCol.writeSpaceRow();
		
		// Locales
		twoCol.writeRow(getString("admin:GeneralConfig.Locales"));
		new ControlArray<Locale>(twoCol, "locales", locales)
		{
			@Override
			public void renderRow(int rowNum, Locale loc) throws Exception
			{
				writeTypeAheadInput("loc_" + rowNum, loc==null? null : loc.toString(), loc==null? null : loc.getDisplayName(getLocale()), 40, 64, getPageURL(LocaleTypeAhead.COMMAND));
			}
		}.render();
	
		twoCol.writeSpaceRow();

		// On-boarding
		twoCol.writeRow(getString("admin:GeneralConfig.OnBoarding"));
		twoCol.writeRadioButton("open", getString("admin:GeneralConfig.OpenToPublic"), "1", fed.isOpenRegistration()?"1":"0");
		twoCol.write("<br>");
		twoCol.writeRadioButton("open", getString("admin:GeneralConfig.ByInvitationOnly"), "0", fed.isOpenRegistration()?"1":"0");
		
		twoCol.render();

		write("<br>");
		writeSaveButton(fed);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:GeneralConfig.Title");
	}
}
