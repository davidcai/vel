package baby.app;

import java.util.ArrayList;
import java.util.List;

import samoyan.core.image.LargestCropSizer;
import samoyan.core.image.ShrinkToFitSizer;
import samoyan.database.DataBean;
import samoyan.database.DataBeanStore;
import samoyan.database.ImageStore;
import samoyan.database.LinkStore;
import samoyan.database.PermissionStore;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.servlet.Controller;
import samoyan.servlet.Dispatcher;
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
import baby.pages.content.ChecklistListPage;
import baby.pages.content.ContentHomePage;
import baby.pages.content.EditChecklistPage;
import baby.pages.content.EditArticlePage;
import baby.pages.content.ArticleListPage;
import baby.pages.content.ImportArticlePage;
import baby.pages.content.MeasureListPage;
import baby.pages.content.MeasurePage;
import baby.pages.content.ResourceListPage;
import baby.pages.info.ViewArticlePage;
import baby.pages.info.ViewArticleListPage;
import baby.pages.info.InformationHomePage;
import baby.pages.info.ResourcesPage;
import baby.pages.info.SearchPage;
import baby.pages.master.LessStylesheetPage;
import baby.pages.master.LoginPage;
import baby.pages.master.RootPage;
import baby.pages.master.WelcomePage;
import baby.pages.profile.BabiesPage;
import baby.pages.profile.ConsolidatedProfilePage;
import baby.pages.profile.MedicalCenterPage;
import baby.pages.profile.StagePage;
import baby.pages.profile.UnitsPage;
import baby.pages.scrapbook.CalendarPage;
import baby.pages.scrapbook.ChartsPage;
import baby.pages.scrapbook.DaySummaryPage;
import baby.pages.scrapbook.GalleryPage;
import baby.pages.scrapbook.JournalEntryPage;
import baby.pages.scrapbook.JournalPage;
import baby.pages.scrapbook.KickCounterPage;
import baby.pages.scrapbook.PhotoPage;
import baby.pages.scrapbook.ScrapbookHomePage;
import baby.pages.todo.AppointmentsPage;
import baby.pages.todo.ChecklistAjaxPage;
import baby.pages.todo.ChecklistPage;
import baby.pages.todo.TodoHomePage;

public class BabyController extends Controller
{
	@Override
	protected List<DataBeanStore<? extends DataBean>> getDataBeanStores()
	{
		List<DataBeanStore<? extends DataBean>> result = new ArrayList<DataBeanStore<? extends DataBean>>();
		
		result.add(JournalEntryStore.getInstance());
		result.add(MotherStore.getInstance());
		result.add(BabyStore.getInstance());
		result.add(ArticleStore.getInstance());
		result.add(MeasureStore.getInstance());
		result.add(MeasureRecordStore.getInstance());
		result.add(ChecklistStore.getInstance());
		result.add(CheckItemStore.getInstance());
		result.add(AppointmentStore.getInstance());

		return result;
	}

	@Override
	protected List<LinkStore> getLinkStores()
	{
		List<LinkStore> result = new ArrayList<LinkStore>();
		
		result.add(ChecklistUserLinkStore.getInstance());
		result.add(CheckItemUserLinkStore.getInstance());

		return result;
	}
	
	@Override
	protected void initController() throws Exception
	{
		// Envelope page
		Dispatcher.bindEnvelope(BabyEnvelopePage.class);
		
		// Images
		ImageStore.getInstance().bindSizer(BabyConsts.IMAGESIZE_THUMB_150X150, new LargestCropSizer(150, 150));
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
		Dispatcher.bindPage(ResourcesPage.COMMAND, 				ResourcesPage.class);
		Dispatcher.bindPage(ViewArticlePage.COMMAND, 				ViewArticlePage.class);
		Dispatcher.bindPage(SearchPage.COMMAND, 				SearchPage.class);
		
		// Scrapbook
		Dispatcher.bindPage(ScrapbookHomePage.COMMAND, 			ScrapbookHomePage.class);
		Dispatcher.bindPage(JournalPage.COMMAND, 				JournalPage.class);
		Dispatcher.bindPage(JournalEntryPage.COMMAND, 			JournalEntryPage.class);
		Dispatcher.bindPage(GalleryPage.COMMAND, 				GalleryPage.class);
		Dispatcher.bindPage(PhotoPage.COMMAND, 					PhotoPage.class);
		Dispatcher.bindPage(ChartsPage.COMMAND, 				ChartsPage.class);
		Dispatcher.bindPage(CalendarPage.COMMAND, 				CalendarPage.class);
		Dispatcher.bindPage(KickCounterPage.COMMAND, 			KickCounterPage.class);
		Dispatcher.bindPage(DaySummaryPage.COMMAND, 			DaySummaryPage.class);

		// To do
		Dispatcher.bindPage(TodoHomePage.COMMAND, 				TodoHomePage.class);
		Dispatcher.bindPage(ChecklistPage.COMMAND, 				ChecklistPage.class);
		Dispatcher.bindPage(AppointmentsPage.COMMAND, 			AppointmentsPage.class);
		Dispatcher.bindPage(ChecklistAjaxPage.COMMAND, 			ChecklistAjaxPage.class);
		
		// Content
		Dispatcher.bindPage(ContentHomePage.COMMAND, 			ContentHomePage.class);
		Dispatcher.bindPage(EditArticlePage.COMMAND, 			EditArticlePage.class);
		Dispatcher.bindPage(ArticleListPage.COMMAND, 			ArticleListPage.class);
		Dispatcher.bindPage(ResourceListPage.COMMAND, 			ResourceListPage.class);
		Dispatcher.bindPage(MeasureListPage.COMMAND, 			MeasureListPage.class);
		Dispatcher.bindPage(MeasurePage.COMMAND, 				MeasurePage.class);
		Dispatcher.bindPage(ChecklistListPage.COMMAND, 			ChecklistListPage.class);
		Dispatcher.bindPage(EditChecklistPage.COMMAND, 			EditChecklistPage.class);
		Dispatcher.bindPage(ImportArticlePage.COMMAND, 		ImportArticlePage.class);

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
	}

	@Override
	protected void termController() throws Exception
	{
		// Crawler
		CrawlExecutor.term();
	}
}
