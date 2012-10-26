package elert.pages.physician;

import samoyan.servlet.Dispatcher;

public class PhysicianApp
{
	public static void init()
	{
		Dispatcher.bindPage(PhysicianHomePage.COMMAND,				PhysicianHomePage.class);
		Dispatcher.bindPage(UpcomingPatientsPage.COMMAND,			UpcomingPatientsPage.class);
	}
}
