package samoyan.database;

import java.util.Date;

public final class Trackback extends DataBean
{
	public final static int MAXSIZE_ADDRESSEE = 128;
	public final static int MAX_ROUNDROBIN = 10000;
	public final static int MAXSIZE_CHANNEL = 8;
	public final static int MAXSIZE_EXTERNALID = 128;
	public final static String PREFIX = "#";
	
	public Trackback()
	{
		init("Created", new Date());
	}
	
	public String getChannel()
	{
		return (String) get("Channel");
	}
	public void setChannel(String channel)
	{
		set("Channel", channel);
	}

	public String getAddressee()
	{
		return (String) get("Addressee");
	}
	public void setAddressee(String addressee)
	{
		set("Addressee", addressee);
	}

	public String getExternalID()
	{
		return (String) get("ExternalID");
	}
	public void setExternalID(String externalID)
	{
		set("ExternalID", externalID);
	}

	public String getRoundRobinAsString()
	{
		String s = String.valueOf(getRoundRobin());
		while (s.length()<Math.log10(MAX_ROUNDROBIN))
		{
			s = "0" + s;
		}
		return s;
	}
	public int getRoundRobin()
	{
		return (Integer) get("RoundRobin", 0);
	}
	void setRoundRobin(int roundRobin)
	{
		set("RoundRobin", roundRobin);
	}
	
	public Date getCreated()
	{
		return (Date) get("Created");
	}
	public void setCreated(String created)
	{
		set("Created", created);
	}	
}
