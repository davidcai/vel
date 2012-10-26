package samoyan.database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import samoyan.core.Debug;
import samoyan.core.Util;

public final class LinkTableDef
{
	private static Map<String, LinkTableDef> instances = new ConcurrentHashMap<String, LinkTableDef>();

	private String name = "";
	private String weightCol = "";
	private boolean cache = true;

	private LinkKeyDef key1 = null;
	private LinkKeyDef key2 = null;

	public static LinkTableDef newInstance(String name)
	{
		LinkTableDef td = new LinkTableDef(name);
		if (instances.containsKey(name))
		{
			throw new InstantiationError("LinkTable " + name + " is already defined");
		}
		Debug.logln("LinkTableDef: " + name);
		instances.put(name, td);
		return td;
	}
	public static LinkTableDef getInstance(String name)
	{
		return instances.get(name);
	}
	
	private LinkTableDef(String name)
	{
		this.name = name;
	}
		
	public String getName()
	{
		return name;
	}

	public LinkKeyDef getKey1()
	{
		return this.key1;
	}
	public LinkKeyDef setKey1(String keyCol1, String foreignTable1)
	{
		this.key1 = new LinkKeyDef(keyCol1, foreignTable1);
		return this.key1;
	}

	public LinkKeyDef getKey2()
	{
		return this.key2;
	}
	public LinkKeyDef setKey2(String keyCol2, String foreignTable2)
	{
		this.key2 = new LinkKeyDef(keyCol2, foreignTable2);
		return this.key2;
	}
	
	public String getWeightColumn()
	{
		return weightCol;
	}
	public void setWeightColumn(String weightCol)
	{
		this.weightCol = weightCol;
	}
	
	public boolean isCache()
	{
		return this.cache;
	}
	public void setCache(boolean cacheOnLoad)
	{
		this.cache = cacheOnLoad;
	}
	
	public boolean hasWeight()
	{
		return !Util.isEmpty(this.weightCol);
	}
}
