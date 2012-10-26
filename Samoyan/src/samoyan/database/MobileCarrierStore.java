package samoyan.database;

import java.util.List;
import java.util.UUID;

/**
 * @see http://en.wikipedia.org/wiki/List_of_SMS_gateways
 * @see http://www.mutube.com/projects/open-email-to-sms/gateway-list/
 * @author brian
 */
public final class MobileCarrierStore extends CsvDataBeanStore<MobileCarrier>
{
	private static MobileCarrierStore instance = new MobileCarrierStore();

	protected MobileCarrierStore()
	{
	}
	public final static MobileCarrierStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<MobileCarrier> getBeanClass()
	{
		return MobileCarrier.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("MobileCarriers", this);
		
		td.defineCol("Name", String.class).invariant();
		td.defineCol("SMSEmail", String.class).invariant();
		td.defineCol("MMSEmail", String.class).invariant();
		td.defineCol("CountryCode", String.class).invariant();
		td.defineCol("Minor", Boolean.class).invariant();
		
		return td;
	}

	// - - -
	
	public List<UUID> queryByCountryCode(String countryCode) throws Exception
	{
		return getInstance().queryListUUIDByColumn("CountryCode", countryCode);
	}
}
