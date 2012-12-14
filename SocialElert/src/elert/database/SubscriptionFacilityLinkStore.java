package elert.database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import samoyan.database.LinkStore;
import samoyan.database.LinkTableDef;

public final class SubscriptionFacilityLinkStore extends LinkStore
{
	private static SubscriptionFacilityLinkStore instance = new SubscriptionFacilityLinkStore();

	protected SubscriptionFacilityLinkStore()
	{
	}
	public final static SubscriptionFacilityLinkStore getInstance()
	{
		return instance;
	}

	@Override
	protected LinkTableDef defineMapping()
	{
		LinkTableDef td = createLinkTableDef("SubscriptionFacilityLink");
		
		td.setKey1("SubscriptionID", "Subscriptions");
		td.setKey2("FacilityID", "Facilities").disallowRemoveIfHasLinks();
		
		return td;
	}
	
	// - - -
	
	public void addFacilityToSubscription(UUID facilityID, UUID subscriptionID) throws SQLException
	{
		link(subscriptionID, facilityID);
	}
	
	public void clearFacilitiesOfSubscription(UUID subscriptionID) throws SQLException
	{
		unlinkByKey1(subscriptionID);
	}

	public List<UUID> getFacilitiesForSubscription(UUID subID) throws SQLException
	{
		return getByKey1(subID);
	}
	
	public boolean isFacilityLinkedToSubscription(UUID facilityID, UUID subscriptionID) throws SQLException
	{
		return isLinked(subscriptionID, facilityID);
	}
	
	public void removeFacilityFromSubscription(UUID facilityID, UUID subscriptionID) throws SQLException
	{
		unlink(subscriptionID, facilityID);
	}
}
