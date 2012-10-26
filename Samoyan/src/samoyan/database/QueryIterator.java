package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import samoyan.core.Debug;

public abstract class QueryIterator<E> implements Iterator<E>
{
	private Query query;
	private ResultSet rs;
	private boolean hasNext;
	private E bean;
	
	public QueryIterator(String sql, List<Object> params) throws SQLException
	{
		this.query = new Query();
		this.rs = this.query.select(sql, params);
		this.hasNext = this.rs.next();
		if (this.hasNext==false)
		{
			close();
		}
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			close();
		}
		catch (SQLException e)
		{
			// Can't do much here
			Debug.logStackTrace(e);
		}
		
		super.finalize();
	}

	@Override
	public boolean hasNext()
	{
		if (this.hasNext==false)
		{
			try
			{
				close();
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return this.hasNext;
	}

	@Override
	public E next()
	{
		if (this.hasNext==false)
		{
			throw new NoSuchElementException();
		}
		
		try
		{
			this.bean = fromResultSet(this.rs);	
			this.hasNext = this.rs.next();
			return this.bean;
		}
		catch (Exception e)
		{
			try
			{
				close();
			}
			catch (Exception e1)
			{
				// Ignore, we're throwing an exception anyway
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * To be overridden by the subclass to return the bean corresponding to the current row in the resultset.
	 * @param rs
	 * @return
	 */
	protected abstract E fromResultSet(ResultSet rs) throws Exception;

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
	public void close() throws SQLException
	{
		if (this.query!=null)
		{
			this.query.close();
			this.query = null;
		}
		this.rs = null;
		this.hasNext = false;
	}
}
