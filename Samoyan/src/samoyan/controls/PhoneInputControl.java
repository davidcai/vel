package samoyan.controls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.Country;
import samoyan.database.CountryStore;
import samoyan.servlet.WebPage;

public class PhoneInputControl extends TextInputControl
{
	private Set<String> limitedCountries = null;
	
	public PhoneInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "tel");
	}
	
	/**
	 * Limits this phone control to the given country codes.
	 * If the control was already limited, then the given countries are added to the allowed countries.
	 * @param countriesIso2 A collection of the countries' ISO2 code, e.g. "US" for United States.
	 */
	public PhoneInputControl limitCountries(Collection<String> countriesIso2)
	{
		if (countriesIso2==null)
		{
			return this;
		}
		else if (this.limitedCountries==null)
		{
			this.limitedCountries = new HashSet<String>(countriesIso2);
		}
		else
		{
			this.limitedCountries.addAll(countriesIso2);
		}
		return this;
	}
	
	/**
	 * Limits this phone control to the given country.
	 * If the control was already limited, then the given country is added to the allowed countries.
	 * @param countryIso2 An ISO2 country code, e.g. "US" for United States.
	 */
	public PhoneInputControl limitCountry(String countryIso2)
	{
		if (countryIso2==null)
		{
			return this;
		}
		else if (this.limitedCountries==null)
		{
			this.limitedCountries = new HashSet<String>(1);
			this.limitedCountries.add(countryIso2);
		}
		else
		{
			this.limitedCountries.add(countryIso2);
		}
		return this;
	}
	
	@Override
	public String getCurrentValue()
	{
		WebPage out = getOutputPage();
		
		String local = out.getContext().getParameter(this.getName());
		String prefix = out.getContext().getParameter("_prefix_" + this.getName());
		String value = null;
		if (local!=null && prefix!=null)
		{
			value = prefix + local;
		}
		
		Object initialValue = this.getInitialValue();
		if (value==null && initialValue!=null)
		{
			value = initialValue.toString();
		}
		return value;
	}

	@Override
	public void render()
	{
		try
		{
			String countryCode = null;
			String prefix = null;
			String localNumber = null;
			String v = getCurrentValue();
			if (v!=null)
			{
				int slash = v.indexOf("/");
				if (slash>=0)
				{
					countryCode = v.substring(0, slash);
					prefix = CountryStore.getInstance().loadByCodeISO2(countryCode).getPhonePrefix();
					localNumber = v.substring(slash + 1 + prefix.length());
				}
				else if (v.startsWith("1"))
				{
					// Backward compatibility for US numbers
					countryCode = "US";
					prefix = "1";
					localNumber = v.substring(1);
				}
				else
				{
					v = null;
				}
			}
	
			write("<table class=PhoneInput><tr><td>");
						
				// Populate countries combo
				List<Country> countries = getCountries(countryCode);
				
				SelectInputControl select = new SelectInputControl(this.getOutputPage(), "_prefix_" + this.getName());
				if (countries.size()!=1)
				{
					select.addOption("", "");
				}
				for (Country country : countries)
				{
					select.addOption(country.getCodeISO3() + " (+" + country.getPhonePrefix() + ")", country.getCodeISO2() + "/" + country.getPhonePrefix());
				}
				select.setInitialValue(countryCode + "/" + prefix);
				select.render();
			
			write("</td><td>");
			
				setAttribute("value", localNumber);
				setSize(12);
				setMaxLength(20);
				writeTag("input");
			
			write("</td></tr></table>");
		}
		catch (Exception e)
		{
			// Shouldn't happen
			Debug.logStackTrace(e);
		}
	}
	
	private static List<Country> allCountries = null; 
	private List<Country> getCountries(String currentCountryCode) throws Exception
	{
		if (this.limitedCountries==null)
		{
			// Return list of all countries
			if (allCountries==null)
			{
				List<UUID> countryIDs = CountryStore.getInstance().queryAll(); // !$! Need to sort, allow to limit
				List<Country> countries = new ArrayList<Country>(countryIDs.size());
				for (UUID id : countryIDs)
				{
					Country country = CountryStore.getInstance().load(id);
					if (country!=null && !Util.isEmpty(country.getPhonePrefix()) && !Util.isEmpty(country.getCodeISO2()))
					{
						countries.add(country);
					}
				}
				
				Collections.sort(countries, new Country.SortByPhonePrefix());
				allCountries = countries;
			}
			return allCountries;
		}
		else
		{
			// Return limited list, making sure currentCountryCode is included
			List<Country> countries = new ArrayList<Country>(this.limitedCountries.size()+1);
			for (String iso2 : this.limitedCountries)
			{
				Country country = CountryStore.getInstance().loadByCodeISO2(iso2);
				if (country!=null && !Util.isEmpty(country.getPhonePrefix()) && !Util.isEmpty(country.getCodeISO2()))
				{
					countries.add(country);
				}
			}
			
			if (!Util.isEmpty(currentCountryCode) && this.limitedCountries.contains(currentCountryCode)==false)
			{
				Country country = CountryStore.getInstance().loadByCodeISO2(currentCountryCode);
				if (country!=null && !Util.isEmpty(country.getPhonePrefix()) && !Util.isEmpty(country.getCodeISO2()))
				{
					countries.add(country);
				}
			}
			
			Collections.sort(countries, new Country.SortByPhonePrefix());
			return countries;
		}
	}
}
