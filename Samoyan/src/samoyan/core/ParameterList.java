package samoyan.core;

import java.util.ArrayList;

public class ParameterList extends ArrayList<Object>
{
	public ParameterList()
	{
	}
	
	public ParameterList(Object v)
	{
		super();
		super.add(v);
	}
	
	public ParameterList plus(Object v)
	{
		super.add(v);
		return this;
	}
}
