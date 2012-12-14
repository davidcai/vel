package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import samoyan.core.Cache;
import samoyan.core.ParameterList;
import samoyan.core.Util;

public abstract class DataBeanStore<T extends DataBean>
{
	private static final int MAX_VALUE_LEN = 256;
	private TableDef tableDef = null;
	private List<DataBeanEventHandler> eventHandlers = null;
	
	protected DataBeanStore()
	{
	}
	
	/**
	 * To be overridden by subclasses to return the class of the DataBean.
	 * @return
	 */
	protected abstract Class<T> getBeanClass();
	
	/**
	 * To be overridden by subclasses to define the mapping of this bean to the database.
	 * @param td
	 */
	protected abstract TableDef defineMapping();
	
	public TableDef getTableDef()
	{
		if (this.tableDef==null)
		{
			throw new NullPointerException("Definition missing for " + this.getClass().getName());
		}
		return this.tableDef;
	}
	
	public TableDef createTableDef(String name)
	{
		if (this.tableDef!=null)
		{
			throw new NullPointerException("Definition already created for " + this.getClass().getName());
		}
		
		this.tableDef = TableDef.newInstance(name, this);
		
		return this.tableDef;
	}
	
	public void define()
	{
		if (this.tableDef==null) { synchronized(this) { if (this.tableDef==null)
		{
			this.tableDef = defineMapping();
			
			for (PropDef colDef : this.tableDef.getCols())
			{
				final String tableName = this.tableDef.getName();
				final String colName = colDef.getName();

				if (!Util.isEmpty(colDef.getOwnedBy()))
				{
					// Automatically remove beans when owner table bean is removed
					TableDef.getStore(colDef.getOwnedBy()).attachEventHandler(new DataBeanEventHandler()
					{
						@Override
						public void onBeforeRemove(UUID ownerBeanID) throws Exception
						{
							removeMany(queryByColumn(colName, ownerBeanID));
						}
					});
				}
				if (!Util.isEmpty(colDef.getRefersTo()))
				{
					TableDef.getStore(colDef.getRefersTo()).attachEventHandler(new DataBeanEventHandler()
					{
						@Override
						public boolean canRemove(UUID referredToBeanID) throws Exception
						{
							Query q = new Query();
							try
							{
								ResultSet rs = q.select("SELECT 1 FROM " + tableName + " WHERE " + colName + "=?",  new ParameterList(referredToBeanID));
								return !rs.next();
							}
							finally
							{
								q.close();
							}		
						}
					});					
				}
			}
		}}}
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void attachEventHandler(DataBeanEventHandler eventHandler)
	{
		if (eventHandlers==null)
		{
			synchronized(this)
			{
				if (eventHandlers==null)
				{
					eventHandlers = new ArrayList<DataBeanEventHandler>();
				}
			}
		}
		eventHandlers.add(eventHandler);
	}
	
	private void dispatchBeforeSave(DataBean bean, boolean insert) throws Exception
	{
		if (eventHandlers!=null)
		{
			for (DataBeanEventHandler handler : eventHandlers)
			{
				handler.onBeforeSave(bean, insert);
			}
		}
	}
	private void dispatchAfterSave(DataBean bean, boolean insert) throws Exception
	{
		if (eventHandlers!=null)
		{
			for (DataBeanEventHandler handler : eventHandlers)
			{
				handler.onAfterSave(bean, insert);
			}
		}
	}
	
	private boolean dispatchCanRemove(UUID beanID) throws Exception
	{
		if (eventHandlers!=null)
		{
			for (DataBeanEventHandler handler : eventHandlers)
			{
				if (handler.canRemove(beanID)==false)
				{
					return false;
				}
			}
		}
		return true;
	}
	private void dispatchBeforeRemove(UUID beanID) throws Exception
	{
		if (eventHandlers!=null)
		{
			for (DataBeanEventHandler handler : eventHandlers)
			{
				handler.onBeforeRemove(beanID);
			}
		}
	}
	private void dispatchAfterRemove(UUID beanID) throws Exception
	{
		if (eventHandlers!=null)
		{
			for (DataBeanEventHandler handler : eventHandlers)
			{
				handler.onAfterRemove(beanID);
			}
		}
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Returns a writable bean which can later be passed to {@link #save(DataBean)}.
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public T open(UUID id) throws Exception
	{
		T bean = load(id);
		return (T) (bean==null? null : bean.clone());
	}
	
	/**
	 * Queries the database for the bean's data and fills in the bean.
	 * The bean returned by this method may be shared across threads. As such, it is read-only.
	 * To get a writable bean, <code>clone</code> the bean, or call {@link #open(UUID)}
	 * which performs the cloning internally.
	 * @param id The ID used to query the database.
	 * @return The bean, or <code>null</code> if not found.
	 * @throws Exception
	 */
	public T load(UUID id) throws Exception
	{
		if (id==null) return null;
		
		// Get the table definition
		TableDef td = getTableDef();

		// Check cache
		String cacheKey = "bean:" + td.getName() + "." + id.toString();
		if (td.isCacheOnLoad() || td.isCacheOnSave())
		{
			T cached = (T) Cache.get(cacheKey);
			if (cached!=null)
			{
				return cached;
			}
		}
		
		// Create the bean object
		T bean = getBeanClass().newInstance();
		
		List<Object> params = new ArrayList<Object>(1);
		params.add(id);
		Query q = new Query();

		// Load column data
		if (td.hasColumns())
		{
			// Compose the SQL query
			StringBuffer sql = new StringBuffer();
			sql.append("SELECT ");
			for (PropDef col : td.getCols())
			{
				sql.append(col.getName());
				sql.append(",");
			}
			sql.append("ID FROM ");
			sql.append(td.getName());
			sql.append(" WHERE ID=?");
			
			try
			{
				ResultSet rs = q.select(sql.toString(), params);
				if (rs.next())
				{
					DataBeanStoreUtil.resultSetToBean(rs, bean, td);
				}
				else
				{
					return null;
				}
			}
			finally
			{
				q.close();
			}
		}
		
		// - - - - -
		
		// Load properties
		if (td.hasProps())
		{
			try
			{
				ResultSet rs = q.select("SELECT ID,Name,Typ,ValStr=CASE WHEN Val IS NULL THEN ValText ELSE Val END,ValBytes=CASE WHEN ValBin IS NULL THEN ValImage ELSE ValBin END,ValNum FROM Props WHERE LinkedID=?", params);
//				ResultSet rs = q.select("SELECT ID,Name,Typ,Val,ValBin,ValNum,HasImage=CASE WHEN ValImage IS NULL THEN 0 ELSE 1 END,HasText=CASE WHEN ValText IS NULL THEN 0 ELSE 1 END FROM Props WHERE LinkedID=?", params);
				while (rs.next())
				{
					Prop prop = new Prop();
					DataBeanStoreUtil.resultSetToProp(rs, prop, td);
					if (td.isColumn(prop.name)==false) // Safety check: do not insert props with same name as a column
					{
						bean.putReadProp(prop.name, prop);
					}
				}
			}
			finally
			{
				q.close();
			}
		}
		
		if (td.hasImages())
		{
			try
			{
				ResultSet rs = q.select("SELECT ID,Name FROM Images WHERE LinkedID=?", params);
				while (rs.next())
				{
					Prop prop = new Prop();
					prop.id = Util.bytesToUUID(rs.getBytes("ID"));
					prop.name = rs.getString("Name");
					prop.img = true;
					prop.value = null;
					
					if (td.isColumn(prop.name)==false) // Safety check: do not insert renegade props with same name as a column
					{
						bean.putReadProp(prop.name, prop);
					}
				}
			}
			finally
			{
				q.close();
			}
		}
		
		// - - - - -
		
		// Cache and return
		bean.setSaved(true);
		bean.setWritable(false); // Disallow writing because this may be a shared instance
		if (td.isCacheOnLoad())
		{
			Cache.insert(cacheKey, bean);
		}
		return bean;
	}
			
	public void save(T bean) throws Exception
	{
		if (bean==null || bean.isDirty()==false) return;
		if (bean.isWritable()==false)
		{
			throw new IllegalStateException("Bean is not writable");
		}
		
		Query q = new Query();
		
		// Get the table definition
		TableDef td = getTableDef();

		// Get the ID of the bean
		boolean insert = !bean.isSaved();
		UUID beanID = bean.getID();
		byte[] beanIDBytes = Util.uuidToBytes(beanID);
		
		dispatchBeforeSave(bean, insert);
		
		// - - - - -

		// Save column data
		boolean dirtyColumns = insert;
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT ");
		for (PropDef col : td.getCols())
		{
			if (!insert && bean.isDirty(col.getName())==false) continue;
			if (!insert && col.isInvariant())
			{
				throw new SQLException("Invariant column " + td.getName() + "." + col.getName());
			}
			dirtyColumns = true;

			sql.append(col.getName());
			sql.append(",");
		}
		sql.append("ID FROM ");
		sql.append(td.getName());
		sql.append(" WHERE ID=?");
		
		if (dirtyColumns)
		{
			List<Object> params = new ArrayList<Object>(1);
			params.add(beanIDBytes);
			
			try
			{
				ResultSet rs = q.updatableSelect(sql.toString(), params);
				if (insert)
				{
					rs.moveToInsertRow();
				}
				else
				{
					if (!rs.next())
					{
						// !$! Record has been deleted by another thread?
						throw new SQLException("Record not found in database for ID " + bean.getID().toString());
					}
				}
				
				DataBeanStoreUtil.beanToResultSet(bean, rs, td);
				
				if (insert)
				{
					rs.insertRow();
				}
				else
				{
					rs.updateRow();
				}
				bean.setSaved(true);
			}
			finally
			{
				q.close();
			}
		}
		
		// - - - - -
		
		// Save properties
		List<Object> params = new ArrayList<Object>(1);
		Iterator<Prop> iter = bean.getDirtyProps().iterator();
		while (iter.hasNext())
		{
			Prop prop = iter.next();
			if (td.getColDef(prop.name)!=null) continue; // This is a column (persisted above)
			
			Prop readProp = bean.getReadProp(prop.name); // readProp may be null
			
			PropDef pd = td.getPropDef(prop.name); // pd can be null for dynamic properties
			if (!insert && pd!=null && pd.isInvariant())
			{
				throw new SQLException("Invariant property " + td.getName() + "." + prop.name);
			}

			if (prop.value==null)
			{
				prop.id = null;
				if (readProp!=null && readProp.id!=null)
				{
					if (readProp.img)
					{
						ImageStore.getInstance().remove(readProp.id);
					}
					else
					{
						try
						{
							params.clear();
							params.add(readProp.id);
							q.update("DELETE FROM Props WHERE ID=?", params);
						}
						finally
						{
							q.close();
						}					
					}
				}
			}
			else
			{
				if (prop.img)
				{
					Image img = (Image) prop.value;
//					img = (Image) img.clone();
					if (readProp!=null && readProp.id!=null)
					{
						// Force overriding the previous version
						img.setID(readProp.id);
					}
					img.setLinkedID(bean.getID());
					img.setName(prop.name);
					ImageStore.getInstance().save(img);
					
					prop.img = true; // Should already be the case
					prop.id = img.getID(); // Update the ID after saving
					prop.value = null; // Remove the image data from the bean
				}
				else
				{
					boolean insertProp;
					if (readProp==null || readProp.id==null)
					{
						prop.id = UUID.randomUUID();
						insertProp = true;
					}
					else
					{
						prop.id = readProp.id;
						insertProp = false;
					}
					
					try
					{
						params.clear();
						params.add(prop.id);
						ResultSet rs = q.updatableSelect("SELECT * FROM Props WHERE ID=?", params);
						if (insertProp)
						{
							rs.moveToInsertRow();
						}
						else
						{
							rs.next();
						}
		
						DataBeanStoreUtil.propToResultSet(prop, rs, td, bean.getID());
						
						if (insertProp)
						{
							rs.insertRow();
						}
						else
						{
							rs.updateRow();
						}
					}
					finally
					{
						q.close();
					}
				}
			}
		}
		
		// - - - - -

/*		
		// Clean cached queries
		List<String> queryKeys = Cache.getPrefixKeys("bean.query:" + td.getName() + ".");
		iter = bean.getDirtyProps().iterator();
		while (iter.hasNext())
		{
			Prop prop = iter.next();
			for (String cacheKey : queryKeys)
			{
				if (cacheKey.indexOf("." + prop.name + ".")>=0)
				{
					Cache.invalidate(cacheKey);
				}
			}
//			Cache.invalidatePrefix("bean.query:" + td.getName() + "." + prop.name + ".");
		}
*/
		
		// Dispatch event, before clearing dirty flags
		dispatchAfterSave(bean, insert);

		// Clear dirty flags
		bean.clearDirty();
		

		// Cache a read-only clone of the cached bean
		String cacheKey = "bean:" + td.getName() + "." + beanID.toString();
		if (td.isCacheOnSave())
		{
			T clone = (T) bean.clone();
			clone.setSaved(true);
			clone.setWritable(false); // Disallow writing because this will be a shared instance
			Cache.insert(cacheKey, clone);
		}
		else if (!insert && td.isCacheOnLoad())
		{
			Cache.invalidate(cacheKey);
		}
	}
		
	public void removeMany(List<UUID> beanIDs) throws Exception
	{
		for (UUID id : beanIDs)
		{
			remove(id);
		}
	}
	
//	/**
//	 * Performs a SQL query to determine which records to delete, then deletes them.
//	 * @param sql A SQL SELECT query that results in a list of IDs of this data bean, e.g.
//	 * "SELECT ID FROM MyTable WHERE Size>?".
//	 * @param params Parameters to pass to the query.
//	 * @throws Exception
//	 */
//	@Deprecated
//	protected void removeByQuery(String sql, List<Object> params) throws Exception
//	{
//		// First, query the database for the IDs
//		List<UUID> idsToDelete = Query.queryListUUID(sql, params);
//		if (idsToDelete.size()==0)
//		{
//			return;
//		}
//
//		// Dispatch events
//		if (eventHandlers!=null)
//		{
//			for (UUID id : idsToDelete)
//			{
//				dispatchBeforeRemove(id);
//			}
//		}
//
//		// Get the table definition
//		TableDef td = getTableDef();
//		
//		boolean onProps = sql.toUpperCase(Locale.US).matches("\bPROPS\b");
//		boolean onSelf = sql.toUpperCase(Locale.US).matches("\b" + td.getName().toUpperCase(Locale.US) + "\b");
//		if (onProps && onSelf && td.hasProps())
//		{
//			throw new IllegalArgumentException("Joined query on " + td.getName() + " and Props is not allowed in this context");
//		}
//		
//		Query q = new Query();
//
//		// Delete from Props table first, if the query is on the table itself
//		if (onSelf && td.hasProps())
//		{
//			try
//			{
//				q.update("DELETE FROM Props WHERE LinkedID IN (" + sql + ")", params);
//			}
//			finally
//			{
//				q.close();
//			}
//		}
//		
//		// Delete from main table
//		try
//		{
//			q.update("DELETE FROM " + td.getName() + " WHERE ID IN (" + sql + ")", params);
//		}
//		finally
//		{
//			q.close();
//		}
//		
//		// Invalidate cache
//		if (td.isCacheOnLoad() || td.isCacheOnSave())
//		{
//			for (UUID id : idsToDelete)
//			{
//				Cache.invalidate("bean:" + td.getName() + "." + id.toString());
//			}
//		}
//		
//		// Delete from Props table last, if the query was not on the table itself
//		if (!onSelf && td.hasProps())
//		{
//			try
//			{
//				q.update("DELETE FROM Props WHERE LinkedID IN (" + sql + ")", params);
//			}
//			finally
//			{
//				q.close();
//			}
//		}
//
//		// Dispatch events
//		if (eventHandlers!=null)
//		{
//			for (UUID id : idsToDelete)
//			{
//				dispatchAfterRemove(id);
//			}
//		}
//	}
	
	public void remove(UUID id) throws Exception
	{
		if (id==null) return;
		
		if (canRemove(id)==false)
		{
			throw new SQLException("Bean cannot be removed");
		}
		
		dispatchBeforeRemove(id);
		
		// Get the table definition
		TableDef td = getTableDef();

		List<Object> params = new ArrayList<Object>(1);
		params.add(Util.uuidToBytes(id));

		Query q = new Query();

		// Delete from main table
		try
		{
			q.update("DELETE FROM " + td.getName() + " WHERE ID=?", params);
		}
		finally
		{
			q.close();
		}
		
		// Cache
		if (td.isCacheOnLoad() || td.isCacheOnSave())
		{
			Cache.invalidate("bean:" + td.getName() + "." + id.toString());
		}

		// Delete from Props table
		if (td.hasProps())
		{
			try
			{
				q.update("DELETE FROM Props WHERE LinkedID=?", params);
			}
			finally
			{
				q.close();
			}
		}
		
		// Delete from Images table
		if (td.hasImages())
		{
			ImageStore.getInstance().removeByLinkedID(id);
		}

		dispatchAfterRemove(id);

/*
		// Update cache of queries
		List<String> queryKeys = Cache.getPrefixKeys("bean.query:" + td.getName() + ".");
		for (String cacheKey : queryKeys)
		{
			List<UUID> cachedList = (List<UUID>) Cache.get(cacheKey);
			for (int pos=0; pos<cachedList.size(); pos++)
			{
				if (cachedList.get(pos).equals(id))
				{
					if (cachedList.size()<=1)
					{
						Cache.invalidate(cacheKey);
					}
					else
					{
						List<UUID> newList = new ArrayList<UUID>(cachedList.size()-1);
						newList.addAll(cachedList.subList(0, pos));
						newList.addAll(cachedList.subList(pos+1, cachedList.size()));
						Cache.insert(cacheKey, newList);
					}
					break;
				}
			}
		}
*/
	}
	
	protected boolean canRemoveMany(List<UUID> beanIDs) throws Exception
	{
		for (UUID id : beanIDs)
		{
			if (dispatchCanRemove(id)==false)
			{
				return false;
			}
		}
		return true;
	}

	public boolean canRemove(UUID beanID) throws Exception
	{
		return dispatchCanRemove(beanID);
	}
	
	protected List<UUID> queryAll() throws Exception
	{
		return queryAll(null, true);
	}
	
	protected List<UUID> queryAll(String sortColumn, boolean ascending) throws Exception
	{
		// Get the table definition
		TableDef td = getTableDef();

		String sql = "SELECT ID FROM " + td.getName();
		if (sortColumn!=null)
		{
			sql += " ORDER BY " + sortColumn;
			if (ascending)
			{
				sql += " ASC";
			}
			else
			{
				sql += " DESC";
			}
		}

		return Query.queryListUUID(sql, null);		
	}
	
	protected T openByColumn(String columnName, Object columnValue) throws Exception
	{
		T bean = loadByColumn(columnName, columnValue);
		return (T) (bean==null? null : bean.clone());
	}
	
	/**
	 * Queries the database for the bean matching the given column name and value,
	 * basically running a "SELECT * FROM Table WHERE ColNam=ColVal" query. 
	 * The bean returned by this method may be shared across threads. As such, it is read-only.
	 * To get a writable bean, <code>clone</code> the bean, or call {@link #open(UUID)}
	 * which performs the cloning internally.
	 * @param columnName The name of the column to query. Should be a unique index.
	 * @param columnValue The value to match. Must not be <code>null</code>.
	 * @return The bean, or <code>null</code> if the record was not found, or if more than one
	 * record was found for the query.
	 * @throws Exception
	 */
	protected T loadByColumn(String columnName, Object columnValue) throws Exception
	{
		if (columnValue==null)
		{
			return null;
		}
				
		TableDef td = getTableDef();
		
		// Check cache
		String cacheKey = "bean:" + td.getName() + "." + columnName + "=" + columnValue.hashCode();
		UUID cached = (UUID) Cache.get(cacheKey);
		if (cached!=null)
		{
			T bean = load(cached);
			if (bean!=null && bean.get(columnName).equals(columnValue)) // Verify the bean's column hasn't changed
			{
				return bean;
			}
			else
			{
				Cache.invalidate(cacheKey);
			}
		}
		
		// Load from disk
		List<UUID> ids = Query.queryListUUID("SELECT ID FROM " + td.getName() + " WHERE " + columnName + "=?", new ParameterList(columnValue));
		if (ids.size()==1)
		{
			T result = load(ids.get(0));
			if (result!=null)
			{
				Cache.insert(cacheKey, ids.get(0));
			}			
			return result;
		}
		else
		{
			return null;
		}
	}
	
	protected List<UUID> queryByColumn(String columnName, Object value) throws SQLException
	{
		return queryByColumn(columnName, value, null, false);
	}
	
	protected List<UUID> queryByColumn(String columnName, Object value, String sortColumnName, boolean ascending) throws SQLException
	{
		TableDef td = getTableDef();
		
		if (td.isColumn(columnName))
		{
			if (sortColumnName==null)
			{
				return Query.queryListUUID("SELECT ID FROM " + td.getName() + " WHERE " + columnName + "=?", new ParameterList(value));
			}
			else if (td.isColumn(sortColumnName))
			{
				return Query.queryListUUID("SELECT ID FROM " + td.getName() + " WHERE " + columnName + "=? ORDER BY " + sortColumnName + (ascending? " ASC" : " DESC"), new ParameterList(value));
			}
			else
			{
				// !$! Implement
			}
		}
		else
		{
			String valColumn = (value.toString().length()<=MAX_VALUE_LEN? "Val" : "ValText");
			if (value instanceof Date || value instanceof Integer || value instanceof Long || value instanceof Boolean)
			{
				valColumn = "ValNum";
			}
			else if (value instanceof UUID)
			{
				valColumn = "ValBin";
			}

			if (sortColumnName==null)
			{
				return Query.queryListUUID("SELECT t.ID FROM " + td.getName() + " AS t, Props AS p WHERE p.LinkedID=t.ID AND p.Name=? AND p." + valColumn + "=?", new ParameterList(columnName).plus(value));
			}
			else if (td.isColumn(sortColumnName))
			{
				// !$! Implement
			}
			else
			{
				// !$! Implement
			}
		}
		
		throw new UnsupportedOperationException();
	}
	
	protected QueryIterator<T> createQueryIterator(String sql, List<Object> params) throws SQLException
	{
		return new QueryIterator<T>(sql, params)
		{
			@Override
			protected T fromResultSet(ResultSet rs) throws Exception
			{
				T bean = getBeanClass().newInstance();
				DataBeanStoreUtil.resultSetToBean(rs, bean, getTableDef());
				bean.setWritable(false);
				bean.setSaved(true);
				return bean;
			}
		};
	}
		
/*
	protected List<UUID> getByColumn(String column, String operator, Object value, String sortColumn) throws Exception
	{
		if (value==null)
		{
			throw new NullPointerException();
		}
		if (sortColumn==null)
		{
			sortColumn = column;
		}
		
		// Get the table definition
		TableDef td = getTableDef();

		// Search for result in cache
		String cacheKey = "bean.query:" + td.getName() + "." + sortColumn + "." + column + "." + operator + "." + value.toString();
		List<UUID> cached = (List<UUID>) Cache.get(cacheKey);
		if (cached!=null)
		{
			return cached;
		}

		// Convert value, if needed
		if (value instanceof UUID)
		{
			value = Util.uuidToBytes((UUID) value);
		}
		else if (value instanceof Date)
		{
			value = ((Date) value).getTime();
		}
		else if (value instanceof Boolean)
		{
			value = ((Boolean) value)?1:0;
		}
		
		List<UUID> result = queryListUUID("SELECT ID FROM " + td.getName() + " WHERE " + column + " " + operator + " ? ORDER BY " + sortColumn + " ASC", new ParameterList(value));
		Cache.insert(cacheKey, result);
		return result;
	}
	
	protected List<UUID> getByProperty(String propName, Object value) throws Exception
	{
		if (value==null)
		{
			throw new NullPointerException();
		}
		
		// Get the table definition
		TableDef td = getTableDef();

		// Search for result in cache
		String cacheKey = "bean.query:" + td.getName() + "." + propName + ".=." + value.toString();
		List<UUID> cached = (List<UUID>) Cache.get(cacheKey);
		if (cached!=null)
		{
			return cached;
		}
		
		// Figure out column to query
		String colName = "Val";
		if (value instanceof String)
		{
			if (((String) value).length()<=MAX_VALUE_LEN)
			{
				colName = "Val";
			}
			else
			{
				colName = "ValText";
			}
		}
		else if (value instanceof Long ||
				value instanceof Integer ||
				value instanceof Short ||
				value instanceof Boolean ||
				value instanceof Date)
		{
			colName = "ValNum";
		}
		else if (value instanceof UUID)
		{
			colName = "ValBin";
			value = Util.uuidToBytes((UUID) value);
		}
		else if (value instanceof byte[])
		{
			if (((byte[]) value).length<=MAX_VALUE_LEN)
			{
				colName = "ValBin";
			}
			else
			{
				colName = "ValImage";
			}
		}
		else
		{
			throw new SQLException("Unknown type: " + td.getName() + " " + propName + " " + value.getClass().getName());
		}
		
		List<UUID> result = queryListUUID("SELECT LinkedID FROM Props WHERE LinkedTable=? AND Name=? AND " + colName + "=?", new ParameterList(td.getName()).plus(propName).plus(value));
		Cache.insert(cacheKey, result);
		return result;
	}
*/
}
