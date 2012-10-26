package elert.pages.master;

import java.util.ArrayList;
import java.util.List;

import elert.app.ElertConsts;
import elert.pages.govern.GovernHomePage;
import elert.pages.patient.ConsentFormPage;
import elert.pages.patient.PatientHomePage;
import elert.pages.physician.PhysicianHomePage;
import elert.pages.schedule.HomeServiceAreasPage;
import elert.pages.schedule.PhysiciansPage;
import elert.pages.schedule.ProceduresPage;
import elert.pages.schedule.ScheduleHomePage;
import samoyan.apps.admin.AdminHomePage;
import samoyan.apps.guidedsetup.StartPage;
import samoyan.apps.profile.MobilePage;
import samoyan.apps.profile.PersonalInfoPage;
import samoyan.apps.profile.PhonePage;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.UnauthorizedException;

public class WelcomePage extends samoyan.apps.master.WelcomePage
{
	@Override
	public void renderHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		if (user==null)
		{
			throw new UnauthorizedException();
		}
		
		// Initialize the guided setup
		if (user.getGuidedSetupPages()==null)
		{
			Server fed = ServerStore.getInstance().loadFederation();
			List<String> guidedSetupPages = new ArrayList<String>();

			if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), ElertConsts.PERMISSION_SCHEDULING))
			{
				// Scheduler
				guidedSetupPages.add(PersonalInfoPage.COMMAND);
				guidedSetupPages.add(HomeServiceAreasPage.COMMAND);
				guidedSetupPages.add(ProceduresPage.COMMAND_STANDARD);
				guidedSetupPages.add(PhysiciansPage.COMMAND);
			}
			else if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), Permission.SYSTEM_ADMINISTRATION))
			{
				// Admin
			}
			else if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), ElertConsts.PERMISSION_APPLICATION_GOVERNMENT))
			{
				// Governor
			}
//			else if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), ElertConsts.PERMISSION_PHYSICIAN))
//			{
//				// Physician
//			}
			else
			{
				// Patient
				guidedSetupPages.add(PersonalInfoPage.COMMAND);
				if (fed.isChannelEnabled(Channel.SMS))
				{
					guidedSetupPages.add(MobilePage.COMMAND);
				}
				if (fed.isChannelEnabled(Channel.VOICE))
				{
					guidedSetupPages.add(PhonePage.COMMAND);
				}
				guidedSetupPages.add(ConsentFormPage.COMMAND);
			}
			
			if (guidedSetupPages.size()>0)
			{
				user = (User) user.clone();
				user.setGuidedSetupPages(guidedSetupPages);
				UserStore.getInstance().save(user);
			}
		}

		// Guided setup
		if (user.isGuidedSetup())
		{
			throw new RedirectException(StartPage.COMMAND, null);
		}

		// Admin
		if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), Permission.SYSTEM_ADMINISTRATION))
		{
			throw new RedirectException(AdminHomePage.COMMAND, null);
		}
		
		// Governor
		if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), ElertConsts.PERMISSION_APPLICATION_GOVERNMENT))
		{
			throw new RedirectException(GovernHomePage.COMMAND, null);
		}
		
		// Scheduler
		if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), ElertConsts.PERMISSION_SCHEDULING))
		{
			throw new RedirectException(ScheduleHomePage.COMMAND, null);
		}
		
		// Physician
		if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), ElertConsts.PERMISSION_PHYSICIAN))
		{
			throw new RedirectException(PhysicianHomePage.COMMAND, null);
		}

		// Patient
		throw new RedirectException(PatientHomePage.COMMAND, null);
	}
}
