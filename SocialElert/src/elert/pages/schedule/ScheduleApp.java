package elert.pages.schedule;

import samoyan.servlet.Dispatcher;

public final class ScheduleApp
{
	public static void init()
	{
		Dispatcher.bindPage(ScheduleHomePage.COMMAND,				ScheduleHomePage.class);
		Dispatcher.bindPage(HomeServiceAreasPage.COMMAND,			HomeServiceAreasPage.class);
		Dispatcher.bindPage(ProcedurePage.COMMAND,					ProcedurePage.class);
		Dispatcher.bindPage(ProceduresPage.COMMAND_STANDARD,		ProceduresPage.class);
		Dispatcher.bindPage(ProceduresPage.COMMAND_CUSTOM,			ProceduresPage.class);
		Dispatcher.bindPage(PhysiciansPage.COMMAND,					PhysiciansPage.class);
		Dispatcher.bindPage(RecentSubscriptionsPage.COMMAND,		RecentSubscriptionsPage.class);
		Dispatcher.bindPage(LogNewOpeningPage.COMMAND,				LogNewOpeningPage.class);
		Dispatcher.bindPage(PatientProfilePage.COMMAND,				PatientProfilePage.class);
		Dispatcher.bindPage(VerifySubscriptionPage.COMMAND,			VerifySubscriptionPage.class);
		Dispatcher.bindPage(UnresolvedOpeningsPage.COMMAND,			UnresolvedOpeningsPage.class);
		Dispatcher.bindPage(OpeningPage.COMMAND,					OpeningPage.class);
		Dispatcher.bindPage(FinalizePage.COMMAND,					FinalizePage.class);
		Dispatcher.bindPage(PhysicianProfilePage.COMMAND,			PhysicianProfilePage.class);
		Dispatcher.bindPage(OpeningsReportPage.COMMAND,				OpeningsReportPage.class);
		Dispatcher.bindPage(OutgoingReportPage.COMMAND,				OutgoingReportPage.class);
		Dispatcher.bindPage(IncomingReportPage.COMMAND,				IncomingReportPage.class);
		Dispatcher.bindPage(SearchPatientsPage.COMMAND,				SearchPatientsPage.class);
		Dispatcher.bindPage(CalendarPage.COMMAND,					CalendarPage.class);
		Dispatcher.bindPage(EmailPatientPage.COMMAND,				EmailPatientPage.class);
	}
}
