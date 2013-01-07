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
		if (map!=null)
		{
			super.putAll(map);
		}
	}

	public ParameterMap(String n, Object v)
	{
		super();
		if (n!=null && v!=null)
		{
			super.put(n, v.toString());
		}
	}
	
	public ParameterMap plus(String n, Object v)
	{
		if (n!=null && v!=null)
		{
			super.put(n, v.toString());
		}
		return this;
	}
	
	public ParameterMap plus(Map<String, String> map)
	{
		if (map!=null)
		{
			super.putAll(map);
		}
		return this;
	}
}
