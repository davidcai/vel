package samoyan.apps.admin;

import java.io.File;
import java.net.InetAddress;
import java.util.*;

import samoyan.controls.TwoColFormControl;
import samoyan.core.*;
import samoyan.database.Database;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.UserStore;
import samoyan.email.EmailServer;
import samoyan.servlet.Controller;
import samoyan.sms.SmsServer;
import samoyan.twitter.TwitterServer;

public class SystemOverviewPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/system-overview";

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:Overview.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		
		if (isParameter("gc"))
		{
			System.gc();
		}
		if (isParameter("emptycache"))
		{
			Cache.clearAll();
		}
		if (isParameter("closedb"))
		{
			Database.getInstance().close();
		}
		if (isParameter("poll"))
		{
			EmailServer.poll();
		}

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Server name
		InetAddress inetAddr = InetAddress.getLocalHost();
		 twoCol.writeRow(getString("admin:Overview.Server"));
		twoCol.writeEncode(inetAddr.getHostName());

		// Inet address
		twoCol.writeRow(getString("admin:Overview.IP"));
		twoCol.writeEncode(inetAddr.getHostAddress());
		
		// Time
		TimeZone tz = getTimeZone();
		String tzName = tz.getDisplayName(tz.inDaylightTime(new Date()), TimeZone.LONG, this.getLocale());
		twoCol.writeRow(getString("admin:Overview.Time"));
		twoCol.writeEncodeDateTime(new Date());
		twoCol.write(", ");
		twoCol.writeEncode(tzName);

		twoCol.writeSpaceRow();

		// Running since
		long startupTime = Controller.getStartTime();
		long now = System.currentTimeMillis();
		twoCol.writeRow(getString("admin:Overview.RunningSince"));
		twoCol.writeEncodeDateTime(new Date(startupTime));
		twoCol.write(" (");
		twoCol.writeDuration(now - startupTime);
		twoCol.write(")");
		
		// Request count
		long hits = Controller.getTotalHitCount();
		long hitsPerDay = (long) ( (24.0D*60.0D*60.0D * (double)hits) / ((double)(now - startupTime)/1000.0D));
		long pageHits = Controller.getPageHitCount();
		long pageHitsPerDay = (long) ( (24.0D*60.0D*60.0D * (double)pageHits) / ((double)(now - startupTime)/1000.0D));
		twoCol.writeRow(getString("admin:Overview.Hits"));
		twoCol.writeEncode(getString("admin:Overview.TotalHitStats", hits, hitsPerDay));
		twoCol.write("<br>");
		twoCol.writeEncode(getString("admin:Overview.PageHitStats", pageHits, pageHitsPerDay));

		// Online users
		int onlineUsers = UserStore.getInstance().getActive().size();
		twoCol.writeRow(getString("admin:Overview.Online"));
		twoCol.writeEncodeLong(onlineUsers);

		twoCol.writeSpaceRow();

		// Email server
		twoCol.writeRow(getString("admin:Overview.Email"));
		twoCol.writeEncode(getString("admin:Overview.ChannelStats", EmailServer.getCountMessagesSent(), EmailServer.getCountMessagesReceived(), EmailServer.getCountDeliveryFailures()));
		if (fed.isIMAPActive())
		{
			twoCol.write(" <small>");
			twoCol.writeLink(getString("admin:Overview.Poll"), getPageURL(COMMAND, new ParameterMap("poll", "1")));
			twoCol.write("</small>");
		}
		
		// SMS server
		twoCol.writeRow(getString("admin:Overview.SMS"));
		twoCol.writeEncode(getString("admin:Overview.ChannelStats", SmsServer.getCountMessagesSent(), SmsServer.getCountMessagesReceived(), SmsServer.getCountDeliveryFailures()));

		// Twitter server
		twoCol.writeRow(getString("admin:Overview.Twitter"));
		twoCol.writeEncode(getString("admin:Overview.ChannelStats", TwitterServer.getCountMessagesSent(), TwitterServer.getCountMessagesReceived(), TwitterServer.getCountDeliveryFailures()));

		twoCol.writeSpaceRow();
		
		// CPUs
		Runtime rt = Runtime.getRuntime();
		twoCol.writeRow(getString("admin:Overview.CPUs"));
		twoCol.writeEncodeLong(rt.availableProcessors());

		// Disk space
		twoCol.writeRow(getString("admin:Overview.Disks"));
		boolean diskPrinted = false;
		File[] roots = File.listRoots();
		for (int i=0; i<roots.length; i++)
		{
			if (diskPrinted)
			{
				twoCol.write("<br>");
			}
			long diskSpace = roots[i].getFreeSpace();
			twoCol.writeEncode(getString("admin:Overview.FreeDiskSpace", roots[i].getCanonicalPath(), diskSpace/1024L/1024L));
			diskPrinted = true;
		}
		if (diskPrinted==false)
		{
			twoCol.writeEncode(getString("admin:Overview.NoDisks"));
		}

		// Memory
		long rtMax = rt.maxMemory();
		long rtTotal = rt.totalMemory();
		long rtFree = rt.freeMemory();
		long usedMem = rtTotal - rtFree;
		double pctUsed = (double) usedMem / (double) rtMax;

		twoCol.writeRow(getString("admin:Overview.Memory"));
		twoCol.writeEncode(getString("admin:Overview.MemoryReport", rtMax/1024L/1024, pctUsed));
		twoCol.write(" <small>");
		twoCol.writeLink(getString("admin:Overview.GarbageCollect"), getPageURL(COMMAND, new ParameterMap("gc", "1")));
		twoCol.write("</small>");

		// Cache stats
		twoCol.writeRow(getString("admin:Overview.Cache"));
		twoCol.writeEncode(getString("admin:Overview.CacheStats", Cache.getCount(), (int) (100F * Cache.getSuccessRate())));
		twoCol.write(" <small>");
		twoCol.writeLink(getString("admin:Overview.EmptyCache"), getPageURL(COMMAND, new ParameterMap("emptycache", "1")));
		twoCol.write("</small>");
		
		// Database stats
		Database db = Database.getInstance();
		twoCol.writeRow(getString("admin:Overview.Database"));
//		twoCol.writeEncode(db.getURL());
//		twoCol.write("<br>");
		twoCol.writeEncode(getString("admin:Overview.DatabaseStats", db.getOpenConnectionCount()));
		twoCol.write(" <small>");
		twoCol.writeLink(getString("admin:Overview.CloseDatabase"), getPageURL(COMMAND, new ParameterMap("closedb", "1")));
		twoCol.write("</small>");

		twoCol.writeSpaceRow();

		// Locale
		Locale loc = Locale.getDefault();
		String locName = loc.getDisplayName(this.getLocale());
		twoCol.writeRow(getString("admin:Overview.Locale"));
		twoCol.writeEncode(locName);
		twoCol.write(" /");
		twoCol.writeEncode(loc.toString());
		twoCol.write("/");

		// Java stats
		Properties sysProps = System.getProperties();
		twoCol.writeRow(getString("admin:Overview.JavaVM"));
		twoCol.writeEncode(sysProps.getProperty("java.vm.name"));
		twoCol.write(" / ");
		twoCol.writeEncode(sysProps.getProperty("java.vm.vendor"));

		twoCol.write("<br>");
		twoCol.writeEncode(getString("admin:Overview.JavaVersion", sysProps.getProperty("java.vm.version")));
		twoCol.write("<br>");
		twoCol.writeEncode(Util.strReplace(sysProps.getProperty("java.home"), File.separator, " " + File.separator + " "));

		twoCol.render();
	}
}
