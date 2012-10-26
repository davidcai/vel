package samoyan.database;

public class PropDef
{
	private String name;
	private Class<?> type;
	private int minSize = 0;
	private int maxSize = 0;
	private boolean column = false;
	private boolean invariant = false;
//	private Object defaultValue = null;
	private String ownerTable = null;
	private String referredTable = null;
	private int width = 0;
	private int height = 0;
	
	PropDef(String name, Class<?> type)
	{
		this.name = name;
		this.type = type;
	}
	
	// - - -
	
	public String getName()
	{
		return name;
	}
	public Class<?> getType()
	{
		return type;
	}
	public int getMinSize()
	{
		return minSize;
	}
	public int getMaxSize()
	{
		return maxSize;
	}
	public boolean isInvariant()
	{
		return this.invariant;
	}
//	public Object getDefaultValue()
//	{
//		return this.defaultValue;
//	}
	public int getWidth()
	{
		return width;
	}
	public int getHeight()
	{
		return height;
	}	
	/**
	 * 
	 * @return The name of the owner table, or <code>null</code>.
	 */
	public String getOwnedBy()
	{
		return this.ownerTable;
	}
	/**
	 * 
	 * @return The name of the table referring to, or <code>null</code>.
	 */
	public String getRefersTo()
	{
		return this.referredTable;
	}
	
	// - - -
	
	/**
	 * Define the min and max size of the string.
	 * @param min
	 * @param max
	 * @return
	 */
	public PropDef size(int min, int max)
	{
		this.minSize = min;
		this.maxSize = max;
		return this;
	}
	public PropDef type(Class<?> type)
	{
		this.type = type;
		return this;
	}
	public PropDef invariant()
	{
		this.invariant = true;
		return this;
	}
//	public PropDef defaultValue(Object defVal)
//	{
//		this.defaultValue = defVal;
//		return this;
//	}
	/**
	 * Define the max width and height an image.
	 * @param width
	 * @param height
	 * @return
	 */
	public PropDef dimensions(int width, int height)
	{
		this.width = width;
		this.height = height;
		return this;
	}
	/**
	 * Cause the record of this table to be removed when the record of the <code>ownerTable</code> is removed.
	 * @param ownerTable
	 * @return
	 */
	public PropDef ownedBy(String ownerTable)
	{
		this.ownerTable = ownerTable;
		return this;
	}
	/**
	 * Disallow the removal of a record in the <code>referredTable</code> while it has linked records in this table.
	 * @param referredTable
	 * @return
	 */
	public PropDef refersTo(String referredTable)
	{
		this.referredTable = referredTable;
		return this;
	}
}
