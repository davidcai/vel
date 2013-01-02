package mind.app;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import mind.database.*;
import mind.pages.master.MasterTab;
import mind.pages.patient.coaching.CoachingTab;
import mind.pages.patient.reminders.RemindersTab;

import samoyan.apps.admin.AdminTab;
import samoyan.apps.master.PrivacyPage;
import samoyan.apps.master.TermsPage;
import samoyan.apps.messaging.MessagingTab;
import samoyan.apps.profile.ProfileTab;
import samoyan.controls.MetaTagControl;
import samoyan.database.*;
import samoyan.servlet.Setup;
import samoyan.servlet.RequestContext;
import samoyan.servlet.EnvelopePage;
import samoyan.servlet.EnvelopeTab;

public class MindEnvelopePage extends EnvelopePage
{
	private static EnvelopeTab masterTab = new MasterTab();
	private static EnvelopeTab adminTab = new AdminTab();
	private static EnvelopeTab remindersTab = new RemindersTab();
	private static EnvelopeTab coachingTab = new CoachingTab();
	private static EnvelopeTab accountTab = new ProfileTab();
	private static EnvelopeTab messagingTab = new MessagingTab();
		
	@Override
	protected List<EnvelopeTab> getTabs() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		boolean admin = (user!=null && PermissionStore.getInstance().isUserGrantedPermission(user.getID(), Permission.SYSTEM_ADMINISTRATION));
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		
//		// Check cache
//		String cacheKey = "mindtabs:";
//		if (user==null)
//		{
//			cacheKey += "nouser,";
//		}
//		if (admin)
//		{
//			cacheKey += "admin,";
//		}
//		if (patient!=null)
//		{
//			cacheKey += "account,";
//		}
//
//		List<EnvelopeTab> cached = (List<EnvelopeTab>) Cache.get(cacheKey);
//		if (cached!=null)
//		{
//			return cached;
//		}
		
		// Create new
		List<EnvelopeTab> result = new ArrayList<EnvelopeTab>(6);
		
		result.add(masterTab);
		
		if (admin)
		{
			result.add(adminTab);
		}
		
// !$! For demo 2012-11-05
if (user!=null && PermissionStore.getInstance().isUserGrantedPermission(user.getID(), "Demo"))
{
	result.add(messagingTab);
}
		
		if (patient!=null)
		{
			result.add(remindersTab);
			result.add(coachingTab);
			result.add(accountTab);
		}
		
//		// Cache for later use
//		Cache.insert(cacheKey, result);
		
		// Return
		return result;
	}

	
	@Override
	protected void renderHTMLMetaTagsAndIncludes() throws Exception
	{
		writeIncludeCSS("mind/mind.less");
		
		MetaTagControl ctrl = new MetaTagControl(this);
		ctrl.appleTouchIcon(false, "mind/apple-touch-icon.png", null);
		ctrl.appleTouchStartupImage("mind/iphone-splash.png", null, null, null, null);
		ctrl.favicon("mind/favicon.png");
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
			writeImage("mind/footer-logo.png", appOwner, "http://www.kp.org");
			write("</td><td align=right>");
			writeEncode(getString("mind:App.Copyright", String.valueOf(cal.get(Calendar.YEAR)), appOwner));
			write(" | ");
			writeLink(getString("mind:Nav.Terms"), getPageURL(TermsPage.COMMAND));
			write(" | ");
			writeLink(getString("mind:Nav.Privacy"), getPageURL(PrivacyPage.COMMAND));
			write("</td></tr></table>");
		}
		else
		{
			write("<div align=center>");
			writeImage("mind/footer-logo.png", appOwner, "http://www.kp.org");
			write("<br>");
			writeEncode(getString("mind:App.Copyright", String.valueOf(cal.get(Calendar.YEAR)), appOwner));
			write("<br>");
			writeLink(getString("mind:Nav.Terms"), getPageURL(TermsPage.COMMAND));
			write(" | ");
			writeLink(getString("mind:Nav.Privacy"), getPageURL(PrivacyPage.COMMAND));
			write("</div>");
		}
	}

	@Override
	protected String getApplicationLogo()
	{
		return "mind/email-banner.png";
	}

	@Override
	protected String getOwnerLogo()
	{
		return "mind/email-logo.png";
	}
}
