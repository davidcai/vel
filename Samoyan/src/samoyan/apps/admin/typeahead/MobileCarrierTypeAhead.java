package samoyan.apps.admin.typeahead;

import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.system.TypeAhead;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;

public class MobileCarrierTypeAhead extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/mobilecarrier.typeahead";

	public MobileCarrierTypeAhead()
	{
		super();
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String q) throws Exception
			{
				for (UUID carrierID : MobileCarrierStore.getInstance().getAllBeanIDs())
				{
					MobileCarrier mc = MobileCarrierStore.getInstance().load(carrierID);
					if (mc.getName().toLowerCase(getLocale()).indexOf(q.toLowerCase(getLocale()))>=0)
					{
						addOption(mc.getID(), mc.getName());
					}
				}
			}
		});
	}
}
