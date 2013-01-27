package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public final class CheckItem extends DataBean
{
	public static final int MAXSIZE_TEXT = 512;
	public static final int MAXSIZE_LINK = 512;
	
	public String getText()
	{
		return (String) get("Text");
	}
	public void setText(String desc)
	{
		set("Text", desc);
	}
	
	public String getLink()
	{
		return (String) get("Link");
	}
	public void setLink(String link)
	{
		set("Link", link);
	}

	public UUID getChecklistID()
	{
		return (UUID) get("ChecklistID");
	}
	public void setChecklistID(UUID checklistID)
	{
		set("ChecklistID", checklistID);
	}
	
	public int getOrderSequence()
	{
		return (Integer) get("OrderSeq", 0);
	}
	public void setOrderSequence(int seq)
	{
		set("OrderSeq", seq);
	}
}
