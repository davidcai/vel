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

	public ParameterMap(String n, Object v)
	{
		super();
		super.put(n, v.toString());
	}
	
	public ParameterMap plus(String n, Object v)
	{
		super.put(n, v.toString());
		return this;
	}
	
	public ParameterMap plus(Map<String, String> map)
	{
		super.putAll(map);
		return this;
	}
}
