package samoyan.database;

import java.util.*;

import samoyan.core.Debug;
import samoyan.core.Util;

public abstract class DataBean implements Cloneable
{
	private UUID id = UUID.randomUUID();
	private HashMap<String, Prop> readProps = null;
	private HashMap<String, Prop> writeProps = null;
	private boolean writable = true;
	private boolean saved = false;
	
	/**
	 * Creates a writable clone of the bean.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		DataBean clone = (DataBean) super.clone();
		if (clone.writeProps!=null)
		{
			clone.writeProps = (HashMap<String, Prop>) clone.writeProps.clone();
		}
		clone.writable = true;
		return clone;
	}

	public final UUID getID()
	{
		return this.id;
	}
	final void setID(UUID id)
	{
		this.id = id;
	}
	
	public final boolean isSaved()
	{
		return this.saved;
	}
	final void setSaved(boolean b)
	{
		this.saved = b;
	}
	public final boolean isDirty()
	{
		return this.writeProps!=null;
	}
	public final boolean isWritable()
	{
		return this.writable;
	}
	final void setWritable(boolean b)
	{
		this.writable = b;
	}
	
	final void clearDirty()
	{
		if (this.writeProps==null)
		{
			return;
		}

		// Copy writeProps to readProps
		// (Must not change the original readProps since they can be shared)
		HashMap<String, Prop> newReadProps = new HashMap<String, Prop>();
		if (this.readProps!=null)
		{
			newReadProps.putAll(this.readProps);
		}
		for (Prop p : this.writeProps.values())
		{
			if (p.value==null && !p.img)
			{
				newReadProps.remove(p.name);
			}
			else
			{
				newReadProps.put(p.name, p);
			}
		}
		this.readProps = newReadProps;
		
		// Clear writeProps
		this.writeProps = null;		
	}
		
	final Collection<Prop> getDirtyProps()
	{
		if (this.writeProps==null)
		{
			return null;
		}
		else
		{
			return this.writeProps.values();
		}
	}
	
	final void putReadProp(String name, Prop prop)
	{
		if (this.readProps==null)
		{
			this.readProps = new HashMap<String, Prop>();
		}
		this.readProps.put(name, prop);
	}
	final Prop getReadProp(String name)
	{
		if (this.readProps==null)
		{
			return null;
		}
		else
		{
			return this.readProps.get(name);
		}
	}
		
	// - - - - -

	/**
	 * Initializes the value of a property. To be called from the constructor of the bean only.
	 * @param name
	 * @param value
	 */
	protected final void init(String name, Object value)
	{
		if (name==null) return;
		
		// Create the readProps map if needed
		if (this.readProps==null)
		{
			this.readProps = new HashMap<String, Prop>();
		}
				
		// Create the new prop
		Prop prop = new Prop();
		prop.id = null;
		prop.name = name;
		prop.value = value;
		prop.img = false;
		
		this.readProps.put(name, prop);
	}

	/**
	 * Sets the value of the named property.
	 * @param name
	 * @param value
	 * @return
	 */
	protected final void set(String name, Object value)
	{
		if (this.isWritable()==false)
		{
			throw new IllegalStateException("Bean is not writable");
		}
		
		if (name==null) return;
		
		// Create the writeProps map if needed
		if (this.writeProps==null)
		{
			this.writeProps = new HashMap<String, Prop>();
		}
		
//		// See if the prop is already in the writeProps
//		Prop prop = this.writeProps.get(name);
//		if (prop!=null)
//		{
//			prop.value = value;
//			return;
//		}
//		
//		// Find the ID of the original prop
//		UUID id = null;
//		if (this.readProps!=null)
//		{
//			prop = this.readProps.get(name);
//			if (prop!=null)
//			{
//				id = prop.id;
//			}
//		}
		
		// Check if the prop value was changed
		if (this.readProps!=null)
		{
			Prop readProp = this.readProps.get(name);
			if (readProp!=null && !readProp.img && Util.objectsEqual(readProp.value, value))
			{
				return; // Same value re-entered. Don't set.
			}
			if (readProp!=null && readProp.img && value!=null && value instanceof Image)
			{
				Image img = (Image) value;
				if (Util.objectsEqual(readProp.id, img.getID()))
				{
					return; // Same image re-entered. Don't set.
				}
			}
		}
	
		// Create the new prop
		Prop prop = new Prop();
//		prop.id = id;
		prop.id = null;
		prop.name = name;
		prop.value = value;
		prop.img = (value!=null && value instanceof Image);
		
		this.writeProps.put(name, prop);
	}
	
	/**
	 * Gets the value of the named property.
	 * @param name
	 * @return
	 */
	protected final Object get(String name)
	{
		return get(name, null);
	}
	
	/**
	 * Gets the value of the named property, or the <code>defaultValue</code> if the property had no value set.
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	protected final Object get(String name, Object defaultValue)
	{
		if (name==null) return null;
		
		// Look in writeProps
		if (this.writeProps!=null)
		{
			Prop prop = this.writeProps.get(name);
			if (prop!=null)
			{
				return prop.value;
			}
		}
		
		// Look in readProps
		if (this.readProps!=null)
		{
			Prop prop = this.readProps.get(name);
			if (prop!=null)
			{
				if (prop.img)
				{
					try
					{
						return ImageStore.getInstance().load(prop.id);
					}
					catch (Exception e)
					{
						Debug.logStackTrace(e);
						return null;
					}
				}
				else
				{
					return prop.value;
				}
			}
		}
				
		return defaultValue;
	}
	
	/**
	 * Indicates if the named property has changed since this bean was loaded.
	 * @param name
	 * @return
	 */
	protected final boolean isDirty(String name)
	{
		if (this.writeProps==null)
		{
			return false;
		}
		
		return this.writeProps.containsKey(name);
	}
	
	/**
	 * Clears the named property of the bean.
	 * @param name
	 */
	protected final void clear(String name)
	{
		set(name, null);
	}
	
	/**
	 * Returns the set of the names of all properties of this bean.
	 * @return
	 */
	protected final Set<String> names()
	{
		Set<String> names = new HashSet<String>();
		if (this.readProps!=null)
		{
			names.addAll(this.readProps.keySet());
		}
		if (this.writeProps!=null)
		{
			names.addAll(this.writeProps.keySet());
		}
		return names;
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(this.getClass().getName());
		buf.append("\r\n");
		buf.append("ID=");
		if (this.id!=null)
		{
			buf.append(this.id.toString());
		}
		else
		{
			buf.append("null");
		}
		buf.append("\r\n");
		for (String n : names())
		{
			Object v = get(n);
			if (v==null) continue;
			
			buf.append(n);
			buf.append("=");
			buf.append(v.toString());
			buf.append("\r\n");
		}
		return buf.toString();
	}
}
