package samoyan.database;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;

public final class InternalMessageStore extends DataBeanStore<InternalMessage>
{
	private static InternalMessageStore instance = new InternalMessageStore();

	protected InternalMessageStore()
	{
	}
	
	public final static InternalMessageStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<InternalMessage> getBeanClass()
	{
		return InternalMessage.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = TableDef.newInstance("InternalMessages", this);
		
		td.defineCol("Subject", String.class).size(0, InternalMessage.MAXSIZE_SUBJECT).invariant();
		td.defineCol("SenderUserID", UUID.class).refersTo("Users").invariant();
		td.defineCol("CreatedDate", Date.class).invariant();
		td.defineCol("ReadFlag", Boolean.class);
		td.defineCol("ImportantFlag", Boolean.class);
		td.defineCol("SenderDeletedFlag", Boolean.class);
		td.defineCol("ThreadID", UUID.class).invariant();
		
		td.defineProp("Body", String.class);
		
		return td;
	}

	// - - -

	/**
	 * Return the IDs of messages addressed to the given user.
	 * @param recipientUserID The user ID of the recipient.
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> queryInbox(UUID recipientUserID) throws SQLException
	{
		return Query.queryListUUID(	"SELECT m.ID FROM InternalMessages AS m, InternalMessageRecipients AS r " +
									"WHERE m.ID=r.InternalMessageID AND r.RecipientUserID=? AND r.RecipientDeletedFlag=0 " + 
									"ORDER BY m.CreatedDate DESC",
									new ParameterList(recipientUserID));
	}

	public void markDeletedForSender(UUID internalMessageID, UUID senderUserID) throws SQLException
	{
		Query q = new Query();
		try
		{
			q.update("UPDATE InternalMessages SET SenderDeletedFlag=1 WHERE SenderUserID=? AND ID=?", new ParameterList(senderUserID).plus(internalMessageID));
		}
		finally
		{
			q.close();
		}
	}

	public List<UUID> queryOutbox(UUID senderUserID) throws SQLException
	{
		return Query.queryListUUID(	"SELECT ID FROM InternalMessages WHERE SenderUserID=? AND SenderDeletedFlag=0 ORDER BY CreatedDate DESC", new ParameterList(senderUserID));
	}	
}
