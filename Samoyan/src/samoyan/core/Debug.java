package samoyan.core;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Debug
{
	private final static int MAX_LINES = 1000;
	
	private static Queue<String> console = new ConcurrentLinkedQueue<String>();
	private static boolean enabled = false;
	
	public static void init(boolean enable)
	{
		enabled = enable;
	}
	
	public static void logln(String str)
	{
		if (enabled)
		{
			out(str);
			addToConsole(str);
		}
	}

	public static void logStackTrace(Throwable t)
	{
		if (enabled)
		{
			String desc = Util.exceptionDesc(t);
			out(desc);
			addToConsole(desc);
		}
	}
	
	private static void out(String str)
	{
		String timeStr = getTimeStr();
		if (str.indexOf("\n")>=0 || str.indexOf("\r")>=0)
		{
			System.out.println("[" + timeStr + "]\r\n" + str);
		}
		else
		{
			System.out.println("[" + timeStr + "] " + str);
		}		
	}
	
	private static String getTimeStr()
	{
		Calendar cal = Calendar.getInstance();
		int hh = cal.get(Calendar.HOUR_OF_DAY);
		int mm = cal.get(Calendar.MINUTE);
		int ss = cal.get(Calendar.SECOND);
		
		String str = "";		
		if (hh<10) str += "0";
		str += String.valueOf(hh);
		str += ":";
		if (mm<10) str += "0";
		str += String.valueOf(mm);
		str += ":";
		if (ss<10) str += "0";
		str += String.valueOf(ss);
		
		return str;
	}
	
	private static void addToConsole(String str)
	{
		int sz = console.size();
		for (int i=0; i<sz-MAX_LINES; i++)
		{
			console.poll();
		}
		
		String timeStr = getTimeStr();
		if (str.indexOf("\n")>=0 || str.indexOf("\r")>=0)
		{
			console.add("[" + timeStr + "]\r\n" + str + "\r\n");
		}
		else
		{
			console.add("[" + timeStr + "] " + str + "\r\n");
		}
	}
	
	public static List<String> getConsole()
	{
		List<String> result = new ArrayList<String>();
		result.addAll(console);
		return result;
	}
	
	public static void clearConsole()
	{
		console.clear();
	}
}
