package baby.pages;

import samoyan.apps.messaging.MessagingPage;
import samoyan.apps.profile.ProfilePage;
import samoyan.controls.TabControl;
import samoyan.database.PermissionStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import baby.app.BabyConsts;
import baby.pages.profile.ConsolidatedProfilePage;
import baby.pages.profile.PregnancyProfilePage;
import baby.pages.scrapbook.ChartsPage;
import baby.pages.scrapbook.GalleryPage;
import baby.pages.scrapbook.JournalPage;

public class BabyPage extends WebPage
{
	public final static String COMMAND_INFORMATION = "info";
	public final static String COMMAND_SCRAPBOOK = "scrapbook";
	public static final String COMMAND_CONTENT = "content";
	public static final String COMMAND_PROFILE = ProfilePage.COMMAND;

	@Override
	public boolean isAuthorized() throws Exception
	{
		RequestContext ctx = getContext();
		String cmd1 = ctx.getCommand(1);
		
		if (cmd1.equalsIgnoreCase(COMMAND_CONTENT))
		{
			return PermissionStore.getInstance().isUserGrantedPermission(ctx.getUserID(), BabyConsts.PERMISSION_CONTENT_MANAGEMENT);
		}
		else if (cmd1.equalsIgnoreCase(COMMAND_INFORMATION) ||
				cmd1.equalsIgnoreCase(COMMAND_SCRAPBOOK) ||
				cmd1.equalsIgnoreCase(ProfilePage.COMMAND) ||
				cmd1.equalsIgnoreCase(MessagingPage.COMMAND))
		{
			return ctx.getUserID() != null;
		}
		else
		{
			return true;
		}
	}
	
	protected void writeHorizontalNav(String currentTab) throws Exception
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			return;
		}
		
		String cmd1 = getContext().getCommand(1);
//		if (cmd1.equals(COMMAND_INFORMATION))
//		{
//			new TabControl(this)
//				.addTab(ViewArticleListPage.COMMAND, getString("information:Articles.Title"), getPageURL(ViewArticleListPage.COMMAND))
//				.addTab(ViewResourceListPage.COMMAND, getString("information:Resources.Title"), getPageURL(ViewResourceListPage.COMMAND))
//				.addTab(SearchPage.COMMAND, getString("information:Search.Title"), getPageURL(SearchPage.COMMAND))
//				.setCurrentTab(currentTab)
//				.render();
//		}
//		else
		if (cmd1.equals(COMMAND_SCRAPBOOK))
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("scrapbook:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("scrapbook:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("scrapbook:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.setCurrentTab(currentTab)
				.render();
		}
//		else if (cmd1.equals(COMMAND_TODO))
//		{
//			new TabControl(this)
//				.addTab(ChecklistPage.COMMAND, getString("todo:Checklist.Title"), getPageURL(ChecklistPage.COMMAND))
//				.addTab(AppointmentsPage.COMMAND, getString("todo:Appointments.Title"), getPageURL(AppointmentsPage.COMMAND))
//				.addTab(CalendarPage.COMMAND, getString("todo:Calendar.Title"), getPageURL(CalendarPage.COMMAND))
//				.setCurrentTab(currentTab)
//				.render();
//		}
		else if (cmd1.equals(COMMAND_PROFILE))
		{
			new TabControl(this)
				.addTab(PregnancyProfilePage.COMMAND, getString("babyprofile:Pregnancy.Title"), getPageURL(PregnancyProfilePage.COMMAND))
				.addTab(ConsolidatedProfilePage.COMMAND, getString("babyprofile:Consolidated.Title"), getPageURL(ConsolidatedProfilePage.COMMAND))
				.setCurrentTab(currentTab)
				.render();
		}
	}
}
