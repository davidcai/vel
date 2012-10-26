package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import samoyan.core.Cache;
import samoyan.core.ParameterList;
import samoyan.core.Util;

public abstract class LinkStore
{
	private class Link
	{
		private UUID key;
		private int weight;
	}
	
	private LinkTableDef linkTableDef = null;
	
	protected LinkStore()
	{
	}

	/**
	 * To be overridden by subclasses to define the mapping of this link table to the database.
	 * @param td
	 */
	protected abstract LinkTableDef defineMapping();

	public LinkTableDef getLinkTableDef()
	{
		if (this.linkTableDef==null) { synchronized(this) {	if (this.linkTableDef==null)
		{
			this.linkTableDef = defineMapping();

			// TABLE 1
			if (this.linkTableDef.getKey1().isDisallowRemoveIfHasLinks())
			{
				// Disallow removal of a bean from table1 if it is linked with bean(s) in table2
				TableDef.getStore(this.linkTableDef.getKey1().getForeignTable()).attachEventHandler(new DataBeanEventHandler()
				{
					@Override
					public boolean canRemove(UUID foreignBeanID) throws Exception
					{
						return (getByKey(1, foreignBeanID, null).size()==0);
					}
				});
			}
			else
			{
				// Automatically remove links when table1 bean is removed
				TableDef.getStore(this.linkTableDef.getKey1().getForeignTable()).attachEventHandler(new DataBeanEventHandler()
				{
					@Override
					public void onBeforeRemove(UUID foreignBeanID) throws Exception
					{
						unlinkByKey(1, foreignBeanID);
					}
				});
			}
			
			// TABLE 2
			if (this.linkTableDef.getKey2().isDisallowRemoveIfHasLinks())
			{
				// Disallow removal of a bean from table2 if it is linked with bean(s) in table2
				TableDef.getStore(this.linkTableDef.getKey2().getForeignTable()).attachEventHandler(new DataBeanEventHandler()
				{
					@Override
					public boolean canRemove(UUID foreignBeanID) throws Exception
					{
						return (getByKey(2, foreignBeanID, null).size()==0);
					}
				});
			}
			else
			{
				// Automatically remove links when table2 bean is removed
				TableDef.getStore(this.linkTableDef.getKey2().getForeignTable()).attachEventHandler(new DataBeanEventHandler()
				{
					@Override
					public void onBeforeRemove(UUID foreignBeanID) throws Exception
					{
						unlinkByKey(2, foreignBeanID);
					}
				});
			}
		}}}
		
		return this.linkTableDef;
	}
	
	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Returns the UUIDs of key 2 that are associated with the given key 1.
	 * @param key1
	 * @return
	 * @throws SQLException 
	 */
	protected List<UUID> getByKey1(UUID key1) throws SQLException
	{
		return getByKey(1, key1, null);
	}
	
	/**
	 * Returns the UUIDs of key 1 that are associated with the given key 2.
	 * @param key1
	 * @return
	 * @throws SQLException 
	 */
	protected List<UUID> getByKey2(UUID key2) throws SQLException
	{
		return getByKey(2, key2, null);
	}
	
	/**
	 * Returns the UUIDs of key 2 that are associated with the given key 1.
	 * @param key1
	 * @param filter Returns only records with this weight.
	 * @return
	 * @throws SQLException 
	 */
	protected List<UUID> getByKey1(UUID key1, int filter) throws SQLException
	{
		return getByKey(1, key1, filter);
	}
	
	/**
	 * Returns the UUIDs of key 1 that are associated with the given key 2.
	 * @param key1
	 * @param filter Returns only records with this weight.
	 * @return
	 * @throws SQLException 
	 */
	protected List<UUID> getByKey2(UUID key2, int filter) throws SQLException
	{
		return getByKey(2, key2, filter);
	}

	private List<UUID> getByKey(int keyIndex, UUID searchKey, Integer filter) throws SQLException
	{
		LinkTableDef td = getLinkTableDef();
		
		String cacheKey = "link:" + td.getName() + "." + searchKey.toString();
		if (td.isCache())
		{
			if (td.hasWeight()==false)
			{
				// The UUIDs are cached directly
				List<UUID> cached = (List<UUID>) Cache.get(cacheKey);
				if (cached!=null)
				{
					return cached;
				}
			}
			else
			{
				// We extract the UUIDs from the cached Link objects
				List<Link> cached = (List<Link>) Cache.get(cacheKey);
				if (cached!=null)
				{
					List<UUID> result = new ArrayList<UUID>(cached.size());
					for (Link l : cached)
					{
						if (filter==null || l.weight==filter)
						{
							result.add(l.key);
						}
					}
					return result;
				}
			}
		}
				
		// Choose columns for query
		String keyCol;
		String resultCol;
		if (keyIndex==1)
		{
			keyCol = td.getKey1().getColumnName();
			resultCol = td.getKey2().getColumnName();
		}
		else
		{
			keyCol = td.getKey2().getColumnName();
			resultCol = td.getKey1().getColumnName();
		}
		
		// Query
		Query q = new Query();
		try
		{
			List<UUID> result = new ArrayList<UUID>();
			if (td.hasWeight()==false)
			{
				ResultSet rs = q.select("SELECT " + resultCol + " FROM " + td.getName() + " WHERE " + keyCol + "=?", new ParameterList(searchKey));
				while (rs.next())
				{
					result.add(Util.bytesToUUID(rs.getBytes(1)));
				}
				if (td.isCache())
				{
					// We cache the UUIDs
					Cache.insert(cacheKey, result);
				}
				return result;
			}
			else
			{
				List<Link> links = new ArrayList<Link>();
				ResultSet rs = q.select("SELECT " + resultCol + "," + td.getWeightColumn() + " FROM " + td.getName() + " WHERE " + keyCol + "=?", new ParameterList(searchKey));
				while (rs.next())
				{
					Link link = new Link();
					link.key = Util.bytesToUUID(rs.getBytes(1));
					link.weight = rs.getInt(2);
					
					links.add(link);
					if (filter==null || link.weight==filter)
					{
						result.add(link.key);
					}
				}
				if (td.isCache())
				{
					// We cache the Link objects
					Cache.insert(cacheKey, links);
				}
				return result;
			}
		}
		finally
		{
			q.close();
		}
	}

	protected void link(UUID key1, UUID key2) throws SQLException
	{
		link(key1, key2, 1);
	}
	
	/**
	 * Links the two IDs.
	 */
	protected void link(UUID key1, UUID key2, int weight) throws SQLException
	{
		LinkTableDef td = getLinkTableDef();

		// Check in cache if already linked
		if (td.isCache())
		{
			if (td.hasWeight()==false)
			{
				// The UUIDs are cached directly
				List<UUID> cached = (List<UUID>) Cache.get("link:" + td.getName() + "." + key1.toString());
				if (cached!=null)
				{
					for (UUID id : cached)
					{
						if (id.equals(key2))
						{
							// Already linked together
							return;
						}
					}
				}
				else // check reverse relationship
				{
					cached = (List<UUID>) Cache.get("link:" + td.getName() + "." + key2.toString());
					if (cached!=null)
					{
						for (UUID id : cached)
						{
							if (id.equals(key1))
							{
								// Already linked together
								return;
							}
						}
					}
				}
			}
			else
			{
				// We extract the UUIDs from the cached Link objects
				List<Link> cached = (List<Link>) Cache.get("link:" + td.getName() + "." + key1.toString());
				if (cached!=null)
				{
					List<UUID> result = new ArrayList<UUID>(cached.size());
					for (Link l : cached)
					{
						if (l.key.equals(key2) && l.weight==weight)
						{
							// Already linked together with same weight
							return;
						}
					}
				}
				else // check reverse relationship
				{
					cached = (List<Link>) Cache.get("link:" + td.getName() + "." + key2.toString());
					if (cached!=null)
					{
						List<UUID> result = new ArrayList<UUID>(cached.size());
						for (Link l : cached)
						{
							if (l.key.equals(key1) && l.weight==weight)
							{
								// Already linked together with same weight
								return;
							}
						}
					}
				}
			}
		}

		// Access database
		Query q = new Query();
		try
		{
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");
			sql.append(td.getKey1().getColumnName());
			sql.append(",");
			sql.append(td.getKey2().getColumnName());
			if (td.hasWeight())
			{
				sql.append(",");
				sql.append(td.getWeightColumn());
			}
			sql.append(" FROM ");
			sql.append(td.getName());
			sql.append(" WHERE ");
			sql.append(td.getKey1().getColumnName());
			sql.append("=? AND ");
			sql.append(td.getKey2().getColumnName());
			sql.append("=?");
			
			ResultSet rs = q.updatableSelect(sql.toString(), new ParameterList(key1).plus(key2));
			if (!rs.next())
			{
				rs.moveToInsertRow();
				rs.updateBytes(1, Util.uuidToBytes(key1));
				rs.updateBytes(2, Util.uuidToBytes(key2));
				if (td.hasWeight())
				{
					rs.updateInt(3, weight);
				}
				rs.insertRow();
				
				Cache.invalidate("link:" + td.getName() + "." + key1.toString());
				Cache.invalidate("link:" + td.getName() + "." + key2.toString());
			}
			else if (td.hasWeight())
			{
				int currentWeight = rs.getInt(3);
				if (currentWeight!=weight)
				{
					rs.updateInt(3, weight);
					rs.updateRow();

					Cache.invalidate("link:" + td.getName() + "." + key1.toString());
					Cache.invalidate("link:" + td.getName() + "." + key2.toString());
				}
			}
		}
		finally
		{
			q.close();
		}
	}
	
	/**
	 * Unlinks the two objects
	 */
	protected void unlink(UUID key1, UUID key2) throws SQLException
	{
		LinkTableDef td = getLinkTableDef();

		// Check in cache if already linked
		boolean linked = false;
		boolean cacheExists = false;
		if (td.isCache())
		{
			if (td.hasWeight()==false)
			{
				// The UUIDs are cached directly
				List<UUID> cached = (List<UUID>) Cache.get("link:" + td.getName() + "." + key1.toString());
				if (cached!=null)
				{
					cacheExists = true;
					for (UUID id : cached)
					{
						if (id.equals(key2))
						{
							// Linked together
							linked = true;
							break;
						}
					}
				}
				else // check reverse relationship
				{
					cached = (List<UUID>) Cache.get("link:" + td.getName() + "." + key2.toString());
					if (cached!=null)
					{
						cacheExists = true;
						for (UUID id : cached)
						{
							if (id.equals(key1))
							{
								// Linked together
								linked = true;
								break;
							}
						}
					}
				}
			}
			else
			{
				// We extract the UUIDs from the cached Link objects
				List<Link> cached = (List<Link>) Cache.get("link:" + td.getName() + "." + key1.toString());
				if (cached!=null)
				{
					cacheExists = true;
					List<UUID> result = new ArrayList<UUID>(cached.size());
					for (Link l : cached)
					{
						if (l.key.equals(key2))
						{
							// Linked together
							linked = true;
							break;
						}
					}
				}
				else // check reverse relationship
				{
					cached = (List<Link>) Cache.get("link:" + td.getName() + "." + key2.toString());
					if (cached!=null)
					{
						cacheExists = true;
						List<UUID> result = new ArrayList<UUID>(cached.size());
						for (Link l : cached)
						{
							if (l.key.equals(key1))
							{
								// Linked together
								linked = true;
								break;
							}
						}
					}
				}
			}
		}
		if (cacheExists==true && linked==false)
		{
			return;
		}
		
		// Access database
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM ");
		sql.append(td.getName());
		sql.append(" WHERE ");
		sql.append(td.getKey1().getColumnName());
		sql.append("=? AND ");
		sql.append(td.getKey2().getColumnName());
		sql.append("=?");

		Query q = new Query();
		try
		{
			int res = q.update(sql.toString(), new ParameterList(key1).plus(key2));
			if (res>0)
			{
				Cache.invalidate("link:" + td.getName() + "." + key1.toString());
				Cache.invalidate("link:" + td.getName() + "." + key2.toString());
			}
		}
		finally
		{
			q.close();
		}
	}
	
	/**
	 * Removes all links associated with this key.
	 * @param key1
	 * @throws SQLException
	 */
	protected void unlinkByKey1(UUID key1) throws SQLException
	{
		unlinkByKey(1, key1);
	}
	
	/**
	 * Removes all links associated with this key.
	 * @param key2
	 * @throws SQLException
	 */
	protected void unlinkByKey2(UUID key2) throws SQLException
	{
		unlinkByKey(2, key2);
	}
	
	private void unlinkByKey(int keyIndex, UUID key) throws SQLException
	{
		// Fetch all opposite keys linked to this key
		List<UUID> oppositeKeys = getByKey(keyIndex, key, null);
		if (oppositeKeys.size()==0)
		{
			return;
		}
		
		LinkTableDef td = getLinkTableDef();

		// Choose column for query
		String keyCol;
		if (keyIndex==1)
		{
			keyCol = td.getKey1().getColumnName();
		}
		else
		{
			keyCol = td.getKey2().getColumnName();
		}

		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM ");
		sql.append(td.getName());
		sql.append(" WHERE ");
		sql.append(keyCol);
		sql.append("=?");

		Query q = new Query();
		try
		{
			int res = q.update(sql.toString(), new ParameterList(key));
		}
		finally
		{
			q.close();
		}

		// Clear cache
		Cache.invalidate("link:" + td.getName() + "." + key.toString());
		for (UUID oppositeKey : oppositeKeys)
		{
			Cache.invalidate("link:" + td.getName() + "." + oppositeKey.toString());
		}
	}
	
	/**
	 * Returns the weight of the relationship between the two objects.
	 * @param key1
	 * @param key2
	 * @return The weight, or <code>null</code> if no link is established, or if the relationship is not weighted.
	 * @throws SQLException 
	 */
	protected Integer getWeight(UUID key1, UUID key2) throws SQLException
	{
		LinkTableDef td = getLinkTableDef();
		if (td.hasWeight()==false)
		{
			return null;
		}
		
		// Look in key1->key2 cache
		List<Link> links = (List<Link>) Cache.get("link:" + td.getName() + "." + key1.toString());
		if (links!=null)
		{
			for (Link l : links)
			{
				if (l.key.equals(key2))
				{
					return l.weight;
				}
			}
			return null;
		}
		
		// Look in key2->key1 cache
		links = (List<Link>) Cache.get("link:" + td.getName() + "." + key2.toString());
		if (links!=null)
		{
			for (Link l : links)
			{
				if (l.key.equals(key1))
				{
					return l.weight;
				}
			}
			return null;
		}
		
		// Not found in cache, then load relationship key1->key2 and look again in cache
		List<UUID> keys = getByKey1(key1);
		
		links = (List<Link>) Cache.get("link:" + td.getName() + "." + key1.toString());
		if (links!=null)
		{
			for (Link l : links)
			{
				if (l.key.equals(key2))
				{
					return l.weight;
				}
			}
			return null;
		}
		else
		{
			// Query database
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");
			sql.append(td.getWeightColumn());
			sql.append(" FROM ");
			sql.append(td.getName());
			sql.append(" WHERE ");
			sql.append(td.getKey1());
			sql.append("=? AND ");
			sql.append(td.getKey2());
			sql.append("=?");

			Query q = new Query();
			try
			{
				ResultSet rs = q.select(sql.toString(), new ParameterList(key1).plus(key2));
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
			finally
			{
				q.close();
			}
		}
		
		// Not found
		return null;
	}

	/**
	 * Returns whether or not there's a relationship between the two objects.
	 * @param key1
	 * @param key2
	 * @return
	 * @throws SQLException 
	 */
	protected boolean isLinked(UUID key1, UUID key2) throws SQLException
	{
		LinkTableDef td = getLinkTableDef();
		
		// Look in key1->key2 cache
		if (td.hasWeight()==true)
		{
			List<Link> links = (List<Link>) Cache.get("link:" + td.getName() + "." + key1.toString());
			if (links!=null)
			{
				for (Link l : links)
				{
					if (l.key.equals(key2))
					{
						return true;
					}
				}
				return false;
			}
		}
		else
		{
			List<UUID> keys = (List<UUID>) Cache.get("link:" + td.getName() + "." + key1.toString());
			if (keys!=null)
			{
				return keys.contains(key2);
			}
		}
		
		// Look in key2->key1 cache
		if (td.hasWeight()==true)
		{
			List<Link> links = (List<Link>) Cache.get("link:" + td.getName() + "." + key2.toString());
			if (links!=null)
			{
				for (Link l : links)
				{
					if (l.key.equals(key1))
					{
						return true;
					}
				}
				return false;
			}
		}
		else
		{
			List<UUID> keys = (List<UUID>) Cache.get("link:" + td.getName() + "." + key2.toString());
			if (keys!=null)
			{
				return keys.contains(key1);
			}
		}
		
		// Not found in cache, then load relationship key1->key2
		List<UUID> keys = getByKey1(key1);
		return keys.contains(key2);
	}
}
