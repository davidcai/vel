package baby.pages.master;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import baby.app.BabyConsts;
import baby.controls.TimelineControl;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.CheckItemStore;
import baby.database.CheckItemUserLinkStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.content.ChecklistListPage;
import baby.pages.info.ViewArticlePage;
import baby.pages.info.ViewArticleListPage;
import baby.pages.profile.StagePage;
import baby.pages.todo.AppointmentsPage;
import samoyan.apps.master.JoinPage;
import samoyan.controls.LoginControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

public class RootPage extends WebPage
{
	public final static String COMMAND = "";
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		if (user==null)
		{
			// Master home page for public
			renderLoginScreen();
		}
		else if (user.isGuidedSetup())
		{
			// Redirect to GuidedSetup
			throw new RedirectException(UrlGenerator.COMMAND_SETUP, null);
		}
		else
		{
			// Internal portal page for logged in users
			renderPortal();
		}
	}

	private void renderPortal() throws Exception
	{
		RequestContext ctx = getContext();
		Mother mother = MotherStore.getInstance().loadByUserID(ctx.getUserID());
		Stage stage = mother.getPregnancyStage();
		Date now = new Date();
		
		// Stage status
		String status = null;
		if (stage.isPreconception())
		{
			status = getString("baby:Root.StatusPreconception");
		}
		else if (stage.isPregnancy())
		{
			Date due = mother.getDueDate();
			long days = (due.getTime() - now.getTime()) / (24L*60L*60L*1000L) + 1;
			if (days<1L)
			{
				// Overdue
				status = getString("baby:Root.StatusImminent");
			}
			else
			{
				status = getString("baby:Root.StatusPregnancy", stage.getPregnancyWeek(), days);
			}
		}
		else if (stage.isInfancy())
		{
			String names = "";
			List<UUID> babyIDs = BabyStore.getInstance().getAtLeastOneBaby(ctx.getUserID());
			if (babyIDs.size()<=2)
			{
				for (UUID babyID : babyIDs)
				{
					Baby baby = BabyStore.getInstance().load(babyID);
					if (Util.isEmpty(baby.getName()))
					{
						names = "";
						break;
					}
					else
					{
						if (names.length()>0)
						{
							names += getString("baby:Root.And");
						}
						names += baby.getName();
					}
				}
			}
			if (names.length()==0)
			{
				names = getString("baby:Root.BabyCountName." + babyIDs.size());
			}
			
			Date birth = mother.getBirthDate();
			long weeks = (now.getTime() - birth.getTime()) / (7L*24L*60L*60L*1000L) + 1;
			if (weeks<=18)
			{
				status = getString("baby:Root.StatusInfancyWeeks", names, babyIDs.size(), weeks);
			}
			else
			{
				status = getString("baby:Root.StatusInfancyMonths", names, babyIDs.size(), stage.getInfancyMonth());
			}
		}
		write("<h2>");
		writeEncode(status);
		write("</h2>");
		if (stage.isPreconception())
		{
			write("<small>");
			writeLink(getString("baby:Root.AreYouPregnant"), getPageURL(StagePage.COMMAND));
			write("</small><br><br>");
		}
		else if (stage.isPregnancy() && stage.getPregnancyWeek()>=35)
		{
			write("<small>");
			writeLink(getString("baby:Root.DidYouGiveBirth"), getPageURL(StagePage.COMMAND));
			write("</small><br><br>");
		}
		
		
		
		// Pinned articles
		int low = TimelineControl.getLowRange(stage.toInteger());
		int high = TimelineControl.getHighRange(stage.toInteger());
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySectionAndTimeline(BabyConsts.SECTION_INFO, low, high);
		List<Article> pinnedArticles = new ArrayList<Article>();
		List<Article> standardArticles = new ArrayList<Article>();
		for (UUID id : articleIDs)
		{
			Article article = ArticleStore.getInstance().load(id);
			if (article.getPriority()>0)
			{
				pinnedArticles.add(article);
			}
			else
			{
				standardArticles.add(article);
			}
		}
		Random rnd = new Random(System.currentTimeMillis()/Setup.getSessionLength()); // Sticky random, changes every session
		while (pinnedArticles.size()<3 && standardArticles.size()>0)
		{
			// Complement pinned articles with random standard articles
			Article art = standardArticles.remove(rnd.nextInt(standardArticles.size()));
			pinnedArticles.add(art);
		}
		if (pinnedArticles.size()>0)
		{
			write("<table width=\"100%\">");
			for (Article article : pinnedArticles)
			{
				String url = getPageURL(ViewArticlePage.COMMAND, new ParameterMap(ViewArticlePage.PARAM_ID, article.getID().toString()));

				write("<tr>");
				write("<td width=\"1%\">");
				if (article.getPhoto()==null)
				{
					writeImage("baby/article-thumbnail.png", article.getTitle(), url);
				}
				else
				{
					writeImage(article.getPhoto(), Image.SIZE_THUMBNAIL, article.getTitle(), url);
				}
				write("</td><td>");
				writeLink(article.getTitle(), url);
				String summary = article.getSummary();
				if (Util.isEmpty(summary))
				{
					summary = article.getPlainText();
				}
				if (!Util.isEmpty(summary))
				{
					write("<br>");
					writeEncode(Util.getTextAbstract(summary, Article.MAXSIZE_SUMMARY));
				}
				write("</td></tr>");
			}
			write("</table>");
		}
		if (articleIDs.size()>0)
		{
			String linkStr = null;
			if (stage.isPreconception())
			{
				linkStr = "baby:Root.MoreInfoPreconception";
			}
			else if (stage.isPregnancy())
			{
				linkStr = "baby:Root.MoreInfoPregnancy";
			}
			if (stage.isInfancy())
			{
				linkStr = "baby:Root.MoreInfoInfancy";
			}
			write("<br>");
			writeLink(getString(linkStr), getPageURL(ViewArticleListPage.COMMAND));
			write("<br><br>");
		}
		
		
		
		// Upcoming appointment
		Appointment upcomingAppointment = null;
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getAll(getContext().getUserID());
		for (UUID id : appointmentIDs)
		{
			Appointment appointment = AppointmentStore.getInstance().load(id);
			if (appointment.getDateTime().after(now))
			{
				upcomingAppointment = (Appointment) appointment.clone();
			}
			else
			{
				break;
			}
		}
		if (upcomingAppointment!=null)
		{
			write("<h2>");
			writeEncode(getString("baby:Root.NextAppointment"));
			write("</h2>");
			writeLink(upcomingAppointment.getDescription(), getPageURL(AppointmentsPage.COMMAND, new ParameterMap(AppointmentsPage.PARAM_ID, upcomingAppointment.getID())));
			write(" ");
			writeEncodeDateTime(upcomingAppointment.getDateTime());
			write("<br><br>");
		}
		
		
		// Common checklists
		boolean first = true;
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryBySectionAndTimeline(BabyConsts.SECTION_TODO, Stage.preconception().toInteger(), high);
		for (UUID checklistID : checklistIDs)
		{
			int complete = 0;
			List<UUID> checkItemIDs = CheckItemStore.getInstance().getByChecklistID(checklistID);
			for (UUID checkitemID : checkItemIDs)
			{
				if (CheckItemUserLinkStore.getInstance().isChecked(checkitemID, ctx.getUserID()))
				{
					complete ++;
				}
			}
				
			if (complete < checkItemIDs.size())
			{
				if (first)
				{
					write("<h2>");
					writeEncode(getString("baby:Root.Checklists"));
					write("</h2>");

					write("<table width=\"100%\">");
					first = false;
				}
				
				write("<tr><td>");
				Checklist checklist = ChecklistStore.getInstance().load(checklistID);
				writeLink(checklist.getTitle(), getPageURL(ChecklistListPage.COMMAND));
				write(" ");
				writeEncode(getString("baby:Root.ChecklistComplete", complete, checkItemIDs.size()));
				write("</td></tr>");
			}
		}
		if (!first)
		{
			write("</table><br>");
		}

		
		
		// !$! More?
	}
	
	private void renderLoginScreen() throws Exception
	{
		RequestContext ctx = getContext();
		boolean phone = ctx.getUserAgent().isSmartPhone();
		String appTitle = Setup.getAppTitle(getLocale());
		String appOwner = Setup.getAppOwner(getLocale());
		Server fed = ServerStore.getInstance().loadFederation();
		
		if (!phone)
		{
			write("<table><tr><td width=\"67%\" id=spiel>");
			
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));
			if (fed.isOpenRegistration())
			{
				write("<br><br>");
				writeFormOpen("GET", JoinPage.COMMAND);
				writeButton(getString("baby:Root.Register"));
				writeFormClose();
			}
			
			write("</td><td width=\"33%\">");
			
			write("<div id=loginframe>");
			new LoginControl(this).showPrompt(false).render();
			write("</div>");
			
			write("</td></tr></table>");
			write("<br>");
			
			write("<table id=benefits><tr>");
			for (int i=1; i<=3; i++)
			{
				write("<td width=\"33%\">");
				write("<big>");
				writeEncode(getString("baby:Root.BenefitTitle." + i));
				write("</big><br>");
				writeEncode(getString("baby:Root.Benefit." + i, appTitle, appOwner));
				write("</td>");
			}
			write("</tr></table>");			
		}
		else
		{
			write("<h2>");
			writeEncode(getString("baby:Root.Welcome", appTitle, appOwner));
			write("</h2>");
			write(Util.textToHtml(getString("baby:Root.Spiel", appTitle, appOwner)));

			write("<div id=loginframe>");
			new LoginControl(this).render();
			write("</div>");
			
			for (int i=1; i<=3; i++)
			{
				write("<big>");
				writeEncode(getString("baby:Root.BenefitTitle." + i));
				write("</big><br>");
				writeEncode(getString("baby:Root.Benefit." + i, appTitle, appOwner));
				write("<br><br>");
			}
		}
		
		// Custom CSS
		write("<style>");
			if (!phone)
			{
				// Background image on #middle
				write("#middle{background-image:url(\"");
				writeEncode(getResourceURL("baby/babies-background.jpg"));
				write("\");background-position:top center;background-repeat:no-repeat;}");
			}
			if (ctx.getUserID()==null)
			{
				// Hide navbar when not logged in
				write("#navbar{display:none;}");
			}
		write("</style>");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return Setup.getAppTitle(getLocale());
	}

//	@Override
//	public boolean isSecureSocket() throws Exception
//	{
//		return getContext().isSecureSocket();
//	}
}
