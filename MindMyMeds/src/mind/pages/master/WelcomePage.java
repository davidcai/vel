package mind.pages.master;

import mind.database.Patient;
import mind.database.PatientStore;
import mind.pages.patient.reminders.PrescriptionListPage;
import samoyan.apps.admin.SystemOverviewPage;
import samoyan.apps.profile.PersonalInfoPage;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;

public class WelcomePage extends samoyan.apps.master.WelcomePage
{
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		boolean admin = (user!=null && PermissionStore.getInstance().isUserGrantedPermission(user.getID(), Permission.SYSTEM_ADMINISTRATION));

		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		if (patient!=null)
		{
			if (admin)
			{
				throw new RedirectException(SystemOverviewPage.COMMAND, null);
			}
			else
			{
				throw new RedirectException(PrescriptionListPage.COMMAND, null);
			}
		}
		
		// !$! For now we always create a patient record when we encounter a new login.
		// Later, we should let the user choose to register as a patient or a doctor by
		// redirecting to another screen where registration can be finalized.
		else if (ctx.getUserID()!=null)
		{
			patient = new Patient();
			patient.setLoginID(ctx.getUserID());
			PatientStore.getInstance().save(patient);
			
			if (admin)
			{
				throw new RedirectException(SystemOverviewPage.COMMAND, null);
			}
			else
			{
				// Redirect new patients to their profile page
				throw new RedirectException(PersonalInfoPage.COMMAND, null);
			}
		}
		else
		{
			throw new PageNotFoundException();
		}
	}
}
