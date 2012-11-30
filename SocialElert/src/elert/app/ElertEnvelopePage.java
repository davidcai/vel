package elert.app;

import java.util.ArrayList;
import java.util.List;

import samoyan.apps.admin.AdminTab;
import samoyan.apps.guidedsetup.GuidedSetupTab;
import samoyan.apps.master.LoginPage;
import samoyan.apps.profile.ProfileTab;
import samoyan.controls.MetaTagControl;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.EnvelopePage;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import elert.pages.govern.GovernTab;
import elert.pages.master.MasterTab;
import elert.pages.patient.PatientTab;
import elert.pages.physician.PhysicianTab;
import elert.pages.schedule.ScheduleTab;

public class ElertEnvelopePage extends EnvelopePage
{
	private EnvelopeTab masterTab = new MasterTab();
	private EnvelopeTab adminTab = new AdminTab();
	private EnvelopeTab governTab = new GovernTab();
	private EnvelopeTab patientTab = new PatientTab();
	private EnvelopeTab schedulerTab = new ScheduleTab();
	private EnvelopeTab profileTab = new ProfileTab();
	private EnvelopeTab guidedSetupTab = new GuidedSetupTab();
	private EnvelopeTab physicianTab = new PhysicianTab();
	
	@Override
	protected void renderHTMLNavBar() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		// User name
		boolean smartPhone = ctx.getUserAgent().isSmartPhone();
		if (!smartPhone && user!=null)
		{
			write("<div id=welcome>");
			write("<small>");
			writeEncode(getString("elert:Envelope.Welcome"));
			write("</small><br>");
			writeEncode(user.getDisplayName());
			write("</div>");
			
			Image avatar = user.getAvatar();
			if (avatar!=null)
			{
				writeImage(avatar, ElertConsts.IMAGESIZE_SQUARE_150, user.getDisplayName(), null);
			}
		}

		// Render the nav bar of the current tab
		super.getCurrentTab().getNavTree(this).render();
	}

	@Override
	protected List<EnvelopeTab> getTabs() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		List<EnvelopeTab> result = new ArrayList<EnvelopeTab>();
		result.add(masterTab);
		
		if (user!=null)
		{
			String cmd1 = ctx.getCommand(1);
			
			if (user.isGuidedSetup())
			{
				result.add(guidedSetupTab);
			}
			
			if (!user.isGuidedSetup() || !cmd1.equals(UrlGenerator.COMMAND_SETUP))
			{
				if (PermissionStore.getInstance().isUserGrantedPermission(user.getID(), Permission.SYSTEM_ADMINISTRATION))
				{
					result.add(adminTab);
				}
				if (PermissionStore.getInstance().isUserGrantedPermission(user.getID(), ElertConsts.PERMISSION_APPLICATION_GOVERNMENT))
				{
					result.add(governTab);
				}
				if (PermissionStore.getInstance().isUserGrantedPermission(user.getID(), ElertConsts.PERMISSION_SCHEDULING))
				{
					result.add(schedulerTab);
				}
				if (PermissionStore.getInstance().isUserGrantedPermission(user.getID(), ElertConsts.PERMISSION_PHYSICIAN))
				{
					result.add(physicianTab);
				}
				result.add(patientTab);
				result.add(profileTab);
			}
		}
		
		return result;
	}
	
	@Override
	protected void renderHTMLMetaTagsAndIncludes() throws Exception
	{		
		MetaTagControl ctrl = new MetaTagControl(this);
		ctrl.appleTouchIcon("elert/apple-touch-icon.png");
		ctrl.appleTouchStartupImage("elert/iphone-splash.png");
		ctrl.favicon("elert/favicon.png");
		ctrl.render();
		
		writeIncludeCSS("elert/elert.less");
	}
	
	@Override
	protected String getApplicationLogo()
	{
		return "elert/email-banner.png";
	}

	@Override
	protected String getOwnerLogo()
	{
		return "elert/email-logo.png";
	}

	@Override
	protected void renderSimpleHTMLFooter() throws Exception
	{
		if (getContext().getChannel().equals(Channel.EMAIL))
		{
			StringBuffer link = new StringBuffer();
			link.append("<a href=\"");
			link.append(getPageURL(LoginPage.COMMAND));
			link.append("\">");
			link.append(Util.htmlEncode(getString("elert:Envelope.Login", Setup.getAppTitle(getLocale())).toUpperCase(getLocale())));
			link.append("</a>");
			
			String pattern = Util.htmlEncode(getString("elert:Envelope.EmailFooter", "$LINK$")).toUpperCase(getLocale());
			pattern = Util.strReplace(pattern, "$LINK$", link.toString());
			write(pattern);
			
			// Logo
			write("<div align=right>");
			writeImage(getOwnerLogo(), Setup.getAppOwner(getLocale()));
			write("</div>");
		}
	}
	
	@Override
	protected void renderVoiceXMLUnresponsive() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		write("<prompt bargein=\"false\">");
		
		writeEncode(getString("elert:Envelope.VoiceUnresponsive", Setup.getAppTitle(getLocale()), user.getName()));
		write("<break time=\"1000ms\"/>");
		
		writeEncode(getString("elert:Envelope.VoiceRepeat"));
//		write("<audio src=\"");
//		write(getResourceURL("elert/marimbamix.mp3"));
//		write("\"/>");
		write("<break time=\"1000ms\"/>");
				
		writeEncode(getString("elert:Envelope.VoiceUnresponsive", Setup.getAppTitle(getLocale()), user.getName()));
		write("<break time=\"500ms\"/>");
		
		write("</prompt>");
	}

}
