package samoyan.database;

import java.io.InputStream;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.*;

import samoyan.core.DateFormatEx;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.servlet.Controller;

public abstract class CsvDataBeanStore<T extends DataBean> extends DataBeanStore<T>
{
	private Map<UUID, T> instances = null;
	
	private void readAllBeansFromFile() throws Exception
	{
		if (this.instances!=null) return;
		
		TableDef td = getTableDef();
		
		InputStream stm = Controller.getResourceAsStream("WEB-INF/csvdb/" + td.getName() + ".csv");
		String text = Util.inputStreamToString(stm, "UTF-8");
		List<String> rows = Util.tokenize(text, "\n");
		
		Map<UUID, T> result = new HashMap<UUID, T>(rows.size());
		
		List<String> columnNames = Util.tokenize(rows.get(0), "\t");
		for (int r=1; r<rows.size(); r++)
		{
			String row = rows.get(r);
			if (row.matches("\\s*")) continue; // Skip empty rows, most likely at the end of the file
			
			T bean = getBeanClass().newInstance();
			
			List<String> columnValues = Util.tokenize(row, "\t");
			for (int c=0; c<columnValues.size() && c<columnNames.size(); c++)
			{
				String col = columnNames.get(c).trim();
				if (col.startsWith("\"") && col.endsWith("\""))
				{
					col = col.substring(1, col.length()-1);
				}
				col = col.trim();
				
				String val = columnValues.get(c).trim();
				if (val.startsWith("\"") && val.endsWith("\""))
				{
					val = val.substring(1, val.length()-1);
				}
				val = val.trim();
				if (Util.isEmpty(val))
				{
					continue;
				}
				
				if (col.equalsIgnoreCase("ID") && Util.isUUID(val))
				{
					bean.setID(UUID.fromString(val));
					continue;
				}
				
				Prop prop = new Prop();
				prop.id = null;
				prop.name = col;
				prop.img = false;

				PropDef pd = td.getColDef(col);
				if (pd==null || pd.getType().equals(String.class))
				{
					prop.value = val;
				}
				else if (pd.getType().equals(Long.class))
				{
					prop.value = Long.parseLong(val);
				}
				else if (pd.getType().equals(Integer.class))
				{
					prop.value = Integer.parseInt(val);
				}
				else if (pd.getType().equals(Short.class))
				{
					prop.value = Short.parseShort(val);
				}
				else if (pd.getType().equals(Float.class))
				{
					prop.value = Float.parseFloat(val);
				}
				else if (pd.getType().equals(Double.class))
				{
					prop.value = Double.parseDouble(val);
				}
				else if (pd.getType().equals(Boolean.class))
				{
					prop.value = Boolean.parseBoolean(val);
				}
				else if (pd.getType().equals(Date.class))
				{
					DateFormat df = DateFormatEx.getSimpleInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US, TimeZoneEx.GMT);
					prop.value = df.parse(val);
				}
				else if (pd.getType().equals(UUID.class))
				{
					prop.value = UUID.fromString(val);
				}
				else if (pd.getType().equals(InetAddress.class))
				{
					prop.value = InetAddress.getByName(val);
				}
				else if (pd.getType().equals(byte[].class))
				{
					prop.value = Util.hexStringToByteArray(val);
				}
				else if (pd.getType().equals(BitSet.class))
				{
//					prop.value = BitSet.valueOf(Util.hexStringToByteArray(val));
					prop.value = Util.bytesToBitSet(Util.hexStringToByteArray(val)); // BitSet.valueOf in JDK 7, using Util to maintain JDK 6 compatibility
				}
				else if (pd.getType().equals(TimeZone.class))
				{
					prop.value = TimeZone.getTimeZone(val);
				}
				else
				{
					throw new Exception("Unknown type: " + td.getName() + " " + prop.name + " " + pd.getType().getName());
				}

				bean.putReadProp(prop.name, prop);
			}
			
			if (bean.getID()!=null)
			{
				bean.setWritable(false);
				bean.setSaved(true);
				result.put(bean.getID(), bean);
			}
		}
		
		this.instances = result;
	}
	
	@Override
	public T load(UUID id) throws Exception
	{
		if (id==null) return null;
		readAllBeansFromFile();
		return instances.get(id);
	}

	@Override
	protected T loadByColumn(String columnName, Object columnValue) throws Exception
	{
		if (columnValue==null) return null;

		readAllBeansFromFile();
		
		for (T bean : this.instances.values())
		{
			if (Util.objectsEqual(bean.get(columnName), columnValue))
			{
				return bean;
			}
		}

		return null;
	}
	
	protected List<UUID> queryListUUIDByColumn(String columnName, Object columnValue) throws Exception
	{
		if (columnValue==null) return null;

		readAllBeansFromFile();
		
		List<UUID> result = new ArrayList<UUID>();
		for (T bean : this.instances.values())
		{
			if (Util.objectsEqual(bean.get(columnName), columnValue))
			{
				result.add(bean.getID());
			}
		}
		
		return result;
	}

	@Override
	public List<UUID> getAllBeanIDs() throws Exception
	{
		readAllBeansFromFile();
		return new ArrayList<UUID>(this.instances.keySet());
	}
	
	// - - -
	
	@Override
	public T open(UUID id) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void save(T bean) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void removeByQuery(String sql, List<Object> params) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(UUID id) throws Exception
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected T openByColumn(String columnName, Object columnValue) throws Exception
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected List<UUID> getAllBeanIDs(String sortColumn, boolean ascending) throws Exception
	{
		throw new UnsupportedOperationException();
	}
}
