package baby.app;


import samoyan.core.image.LargestCropSizer;
import samoyan.core.image.ShrinkToFitSizer;
import samoyan.database.ImageStore;
import samoyan.database.PermissionStore;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.servlet.Controller;
import samoyan.servlet.Dispatcher;
import samoyan.tasks.TaskManager;
import baby.crawler.CrawlExecutor;
import baby.database.AppointmentStore;
import baby.database.ArticleStore;
import baby.database.BabyStore;
import baby.database.CheckItemStore;
import baby.database.CheckItemUserLinkStore;
import baby.database.ChecklistStore;
import baby.database.ChecklistUserLinkStore;
import baby.database.JournalEntryStore;
import baby.database.MeasureRecordStore;
import baby.database.MeasureStore;
import baby.database.MotherStore;
import baby.pages.content.ArticleListPage;
import baby.pages.content.ChecklistListPage;
import baby.pages.content.ContentHomePage;
import baby.pages.content.DeleteAllPage;
import baby.pages.content.EditArticlePage;
import baby.pages.content.EditChecklistPage;
import baby.pages.content.ImportArticlePage;
import baby.pages.content.ImportChecklistPage;
import baby.pages.content.KPOrgCrawlerPage;
import baby.pages.content.MeasureListPage;
import baby.pages.content.MeasurePage;
import baby.pages.content.MedicalCenterTypeAhead;
import baby.pages.content.RegionTypeAhead;
import baby.pages.info.AppointmentReminderNotif;
import baby.pages.info.AppointmentsChoicePage;
import baby.pages.info.AppointmentsListPage;
import baby.pages.info.CalendarPage;
import baby.pages.info.ChecklistAjaxPage;
import baby.pages.info.ChecklistPage;
import baby.pages.info.EditAppointmentPage;
import baby.pages.info.InformationHomePage;
import baby.pages.info.SearchArticlesPage;
import baby.pages.info.SearchResourcesPage;
import baby.pages.info.ViewArticleListPage;
import baby.pages.info.ViewArticlePage;
import baby.pages.info.ViewResourceListPage;
import baby.pages.journey.ChartsPage;
import baby.pages.journey.GalleryPage;
import baby.pages.journey.JournalEntryPage;
import baby.pages.journey.JournalPage;
import baby.pages.journey.JourneyHomePage;
import baby.pages.journey.MeasureRecordsPage;
import baby.pages.journey.PhotoPage;
import baby.pages.master.LessStylesheetPage;
import baby.pages.master.LoginPage;
import baby.pages.master.RootPage;
import baby.pages.master.WelcomePage;
import baby.pages.profile.BabiesPage;
import baby.pages.profile.ConsolidatedProfilePage;
import baby.pages.profile.MedicalCenterPage;
import baby.pages.profile.StagePage;
import baby.pages.profile.UnitsPage;
import baby.tasks.CrawlResourcesRecurringTask;

public class BabyController extends Controller
{	
	@Override
	protected void preStart() throws Exception
	{
		JournalEntryStore.getInstance().define();
		MotherStore.getInstance().define();
		BabyStore.getInstance().define();
		ArticleStore.getInstance().define();
		MeasureStore.getInstance().define();
		MeasureRecordStore.getInstance().define();
		ChecklistStore.getInstance().define();
		CheckItemStore.getInstance().define();
		AppointmentStore.getInstance().define();
		
		ChecklistUserLinkStore.getInstance().define();
		CheckItemUserLinkStore.getInstance().define();
	}
	
	@Override
	protected void start() throws Exception
	{
		// Envelope page
		Dispatcher.bindEnvelope(BabyEnvelopePage.class);
		
		// Images
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_THUMB_150X150, new LargestCropSizer(150, 150));
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_THUMB_100X100, new LargestCropSizer(100, 100));
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_THUMB_50X50, new LargestCropSizer(50, 50));
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_BOX_800X800, new ShrinkToFitSizer(800, 800));
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_BOX_400X400, new ShrinkToFitSizer(400, 400));
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_BOX_150X150, new ShrinkToFitSizer(150, 150));
		
		// Master
		Dispatcher.bindPage(LoginPage.COMMAND,					LoginPage.class);
		Dispatcher.bindPage(WelcomePage.COMMAND,				WelcomePage.class);
		Dispatcher.bindPage(RootPage.COMMAND,					RootPage.class);
		Dispatcher.bindPage(LessStylesheetPage.COMMAND,			LessStylesheetPage.class);
		
		// Info
		Dispatcher.bindPage(InformationHomePage.COMMAND, 		InformationHomePage.class);
		Dispatcher.bindPage(ViewArticleListPage.COMMAND, 		ViewArticleListPage.class);
		Dispatcher.bindPage(ViewResourceListPage.COMMAND, 		ViewResourceListPage.class);
		Dispatcher.bindPage(ViewArticlePage.COMMAND, 			ViewArticlePage.class);
		Dispatcher.bindPage(SearchResourcesPage.COMMAND, 		SearchResourcesPage.class);
		Dispatcher.bindPage(SearchArticlesPage.COMMAND, 		SearchArticlesPage.class);
		Dispatcher.bindPage(ChecklistPage.COMMAND, 				ChecklistPage.class);
		Dispatcher.bindPage(ChecklistAjaxPage.COMMAND, 			ChecklistAjaxPage.class);
		Dispatcher.bindPage(AppointmentsListPage.COMMAND, 		AppointmentsListPage.class);
		Dispatcher.bindPage(CalendarPage.COMMAND, 				CalendarPage.class);
		Dispatcher.bindPage(AppointmentsChoicePage.COMMAND, 	AppointmentsChoicePage.class);
		Dispatcher.bindPage(EditAppointmentPage.COMMAND, 		EditAppointmentPage.class);
		Dispatcher.bindPage(AppointmentReminderNotif.COMMAND, 	AppointmentReminderNotif.class);
		
//		// Scrapbook
//		Dispatcher.bindPage(ScrapbookHomePage.COMMAND, 			ScrapbookHomePage.class);
//		Dispatcher.bindPage(JournalPage.COMMAND, 				JournalPage.class);
//		Dispatcher.bindPage(JournalEntryPage.COMMAND, 			JournalEntryPage.class);
//		Dispatcher.bindPage(GalleryPage.COMMAND, 				GalleryPage.class);
//		Dispatcher.bindPage(PhotoPage.COMMAND, 					PhotoPage.class);
//		Dispatcher.bindPage(ChartsPage.COMMAND, 				ChartsPage.class);
//		Dispatcher.bindPage(KickCounterPage.COMMAND, 			KickCounterPage.class);
		
		// Journey
		Dispatcher.bindPage(JourneyHomePage.COMMAND, 			JourneyHomePage.class);
		Dispatcher.bindPage(JournalPage.COMMAND, 				JournalPage.class);
		Dispatcher.bindPage(GalleryPage.COMMAND, 				GalleryPage.class);
		Dispatcher.bindPage(ChartsPage.COMMAND, 				ChartsPage.class);
		Dispatcher.bindPage(PhotoPage.COMMAND, 					PhotoPage.class);
		Dispatcher.bindPage(JournalEntryPage.COMMAND, 			JournalEntryPage.class);
		Dispatcher.bindPage(MeasureRecordsPage.COMMAND, 		MeasureRecordsPage.class);
		
		// Content
		Dispatcher.bindPage(ContentHomePage.COMMAND, 			ContentHomePage.class);
		Dispatcher.bindPage(EditArticlePage.COMMAND, 			EditArticlePage.class);
		Dispatcher.bindPage(ArticleListPage.COMMAND, 			ArticleListPage.class);
		Dispatcher.bindPage(KPOrgCrawlerPage.COMMAND, 			KPOrgCrawlerPage.class);
		Dispatcher.bindPage(MeasureListPage.COMMAND, 			MeasureListPage.class);
		Dispatcher.bindPage(MeasurePage.COMMAND, 				MeasurePage.class);
		Dispatcher.bindPage(ChecklistListPage.COMMAND, 			ChecklistListPage.class);
		Dispatcher.bindPage(EditChecklistPage.COMMAND, 			EditChecklistPage.class);
		Dispatcher.bindPage(ImportArticlePage.COMMAND, 			ImportArticlePage.class);
		Dispatcher.bindPage(ImportChecklistPage.COMMAND, 		ImportChecklistPage.class);
		Dispatcher.bindPage(DeleteAllPage.COMMAND, 				DeleteAllPage.class);
		Dispatcher.bindPage(RegionTypeAhead.COMMAND, 			RegionTypeAhead.class);
		Dispatcher.bindPage(MedicalCenterTypeAhead.COMMAND, 	MedicalCenterTypeAhead.class);

		// Profile
		Dispatcher.bindPage(MedicalCenterPage.COMMAND, 			MedicalCenterPage.class);
		Dispatcher.bindPage(StagePage.COMMAND, 					StagePage.class);
		Dispatcher.bindPage(ConsolidatedProfilePage.COMMAND, 	ConsolidatedProfilePage.class);
//		Dispatcher.bindPage(BabyProfileHomePage.COMMAND, 		BabyProfileHomePage.class);
		Dispatcher.bindPage(UnitsPage.COMMAND, 					UnitsPage.class);
		Dispatcher.bindPage(BabiesPage.COMMAND, 				BabiesPage.class);
		
		// Create groups and permissions
		String[] specs = {	BabyConsts.GROUP_GOVERNORS,			BabyConsts.PERMISSION_CONTENT_MANAGEMENT};
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

		// Crawler
		CrawlExecutor.init();
		
		// Recurring tasks
		TaskManager.addRecurring(new CrawlResourcesRecurringTask());
	}

	@Override
	protected void terminate() throws Exception
	{
		// Crawler
		CrawlExecutor.term();
	}
}
