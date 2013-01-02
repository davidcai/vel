package mind.app;

import mind.database.DoseStore;
import mind.database.DrugStore;
import mind.database.PatientStore;
import mind.database.PrescriptionStore;
import mind.pages.*;
import mind.pages.master.*;
import mind.pages.omh.OmhApp;
import mind.pages.patient.coaching.*;
import mind.pages.patient.reminders.*;
import mind.pages.profile.ProfileExtraPage;
import mind.tasks.GenerateNewDosesRecurringTask;
import samoyan.servlet.Controller;
import samoyan.servlet.Dispatcher;
import samoyan.tasks.TaskManager;

public class MindController extends Controller
{
	@Override
	protected void preStart()
	{
		PatientStore.getInstance().define();
		DrugStore.getInstance().define();
		PrescriptionStore.getInstance().define();
		DoseStore.getInstance().define();
	}
	
	@Override
	protected void start()
	{
		// Envelope page
		Dispatcher.bindEnvelope(MindEnvelopePage.class);
		
		// General
		Dispatcher.bindPage(LoginPage.COMMAND,				LoginPage.class);
		Dispatcher.bindPage(WelcomePage.COMMAND,			WelcomePage.class);
		Dispatcher.bindPage(RootPage.COMMAND,				RootPage.class);
		Dispatcher.bindPage(DrugChooserTypeAhead.COMMAND,	DrugChooserTypeAhead.class);
		
		// Reminders
		Dispatcher.bindPage(RemindersHomePage.COMMAND,		RemindersHomePage.class);
		Dispatcher.bindPage(EditPrescriptionPage.COMMAND,	EditPrescriptionPage.class);
		Dispatcher.bindPage(PrescriptionListPage.COMMAND,	PrescriptionListPage.class);
		Dispatcher.bindPage(DoseListPage.COMMAND,			DoseListPage.class);
		Dispatcher.bindPage(DoseReminderNotif.COMMAND,		DoseReminderNotif.class);

		// Coaching
		Dispatcher.bindPage(CoachingHomePage.COMMAND,		CoachingHomePage.class);
		Dispatcher.bindPage(BotChatPage.COMMAND,			BotChatPage.class);
		Dispatcher.bindPage(DrugInfoPage.COMMAND,			DrugInfoPage.class);
		Dispatcher.bindPage(SearchDrugsPage.COMMAND,		SearchDrugsPage.class);
		Dispatcher.bindPage(DrugInteractionPage.COMMAND,	DrugInteractionPage.class);
		
		// Account
		Dispatcher.bindPage(ProfileExtraPage.COMMAND,			ProfileExtraPage.class);
		
		// Tasks
		TaskManager.addRecurring(new GenerateNewDosesRecurringTask());
//		ExecutionManager.addRecurring(new RemoveOrphanedDrugsTask());
		
		// OMH
		OmhApp.init();
	}
	
}
