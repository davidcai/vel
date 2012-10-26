package samoyan.database;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import samoyan.core.Debug;

public final class TableDef
{
	private static Map<String, TableDef> instances = new ConcurrentHashMap<String, TableDef>();
	private static Map<String, DataBeanStore<? extends DataBean>> stores = new ConcurrentHashMap<String, DataBeanStore<? extends DataBean>>();
	
	private Map<String, PropDef> props = new HashMap<String, PropDef>();
	private Map<String, PropDef> cols = new HashMap<String, PropDef>();
	private boolean cacheOnSave = true;
	private boolean cacheOnLoad = true;
	
	private String name = "";
	
	public static TableDef newInstance(String name, DataBeanStore<? extends DataBean> store)
	{
		Debug.logln("TableDef: " + name);

		if (instances.containsKey(name))
		{
			throw new IllegalStateException("Table " + name + " is already defined");
		}

		stores.put(name, store);
		
		TableDef td = new TableDef(name);
		instances.put(name, td);
		return td;
	}
	public static TableDef getInstance(String name)
	{
		return instances.get(name);
	}
	public static DataBeanStore<? extends DataBean> getStore(String name)
	{
		DataBeanStore<? extends DataBean> store = stores.get(name);
		if (store==null)
		{
			throw new IllegalStateException("Table " + name + " not yet defined");
		}
		return store;
	}
	
	private TableDef(String name)
	{
		this.name = name;
	}
		
	public String getName()
	{
		return name;
	}
	
	public boolean isCacheOnSave()
	{
		return this.cacheOnSave;
	}

	public TableDef setCacheOnSave(boolean cacheOnSave)
	{
		this.cacheOnSave = cacheOnSave;
		return this;
	}

	public boolean isCacheOnLoad()
	{
		return this.cacheOnLoad;
	}

	public TableDef setCacheOnLoad(boolean cacheOnLoad)
	{
		this.cacheOnLoad = cacheOnLoad;
		return this;
	}

	public PropDef defineProp(String name, Class<?> type)
	{
		PropDef propDef = new PropDef(name, type);
		this.props.put(name, propDef);
		return propDef;
	}
	
	public PropDef defineCol(String name, Class<?> type)
	{
		PropDef propDef = new PropDef(name, type);
		this.cols.put(name, propDef);
		return propDef;
	}

	public PropDef getPropDef(String name)
	{
		return this.props.get(name);
	}
	public PropDef getColDef(String name)
	{
		return this.cols.get(name);
	}
	
	public Collection<PropDef> getCols()
	{
		return this.cols.values();
	}
	public Collection<PropDef> getProps()
	{
		return this.props.values();
	}
	
	public boolean hasColumns()
	{
		return this.cols.size()>0;
	}
	public boolean hasProps()
	{
		return this.props.size()>0;
	}
	public boolean hasImages()
	{
		for (PropDef pd : this.props.values())
		{
			if (pd.getType().equals(Image.class))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isColumn(String name)
	{
		return this.cols.containsKey(name);
	}
	
	public boolean isImage(String name)
	{
		PropDef pd = this.props.get(name);
		return (pd!=null && pd.getType().equals(Image.class));
	}
}
