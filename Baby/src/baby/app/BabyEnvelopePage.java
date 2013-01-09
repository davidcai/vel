package baby.app;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import samoyan.apps.master.PrivacyPage;
import samoyan.apps.master.TermsPage;
import samoyan.controls.MetaTagControl;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.EnvelopePage;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import baby.pages.admin.AdminTab;
import baby.pages.content.ContentTab;
import baby.pages.info.InfoTab;
import baby.pages.journey.JourneyTab;
import baby.pages.master.MasterTab;
import baby.pages.profile.BabyProfileTab;
import baby.pages.setup.SetupTab;

public class BabyEnvelopePage extends EnvelopePage
{
	private static EnvelopeTab masterTab = new MasterTab();
	private static EnvelopeTab adminTab = new AdminTab();
	private static EnvelopeTab contentTab = new ContentTab();
	private static EnvelopeTab infoTab = new InfoTab();
//	private static EnvelopeTab scrapbookTab = new ScrapbookTab();
	private static EnvelopeTab journeyTab = new JourneyTab();
	private static EnvelopeTab profileTab = new BabyProfileTab();
//	private static EnvelopeTab messagingTab = new MessagingTab();
	private static EnvelopeTab setupTab = new SetupTab();
	
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
//				result.add(scrapbookTab);
				result.add(journeyTab);
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
		ctrl.appleTouchIcon(false, "baby/apple-touch-icon-114.png", "baby/apple-touch-icon-144.png");
		ctrl.appleTouchStartupImage(
			"baby/iphone-splash-320x460.png",
			"baby/iphone-splash-640x920.png",
			"baby/iphone-splash-640x1096.png",
			"baby/ipad-splash-768x1004.png",
			"baby/ipad-splash-1536x2008.png");
		ctrl.favicon("baby/favicon.png");
		ctrl.render();
	}

	@Override
	protected void renderHTMLFooter() throws Exception
	{
		boolean smartPhone = getContext().getUserAgent().isSmartPhone();
		Calendar cal = Calendar.getInstance(getTimeZone());

		String appOwner = Setup.getAppOwner(getLocale());
		if (!smartPhone)
		{
			write("<table width=\"100%\"><tr valign=middle><td>");
			writeImage("baby/kaiser-permanente-logo.png", appOwner, "http://www.kp.org");
			write("</td><td align=right>");
			writeEncode(getString("baby:Envelope.Copyright", String.valueOf(cal.get(Calendar.YEAR)), appOwner));
			write(" | ");
			writeLink(getString("baby:Envelope.Terms"), getPageURL(TermsPage.COMMAND));
			write(" | ");
			writeLink(getString("baby:Envelope.Privacy"), getPageURL(PrivacyPage.COMMAND));
			write("</td></tr></table>");
		}
		else
		{
			write("<div align=center>");
			writeImage("baby/kaiser-permanente-logo.png", appOwner, "http://www.kp.org");
			write("<br>");
			writeEncode(getString("baby:Envelope.Copyright", String.valueOf(cal.get(Calendar.YEAR)), appOwner));
			write("<br>");
			writeLink(getString("baby:Envelope.Terms"), getPageURL(TermsPage.COMMAND));
			write(" | ");
			writeLink(getString("baby:Envelope.Privacy"), getPageURL(PrivacyPage.COMMAND));
			write("</div>");
		}
	}

//	@Override
//	protected String getApplicationLogo()
//	{
//		return "baby/email-banner.png";
//	}
//
	@Override
	protected String getOwnerLogo()
	{
		return "baby/kaiser-permanente-logo.png";
	}
}
