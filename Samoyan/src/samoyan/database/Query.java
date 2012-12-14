package samoyan.database;

import java.sql.*;
import java.util.*;

import samoyan.core.Debug;
import samoyan.core.Util;

public final class Query
{
//	private static final long SLOW_QUERY_THREASHOLD = 250L; // 250ms
	private Database db = null;
	private ResultSet rs = null;
	
	public Query()
	{
		this.db = Database.getInstance();
	}
	public Query(Database db)
	{
		this.db = db;
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		if (this.rs!=null)
		{
			// If Query properly used, this should not happen.
			Debug.logln("DB: Query object not closed");
			
			// At this point, the connection may have been closed, so discard it rather than recycle it
			Connection cn = this.rs.getStatement().getConnection();
			this.rs.getStatement().close();
			this.rs = null;
			this.db.discardConnection(cn);
		}
		super.finalize();
	}

	public void close() throws SQLException
	{
		if (this.rs!=null)
		{
			Connection cn = this.rs.getStatement().getConnection();
			this.rs.getStatement().close();
			this.rs = null;
			this.db.recycleConnection(cn);
		}
	}

	private List<Object> convertParams(List<Object> params)
	{
		if (params==null)
		{
			return null;
		}
		List<Object> converted = new ArrayList<Object>(params.size());
		for (Object value : params)
		{
			converted.add(DataBeanStoreUtil.convertJavaToSQL(value));
		}
		return converted;
	}
	
	public ResultSet updatableSelect(String sql) throws SQLException
	{
		return select(sql, null, ResultSet.CONCUR_UPDATABLE);
	}

	/**
	 * Execute a query against the database and return a <code>TYPE_FORWARD_ONLY</code>
	 * and <code>CONCUR_UPDATABLE</code> <code>ResultSet<code>.
	 * @param sql The SQL statement.
	 * @param params The parameters to the SQL statement, or <code>null</code> if none.
	 * @return The <code>ResultSet</code>.
	 * @throws SQLException
	 */
	public ResultSet updatableSelect(String sql, List<Object> params) throws SQLException
	{
		return select(sql, params, ResultSet.CONCUR_UPDATABLE);
	}
	
	public ResultSet select(String sql) throws SQLException
	{
		return select(sql, null, ResultSet.CONCUR_READ_ONLY);
	}

	/**
	 * Execute a query against the database and return a <code>TYPE_FORWARD_ONLY</code>
	 * and <code>CONCUR_READ_ONLY</code> <code>ResultSet<code>.
	 * @param sql The SQL statement.
	 * @param params The parameters to the SQL statement, or <code>null</code> if none.
	 * @return The <code>ResultSet</code>.
	 * @throws SQLException
	 */
	public ResultSet select(String sql, List<Object> params) throws SQLException
	{
		return select(sql, params, ResultSet.CONCUR_READ_ONLY);
	}

	private ResultSet select(String sql, List<Object> params, int concurrency) throws SQLException
	{
		this.close();
		
		int attempt = 0;
		params = convertParams(params);
		
		while (true)
		{
			// Get a pooled connection
			Connection cn = null;
			PreparedStatement statement = null;
			try
			{
				cn = this.db.getConnection();
				
				long start = System.currentTimeMillis();
				
				statement = cn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, concurrency);
				if (params!=null)
				{
					for (int i=0; i < params.size(); i++)
					{
						statement.setObject(i+1, params.get(i));
					}
				}
				boolean b = statement.execute();

				// Performance logging
				long end = System.currentTimeMillis();
//				if (Debug.ON)
//				{
//					Debug.println("Database " + (end-start) + "ms: " + sql);
//				}
//				else
//				if (end-start>=SLOW_QUERY_THREASHOLD)
				{
					Debug.logln("DB " + (end-start) + "ms " + sql);
				}

				if (b==true)
				{
					this.rs = statement.getResultSet();
					return this.rs;
				}
				else
				{
					// Shouldn't happen. Invalid query specified.
					statement.close();
					statement = null;
					this.db.recycleConnection(cn);
					cn = null;
					return null; 
				}
			}
			catch (SQLException exc)
			{
//				Debug.logln("DB " + this.name + " Exc");
//				Debug.logStackTrace(exc);

				if (statement!=null)
				{
					statement.close();
					statement = null;
				}
				
				if (isOfflineException(exc))
				{
					this.db.discardConnection(cn);
				}
				else
				{
					this.db.recycleConnection(cn);
				}
				cn = null;

				// Retry the operation ONCE more with a fresh connection
				if (attempt>0)
				{
					throw exc;
				}
			}
						
			attempt++;
		}
	}
	
	public int update(String sql) throws SQLException
	{
		return update(sql, null);
	}

	/**
	 * Execute a query against the database.
	 * @param sql The SQL statement.
	 * @param params The parameters to the SQL statement, or <code>null</code> if none.
	 * @return int Number of rows affected by the update
	 * @throws SQLException
	 */
	public int update(String sql, List<Object> params) throws SQLException
	{
		this.close();
		
		int attempt = 0;
		params = convertParams(params);
		
		while (true)
		{
			// Get a pooled connection
			Connection cn = null;
			PreparedStatement statement = null;
			try
			{
				cn = this.db.getConnection();
				
				long start = System.currentTimeMillis();
				
				// Execute the statement
				statement = cn.prepareStatement(sql);
				if (params!=null)
				{
					for (int i=0; i < params.size(); i++)
					{
						statement.setObject(i+1, params.get(i));
					}
				}
				int rowsAffected = statement.executeUpdate();

				statement.close();
				statement = null;
				this.db.recycleConnection(cn);
				cn = null;
				
				// Performance logging
				long end = System.currentTimeMillis();
//				if (Debug.ON)
//				{
//					Debug.println("Database " + (end-start) + "ms: " + sql);
//				}
//				else
//				if (end-start>=SLOW_QUERY_THREASHOLD)
				{
					Debug.logln("DB " + (end-start) + "ms " + sql);
				}
				
				return rowsAffected;
			}
			catch (SQLException exc)
			{
//				Debug.logln("DB " + this.name + " Exc");
//				Debug.logStackTrace(exc);

				if (statement!=null)
				{
					statement.close();
					statement = null;
				}
				
				if (isOfflineException(exc))
				{
					this.db.discardConnection(cn);
				}
				else
				{
					this.db.recycleConnection(cn);
				}
				cn = null;
				
				// Retry the operation ONCE more with a fresh connection
				if (attempt>0)
				{
					throw exc;
				}
			}

			attempt++;
		}
	}

	/**
	 * Returns <code>true</code> if the given <code>SQLException</code> indicates the database
	 * was offline during the operation. The SQL state of the exception is tested and
	 * <code>true</code> is returned for codes "08S01", "08001" and "08006"
	 * @param exc The <code>SQLException</code> to test.
	 * @return <code>true</code> if this <code>SQLException</code> was caused by the
	 * database going offline.
	 */
	private static boolean isOfflineException(SQLException exc)
	{
		if (exc.getSQLState()==null) return false;
		
		// 08S01 indicates the connection was reset by peer. It happens when the database
		// was restarted without restarting the web server.
		// 08001 indicates an error establishing a socket. Database is offline.
		// 08006 indicates the connection with the server was lost.
		return (	exc.getSQLState().equals("08S01") ||
					exc.getSQLState().equals("08001") ||
					exc.getSQLState().equals("08006"));
	}

	public static List<UUID> queryListUUID(String sql, List<Object> params) throws SQLException
	{
		Query q = new Query();
		try
		{
			List<UUID> result = new ArrayList<UUID>();
			ResultSet rs = q.select(sql, params);
			while (rs.next())
			{
				result.add(Util.bytesToUUID(rs.getBytes(1)));
			}
			return result;
		}
		finally
		{
			q.close();
		}
	}

	public static List<String> queryListString(String sql, List<Object> params) throws SQLException
	{
		Query q = new Query();
		try
		{
			List<String> result = new ArrayList<String>();
			ResultSet rs = q.select(sql, params);
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
			return result;
		}
		finally
		{
			q.close();
		}
	}
}
