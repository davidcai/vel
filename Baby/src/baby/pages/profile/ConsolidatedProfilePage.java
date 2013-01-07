package baby.pages.profile;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import samoyan.apps.master.LogoutPage;
import samoyan.apps.profile.ChangeLoginNamePage;
import samoyan.apps.profile.ChangePasswordPage;
import samoyan.apps.profile.CloseAccountPage;
import samoyan.apps.profile.EmailPage;
import samoyan.apps.profile.MobilePage;
import samoyan.apps.profile.PhonePage;
import samoyan.apps.profile.RealNamePage;
import samoyan.apps.profile.TimeZonePage;
import samoyan.controls.ButtonInputControl;
import samoyan.controls.WideLinkGroupControl;
import samoyan.controls.WideLinkGroupControl.WideLink;
import samoyan.core.Day;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public final class ConsolidatedProfilePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_PROFILE;
		
	private Date gmtToLocalTimeZone(Date date)
	{
		return new Day(TimeZoneEx.GMT, date).getDayStart(getTimeZone());
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Consolidated.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		UUID userID = ctx.getUserID();
		Mother mother = MotherStore.getInstance().loadByUserID(userID);
		User user = UserStore.getInstance().load(userID);
		Server fed = ServerStore.getInstance().loadFederation();
		ParameterMap goBackParams = new ParameterMap(RequestContext.PARAM_GO_BACK_ON_SAVE, "");
		
		// - - -

		write("<h2>");
		writeEncode(getString("babyprofile:Consolidated.SubtitlePregnancy"));
		write("</h2>");
			
		WideLinkGroupControl wlg = new WideLinkGroupControl(this);
		
		// Stage
		WideLink wl = wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Stage"))
			.setURL(getPageURL(StagePage.COMMAND, goBackParams));
		if (mother.getDueDate()!=null)
		{
			wl.setValue(getString("babyprofile:Consolidated.Pregnancy", gmtToLocalTimeZone(mother.getDueDate())));
		}
		else if (mother.getBirthDate()!=null)
		{
			wl.setValue(getString("babyprofile:Consolidated.Infancy", gmtToLocalTimeZone(mother.getBirthDate())));
		}
		else
		{
			wl.setValue(getString("babyprofile:Consolidated.Preconception"));
		}

		// Babies
		List<UUID> babyIDs = BabyStore.getInstance().getAtLeastOneBaby(userID);
		String names = "";
		if (babyIDs.size()<=3)
		{
			for (UUID babyID : babyIDs)
			{
				Baby b = BabyStore.getInstance().load(babyID);
				if (Util.isEmpty(b.getName()))
				{
					names = "";
					break;
				}
				else
				{
					if (names.length()>0)
					{
						names += ", ";
					}
					names += b.getName();
				}
			}
		}
		else if (babyIDs.size()<=8)
		{
			names = getString("babyprofile:Consolidated.BabyCountName." + babyIDs.size());
		}
		else
		{
			names = getString("babyprofile:Consolidated.BabyCountName.N", babyIDs.size());
		}
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Babies", babyIDs.size()))
			.setValue(names)
			.setURL(getPageURL(BabiesPage.COMMAND, goBackParams));
					
		// Medical center
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.MedicalCenter"))
			.setValue(!Util.isEmpty(mother.getMedicalCenter()) ? mother.getMedicalCenter() : getString("babyprofile:Consolidated.EmptyField"))
			.setURL(getPageURL(MedicalCenterPage.COMMAND, goBackParams));
		
		wlg.render();

		// - - -
		
		write("<br><h2>");
		writeEncode(getString("babyprofile:Consolidated.SubtitleAccount"));
		write("</h2>");
			
		wlg = new WideLinkGroupControl(this);
		
		// Real name
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Name"))
			.setValue(!Util.isEmpty(user.getName()) ? user.getName() : getString("babyprofile:Consolidated.EmptyField"))
			.setURL(getPageURL(RealNamePage.COMMAND, goBackParams));
		
		// Login name
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.LoginName"))
			.setValue(user.getLoginName())
			.setURL(getPageURL(ChangeLoginNamePage.COMMAND, goBackParams));
		
		// Password
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Password"))
			.setValue("********")
			.setURL(getPageURL(ChangePasswordPage.COMMAND, goBackParams));

		
		// Email
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Email"))
			.setValue(user.getEmail())
			.setURL(getPageURL(EmailPage.COMMAND, goBackParams));
		
		// Mobile
		if (fed.isChannelEnabled(Channel.SMS))
		{
			String val;
			if (!Util.isEmpty(user.getMobile()))
			{
				val = Util.stripCountryCodeFromPhoneNumber(user.getMobile());
				MobileCarrier mc = MobileCarrierStore.getInstance().load(user.getMobileCarrierID());
				if (mc!=null)
				{
					val += " ";
					val += mc.getName();
				}
			}
			else
			{
				val = getString("babyprofile:Consolidated.EmptyField");
			}
			wlg.addLink()
				.setTitle(getString("babyprofile:Consolidated.Mobile"))
				.setValue(val)
				.setURL(getPageURL(MobilePage.COMMAND, goBackParams));
		}
		
		// Phone
		if (fed.isChannelEnabled(Channel.VOICE))
		{
			String val;
			if (!Util.isEmpty(user.getPhone()))
			{
				val = Util.stripCountryCodeFromPhoneNumber(user.getPhone());
			}
			else
			{
				val = getString("babyprofile:Consolidated.EmptyField");
			}
			wlg.addLink()
				.setTitle(getString("babyprofile:Consolidated.Phone"))
				.setValue(val)
				.setURL(getPageURL(PhonePage.COMMAND));
		}
				
		// Units
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Units"))
			.setValue(mother.isMetric()==false ? getString("babyprofile:Consolidated.Imperial") : getString("babyprofile:Consolidated.Metric"))
			.setURL(getPageURL(UnitsPage.COMMAND, goBackParams));

		// Time zone
		TimeZone tz = user.getTimeZone();
		if (tz==null)
		{
			tz = getTimeZone();
		}
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.TimeZone"))
			.setValue(tz.getDisplayName(getLocale()))
			.setURL(getPageURL(TimeZonePage.COMMAND, goBackParams));

		wlg.render();
		
		// - - -

		// Logout and close account
		write("<br>");
//		write("<table><tr><td>");
//		writeFormOpen("GET", LogoutPage.COMMAND);
//		writeButton(getString("babyprofile:Consolidated.Logout"));
//		writeFormClose();
//		write("</td><td>");
		writeFormOpen("GET", CloseAccountPage.COMMAND);
		new ButtonInputControl(this, null)
			.setStrong(true)
			.setValue(getString("babyprofile:Consolidated.Unsubscribe"))
			.render();
		writeFormClose();
//		write("</td></tr></table>");
		
		write("<div class=NoShow>");
		writeFormOpen("GET", LogoutPage.COMMAND);
		new ButtonInputControl(this, null)
			.setValue(getString("babyprofile:Consolidated.Logout"))
			.setMobileHotAction(true)
			.render();
		writeFormClose();
		write("</div>");
	}
}
