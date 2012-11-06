package samoyan.database;

import java.util.Date;
import java.util.UUID;

public final class InternalMessage extends DataBean
{
	public final static int MAXSIZE_SUBJECT = 256;
	
	public InternalMessage()
	{
		init("ThreadID", UUID.randomUUID());
		init("CreatedDate", new Date());
	}
	
	public String getSubject()
	{
		return (String) get("Subject");
	}
	public void setSubject(String subject)
	{
		set("Subject", subject);
	}
	
	public String getBody()
	{
		return (String) get("Body");
	}
	public void setBody(String body)
	{
		set("Body", body);
	}
	
	public UUID getSenderUserID()
	{
		return (UUID) get("SenderUserID");
	}
	public void setSenderUserID(UUID userID)
	{
		set("SenderUserID", userID);
	}
	
	public Date getCreatedDate()
	{
		return (Date) get("CreatedDate");
	}
	public void setCreatedDate(Date created)
	{
		set("CreatedDate", created);
	}
	
	public boolean isRead()
	{
		return (Boolean) get("ReadFlag", false);
	}
	public void setRead(boolean b)
	{
		set("ReadFlag", b);
	}
	
	public boolean isImportant()
	{
		return (Boolean) get("ImportantFlag", false);
	}
	public void setImportant(boolean b)
	{
		set("ImportantFlag", b);
	}

	public boolean isSenderDeleted()
	{
		return (Boolean) get("SenderDeletedFlag", false);
	}
	public void setSenderDeleted(boolean b)
	{
		set("SenderDeletedFlag", b);
	}

	public UUID getThreadID()
	{
		return (UUID) get("ThreadID");
	}
	public void setThreadID(UUID threadID)
	{
		set("ThreadID", threadID);
	}
}
