package samoyan.core;

import java.util.HashMap;
import java.util.Map;

public class ParameterMap extends HashMap<String, String>
{
	public ParameterMap()
	{
	}
	
	public ParameterMap(Map<String, String> map)
	{
		super();
		super.putAll(map);
	}

	public ParameterMap(String n, String v)
	{
		super();
		super.put(n, v);
	}
	
	public ParameterMap plus(String n, String v)
	{
		super.put(n, v);
		return this;
	}
	
	public ParameterMap plus(Map<String, String> map)
	{
		super.putAll(map);
		return this;
	}
}
