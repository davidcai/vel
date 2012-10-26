package samoyan.servlet.exc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WebFormException extends Exception
{
	private Set<String> fields = new HashSet<String>(1);
	private String message = null;
	
	public WebFormException(String field, String message)
	{
		if (field!=null)
		{
			this.fields.add(field);
		}
		this.message = message;
	}
	
	public WebFormException(String[] fields, String message)
	{
		if (fields!=null)
		{
			for (int f=0; f<fields.length; f++)
			{
				this.fields.add(fields[f]);
			}
		}
		this.message = message;
	}

	public WebFormException(List<String> fields, String message)
	{
		if (fields!=null)
		{
			this.fields.addAll(fields);
		}
		this.message = message;
	}
	
	public WebFormException(String message)
	{
		this.message = message;
	}

	public Set<String> getFields()
	{
		return fields;
	}

	public String getMessage()
	{
		return message;
	}
}
