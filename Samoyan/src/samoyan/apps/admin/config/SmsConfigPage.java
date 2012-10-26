package samoyan.apps.admin.config;

import java.util.ArrayList;
import java.util.List;
import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.CountryTypeAhead;
import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Country;
import samoyan.database.CountryStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class SmsConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/sms-config";
		
	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		int actives = 0;
		
		// General
		int count = getParameterInteger("countries");
		for (int i=0; i<count; i++)
		{
			if (isParameterNotEmpty("cc_" + i) && Util.isEmpty(getParameterTypeAhead("cc_" + i).getKey()))
			{
				throw new WebFormException("cc_" + i, getString("common:Errors.InvalidValue"));
			}
		}

		// Validate OpenMarket
		boolean omStatus = isParameter("om.status");
		actives += (omStatus?1:0);
		
		String omUser = validateParameterString("om.user", omStatus?1:0, Server.MAXSIZE_USER);
		if (omUser.matches("[0-9\\x2d]*")==false) // numbers, hyphen
		{
			throw new WebFormException("om.user", getString("common:Errors.InvalidValue"));
		}
		validateParameterString("om.pw", omStatus?1:0, Server.MAXSIZE_PASSWORD);

		String omSender = validateParameterString("om.sender", omStatus?1:0, Server.MAXSIZE_SENDER_ID);
		if (omSender.matches("[0-9]*")==false)
		{
			throw new WebFormException("om.sender", getString("common:Errors.InvalidValue"));
		}

		String omProgram = validateParameterString("om.program", omStatus?1:0, 8);
		if (omProgram.matches("[0-9]*")==false)
		{
			throw new WebFormException("om.program", getString("common:Errors.InvalidValue"));
		}
		
		// Validate Clickatell
		boolean clickStatus = isParameter("click.status");
		actives += (clickStatus?1:0);

		validateParameterString("click.user", clickStatus?1:0, Server.MAXSIZE_USER);
		validateParameterString("click.pw", clickStatus?1:0, Server.MAXSIZE_PASSWORD);

		String clickSender = validateParameterString("click.sender", clickStatus?1:0, Server.MAXSIZE_SENDER_ID);
		if (clickSender.matches("[0-9]*")==false)
		{
			throw new WebFormException("click.sender", getString("common:Errors.InvalidValue"));
		}

		String clickAPI = validateParameterString("click.api", clickStatus?1:0, 8);
		if (clickAPI.matches("[0-9]*")==false)
		{
			throw new WebFormException("click.api", getString("common:Errors.InvalidValue"));
		}
		
		// Validate BulkSMS
		boolean bulkActive = isParameter("bulk.status");
		actives += (bulkActive?1:0);
		
		validateParameterString("bulk.region", bulkActive?2:0, 2);
		validateParameterString("bulk.user", bulkActive?1:0, Server.MAXSIZE_USER);
		validateParameterString("bulk.pw", bulkActive?1:0, Server.MAXSIZE_PASSWORD);

		// Validate only one provider is enabled
		if (actives>1)
		{
			throw new WebFormException(new String[] {"om.status", "click.status", "bulk.status"}, getString("admin:SmsConfig.OneProviderError"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		// Commit
		Server fed = ServerStore.getInstance().openFederation(); 

		// General
		List<String> countries = new ArrayList<String>();
		int count = getParameterInteger("countries");
		for (int i=0; i<count; i++)
		{
			if (isParameterNotEmpty("cc_" + i))
			{
				countries.add(getParameterTypeAhead("cc_" + i).getKey());
			}
		}
		fed.setSMSCountries(countries);

		// OpenMarket
		fed.setOpenMarketActive(isParameter("om.status"));
		fed.setOpenMarketUser(getParameterString("om.user"));
		fed.setOpenMarketPassword(getParameterString("om.pw"));
		fed.setOpenMarketSenderID(getParameterString("om.sender"));
		fed.setOpenMarketProgramID(getParameterString("om.program"));
		fed.setOpenMarketDemoPrefix(getParameterString("om.demo"));
		
		// Clickatell
		fed.setClickatellActive(isParameter("click.status"));
		fed.setClickatellUser(getParameterString("click.user"));
		fed.setClickatellPassword(getParameterString("click.pw"));
		fed.setClickatellSenderID(getParameterString("click.sender"));
		fed.setClickatellAPIID(getParameterString("click.api"));

		// BulkSMS
		fed.setBulkSMSActive(isParameter("bulk.status"));
		fed.setBulkSMSRegion(getParameterString("bulk.region"));
		fed.setBulkSMSUser(getParameterString("bulk.user"));
		fed.setBulkSMSPassword(getParameterString("bulk.pw"));

		// Email gateways
		fed.setUseSMSEmailGateways(isParameter("gateway"));
		
		ServerStore.getInstance().save(fed);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation(); 

		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeSubtitleRow(getString("admin:SmsConfig.General"));

		twoCol.writeRow(getString("admin:SmsConfig.Countries"));
		new ControlArray<String>(twoCol, "countries", fed.getSMSCountries())
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
		
		twoCol.writeRow(getString("admin:SmsConfig.EmailToSMS"));
		twoCol.writeCheckbox("gateway", getString("admin:SmsConfig.UseEmailGateway"), fed.isUseEmailGatewaysForSMS());

		
		twoCol.writeSubtitleRow(getString("admin:SmsConfig.OpenMarket"));
		
		twoCol.writeRow(getString("admin:SmsConfig.Status"));
		twoCol.writeCheckbox("om.status", getString("admin:SmsConfig.Active"), fed.isOpenMarketActive());
		twoCol.writeRow(getString("admin:SmsConfig.User"));
		twoCol.writeTextInput("om.user", fed.getOpenMarketUser(), 20, Server.MAXSIZE_USER);
		twoCol.writeRow(getString("admin:SmsConfig.Password"));
		twoCol.writePasswordInput("om.pw", fed.getOpenMarketPassword(), 20, Server.MAXSIZE_PASSWORD);
		twoCol.writeRow(getString("admin:SmsConfig.SenderID"));
		twoCol.writeTextInput("om.sender", fed.getOpenMarketSenderID(), 12, Server.MAXSIZE_SENDER_ID);
		twoCol.writeRow(getString("admin:SmsConfig.ProgramID"));
		twoCol.writeTextInput("om.program", fed.getOpenMarketProgramID(), 8, 8);
		twoCol.writeRow(getString("admin:SmsConfig.Demo"));
		twoCol.writeTextInput("om.demo", fed.getOpenMarketDemoPrefix(), 8, 8);
				
		
		twoCol.writeSubtitleRow(getString("admin:SmsConfig.Clickatell"));
		
		twoCol.writeRow(getString("admin:SmsConfig.Status"));
		twoCol.writeCheckbox("click.status", getString("admin:SmsConfig.Active"), fed.isClickatellActive());
		twoCol.writeRow(getString("admin:SmsConfig.User"));
		twoCol.writeTextInput("click.user", fed.getClickatellUser(), 20, Server.MAXSIZE_USER);
		twoCol.writeRow(getString("admin:SmsConfig.Password"));
		twoCol.writePasswordInput("click.pw", fed.getClickatellPassword(), 20, Server.MAXSIZE_PASSWORD);
		twoCol.writeRow(getString("admin:SmsConfig.SenderID"));
		twoCol.writeTextInput("click.sender", fed.getClickatellSenderID(), 12, Server.MAXSIZE_SENDER_ID);
		twoCol.writeRow(getString("admin:SmsConfig.APIID"));
		twoCol.writeTextInput("click.api", fed.getClickatellAPIID(), 8, 8);

		
		twoCol.writeSubtitleRow(getString("admin:SmsConfig.BulkSMS"));
		
		twoCol.writeRow(getString("admin:SmsConfig.Status"));
		twoCol.writeCheckbox("bulk.status", getString("admin:SmsConfig.Active"), fed.isBulkSMSActive());
		
		twoCol.writeRow(getString("admin:SmsConfig.Region"));
		twoCol.writeRadioButton("bulk.region", getString("admin:SmsConfig.US"), Country.UNITED_STATES, fed.getBulkSMSRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("bulk.region", getString("admin:SmsConfig.UK"), Country.UNITED_KINGDOM, fed.getBulkSMSRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("bulk.region", getString("admin:SmsConfig.Germany"), Country.GERMANY, fed.getBulkSMSRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("bulk.region", getString("admin:SmsConfig.Spain"), Country.SPAIN, fed.getBulkSMSRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("bulk.region", getString("admin:SmsConfig.SouthAfrica"), Country.SOUTH_AFRICA, fed.getBulkSMSRegion());
		twoCol.write(" ");
		twoCol.writeRadioButton("bulk.region", getString("admin:SmsConfig.International"), "XX", fed.getBulkSMSRegion());

		twoCol.writeRow(getString("admin:SmsConfig.User"));
		twoCol.writeTextInput("bulk.user", fed.getBulkSMSUser(), 20, Server.MAXSIZE_USER);
		twoCol.writeRow(getString("admin:SmsConfig.Password"));
		twoCol.writePasswordInput("bulk.pw", fed.getBulkSMSPassword(), 20, Server.MAXSIZE_PASSWORD);

		twoCol.render();

		write("<br>");
				
		writeSaveButton(fed);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:SmsConfig.Title");
	}
}
