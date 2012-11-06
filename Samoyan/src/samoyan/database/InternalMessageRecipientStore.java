package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;

public final class InternalMessageRecipientStore extends DataBeanStore<InternalMessageRecipient>
{
	private static InternalMessageRecipientStore instance = new InternalMessageRecipientStore();

	protected InternalMessageRecipientStore()
	{
	}
	
	public final static InternalMessageRecipientStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<InternalMessageRecipient> getBeanClass()
	{
		return InternalMessageRecipient.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("InternalMessageRecipients", this);
		
		td.defineCol("RecipientUserID", UUID.class).refersTo("Users").invariant();
		td.defineCol("InternalMessageID", UUID.class).ownedBy("InternalMessages").invariant();
		td.defineCol("RecipientDeletedFlag", Boolean.class);
				
		return td;
	}

	// - - -
	
	public void markDeletedForRecipient(UUID internalMessageID, UUID recipientUserID) throws SQLException
	{
		Query q = new Query();
		try
		{
			q.update("UPDATE InternalMessageRecipients SET RecipientDeletedFlag=1 WHERE RecipientUserID=? AND InternalMessageID=?", new ParameterList(recipientUserID).plus(internalMessageID));
		}
		finally
		{
			q.close();
		}
	}
	
	public boolean isRecipient(UUID internalMessageID, UUID recipientUserID) throws SQLException
	{
		Query q = new Query();
		try
		{
			ResultSet rs = q.select("SELECT ID FROM InternalMessageRecipients WHERE RecipientUserID=? AND InternalMessageID=? AND RecipientDeletedFlag=0", new ParameterList(recipientUserID).plus(internalMessageID));
			return rs.next();
		}
		finally
		{
			q.close();
		}
	}

	public List<UUID> queryRecipientsOfMessage(UUID internalMessageID) throws SQLException
	{
		return Query.queryListUUID("SELECT RecipientUserID FROM InternalMessageRecipients WHERE InternalMessageID=?", new ParameterList(internalMessageID));
	}
}
