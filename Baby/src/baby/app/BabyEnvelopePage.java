package baby.app;

import java.util.ArrayList;
import java.util.List;

import samoyan.apps.admin.AdminTab;
import samoyan.apps.guidedsetup.GuidedSetupTab;
import samoyan.controls.MetaTagControl;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.EnvelopePage;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.RequestContext;
import baby.pages.content.ContentTab;
import baby.pages.info.InfoTab;
import baby.pages.master.MasterTab;
import baby.pages.profile.BabyProfileTab;
import baby.pages.scrapbook.ScrapbookTab;
import baby.pages.todo.TodoTab;

public class BabyEnvelopePage extends EnvelopePage
{
	private static EnvelopeTab masterTab = new MasterTab();
	private static EnvelopeTab adminTab = new AdminTab();
	private static EnvelopeTab contentTab = new ContentTab();
	private static EnvelopeTab infoTab = new InfoTab();
	private static EnvelopeTab scrapbookTab = new ScrapbookTab();
	private static EnvelopeTab todoTab = new TodoTab();
	private static EnvelopeTab profileTab = new BabyProfileTab();
//	private static EnvelopeTab messagingTab = new MessagingTab();
	private static EnvelopeTab setupTab = new GuidedSetupTab();
	
	@Override
	protected List<EnvelopeTab> getTabs() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		List<EnvelopeTab> result = new ArrayList<EnvelopeTab>(6);		
		result.add(masterTab);
		if (user!=null)
		{
			boolean admin = PermissionStore.getInstance().isUserGrantedPermission(user.getID(), Permission.SYSTEM_ADMINISTRATION);
			boolean contentManager = PermissionStore.getInstance().isUserGrantedPermission(user.getID(), BabyConsts.PERMISSION_CONTENT_MANAGEMENT);
			
			if (admin)
			{
				result.add(adminTab);
			}
			if (contentManager)
			{
				result.add(contentTab);
			}
			
			if (user.isGuidedSetup())
			{
				result.add(setupTab);
			}
			else
			{
				result.add(infoTab);
				result.add(scrapbookTab);
				result.add(todoTab);
	//			result.add(messagingTab); // !$! Testing only
				result.add(profileTab);
			}
		}
		
		return result;
	}
	
	@Override
	protected void renderHTMLMetaTagsAndIncludes() throws Exception
	{
		writeIncludeCSS("baby/baby.less");
		
		MetaTagControl ctrl = new MetaTagControl(this);
		ctrl.appleTouchIcon("baby/apple-touch-icon.png");
		ctrl.appleTouchStartupImage("baby/iphone-splash.png");
		ctrl.favicon("baby/favicon.png");
		ctrl.render();
	}

//	@Override
//	protected void renderHTMLFooter() throws Exception
//	{
//		boolean smartPhone = getContext().getUserAgent().isSmartPhone();
//		Calendar cal = Calendar.getInstance(getTimeZone());
//
//		String appOwner = Setup.getAppOwner(getLocale());
//		if (!smartPhone)
//		{
//			write("<table width=\"100%\"><tr valign=middle><td>");
//			writeImage("baby/footer-logo.png", appOwner, "http://www.kp.org");
//			write("</td><td align=right>");
//			writeEncode(getString("baby:App.Copyright", String.valueOf(cal.get(Calendar.YEAR)), appOwner));
//			write(" | ");
//			writeLink(getString("baby:Nav.Terms"), getPageURL(TermsPage.COMMAND));
//			write(" | ");
//			writeLink(getString("baby:Nav.Privacy"), getPageURL(PrivacyPage.COMMAND));
//			write("</td></tr></table>");
//		}
//		else
//		{
//			write("<div align=center>");
//			writeImage("baby/footer-logo.png", appOwner, "http://www.kp.org");
//			write("<br>");
//			writeEncode(getString("baby:App.Copyright", String.valueOf(cal.get(Calendar.YEAR)), appOwner));
//			write("<br>");
//			writeLink(getString("baby:Nav.Terms"), getPageURL(TermsPage.COMMAND));
//			write(" | ");
//			writeLink(getString("baby:Nav.Privacy"), getPageURL(PrivacyPage.COMMAND));
//			write("</div>");
//		}
//	}

//	@Override
//	protected String getApplicationLogo()
//	{
//		return "baby/email-banner.png";
//	}
//
//	@Override
//	protected String getOwnerLogo()
//	{
//		return "baby/email-logo.png";
//	}
}
