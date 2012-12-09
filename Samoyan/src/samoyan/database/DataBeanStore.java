package samoyan.database;

import java.io.*;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import samoyan.core.Cache;
import samoyan.core.Day;
import samoyan.core.LocaleEx;
import samoyan.core.ParameterList;
import samoyan.core.TimeOfDay;
import samoyan.core.TimeZoneEx;
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
		return this.tableDef;
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
					colsResultSetIntoBean(bean, rs, td);
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
					Prop prop = resultSetToProp(rs);
					
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
	
	private void colsResultSetIntoBean(T bean, ResultSet rs, TableDef td) throws Exception
	{
		ResultSetMetaData meta = rs.getMetaData();
		int colCount = meta.getColumnCount();
		
		for (int i=1; i<=colCount; i++)
		{
			String col = meta.getColumnName(i);
			
			if (col.equalsIgnoreCase("ID"))
			{
				bean.setID(Util.bytesToUUID(rs.getBytes(col)));
				continue;
			}
					
			Prop prop = new Prop();
			prop.id = null;
			prop.name = col;
			prop.img = false;

			PropDef pd = td.getColDef(col);
			if (pd==null || pd.getType().equals(String.class))
			{
				prop.value = rs.getString(col);
			}
			else if (pd.getType().equals(Long.class))
			{
				prop.value = rs.getLong(col);
			}
			else if (pd.getType().equals(Integer.class))
			{
				prop.value = rs.getInt(col);
			}
			else if (pd.getType().equals(Short.class))
			{
				prop.value = rs.getShort(col);
			}
			else if (pd.getType().equals(Float.class))
			{
				prop.value = rs.getFloat(col);
			}
			else if (pd.getType().equals(Double.class))
			{
				prop.value = rs.getDouble(col);
			}
			else if (pd.getType().equals(Boolean.class))
			{
				int v = rs.getInt(col);
				if (!rs.wasNull())
				{
					prop.value = (v!=0);
				}
			}
			else if (pd.getType().equals(Date.class))
			{
				long v = rs.getLong(col);
				if (!rs.wasNull())
				{
					prop.value = new Date(v);
				}
			}
			else if (pd.getType().equals(Day.class))
			{
				long v = rs.getLong(col);
				if (!rs.wasNull())
				{
					prop.value = new Day(TimeZoneEx.GMT, new Date(v));
				}
			}
			else if (pd.getType().equals(TimeOfDay.class))
			{
				int v = rs.getInt(col);
				if (!rs.wasNull())
				{
					prop.value = new TimeOfDay(v);
				}
			}
			else if (pd.getType().equals(UUID.class))
			{
				byte[] v = rs.getBytes(col);
				if (!rs.wasNull())
				{
					prop.value = Util.bytesToUUID(v);
				}
			}
			else if (pd.getType().equals(InetAddress.class))
			{
				byte[] v = rs.getBytes(col);
				if (!rs.wasNull())
				{
					prop.value = InetAddress.getByAddress(v);
				}
			}
			else if (pd.getType().equals(byte[].class))
			{
				prop.value = rs.getBytes(col);
			}
			else if (pd.getType().equals(BitSet.class))
			{
//				prop.value = BitSet.valueOf(rs.getBytes(++c));
				prop.value = Util.bytesToBitSet(rs.getBytes(col)); // BitSet.valueOf is JDK 7, using Util to maintain JDK 6 compatibility
			}
			else if (pd.getType().equals(TimeZone.class))
			{
				String v = rs.getString(col);
				if (!rs.wasNull())
				{
					prop.value = TimeZone.getTimeZone(v);
				}
			}
			else if (pd.getType().equals(Locale.class))
			{
				String v = rs.getString(col);
				if (!rs.wasNull())
				{
					prop.value = LocaleEx.fromString(v);
				}
			}
			else if (classImplements(pd.getType(), Serializable.class))
			{
				prop.value = deserialize(rs.getBytes(col));
			}
			else
			{
				throw new SQLException("Unknown type: " + td.getName() + " " + prop.name + " " + pd.getType().getName());
			}

			if (rs.wasNull()==false)
			{
				bean.putReadProp(prop.name, prop);
			}
		}
	}
	
	private Prop resultSetToProp(ResultSet rs) throws Exception
	{
		Prop prop = new Prop();
		prop.id = Util.bytesToUUID(rs.getBytes("ID"));
		prop.name = rs.getString("Name");
		
		String type = rs.getString("Typ").trim();
		
//		PropDef pd = td.getPropDef(prop.name);
//		if (pd==null || pd.getType().equals(String.class))
		if (type.equals("Str"))
		{
			prop.value = rs.getString("ValStr");
		}
//		else if (pd.getType().equals(Long.class))
		else if (type.equals("Long"))
		{
			prop.value = rs.getLong("ValNum");
		}
//		else if (pd.getType().equals(Integer.class))
		else if (type.equals("Int"))
		{
			prop.value = rs.getInt("ValNum");
		}
//		else if (pd.getType().equals(Short.class))
		else if (type.equals("Shrt"))
		{
			prop.value = rs.getShort("ValNum");
		}
//		else if (pd.getType().equals(Float.class))
		else if (type.equals("Flot"))
		{
			prop.value = rs.getFloat("ValNum");
		}
//		else if (pd.getType().equals(Double.class))
		else if (type.equals("Dbl"))
		{
			prop.value = rs.getDouble("ValNum");
		}
//		else if (pd.getType().equals(Boolean.class))
		else if (type.equals("Bool"))
		{
			prop.value = (rs.getInt("ValNum")!=0L);
		}
//		else if (pd.getType().equals(Date.class))
		else if (type.equals("Date"))
		{
			prop.value = new Date(rs.getLong("ValNum"));
		}
//		else if (pd.getType().equals(Day.class))
		else if (type.equals("Day"))
		{
			prop.value = new Day(TimeZoneEx.GMT, new Date(rs.getLong("ValNum")));
		}
//		else if (pd.getType().equals(TimeOfDay.class))
		else if (type.equals("TmDy"))
		{
			prop.value = new TimeOfDay(rs.getInt("ValNum"));
		}
//		else if (pd.getType().equals(UUID.class))
		else if (type.equals("UUID"))
		{
			prop.value = Util.bytesToUUID(rs.getBytes("ValBytes"));
		}
//		else if (pd.getType().equals(InetAddress.class))
		else if (type.equals("INet"))
		{
			prop.value = InetAddress.getByAddress(rs.getBytes("ValBytes"));
		}
//		else if (pd.getType().equals(byte[].class))
		else if (type.equals("Bin"))
		{
			prop.value = rs.getBytes("ValBytes");
		}
//		else if (pd.getType().equals(BitSet.class))
		else if (type.equals("BitS"))
		{
//			prop.value = BitSet.valueOf(rs.getBytes"ValBytes"5));
			prop.value = Util.bytesToBitSet(rs.getBytes("ValBytes")); // BitSet.valueOf in JDK 7, using Util to maintain JDK 6 compatibility
		}
//		else if (pd.getType().equals(TimeZone.class))
		else if (type.equals("TmZn"))
		{
			prop.value = TimeZone.getTimeZone(rs.getString("ValStr"));
		}
		else if (type.equals("Locl"))
		{
			prop.value = LocaleEx.fromString(rs.getString("ValStr"));
		}
		else if (type.equals("Srlz"))
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(rs.getBytes("ValBytes"));
			ObjectInputStream ois = new ObjectInputStream(bais);
			prop.value = ois.readObject();
			ois.close();
			bais.close();
		}
		else
		{
//			throw new SQLException("Unknown type: " + beanClass.getName() + " " + prop.name + " " + pd.getType().getName());
			throw new SQLException("Unknown type: " + prop.name + " " + type);
		}

		return prop;
	}
	
	private static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException
	{
		if (bytes==null || bytes.length==0) return null;
		
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object result = ois.readObject();
		ois.close();
		bais.close();
		return result;
	}
	
	private static byte[] serialize(Object obj) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.close();
		baos.close();
		
		return baos.toByteArray();
	}
	
	private static boolean classImplements(Class<?> cls, Class<?> intrfc)
	{
		for (Class<?> i : cls.getInterfaces())
		{
			if (i.equals(intrfc))
			{
				return true;
			}
		}
		return false;
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
						throw new SQLException("Bean record not found in database for given ID");
					}
				}
				
				int c = 0;
				for (PropDef pd : td.getCols())
				{
					if (!insert && bean.isDirty(pd.getName())==false) continue;
											
					if (!insert && pd.isInvariant())
					{
						throw new SQLException("Invariant column: " + bean.getClass().getName() + " " + pd.getName());
					}
					
//					Prop readProp = bean.getReadProp(pd.getName()); // readProp may be null

					Object val = bean.get(pd.getName());					
					if (val==null)
					{
						if (!insert)
						{
							rs.updateNull(++c);
						}
						else
						{
							c++; // Allow default values defined in database to be used
						}
					}
					else if (pd==null || pd.getType().equals(String.class))
					{
						rs.updateString(++c, val.toString());
					}
					else if (pd.getType().equals(Long.class))
					{
						rs.updateLong(++c, (Long) val);
					}
					else if (pd.getType().equals(Integer.class))
					{
						rs.updateInt(++c, (Integer) val);
					}
					else if (pd.getType().equals(Short.class))
					{
						rs.updateShort(++c, (Short) val);
					}
					else if (pd.getType().equals(Float.class))
					{
						rs.updateFloat(++c, (Float) val);
					}
					else if (pd.getType().equals(Double.class))
					{
						rs.updateDouble(++c, (Double) val);
					}
					else if (pd.getType().equals(Boolean.class))
					{
						rs.updateInt(++c, ((Boolean) val)? 1 : 0);
					}
					else if (pd.getType().equals(Date.class))
					{
						rs.updateLong(++c, ((Date) val).getTime());
					}
					else if (pd.getType().equals(Day.class))
					{
						rs.updateLong(++c, ((Day) val).getDayStart(TimeZoneEx.GMT).getTime());
					}
					else if (pd.getType().equals(TimeOfDay.class))
					{
						rs.updateInt(++c, ((TimeOfDay) val).getSeconds());
					}
					else if (pd.getType().equals(UUID.class))
					{
						rs.updateBytes(++c, Util.uuidToBytes((UUID) val));
					}
					else if (pd.getType().equals(InetAddress.class))
					{
						byte[] bytes = ((InetAddress) val).getAddress();
						if (bytes.length==4)
						{
							// Convert IPv4 address to its IPv6 representation
							bytes = new byte[] {0, 0, 0, 0,   0, 0, 0, 0,   0, 0, (byte)0xff, (byte)0xff,   bytes[0], bytes[1], bytes[2], bytes[3]};
						}
						rs.updateBytes(++c, bytes);
					}
					else if (pd.getType().equals(TimeZone.class))
					{
						rs.updateString(++c, ((TimeZone) val).getID());
					}
					else if (pd.getType().equals(Locale.class))
					{
						rs.updateString(++c, ((Locale) val).toString());
					}
					else if (pd.getType().equals(byte[].class))
					{
						rs.updateBytes(++c, (byte[]) val);
					}
					else if (pd.getType().equals(BitSet.class))
					{
//						rs.updateBytes(++c, ((BitSet) val).toByteArray());
						rs.updateBytes(++c, Util.bitSetToBytes((BitSet) val)); // BitSet.toByteArray is JDK 7, using Util to maintain JDK 6 compatibility
					}
					else if (classImplements(pd.getType(), Serializable.class))
					{
						rs.updateBytes(++c, serialize(val));
					}
					else
					{
						throw new SQLException("Unknown type: " + bean.getClass().getName() + " " + pd.getName() + " " + pd.getType().getName());
					}
				}
				
				rs.updateBytes("ID", beanIDBytes);

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
				throw new SQLException("Invariant property: " + bean.getClass().getName() + " " + prop.name);
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
						ResultSet rs = q.updatableSelect("SELECT ID,LinkedID,Name,Val,ValText,ValBin,ValImage,ValNum,Typ FROM Props WHERE ID=?", params);
						if (insertProp)
						{
							rs.moveToInsertRow();
						}
						else
						{
							rs.next();
						}
	
						rs.updateBytes(2, beanIDBytes);
						rs.updateString(3, prop.name);
		
						if (prop.value instanceof String)
						{
							String val = (String) prop.value;
							if (val.length()<=MAX_VALUE_LEN)
							{
								// Store short values in Val (nvarchar 256)
								rs.updateString(4, val);
								rs.updateNull(5);
							}
							else
							{
								// Store long values in ValText (ntext)
								rs.updateNull(4);
								rs.updateString(5, val);
							}
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateNull(8);
							rs.updateString(9, "Str");
						}
						else if (prop.value instanceof Long)
						{
							Long val = (Long) prop.value;
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, val);
							rs.updateString(9, "Long");
						}
						else if (prop.value instanceof Integer)
						{
							Integer val = (Integer) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, (long) val);
							rs.updateString(9, "Int");
						}
						else if (prop.value instanceof Short)
						{
							Short val = (Short) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, (long) val);
							rs.updateString(9, "Shrt");
						}
						else if (prop.value instanceof Float)
						{
							Float val = (Float) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateFloat(8, (float) val);
							rs.updateString(9, "Flot");
						}
						else if (prop.value instanceof Double)
						{
							Double val = (Double) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateDouble(8, (double) val);
							rs.updateString(9, "Dbl");
						}
						else if (prop.value instanceof Boolean)
						{
							Boolean val = (Boolean) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, val?1L:0L);
							rs.updateString(9, "Bool");
						}
						else if (prop.value instanceof Date)
						{
							Date val = (Date) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, val.getTime());
							rs.updateString(9, "Date");
						}
						else if (prop.value instanceof Day)
						{
							Day val = (Day) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, val.getDayStart(TimeZoneEx.GMT).getTime());
							rs.updateString(9, "Day");
						}
						else if (prop.value instanceof TimeOfDay)
						{
							TimeOfDay val = (TimeOfDay) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateLong(8, val.getSeconds());
							rs.updateString(9, "TmDy");
						}
						else if (prop.value instanceof UUID)
						{
							UUID uuid = (UUID) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateBytes(6, Util.uuidToBytes(uuid));
							rs.updateNull(7);
							rs.updateNull(8);
							rs.updateString(9, "UUID");
						}
						else if (prop.value instanceof InetAddress)
						{
							InetAddress iNet = (InetAddress) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							rs.updateBytes(6, iNet.getAddress());
							rs.updateNull(7);
							rs.updateNull(8);
							rs.updateString(9, "INet");
						}
						else if (prop.value instanceof TimeZone)
						{
							TimeZone val = (TimeZone) bean.get(prop.name);
							rs.updateString(4, val.getID());
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateNull(8);
							rs.updateString(9, "TmZn");
						}
						else if (prop.value instanceof Locale)
						{
							Locale val = (Locale) bean.get(prop.name);
							rs.updateString(4, val.toString());
							rs.updateNull(5);
							rs.updateNull(6);
							rs.updateNull(7);
							rs.updateNull(8);
							rs.updateString(9, "Locl");
						}
						else if (prop.value instanceof byte[])
						{
							byte[] val = (byte[]) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							if (val.length<=MAX_VALUE_LEN)
							{
								// Store short values in ValBin (varbinary 256)
								rs.updateBytes(6, val);
								rs.updateNull(7);
							}
							else
							{
								// Store long values in ValImage (image)
								rs.updateNull(6);
								rs.updateBytes(7, val);
							}
							rs.updateNull(8);
							rs.updateString(9, "Bin");
						}
						else if (prop.value instanceof BitSet)
						{
							BitSet val = (BitSet) bean.get(prop.name);
							rs.updateNull(4);
							rs.updateNull(5);
							if (val.size()*8<=MAX_VALUE_LEN)
							{
								// Store short values in ValBin (varbinary 256)
	//							rs.updateBytes(6, val.toByteArray());
								rs.updateBytes(6, Util.bitSetToBytes(val)); // BitSet.toByteArray in JDK 7, using Util to maintain JDK 6 compatibility
								rs.updateNull(7);
							}
							else
							{
								// Store long values in ValImage (image)
								rs.updateNull(6);
	//							rs.updateBytes(7, val.toByteArray());
								rs.updateBytes(7, Util.bitSetToBytes(val)); // BitSet.toByteArray in JDK 7, using Util to maintain JDK 6 compatibility
							}
							rs.updateNull(8);
							rs.updateString(9, "BitS");
						}
						else if (prop.value instanceof Serializable)
						{
							byte[] val = serialize(prop.value);
							rs.updateNull(4);
							rs.updateNull(5);
							if (val.length<=MAX_VALUE_LEN)
							{
								// Store short values in ValBin (varbinary 256)
								rs.updateBytes(6, val);
								rs.updateNull(7);
							}
							else
							{
								// Store long values in ValImage (image)
								rs.updateNull(6);
								rs.updateBytes(7, val);
							}
							rs.updateNull(8);
							rs.updateString(9, "Srlz");
						}
						else
						{
							throw new SQLException("Unknown type: " + bean.getClass().getName() + " " + prop.name + " " + prop.value.getClass().getName());
						}
	
						rs.updateBytes(1, Util.uuidToBytes(prop.id));
											
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
	
/*	
	private void writeBeanIntoResultSet(ResultSet rs, T bean, TableDef td) throws IOException, SQLException
	{
		ResultSetMetaData meta = rs.getMetaData();
		int colCount = meta.getColumnCount();
	
		boolean insert = !bean.isSaved();
		
		for (int i=1; i<=colCount; i++)
		{
			String col = meta.getColumnName(i);
			Object val = bean.get(col);
			PropDef pd = td.getPropDef(col);
			
			if (!insert && pd.isInvariant())
			{
				throw new SQLException("Invariant column: " + bean.getClass().getName() + " " + col);
			}
			
			if (val==null)
			{
				if (!insert) // To allow default values defined in database to be used
				{
					rs.updateNull(col);
				}
			}
			else if (pd==null || pd.getType().equals(String.class))
			{
				rs.updateString(col, val.toString());
			}
			else if (pd.getType().equals(Long.class))
			{
				rs.updateLong(col, (Long) val);
			}
			else if (pd.getType().equals(Integer.class))
			{
				rs.updateInt(col, (Integer) val);
			}
			else if (pd.getType().equals(Short.class))
			{
				rs.updateShort(col, (Short) val);
			}
			else if (pd.getType().equals(Float.class))
			{
				rs.updateFloat(col, (Float) val);
			}
			else if (pd.getType().equals(Double.class))
			{
				rs.updateDouble(col, (Double) val);
			}
			else if (pd.getType().equals(Boolean.class))
			{
				rs.updateInt(col, ((Boolean) val)? 1 : 0);
			}
			else if (pd.getType().equals(Date.class))
			{
				rs.updateLong(col, ((Date) val).getTime());
			}
			else if (pd.getType().equals(UUID.class))
			{
				rs.updateBytes(col, Util.uuidToBytes((UUID) val));
			}
			else if (pd.getType().equals(InetAddress.class))
			{
				rs.updateBytes(col, ((InetAddress) val).getAddress());
			}
			else if (pd.getType().equals(TimeZone.class))
			{
				rs.updateString(col, ((TimeZone) val).getID());
			}
			else if (pd.getType().equals(byte[].class))
			{
				rs.updateBytes(col, (byte[]) val);
			}
			else if (pd.getType().equals(BitSet.class))
			{
//				rs.updateBytes(col, ((BitSet) val).toByteArray());
				rs.updateBytes(col, Util.bitSetToBytes((BitSet) val)); // BitSet.toByteArray in JDK 7, using Util to maintain JDK 6 compatibility
			}
			else if (pd.getType().equals(Serializable.class))
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(val);
				oos.close();
				baos.close();
				
				rs.updateBytes(col, baos.toByteArray());
			}
			else
			{
				throw new SQLException("Unknown type: " + bean.getClass().getName() + " " + col + " " + pd.getType().getName());
			}
		}
		
		if (insert)
		{
			rs.updateBytes("ID", Util.uuidToBytes(UUID.randomUUID()));
		}
	}
*/
	
	public void removeMany(List<UUID> beanIDs) throws Exception
	{
		for (UUID id : beanIDs)
		{
			remove(id);
		}
	}
	
	/**
	 * Performs a SQL query to determine which records to delete, then deletes them.
	 * @param sql A SQL SELECT query that results in a list of IDs of this data bean, e.g.
	 * "SELECT ID FROM MyTable WHERE Size>?".
	 * @param params Parameters to pass to the query.
	 * @throws Exception
	 */
	@Deprecated
	protected void removeByQuery(String sql, List<Object> params) throws Exception
	{
		// First, query the database for the IDs
		List<UUID> idsToDelete = Query.queryListUUID(sql, params);
		if (idsToDelete.size()==0)
		{
			return;
		}

		// Dispatch events
		if (eventHandlers!=null)
		{
			for (UUID id : idsToDelete)
			{
				dispatchBeforeRemove(id);
			}
		}

		// Get the table definition
		TableDef td = getTableDef();
		
		boolean onProps = sql.toUpperCase(Locale.US).matches("\bPROPS\b");
		boolean onSelf = sql.toUpperCase(Locale.US).matches("\b" + td.getName().toUpperCase(Locale.US) + "\b");
		if (onProps && onSelf && td.hasProps())
		{
			throw new IllegalArgumentException("Joined query on " + td.getName() + " and Props is not allowed in this context");
		}
		
		Query q = new Query();

		// Delete from Props table first, if the query is on the table itself
		if (onSelf && td.hasProps())
		{
			try
			{
				q.update("DELETE FROM Props WHERE LinkedID IN (" + sql + ")", params);
			}
			finally
			{
				q.close();
			}
		}
		
		// Delete from main table
		try
		{
			q.update("DELETE FROM " + td.getName() + " WHERE ID IN (" + sql + ")", params);
		}
		finally
		{
			q.close();
		}
		
		// Invalidate cache
		if (td.isCacheOnLoad() || td.isCacheOnSave())
		{
			for (UUID id : idsToDelete)
			{
				Cache.invalidate("bean:" + td.getName() + "." + id.toString());
			}
		}
		
		// Delete from Props table last, if the query was not on the table itself
		if (!onSelf && td.hasProps())
		{
			try
			{
				q.update("DELETE FROM Props WHERE LinkedID IN (" + sql + ")", params);
			}
			finally
			{
				q.close();
			}
		}

		// Dispatch events
		if (eventHandlers!=null)
		{
			for (UUID id : idsToDelete)
			{
				dispatchAfterRemove(id);
			}
		}
	}
	
	public void remove(UUID id) throws Exception
	{
		if (id==null) return;
		
		if (canRemoveBean(id)==false)
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

	public boolean canRemoveBean(UUID beanID) throws Exception
	{
		return dispatchCanRemove(beanID);
	}
	
	protected List<UUID> getAllBeanIDs() throws Exception
	{
		return getAllBeanIDs(null, true);
	}
	
	protected List<UUID> getAllBeanIDs(String sortColumn, boolean ascending) throws Exception
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
				colsResultSetIntoBean(bean, rs, getTableDef());
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
