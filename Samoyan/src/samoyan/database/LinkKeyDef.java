package samoyan.database;

public class LinkKeyDef
{
	private String columnName = null;
	private String foreignTable = null;
	private boolean disallowRemoveIfHasLinks = false;

	LinkKeyDef(String keyCol1, String foreignTable1)
	{
		this.columnName = keyCol1;
		this.foreignTable = foreignTable1;		
	}

	// - - -
	
	public boolean isDisallowRemoveIfHasLinks()
	{
		return disallowRemoveIfHasLinks;
	}
	
	public String getColumnName()
	{
		return columnName;
	}

	public String getForeignTable()
	{
		return foreignTable;
	}

	// - - -
	
	public LinkKeyDef disallowRemoveIfHasLinks()
	{
		disallowRemoveIfHasLinks = true;
		return this;
	}	
}
