package samoyan.database;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import samoyan.core.LocaleEx;
import samoyan.core.Util;

public final class Server extends DataBean
{
	public static final int MAXSIZE_NAME = 16;
	
	public static final int MAXSIZE_HOST = 256;
	public static final int MAXSIZE_USER = 256;
	public static final int MAXSIZE_PASSWORD = 64;
	
	public static final int MAXSIZE_SENDER_ID = User.MAXSIZE_PHONE;
	public static final int MAXSIZE_TIMELINE = 241; // bits, not bytes
	
	public static final int SIZE_XCO_API_KEY = 32;
		
	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String serverName)
	{
		set("Name", serverName);
	}
	
	public TimeZone getTimeZone()
	{
		return (TimeZone) get("TimeZone", TimeZone.getDefault());
	}
	public void setTimeZone(TimeZone timeZone)
	{
		set("TimeZone", timeZone);
	}
	
	/**
	 * Returns the list of locales supported by this system.
	 * Will always include at least one locale.
	 * The list returned is a copy. Callers can change it, then call setLocales to persist if needed.
	 * @return
	 */
	public List<Locale> getLocales()
	{
		List<Locale> result = new ArrayList<Locale>();
		String tokenStr = (String) get("Locales");
		if (tokenStr!=null)
		{
			for (String l : Util.tokenize(tokenStr, ";"))
			{
				Locale loc = LocaleEx.fromString(l);
				if (loc!=null)
				{
					result.add(loc);
				}
			}
		}
		if (result.size()==0)
		{
			result.add(Locale.getDefault());
		}
		return result;
	}
	public void setLocales(List<Locale> locales)
	{
		if (locales==null)
		{
			set("Locales", null);
		}
		else
		{
			StringBuffer buf = new StringBuffer();
			for (Locale l : locales)
			{
				buf.append(l.toString() + ";");
			}
			String str = buf.toString();
			if (str.length()>0)
			{
				str = str.substring(0, str.length()-1);
			}
			set("Locales", str);
		}
	}

	public String getPlatformUpgradeVersion()
	{
		return (String) get("PlatformUpgradeVersion", "");
	}
	public void setPlatformUpgradeVersion(String version)
	{
		set("PlatformUpgradeVersion", version);
	}
	public String getApplicationUpgradeVersion()
	{
		return (String) get("AppUpgradeVersion", "");
	}
	public void setApplicationUpgradeVersion(String version)
	{
		set("AppUpgradeVersion", version);
	}

	// - - -
	// SMTP
	
	public boolean isSMTPActive()
	{
		return (Boolean) get("SMTP.Active", false);
	}
	public void setSMTPActive(boolean b)
	{
		set("SMTP.Active", b);
	}

	public String getSMTPHost()
	{
		return (String) get("SMTP.Host");
	}
	public void setSMTPHost(String host)
	{
		set("SMTP.Host", host);
	}
	
	public int getSMTPPort()
	{
		return (Integer) get("SMTP.Port", 465);
	}
	public void setSMTPPort(int port)
	{
		set("SMTP.Port", port);
	}
	
	public String getSMTPUser()
	{
		return (String) get("SMTP.User");
	}
	public void setSMTPUser(String user)
	{
		set("SMTP.User", user);
	}
	
	public String getSMTPPassword()
	{
		return (String) get("SMTP.Password");
	}
	public void setSMTPPassword(String password)
	{
		set("SMTP.Password", password);
	}

	/**
	 * Indicates whether or not to insert web beacons into emails to track when they are opened.
	 * @return <code>true</code> (default) or <code>false</code>.
	 */
	public boolean isUseEmailBeacon()
	{
		return (Boolean) get("UseEmailBeacon", true);
	}
	public void setUseEmailBeacon(boolean b)
	{
		set("UseEmailBeacon", b);
	}
	
	// - - -
	// IMAP
	
	public boolean isIMAPActive()
	{
		return (Boolean) get("IMAP.Active", false);
	}
	public void setIMAPActive(boolean b)
	{
		set("IMAP.Active", b);
	}
	
	public String getIMAPHost()
	{
		return (String) get("IMAP.Host");
	}
	public void setIMAPHost(String host)
	{
		set("IMAP.Host", host);
	}
	
	public int getIMAPPort()
	{
		return (Integer) get("IMAP.Port", 993);
	}
	public void setIMAPPort(int port)
	{
		set("IMAP.Port", port);
	}
	
	public String getIMAPUser()
	{
		return (String) get("IMAP.User");
	}
	public void setIMAPUser(String user)
	{
		set("IMAP.User", user);
	}
	
	public String getIMAPPassword()
	{
		return (String) get("IMAP.Password");
	}
	public void setIMAPPassword(String password)
	{
		set("IMAP.Password", password);
	}
	
	public long getIMAPPollingInterval()
	{
		return (Long) get("IMAP.PollMillis", 60L*1000L);
	}
	public void setIMAPPollingInterval(long millis)
	{
		set("IMAP.PollMillis", millis);
	}

	// - - -
	// x.co
	
	public String getXCoAPIKey()
	{
		return (String) get("setup:XCo.APIKey");
	}
	public void setXCoAPIKey(String xcoAPIKey)
	{
		set("setup:XCo.APIKey", xcoAPIKey);
	}
	
	// - - -
	// SMS
		
	public boolean isUseEmailGatewaysForSMS()
	{
		return (Boolean) get("UseSMSEmailGateways", true);
	}
	public void setUseSMSEmailGateways(boolean b)
	{
		set("UseSMSEmailGateways", b);
	}

	
	public boolean isOpenMarketActive()
	{
		return (Boolean) get("OpenMarket.Active", false);
	}
	public void setOpenMarketActive(boolean b)
	{
		set("OpenMarket.Active", b);
	}
	
	public String getOpenMarketSenderID()
	{
		return (String) get("OpenMarket.Sender");
	}
	/**
	 *
	 * @param senderID The phone number to send the SMS from. Provided by OpenMarket.
	 */
	public void setOpenMarketSenderID(String senderID)
	{
		set("OpenMarket.Sender", senderID);
	}
	
	public String getOpenMarketProgramID()
	{
		return (String) get("OpenMarket.Program");
	}
	/**
	 * 
	 * @param program The program ID provided by OpenMarket.
	 */
	public void setOpenMarketProgramID(String program)
	{
		set("OpenMarket.Program", program);
	}
	
	public String getOpenMarketUser()
	{
		return (String) get("OpenMarket.User");
	}
	public void setOpenMarketUser(String user)
	{
		set("OpenMarket.User", user);
	}
	
	public String getOpenMarketPassword()
	{
		return (String) get("OpenMarket.Password");
	}
	public void setOpenMarketPassword(String password)
	{
		set("OpenMarket.Password", password);
	}

	public String getOpenMarketDemoPrefix()
	{
		return (String) get("OpenMarket.DemoPrefix");
	}
	public void setOpenMarketDemoPrefix(String password)
	{
		set("OpenMarket.DemoPrefix", password);
	}

		
	public boolean isClickatellActive()
	{
		return (Boolean) get("Clickatell.Active", false);
	}
	public void setClickatellActive(boolean b)
	{
		set("Clickatell.Active", b);
	}
	
	public String getClickatellSenderID()
	{
		return (String) get("Clickatell.Sender");
	}
	/**
	 *
	 * @param senderID The phone number to send the SMS from. Provided by Clickatell.
	 */
	public void setClickatellSenderID(String senderID)
	{
		set("Clickatell.Sender", senderID);
	}
	
	public String getClickatellAPIID()
	{
		return (String) get("Clickatell.API");
	}
	/**
	 * 
	 * @param program The API ID provided by Clickatell.
	 */
	public void setClickatellAPIID(String program)
	{
		set("Clickatell.API", program);
	}
	
	public String getClickatellUser()
	{
		return (String) get("Clickatell.User");
	}
	public void setClickatellUser(String user)
	{
		set("Clickatell.User", user);
	}
	
	public String getClickatellPassword()
	{
		return (String) get("Clickatell.Password");
	}
	public void setClickatellPassword(String password)
	{
		set("Clickatell.Password", password);
	}
	
	
	public boolean isBulkSMSActive()
	{
		return (Boolean) get("BulkSMS.Active", false);
	}
	public void setBulkSMSActive(boolean b)
	{
		set("BulkSMS.Active", b);
	}

	public String getBulkSMSUser()
	{
		return (String) get("BulkSMS.User");
	}
	public void setBulkSMSUser(String user)
	{
		set("BulkSMS.User", user);
	}
	
	public String getBulkSMSPassword()
	{
		return (String) get("BulkSMS.Password");
	}
	public void setBulkSMSPassword(String password)
	{
		set("BulkSMS.Password", password);
	}

	public String getBulkSMSRegion()
	{
		return (String) get("BulkSMS.Region");
	}
	/**
	 * 
	 * @param region "US", "UK", "DE", "ES", "ZA" (countries) or "XX" (international).
	 */
	public void setBulkSMSRegion(String region)
	{
		set("BulkSMS.Region", region);
	}
	
	/**
	 * Returns the list of countries that SMS can be sent to.
	 * The list returned is a copy. Callers can change it, then call setLocales to persist if needed.
	 * @return A list of the two-letter ISO code of the country.
	 */
	public List<String> getSMSCountries()
	{
		String tokenStr = (String) get("SMSCountries");
		if (tokenStr!=null)
		{
			return Util.tokenize(tokenStr, ";");
		}
		else
		{
			return new ArrayList<String>();
		}
	}
	public void setSMSCountries(List<String> countries)
	{
		if (countries==null)
		{
			set("SMSCountries", null);
		}
		else
		{
			StringBuilder buf = new StringBuilder();
			for (String s : countries)
			{
				buf.append(s).append(";");
			}
			String str = buf.toString();
			if (str.length()>0)
			{
				str = str.substring(0, str.length()-1);
			}
			set("SMSCountries", str);
		}
	}

	// - - -
	// On-boarding

	/**
	 * Indicates if the system allows users to self-register an account.
	 * @return <code>true</code> (default) if registration is open to the public; <code>false</code> if registration is by invitation only.
	 */
	public boolean isOpenRegistration()
	{
		return (Boolean) get("OpenReg", true);
	}
	public void setOpenRegistration(boolean b)
	{
		set("OpenReg", b);
	}
	
	// - - -
	// Channels + timeline
	
	public boolean isChannelEnabled(String channel)
	{
		return (Boolean) get("Channel." + channel, true);
	}
	public void setChannelEnabled(String channel, boolean enabled)
	{
		set("Channel." + channel, enabled);
	}

	public BitSet getTimeline(String channel)
	{
		return (BitSet) get("Timeline." + channel);
	}
	public void setTimeline(String channel, BitSet bits)
	{
		set("Timeline." + channel, bits);
	}

	public BitSet getTimelineStops()
	{
		BitSet bits = (BitSet) get("TimelineStops");
		if (bits==null)
		{
			bits = new BitSet();
		}
		bits.set(0);
		return bits;
	}
	public void setTimelineStops(BitSet bits)
	{
		set("TimelineStops", bits);
	}
	
	// - - -
	// Twitter
	
	public boolean isTwitterActive()
	{
		return (Boolean) get("Twitter.Active", false);
	}
	public void setTwitterActive(boolean b)
	{
		set("Twitter.Active", b);
	}

	public String getTwitterOAuthConsumerKey()
	{
		return (String) get("Twitter.ConsumerKey");
	}
	
	public String getTwitterOAuthConsumerSecret()
	{
		return (String) get("Twitter.ConsumerSecret");
	}
	
	public String getTwitterOAuthAccessToken()
	{
		return (String) get("Twitter.AccessToken");
	}
	
	public String getTwitterOAuthAccessTokenSecret()
	{
		return (String) get("Twitter.AccessTokenSecret");
	}
	
	public boolean isTwitterDebug()
	{
		return (Boolean) get("Twitter.Debug", false);
	}
	
	public void setTwitterOAuthConsumerKey(String consumerKey)
	{
		set("Twitter.ConsumerKey", consumerKey);
	}
	public void setTwitterOAuthConsumerSecret(String consumerSecret)
	{
		set("Twitter.ConsumerSecret", consumerSecret);
	}
	public void setTwitterOAuthAccessToken(String accessToken)
	{
		set("Twitter.AccessToken", accessToken);
	}
	public void setTwitterOAuthAccessTokenSecret(String accessTokenSecret)
	{
		set("Twitter.AccessTokenSecret", accessTokenSecret);
	}
	public void setTwitterDebug(boolean enabled)
	{
		set("Twitter.Debug", enabled);
	}
	public String getTwitterUserName()
	{
		return (String) get("Twitter.User");
	}
	public void setTwitterUserName(String userName)
	{
		set("Twitter.User", userName);
	}

	public boolean isVoxeoActive()
	{
		return (Boolean) get("Voxeo.Active", false);
	}
	public void setVoxeoActive(boolean active)
	{
		set("Voxeo.Active", active);
	}
	public String getVoxeoRegion()
	{
		return (String) get("Voxeo.Region");
	}
	/**
	 * @param region "US", "EU" or "APAC"
	 */
	public void setVoxeoRegion(String url)
	{
		set("Voxeo.Region", url);
	}		
	public String getVoxeoDialingToken()
	{
		return (String) get("Voxeo.DialingToken");
	}
	public void setVoxeoDialingToken(String token)
	{
		set("Voxeo.DialingToken", token);
	}
	public String getVoxeoCallerID()
	{
		return (String) get("Voxeo.CallerID");
	}
	public void setVoxeoCallerID(String callerID)
	{
		set("Voxeo.CallerID", callerID);
	}
	/**
	 * Returns the list of locales supported by Voxeo.
	 * Will always include Locale.US.
	 * The list returned is a copy. Callers can change it, then call setVoxeoLocales to persist if needed.
	 * @return
	 */
	public List<Locale> getVoxeoLocales()
	{
		List<Locale> result = new ArrayList<Locale>();
		String tokenStr = (String) get("Voxeo.Locales");
		if (tokenStr!=null)
		{
			for (String l : Util.tokenize(tokenStr, ";"))
			{
				Locale loc = LocaleEx.fromString(l);
				if (loc!=null)
				{
					result.add(loc);
				}
			}
		}
		if (result.contains(Locale.US)==false)
		{
			result.add(0, Locale.US);
		}
		return result;
	}
	public void setVoxeoLocales(List<Locale> locales)
	{
		if (locales==null)
		{
			set("Voxeo.Locales", null);
		}
		else
		{
			StringBuffer buf = new StringBuffer();
			for (Locale l : locales)
			{
				buf.append(l.toString() + ";");
			}
			String str = buf.toString();
			if (str.length()>0)
			{
				str = str.substring(0, str.length()-1);
			}
			set("Voxeo.Locales", str);
		}
	}

	/**
	 * Returns the list of countries that voice calls can be initiated to.
	 * The list returned is a copy. Callers can change it, then call setVoiceCountries to persist if needed.
	 * @return A list of the two-letter ISO code of the country.
	 */
	public List<String> getVoiceCountries()
	{
		String tokenStr = (String) get("VoiceCountries");
		if (tokenStr!=null)
		{
			return Util.tokenize(tokenStr, ";");
		}
		else
		{
			return new ArrayList<String>();
		}
	}
	public void setVoiceCountries(List<String> countries)
	{
		if (countries==null)
		{
			set("VoiceCountries", null);
		}
		else
		{
			StringBuilder buf = new StringBuilder();
			for (String s : countries)
			{
				buf.append(s).append(";");
			}
			String str = buf.toString();
			if (str.length()>0)
			{
				str = str.substring(0, str.length()-1);
			}
			set("VoiceCountries", str);
		}
	}
}
