package elert.app;

import java.util.ArrayList;
import java.util.List;

import samoyan.core.image.LargestCropSizer;
import samoyan.database.DataBean;
import samoyan.database.DataBeanStore;
import samoyan.database.ImageStore;
import samoyan.database.LinkStore;
import samoyan.database.PermissionStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.servlet.Controller;
import samoyan.servlet.Dispatcher;
import elert.database.ElertStore;
import elert.database.FacilityStore;
import elert.database.OpeningStore;
import elert.database.PhysicianFacilityLinkStore;
import elert.database.PhysicianOpeningLinkStore;
import elert.database.PhysicianProcedureTypeLinkStore;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureOpeningLinkStore;
import elert.database.ProcedureStore;
import elert.database.ProcedureTypeStore;
import elert.database.RegionStore;
import elert.database.ResourceProcedureLinkStore;
import elert.database.ResourceStore;
import elert.database.ServiceAreaStore;
import elert.database.ServiceAreaUserLinkStore;
import elert.database.SubscriptionFacilityLinkStore;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.database.UserExStore;
import elert.pages.govern.GovernApp;
import elert.pages.master.MasterApp;
import elert.pages.patient.PatientApp;
import elert.pages.physician.PhysicianApp;
import elert.pages.profile.ProfileApp;
import elert.pages.schedule.ScheduleApp;
import elert.pages.typeahead.TypeAheadApp;

public class ElertController extends Controller
{
	@Override
	protected List<DataBeanStore<? extends DataBean>> getDataBeanStores()
	{
		List<DataBeanStore<? extends DataBean>> result = new ArrayList<DataBeanStore<? extends DataBean>>();
		
		result.add(UserExStore.getInstance());

		// Geography
		result.add(RegionStore.getInstance());
		result.add(ServiceAreaStore.getInstance());
		result.add(FacilityStore.getInstance());
		
		// Procedures
		result.add(ProcedureTypeStore.getInstance());
		result.add(ProcedureStore.getInstance());
		result.add(ResourceStore.getInstance());
		
		result.add(SubscriptionStore.getInstance());
		result.add(OpeningStore.getInstance());
		result.add(ElertStore.getInstance());
		
		return result;
	}

	@Override
	protected List<LinkStore> getLinkStores()
	{
		List<LinkStore> result = new ArrayList<LinkStore>();

		result.add(PhysicianFacilityLinkStore.getInstance());
		result.add(ResourceProcedureLinkStore.getInstance());
		result.add(ServiceAreaUserLinkStore.getInstance());
		result.add(ProcedureFacilityLinkStore.getInstance());
		result.add(SubscriptionProcedureLinkStore.getInstance());
		result.add(SubscriptionPhysicianLinkStore.getInstance());
		result.add(ProcedureOpeningLinkStore.getInstance());
		result.add(PhysicianOpeningLinkStore.getInstance());
		result.add(PhysicianProcedureTypeLinkStore.getInstance());
		result.add(SubscriptionFacilityLinkStore.getInstance());

		return result;
	}

	@Override
	protected void initController() throws Exception
	{
		// Envelopes
		Dispatcher.bindEnvelope(ElertEnvelopePage.class);
		
		// Images
		ImageStore.getInstance().bindSizer(ElertConsts.IMAGESIZE_SQUARE_150, new LargestCropSizer(150, 150));
		ImageStore.getInstance().bindSizer(ElertConsts.IMAGESIZE_SQUARE_50, new LargestCropSizer(50, 50));

		// Apps
		MasterApp.init();
		GovernApp.init();
		PatientApp.init();
		ScheduleApp.init();
		PhysicianApp.init();
		ProfileApp.init();
		TypeAheadApp.init();

		// Create groups and permissions
		String[] specs = {	ElertConsts.GROUP_GOVERNORS,		ElertConsts.PERMISSION_APPLICATION_GOVERNMENT,
							ElertConsts.GROUP_SCHEDULERS,		ElertConsts.PERMISSION_SCHEDULING,
							ElertConsts.GROUP_PATIENTS,			null,
							ElertConsts.GROUP_PHYSICIANS,		ElertConsts.PERMISSION_PHYSICIAN};
		for (int i=0; i<specs.length; i+=2)
		{
			UserGroup g = UserGroupStore.getInstance().loadByName(specs[i]);
			if (g==null)
			{
				g = new UserGroup();
				g.setName(specs[i]);
				UserGroupStore.getInstance().save(g);
			}
			PermissionStore.getInstance().authorize(g.getID(), specs[i+1]);
		}
		
		// Upgrade
		Server fed = ServerStore.getInstance().loadFederation();
		String VERSION = "2012-09-06";
		if (fed.getApplicationUpgradeVersion().compareTo(VERSION)<0)
		{
			// - - - begin synchronous upgrade
//			List<String> dummyPages = new ArrayList<String>();
//			dummyPages.add(PersonalInfoPage.COMMAND);
//			Iterator<User> ghosts = UserStore.getInstance().queryAllGhost();
//			while (ghosts.hasNext())
//			{
//				User user = UserStore.getInstance().open(ghosts.next().getID());
//				if (user.getGuidedSetupPages()==null)
//				{
//					user.setGuidedSetupPages(dummyPages);
//					user.setGuidedSetupStep(Integer.MAX_VALUE);
//					UserStore.getInstance().save(user);
//				}
//			}
			// - - - end synchronous upgrade
			
			fed = (Server) fed.clone();
			fed.setApplicationUpgradeVersion(VERSION);
			ServerStore.getInstance().save(fed);
		}
	}
}
