package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public final class CheckItem extends DataBean
{
	public static final int MAXSIZE_TEXT = 256;
	
	public String getText()
	{
		return (String) get("Text");
	}
	public void setText(String desc)
	{
		set("Text", desc);
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
