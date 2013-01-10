package samoyan.database;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import samoyan.core.Debug;
import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.syslog.ExceptionLogEntry;

public final class LogEntryStore extends DataBeanStore<LogEntry>
{
	private static LogEntryStore instance = new LogEntryStore();

	protected LogEntryStore()
	{
	}
	public final static LogEntryStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<LogEntry> getBeanClass()
	{
		return LogEntry.class;
	}
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("LogEntries");
		td.setCacheOnSave(false);
		
		td.defineCol("Server", String.class).size(0, LogEntry.MAXSIZE_SERVER).invariant();
		td.defineCol("Time", Date.class).invariant();
		td.defineCol("Name", String.class).size(0, LogEntry.MAXSIZE_NAME).invariant();
		td.defineCol("Severity", Integer.class).invariant();
		td.defineCol("IP", InetAddress.class).invariant();
		td.defineCol("UserID", UUID.class).invariant();
		td.defineCol("SessionID", UUID.class).invariant();

		td.defineCol("M1", Double.class).invariant();
		td.defineCol("M2", Double.class).invariant();
		td.defineCol("M3", Double.class).invariant();
		td.defineCol("M4", Double.class).invariant();
		
		td.defineCol("S1", String.class).size(0, LogEntry.MAXSIZE_STRING).invariant();
		td.defineCol("S2", String.class).size(0, LogEntry.MAXSIZE_STRING).invariant();
		td.defineCol("S3", String.class).size(0, LogEntry.MAXSIZE_STRING).invariant();
		td.defineCol("S4", String.class).size(0, LogEntry.MAXSIZE_STRING).invariant();
		
		td.defineCol("T1", String.class).invariant();
		td.defineCol("T2", String.class).invariant();
		
		td.defineCol("ReqCtx", String.class).invariant();
		
		return td;
	}

	// - - -

	private static ExecutorService executor = null;
	private static Queue<LogEntry> queue = null;
	private static AtomicInteger queueSize = new AtomicInteger();
	private static Map<String, String> savedTypes = new ConcurrentHashMap<String, String>();
	private static final int WRITE_THRESHOLD = 64;
	
	/**
	 * This task writes all queued <code>LogEntry</code>s to the database.
	 * @author brian
	 *
	 */
	private static class SaveQueueToDatabase implements Runnable
	{
		@Override
		public void run()
		{
			saveQueue();
		}
	}
	
	/**
	 * Initialize the log engine.
	 */
	public static void start()
	{
		queue = new ConcurrentLinkedQueue<LogEntry>();
		executor = Executors.newCachedThreadPool();
	}
	/**
	 * Shutdown the log engine.
	 */
	public static void terminate()
	{
		if (executor!=null)
		{
			Util.shutdownNowAndAwaitTermination(executor);
			executor = null;
		}
		
		// Write remaining queued entries
		saveQueue();
	}
	
	private static void saveQueue()
	{
		// Do not process more than original queue size.
		// Otherwise, more log entries can be added while saving, and this method might never quit.
		int max = queueSize.get();
		
		for (int i=0; i<max; i++)
		{
			LogEntry log = queue.poll();
			if (log==null)
			{
				break;
			}
			queueSize.decrementAndGet();
			
			// Load the type of the log entry
			LogType type = null;
			try
			{
				type = LogTypeStore.getInstance().loadByName(log.getName());
			}
			catch (Exception e)
			{
				// Do not throw exception
				Debug.logStackTrace(e);
			}

			// Save the log type, but not more than once per server session
			if (savedTypes.containsKey(log.getName())==false)
			{
				try
				{
					type = LogTypeStore.getInstance().openByName(log.getName());
				}
				catch (Exception e)
				{
					// Do not throw exception
					Debug.logStackTrace(e);
				}
				
				// Create the bean if necessary
				if (type==null)
				{
					type = new LogType();
					type.setName(log.getName());
				}
				
				// Set its properties
				type.setSeverity(log.getSeverity());
				for (int m=1; m<=LogEntry.NUM_MEASURES; m++)
				{
					type.setMeasureLabel(m, log.getMeasureLabel(m));
				}
				for (int s=1; s<=LogEntry.NUM_STRINGS; s++)
				{
					type.setStringLabel(s, log.getStringLabel(s));
				}
				for (int t=1; t<=LogEntry.NUM_TEXTS; t++)
				{
					type.setTextLabel(t, log.getTextLabel(t));
				}
				
				// Save it
				try
				{
					LogTypeStore.getInstance().save(type);
					savedTypes.put(log.getName(), log.getName());
				}
				catch (Exception e)
				{
					// Do not throw exception
					Debug.logStackTrace(e);
				}
			}
			
			if (type==null || type.getLife()!=0)
			{
				// Save the log entry
				try
				{
					getInstance().superSave(log);
				}
				catch (Exception e)
				{
					// Do not throw exception
					Debug.logStackTrace(e);
				}
			}
		}
	}

	// - - -

	/**
	 * Asynchronously write the log entry to the database. Will not delay the caller thread.
	 * @param log
	 */
	@Override
	public void save(LogEntry log)
	{
		// Add the log entry to the queue
		queue.add(log);
		int sz = queueSize.incrementAndGet();
		
		if (sz>=WRITE_THRESHOLD || log.getSeverity()!=LogEntry.INFO)
		{
			// Write to disk when queue size reaches threshold
			// Immediately write warnings and errors to the database
			executor.execute(new SaveQueueToDatabase());
		}
	}

	private void superSave(LogEntry log) throws Exception
	{
		super.save(log);
	}
	
	/**
	 * Logs a general exception 
	 * @param e
	 */
	public static void log(Throwable e)
	{
		getInstance().save(new ExceptionLogEntry(e));
	}
	public static void log(LogEntry log)
	{
		getInstance().save(log);
	}
	
	@Override
	public LogEntry load(UUID id) throws Exception
	{
		LogEntry log = super.load(id);
		if (log!=null)
		{
			// Need to fill in the labels from the log type
			LogType type = LogTypeStore.getInstance().loadByName(log.getName());
			if (type!=null)
			{
				for (int m=1; m<=LogEntry.NUM_MEASURES; m++)
				{
					log.setMeasureLabel(m, type.getMeasureLabel(m));
				}
				for (int s=1; s<=LogEntry.NUM_STRINGS; s++)
				{
					log.setStringLabel(s, type.getStringLabel(s));
				}
				for (int t=1; t<=LogEntry.NUM_TEXTS; t++)
				{
					log.setTextLabel(t, type.getTextLabel(t));
				}
			}
		}
		return log;
	}
	
	public List<UUID> queryLog(Date from, Date to, String server, UUID userID, String ip, UUID session, Set<String> types) throws SQLException, UnknownHostException
	{
		// First, save all pending log entries to disk to make sure latest entries are returned
		saveQueue();
		
		// Prepare SQL statement
		ParameterList params = new ParameterList();
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ID FROM LogEntries WHERE 1=1 ");
		
		if (from!=null)
		{
			sql.append("AND Time>=? ");
			params.add(from.getTime());
		}
		if (to!=null)
		{
			sql.append("AND Time<? ");
			params.add(to.getTime());
		}
		
		if (types!=null && types.size()>0)
		{
			sql.append("AND (1=0");
			for (String t : types)
			{
				sql.append(" OR Name=?");
				params.add(t);
			}
			sql.append(") ");
		}
		
		if (userID!=null)
		{
			sql.append("AND UserID=? ");
			params.add(Util.uuidToBytes(userID));
		}
		
		if (ip!=null)
		{
			InetAddress inet = InetAddress.getByName(ip);
			sql.append("AND IP=? ");
			params.add(inet.getAddress());
		}

		if (session!=null)
		{
			sql.append("AND SessionID=? ");
			params.add(Util.uuidToBytes(session));
		}

		if (server!=null)
		{
			sql.append("AND Server=? ");
			params.add(server);
		}

		sql.append("ORDER BY Time DESC");

		// Run the query
		return Query.queryListUUID(sql.toString(), params);
	}
}
