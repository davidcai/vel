package elert.pages.patient;

import samoyan.servlet.Dispatcher;

public class PatientApp
{
	public static void init()
	{
		Dispatcher.bindPage(PatientHomePage.COMMAND,				PatientHomePage.class);
		Dispatcher.bindPage(SubscriptionsPage.COMMAND,				SubscriptionsPage.class);
		Dispatcher.bindPage(NewSubscriptionWizardPage.COMMAND,		NewSubscriptionWizardPage.class);
		Dispatcher.bindPage(SubscriptionPage.COMMAND,				SubscriptionPage.class);
		Dispatcher.bindPage(ProcedureInfoPage.COMMAND,				ProcedureInfoPage.class);
		Dispatcher.bindPage(ProcedureSearchPage.COMMAND,			ProcedureSearchPage.class);
		Dispatcher.bindPage(WallPage.COMMAND,						WallPage.class);
		Dispatcher.bindPage(ElertNotif.COMMAND,						ElertNotif.class);
		Dispatcher.bindPage(ChosenNotif.COMMAND,					ChosenNotif.class);
		Dispatcher.bindPage(UnavailNotif.COMMAND,					UnavailNotif.class);
		Dispatcher.bindPage(ConsentFormPage.COMMAND,				ConsentFormPage.class);
	}
}
