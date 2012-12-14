package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public final class SubscriptionProcedureLinkStore extends LinkStore
{
	private static SubscriptionProcedureLinkStore instance = new SubscriptionProcedureLinkStore();

	protected SubscriptionProcedureLinkStore()
	{
	}
	public final static SubscriptionProcedureLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = createLinkTableDef("SubscriptionProcedureLink");
		
		td.setKey1("SubscriptionID", "Subscriptions");
		td.setKey2("ProcedureID", "Procedures").disallowRemoveIfHasLinks();
		
		return td;
	}
	
	// - - -
	
	public void addProcedureToSubscription(UUID procedureID, UUID subscriptionID) throws SQLException
	{
		link(subscriptionID, procedureID);
	}
	
	public List<UUID> getProceduresForSubscription(UUID subID) throws SQLException
	{
		return getByKey1(subID);
	}
	
	public void clearProceduresOfSubscription(UUID subID) throws SQLException
	{
		unlinkByKey1(subID);
	}
}
