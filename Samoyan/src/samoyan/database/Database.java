package samoyan.database;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import samoyan.core.Debug;

public class Database
{
	private static Database instance = null;
	
	private Queue<Connection> availableConnections = new ConcurrentLinkedQueue<Connection>();
	
	private final String password;
	private final String userID;
	private final String dbUrl;
	private final String driver;

	private AtomicInteger dynMaxOpenConns;
	private AtomicInteger dynOpenConns;
	private AtomicLong dynLastPeakReached;
	private long dynPeakDecay;
	
	public static Database createInstance(String driver, String dbUrl, String userID, String password) throws SQLException
	{
		Database db = new Database(driver, dbUrl, userID, password);
		db.test();
		instance = db;
		return instance;
	}
	
	public static Database getInstance()
	{
		return instance;
	}
	
	private Database(String driver, String dbUrl, String userID, String password)
	{
		this.password = password;
		this.userID = userID;
		this.dbUrl = dbUrl;
		this.driver = driver;
				
		// Peak management
		this.dynMaxOpenConns = new AtomicInteger(0);
		this.dynOpenConns = new AtomicInteger(0);
		this.dynLastPeakReached = new AtomicLong(System.currentTimeMillis());
		this.dynPeakDecay = 60L*60L*1000L; // 60 min
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			close();
		}
		finally
		{
			super.finalize();
		}
	}

	public void close()
	{
		Iterator<Connection> iter = this.availableConnections.iterator();
		while (iter.hasNext())
		{
			Connection cn = iter.next();
			iter.remove();
			discardConnection(cn);
		}		
	}
		
	public String getURL()
	{
		return dbUrl;
	}

	public String getPassword()
	{
		return password;
	}

	public String getUserID()
	{
		return userID;
	}

	public String getDriver()
	{
		return driver;
	}

	/**
	 * Number of milliseconds after a connection count peak is reached, that the number of open connections is decreased by one.
	 * @return
	 */
	public long getPeakDecay()
	{
		return dynPeakDecay;
	}
	public void setPeakDecay(long peakDecay)
	{
		dynPeakDecay = peakDecay;
	}

	public int getOpenConnectionCount()
	{
		return dynOpenConns.get();
	}

	public int getFreeConnectionCount()
	{
		return availableConnections.size();
	}

	/**
	 * Puts the connection back in the pool.
	 * @param cn
	 */
	public void recycleConnection(Connection cn)
	{
		if (cn==null)
		{
// Debug
//try
//{
//	throw new NullPointerException("Null connection recylced");
//}
//catch (Exception npe)
//{
//	Debug.logln("DB " + this.name + " Recycle exception");
//	Debug.logStackTrace(npe);
//}
			return;
		}

		// Peak management
		long now = System.currentTimeMillis();
		int max = this.dynMaxOpenConns.get();
		if (max>0 && now - this.dynLastPeakReached.get() > this.dynPeakDecay) // 1hr passed since last peak was reached
		{
			if (this.dynMaxOpenConns.compareAndSet(max, max-1)) // Reduce peak by one
			{
				this.dynLastPeakReached.set(now); // Update last peak reached time
//				Debug.logln("DB " + this.name + " Peak reduced max=" + (max-1));
			}
		}
		
		int open = this.dynOpenConns.get();
		boolean recycle = (open <= max);
// Debug
//if (!recycle)
//{
//	int free = this.availableConnections.size();
//	Debug.logln("DB " + this.name + " Recycle free=" + free + " open=" + open + " max=" + max + " recycle=" + recycle);
//}
		
		if (recycle && cn!=null)
		{
			this.availableConnections.add(cn);
		}
		else
		{
			discardConnection(cn);
		}
	}

	public void discardConnection(Connection cn)
	{
		if (cn==null)
		{
// Debug
//try
//{
//	throw new NullPointerException("Null connection discarded");
//}
//catch (Exception npe)
//{
//	Debug.logln("DB " + this.name + " Discard exception");
//	Debug.logStackTrace(npe);
//}
			return;
		}

		try
		{
			cn.close();
			cn = null;
			
			// Peak management
			int open = this.dynOpenConns.decrementAndGet();
			
//			int max = this.dynMaxOpenConns.get();
//			int free = this.availableConnections.size();
//			Debug.logln("DB " + this.name + " Discard free=" + free + " open=" + open + " max=" + max);

// Debug
//if (open==0 && max==1)
//{
//	throw new SQLException("Discarding last connection, check stack trace");
//}
		}
		catch (SQLException e)
		{
			Debug.logStackTrace(e);
		}			
	}

	/**
	 * Returns or creates a new connection.
	 * @return A connection to the database.
	 */
	public Connection getConnection() throws SQLException
	{
		// Look up pooled connection
		Connection cn = this.availableConnections.poll();		
		if (cn==null)
		{
			// Create a new connection if needed
			cn = newConnection();
		}
		
		return cn;
	}

	private Connection newConnection() throws SQLException
	{
		Connection cn = null;
		try
		{
			Class.forName(this.driver);
			cn = DriverManager.getConnection(this.dbUrl, this.userID, this.password);

			// Peak management
			if (cn!=null)
			{
				int open = this.dynOpenConns.incrementAndGet();
				int max = this.dynMaxOpenConns.get();
				if (open>max && this.dynMaxOpenConns.compareAndSet(max, open))
				{
					this.dynLastPeakReached.set(System.currentTimeMillis());
				}
				max = this.dynMaxOpenConns.get();
				
//				int free = this.availableConnections.size();
//				Debug.logln("DB " + this.name + " New free=" + free + " open=" + open + " max=" + max);
			}
		}
		catch (ClassNotFoundException e)
		{
			throw new SQLException("Can't find database driver " + this.driver);
		}
		return cn;
	}
	
	/**
	 * Test the connection to the database, throwing an exception if there's a problem connecting.
	 * @return <code>true</code> if the connection is valid.
	 * @throws SQLException 
	 */
	public void test() throws SQLException
	{
		Query q = new Query(this);
		try
		{
			q.select("SELECT 'OK'");
		}
		finally
		{
			q.close();
		}
	}
}
