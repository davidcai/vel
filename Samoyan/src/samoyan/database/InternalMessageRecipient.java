package samoyan.database;

import java.util.UUID;

public final class InternalMessageRecipient extends DataBean
{
	public UUID getInternalMessageID()
	{
		return (UUID) get("InternalMessageID");
	}
	public void setInternalMessageID(UUID msgID)
	{
		set("InternalMessageID", msgID);
	}

	public UUID getRecipientUserID()
	{
		return (UUID) get("RecipientUserID");
	}
	public void setRecipientUserID(UUID userID)
	{
		set("RecipientUserID", userID);
	}

	public boolean isRecipientDeleted()
	{
		return (Boolean) get("RecipientDeletedFlag", false);
	}
	public void setRecipientDeleted(boolean b)
	{
		set("RecipientDeletedFlag", b);
	}
}
