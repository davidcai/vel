package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public final class SubscriptionPhysicianLinkStore extends LinkStore
{
	private static SubscriptionPhysicianLinkStore instance = new SubscriptionPhysicianLinkStore();

	protected SubscriptionPhysicianLinkStore()
	{
	}
	public final static SubscriptionPhysicianLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = LinkTableDef.newInstance("SubscriptionPhysicianLink");
		
		td.setKey1("SubscriptionID", "Subscriptions");
		td.setKey2("PhysicianID", "Users");
		
		return td;
	}
	
	// - - -

	public void addPhysicianToSubscription(UUID physicianID, UUID subscriptionID) throws SQLException
	{
		link(subscriptionID, physicianID);
	}
	
	public List<UUID> getPhysiciansForSubscription(UUID subID) throws SQLException
	{
		return getByKey1(subID);
	}
	
	public void clearPhysiciansOfSubscription(UUID subID) throws SQLException
	{
		unlinkByKey1(subID);
	}
}
