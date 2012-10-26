package samoyan.apps.admin.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.CountryTypeAhead;
import samoyan.apps.system.LocaleTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.LocaleEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Country;
import samoyan.database.CountryStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class VoiceConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/voice-config";
	
	@Override
	public void validate() throws Exception
	{
		int min = isParameter("active")? 1:0;	
		
		validateParameterString("region", min, -1);
		validateParameterString("token", min, -1);
		
		if (isParameterNotEmpty("callerid"))
		{
			validateParameterPhone("callerid");
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
		
		// Countries
		count = getParameterInteger("countries");
		for (int i=0; i<count; i++)
		{
			if (isParameterNotEmpty("cc_" + i) && Util.isEmpty(getParameterTypeAhead("cc_" + i).getKey()))
			{
				throw new WebFormException("cc_" + i, getString("common:Errors.InvalidValue"));
			}
		}
	}

	@Override
	public void commit() throws Exception
	{
		Server fed = ServerStore.getInstance().openFederation();

		fed.setVoxeoRegion(getParameterString("region"));	
		fed.setVoxeoDialingToken(getParameterString("token"));
		fed.setVoxeoCallerID(getParameterPhone("callerid"));
		fed.setVoxeoActive(isParameter("active"));

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
		fed.setVoxeoLocales(locales);

		// Countries
		List<String> countries = new ArrayList<String>();
		count = getParameterInteger("countries");
		for (int i=0; i<count; i++)
		{
			if (isParameterNotEmpty("cc_" + i))
			{
				countries.add(getParameterTypeAhead("cc_" + i).getKey());
			}
		}
		fed.setVoiceCountries(countries);

		ServerStore.getInstance().save(fed);

		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation(); 
		
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);			
		
		twoCol.writeRow(getString("admin:VoiceConfig.Status"));
		twoCol.writeCheckbox("active", getString("admin:VoiceConfig.Active"), fed.isVoxeoActive());
			
		twoCol.writeRow(getString("admin:VoiceConfig.Region"));
		twoCol.writeRadioButton("region", getString("admin:VoiceConfig.US"), "US", fed.getVoxeoRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("region", getString("admin:VoiceConfig.EU"), "EU", fed.getVoxeoRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("region", getString("admin:VoiceConfig.APAC"), "APAC", fed.getVoxeoRegion());
		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("admin:VoiceConfig.Token"));
		twoCol.writeTextInput("token", fed.getVoxeoDialingToken(), 60, -1);

		twoCol.writeRow(getString("admin:VoiceConfig.CallerID"));
		twoCol.writePhoneInput("callerid", fed.getVoxeoCallerID());

		twoCol.writeSpaceRow();

		// Locales
		twoCol.writeRow(getString("admin:VoiceConfig.Locales"));
		new ControlArray<Locale>(twoCol, "locales", fed.getVoxeoLocales())
		{
			@Override
			public void renderRow(int rowNum, Locale loc) throws Exception
			{
				writeTypeAheadInput("loc_" + rowNum, loc==null? null : loc.toString(), loc==null? null : loc.getDisplayName(getLocale()), 40, 64, getPageURL(LocaleTypeAhead.COMMAND));
			}
		}.render();

		// Countries
		twoCol.writeRow(getString("admin:VoiceConfig.Countries"));
		new ControlArray<String>(twoCol, "countries", fed.getVoiceCountries())
		{
			@Override
			public void renderRow(int rowNum, String iso2) throws Exception
			{
				Country country = null;
				if (iso2!=null)
				{
					country = CountryStore.getInstance().loadByCodeISO2(iso2);
				}
				writeTypeAheadInput("cc_" + rowNum, country==null? null : country.getCodeISO2(), country==null? null : country.getName(getLocale()), 40, Country.MAXSIZE_NAME, getPageURL(CountryTypeAhead.COMMAND));
			}
		}.render();

		twoCol.render();
		
		write("<br>");
		writeSaveButton(fed);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:VoiceConfig.Title");
	}
}
