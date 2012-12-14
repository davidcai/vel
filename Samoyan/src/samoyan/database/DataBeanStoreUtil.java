package samoyan.database;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import samoyan.core.Day;
import samoyan.core.LocaleEx;
import samoyan.core.TimeOfDay;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;

class DataBeanStoreUtil
{
	/**
	 * Takes a Java object and converts it to the object that will get stored in the database.
	 * For example, a <code>TimeZone</code> is persisted as the <code>String</code> representation of the time zone ID.
	 * @param javaObject
	 * @return The converted object.
	 */
	public static Object convertJavaToSQL(Object javaObject)
	{
		Object convertedValue = javaObject;
		if (javaObject instanceof UUID)
		{
			convertedValue = Util.uuidToBytes((UUID) javaObject);
		}
		else if (javaObject instanceof Boolean)
		{
			convertedValue = ((Boolean)javaObject)?1:0;
		}
		else if (javaObject instanceof Date)
		{
			convertedValue = ((Date) javaObject).getTime();
		}
		else if (javaObject instanceof Day)
		{
			convertedValue = ((Day) javaObject).getDayStart(TimeZoneEx.GMT).getTime();
		}
		else if (javaObject instanceof TimeOfDay)
		{
			convertedValue = ((TimeOfDay) javaObject).getSeconds();
		}
		else if (javaObject instanceof TimeZone)
		{
			convertedValue = ((TimeZone) javaObject).getID();
		}
		else if (javaObject instanceof Locale)
		{
			convertedValue = ((Locale) javaObject).toString();
		}
		else if (javaObject instanceof InetAddress)
		{
			convertedValue = ((InetAddress) javaObject).getAddress();
		}
		else if (javaObject instanceof BitSet)
		{
			//convertedValue = ((BitSet) value).toByteArray();
			convertedValue = Util.bitSetToBytes((BitSet)javaObject);
		}
		return convertedValue;
	}
	
	/**
	 * Reads all columns from the result set, converting them into <code>Prop</code>s that are added to the <code>DataBean</code>.
	 * @param rs The <code>ResultSet</code> of the primary table being queried.
	 * @param bean The bean.
	 * @param td The table definition.
	 * @return
	 * @throws Exception
	 */
	public static void resultSetToBean(ResultSet rs, DataBean bean, TableDef td) throws Exception
	{
		ResultSetMetaData meta = rs.getMetaData();
		int colCount = meta.getColumnCount();
		
		for (int i=1; i<=colCount; i++)
		{
			String col = meta.getColumnName(i);			
			if (col.equalsIgnoreCase("ID"))
			{
				bean.setID(Util.bytesToUUID(rs.getBytes("ID")));					
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
				throw new SQLException("Unknown type " + pd.getType().getName() + " for " + td.getName() + "." + prop.name);
			}

			if (rs.wasNull()==false)
			{
				bean.putReadProp(prop.name, prop);
			}
		}
	}


	/**
	 * Updates the primary result set from the values stored in the bean.
	 * @param rs The <code>ResultSet</code> of the primary table being queried.
	 * @param bean The bean.
	 * @param td The table definition.
	 * @throws Exception
	 */
	public static void beanToResultSet(DataBean bean, ResultSet rs, TableDef td) throws Exception
	{
		ResultSetMetaData meta = rs.getMetaData();
		int colCount = meta.getColumnCount();
		
		for (int i=1; i<=colCount; i++)
		{
			String col = meta.getColumnName(i);
			PropDef pd = td.getColDef(col);
			
			Object val = bean.get(col);
			if (val==null)
			{
				if (bean.isSaved()) // Allow default values defined in database to be used on initial insert
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
			else if (pd.getType().equals(Day.class))
			{
				rs.updateLong(col, ((Day) val).getDayStart(TimeZoneEx.GMT).getTime());
			}
			else if (pd.getType().equals(TimeOfDay.class))
			{
				rs.updateInt(col, ((TimeOfDay) val).getSeconds());
			}
			else if (pd.getType().equals(UUID.class))
			{
				rs.updateBytes(col, Util.uuidToBytes((UUID) val));
			}
			else if (pd.getType().equals(InetAddress.class))
			{
				byte[] bytes = ((InetAddress) val).getAddress();
				if (bytes.length==4)
				{
					// Convert IPv4 address to its IPv6 representation
					bytes = new byte[] {0, 0, 0, 0,   0, 0, 0, 0,   0, 0, (byte)0xff, (byte)0xff,   bytes[0], bytes[1], bytes[2], bytes[3]};
				}
				rs.updateBytes(col, bytes);
			}
			else if (pd.getType().equals(TimeZone.class))
			{
				rs.updateString(col, ((TimeZone) val).getID());
			}
			else if (pd.getType().equals(Locale.class))
			{
				rs.updateString(col, ((Locale) val).toString());
			}
			else if (pd.getType().equals(byte[].class))
			{
				rs.updateBytes(col, (byte[]) val);
			}
			else if (pd.getType().equals(BitSet.class))
			{
//				rs.updateBytes(col, ((BitSet) val).toByteArray());
				rs.updateBytes(col, Util.bitSetToBytes((BitSet) val)); // BitSet.toByteArray is JDK 7, using Util to maintain JDK 6 compatibility
			}
			else if (classImplements(pd.getType(), Serializable.class))
			{
				rs.updateBytes(col, serialize(val));
			}
			else
			{
				throw new SQLException("Unknown type " + pd.getType().getName() + " for " + td.getName() + "." + pd.getName());
			}
		}
		
		rs.updateBytes("ID", Util.uuidToBytes(bean.getID()));
	}
	
	/**
	 * Read the value stored in the result set, converting it into a <code>Prop</code> and adding it to a <code>DataBean</code>.
	 * @param rs The <code>ResultSet</code> of the Props table being queried.
	 * @param prop The bean's property.
	 * @param td The table definition.
	 * @return
	 * @throws Exception
	 */
	public static void resultSetToProp(ResultSet rs, Prop prop, TableDef td) throws Exception
	{
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
//			prop.value = BitSet.valueOf(rs.getBytes("ValBytes"));
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
//			throw new SQLException("Unknown type " + pd.getType().getName() + " for " + td.getName() + "." + prop.name);
			throw new SQLException("Unknown type " + type + " for " + td.getName() + "." + prop.name);
		}
	}

	/**
	 * Updates the Props result set from the values stored in the bean.
	 * @param rs The <code>ResultSet</code> of the Props table being queried.
	 * @param prop The bean's property.
	 * @param td The table definition.
	 * @throws Exception
	 */
	public static void propToResultSet(Prop prop, ResultSet rs, TableDef td, UUID beanID) throws Exception
	{
		final int MAX_VALUE_LEN = 256;
		
		rs.updateBytes("LinkedID", Util.uuidToBytes(beanID));
		rs.updateString("Name", prop.name);

		if (prop.value instanceof String)
		{
			String val = (String) prop.value;
			if (val.length()<=MAX_VALUE_LEN)
			{
				// Store short values in Val (nvarchar 256)
				rs.updateString("Val", val);
				rs.updateNull("ValText");
			}
			else
			{
				// Store long values in ValText (ntext)
				rs.updateNull("Val");
				rs.updateString("ValText", val);
			}
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateNull("ValNum");
			rs.updateString("Typ", "Str");
		}
		else if (prop.value instanceof Long)
		{
			Long val = (Long) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", val);
			rs.updateString("Typ", "Long");
		}
		else if (prop.value instanceof Integer)
		{
			Integer val = (Integer) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", (long) val);
			rs.updateString("Typ", "Int");
		}
		else if (prop.value instanceof Short)
		{
			Short val = (Short) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", (long) val);
			rs.updateString("Typ", "Shrt");
		}
		else if (prop.value instanceof Float)
		{
			Float val = (Float) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateFloat("ValNum", (float) val);
			rs.updateString("Typ", "Flot");
		}
		else if (prop.value instanceof Double)
		{
			Double val = (Double) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateDouble("ValNum", (double) val);
			rs.updateString("Typ", "Dbl");
		}
		else if (prop.value instanceof Boolean)
		{
			Boolean val = (Boolean) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", val?1L:0L);
			rs.updateString("Typ", "Bool");
		}
		else if (prop.value instanceof Date)
		{
			Date val = (Date) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", val.getTime());
			rs.updateString("Typ", "Date");
		}
		else if (prop.value instanceof Day)
		{
			Day val = (Day) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", val.getDayStart(TimeZoneEx.GMT).getTime());
			rs.updateString("Typ", "Day");
		}
		else if (prop.value instanceof TimeOfDay)
		{
			TimeOfDay val = (TimeOfDay) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateLong("ValNum", val.getSeconds());
			rs.updateString("Typ", "TmDy");
		}
		else if (prop.value instanceof UUID)
		{
			UUID uuid = (UUID) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateBytes("ValBin", Util.uuidToBytes(uuid));
			rs.updateNull("ValImage");
			rs.updateNull("ValNum");
			rs.updateString("Typ", "UUID");
		}
		else if (prop.value instanceof InetAddress)
		{
			InetAddress iNet = (InetAddress) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			rs.updateBytes("ValBin", iNet.getAddress());
			rs.updateNull("ValImage");
			rs.updateNull("ValNum");
			rs.updateString("Typ", "INet");
		}
		else if (prop.value instanceof TimeZone)
		{
			TimeZone val = (TimeZone) prop.value;
			rs.updateString("Val", val.getID());
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateNull("ValNum");
			rs.updateString("Typ", "TmZn");
		}
		else if (prop.value instanceof Locale)
		{
			Locale val = (Locale) prop.value;
			rs.updateString("Val", val.toString());
			rs.updateNull("ValText");
			rs.updateNull("ValBin");
			rs.updateNull("ValImage");
			rs.updateNull("ValNum");
			rs.updateString("Typ", "Locl");
		}
		else if (prop.value instanceof byte[])
		{
			byte[] val = (byte[]) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			if (val.length<=MAX_VALUE_LEN)
			{
				// Store short values in ValBin (varbinary 256)
				rs.updateBytes("ValBin", val);
				rs.updateNull("ValImage");
			}
			else
			{
				// Store long values in ValImage (image)
				rs.updateNull("ValBin");
				rs.updateBytes("ValImage", val);
			}
			rs.updateNull("ValNum");
			rs.updateString("Typ", "Bin");
		}
		else if (prop.value instanceof BitSet)
		{
			BitSet val = (BitSet) prop.value;
			rs.updateNull("Val");
			rs.updateNull("ValText");
			if (val.size()*8<=MAX_VALUE_LEN)
			{
				// Store short values in ValBin (varbinary 256)
//				rs.updateBytes("ValBin", val.toByteArray());
				rs.updateBytes("ValBin", Util.bitSetToBytes(val)); // BitSet.toByteArray in JDK 7, using Util to maintain JDK 6 compatibility
				rs.updateNull("ValImage");
			}
			else
			{
				// Store long values in ValImage (image)
				rs.updateNull("ValBin");
//				rs.updateBytes("ValImage", val.toByteArray());
				rs.updateBytes("ValImage", Util.bitSetToBytes(val)); // BitSet.toByteArray in JDK 7, using Util to maintain JDK 6 compatibility
			}
			rs.updateNull("ValNum");
			rs.updateString("Typ", "BitS");
		}
		else if (prop.value instanceof Serializable)
		{
			byte[] val = serialize(prop.value);
			rs.updateNull("Val");
			rs.updateNull("ValText");
			if (val.length<=MAX_VALUE_LEN)
			{
				// Store short values in ValBin (varbinary 256)
				rs.updateBytes("ValBin", val);
				rs.updateNull("ValImage");
			}
			else
			{
				// Store long values in ValImage (image)
				rs.updateNull("ValBin");
				rs.updateBytes("ValImage", val);
			}
			rs.updateNull("ValNum");
			rs.updateString("Typ", "Srlz");
		}
		else
		{
			throw new SQLException("Unknown type " + prop.value.getClass().getName() + " for " + td.getName() + "." + prop.name);
		}

		rs.updateBytes("ID", Util.uuidToBytes(prop.id));
	}
	
	// - - - - -
	
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
}
