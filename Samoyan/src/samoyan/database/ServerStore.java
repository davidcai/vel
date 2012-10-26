package samoyan.database;

import java.util.BitSet;
import java.util.TimeZone;

import samoyan.core.Util;
import samoyan.servlet.Channel;

public final class ServerStore extends DataBeanStore<Server>
{
	private static ServerStore instance = new ServerStore();

	protected ServerStore()
	{
	}
	public final static ServerStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Server> getBeanClass()
	{
		return Server.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("Servers", this);
		
		td.defineCol("Name", String.class).size(0, Server.MAXSIZE_NAME).invariant();
		
		// Locales + TimeZone
		td.defineProp("TimeZone", TimeZone.class);
		td.defineProp("Locales", String.class);
		
		// Upgrade version
		td.defineProp("PlatformUpgradeVersion", String.class);
		td.defineProp("AppUpgradeVersion", String.class);

		// SMTP + IMAP
		td.defineProp("SMTP.Active", Boolean.class);
		td.defineProp("SMTP.Host", String.class).size(0, Server.MAXSIZE_HOST);
		td.defineProp("SMTP.Port", Integer.class);
		td.defineProp("SMTP.User", String.class).size(0, Server.MAXSIZE_USER);
		td.defineProp("SMTP.Password", String.class).size(0, Server.MAXSIZE_PASSWORD);
				
		td.defineProp("IMAP.Active", Boolean.class);
		td.defineProp("IMAP.Host", String.class).size(0, Server.MAXSIZE_HOST);
		td.defineProp("IMAP.Port", Integer.class);
		td.defineProp("IMAP.User", String.class).size(0, Server.MAXSIZE_USER);
		td.defineProp("IMAP.Password", String.class).size(0, Server.MAXSIZE_PASSWORD);

		td.defineProp("UseEmailBeacon", Boolean.class);

		// External services
		td.defineProp("XCo.APIKey", String.class).size(0, Server.SIZE_XCO_API_KEY);
		
		// SMS
		td.defineProp("OpenMarket.Active", Boolean.class);
		td.defineProp("OpenMarket.User", String.class).size(0, Server.MAXSIZE_USER);
		td.defineProp("OpenMarket.Password", String.class).size(0, Server.MAXSIZE_PASSWORD);
		td.defineProp("OpenMarket.Sender", String.class).size(0, Server.MAXSIZE_SENDER_ID);
		td.defineProp("OpenMarket.Program", String.class);

		td.defineProp("Clickatell.Active", Boolean.class);
		td.defineProp("Clickatell.User", String.class).size(0, Server.MAXSIZE_USER);
		td.defineProp("Clickatell.Password", String.class).size(0, Server.MAXSIZE_PASSWORD);
		td.defineProp("Clickatell.Sender", String.class).size(0, Server.MAXSIZE_SENDER_ID);
		td.defineProp("Clickatell.API", String.class);
		
		td.defineProp("BulkSMS.Active", Boolean.class);
		td.defineProp("BulkSMS.Region", String.class);
		td.defineProp("BulkSMS.User", String.class).size(0, Server.MAXSIZE_USER);
		td.defineProp("BulkSMS.Password", String.class).size(0, Server.MAXSIZE_PASSWORD);

		td.defineProp("UseSMSEmailGateways", Boolean.class);
		
		td.defineProp("SMSCountries", String.class);

		// On-boarding
		td.defineProp("OpenReg", Boolean.class);
		
		// Timeline
		td.defineProp("TimelineStops", BitSet.class);
		for (String channel : Channel.getAll())
		{
			td.defineProp("Timeline." + channel, BitSet.class);
			td.defineProp("Channel." + channel, Boolean.class);
		}

		// Twitter
		td.defineProp("Twitter.Active", Boolean.class);
		td.defineProp("Twitter.Url", String.class);
		td.defineProp("Twitter.User", String.class);
		td.defineProp("Twitter.ConsumerKey", String.class);
		td.defineProp("Twitter.ConsumerSecret", String.class);
		td.defineProp("Twitter.AccessToken", String.class);
		td.defineProp("Twitter.AccessTokenSecret", String.class);
		td.defineProp("Twitter.Debug", Boolean.class);
		
		// Voxeo
		td.defineProp("Voxeo.Active", Boolean.class);
		td.defineProp("Voxeo.Region", String.class);
		td.defineProp("Voxeo.DialingToken", String.class);
		td.defineProp("Voxeo.CallerID", String.class);
		
		td.defineProp("VoiceCountries", String.class);

		return td;
	}

	// - - -
	
	/**
	 * The federation <code>Server</code> holds settings shared among all servers of the federation.
	 * Callers must always reload the object just before taking actions because its properties may have been altered
	 * by other servers in the federation.
	 * @return
	 * @throws Exception
	 */
	public Server loadFederation() throws Exception
	{
		Server fed = loadByName("*");
		if (fed==null)
		{
			fed = new Server();
			fed.setName("*");
			try
			{
				save(fed);
			}
			catch (Exception exc)
			{
				// May be a duplicate key exception if just created by another class, so try loading again
				fed = loadByName("*");
				if (fed==null)
				{
					throw exc;
				}
			}
		}
		return fed;
	}
	
	/**
	 * The federation <code>Server</code> holds settings shared among all servers of the federation.
	 * Callers must always reload the object just before taking actions because its properties may have been altered
	 * by other servers in the federation.
	 * @return
	 * @throws Exception
	 */
	public Server openFederation() throws Exception
	{
		return (Server) loadFederation().clone();
	}

	/**
	 * The local <code>Server</code> holds settings specific to this server.
	 * @return
	 * @throws Exception
	 */
	public Server loadLocal() throws Exception
	{
		Server local = loadByName(Util.getLocalHostName());
		if (local==null)
		{
			local = new Server();
			local.setName(Util.getLocalHostName());
			save(local);
		}
		return local;
	}

	/**
	 * The local <code>Server</code> holds settings specific to this server.
	 * @return
	 * @throws Exception
	 */
	public Server openLocal() throws Exception
	{
		return (Server) loadLocal().clone();
	}

	public Server loadByName(String name) throws Exception
	{
		return loadByColumn("Name", name);
	}
	
	private Server openByName(String name) throws Exception
	{
		return openByColumn("Name", name);
	}
}
