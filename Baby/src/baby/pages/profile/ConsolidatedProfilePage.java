package baby.pages.profile;

import java.util.TimeZone;
import java.util.UUID;

import samoyan.apps.master.LogoutPage;
import samoyan.apps.profile.ChangeLoginNamePage;
import samoyan.apps.profile.ChangePasswordPage;
import samoyan.apps.profile.CloseAccountPage;
import samoyan.apps.profile.EmailPage;
import samoyan.apps.profile.MobilePage;
import samoyan.apps.profile.PhonePage;
import samoyan.apps.profile.ProfilePage;
import samoyan.apps.profile.RealNamePage;
import samoyan.apps.profile.TimeZonePage;
import samoyan.controls.ButtonInputControl;
import samoyan.controls.WideLinkGroupControl;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public class ConsolidatedProfilePage extends BabyPage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/account";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Consolidated.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		UUID userID = getContext().getUserID();
		User user = UserStore.getInstance().load(userID);
		Mother mother = MotherStore.getInstance().loadByUserID(userID);
		Server fed = ServerStore.getInstance().loadFederation();
		
		writeHorizontalNav(ConsolidatedProfilePage.COMMAND);

		WideLinkGroupControl wlg = new WideLinkGroupControl(this);
		
		// Real name
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Name"))
			.setValue(!Util.isEmpty(user.getName()) ? user.getName() : getString("babyprofile:Consolidated.EmptyField"))
			.setURL(getPageURL(RealNamePage.COMMAND));
		
		
		// Login name
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.LoginName"))
			.setValue(user.getLoginName())
			.setURL(getPageURL(ChangeLoginNamePage.COMMAND));
		
		// Password
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Password"))
			.setValue("********")
			.setURL(getPageURL(ChangePasswordPage.COMMAND));

		
		// Email
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.Email"))
			.setValue(user.getEmail())
			.setURL(getPageURL(EmailPage.COMMAND));
		
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
				.setURL(getPageURL(MobilePage.COMMAND));
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
			.setURL(getPageURL(UnitsPage.COMMAND));

		// Time zone
		TimeZone tz = user.getTimeZone();
		if (tz==null)
		{
			tz = getTimeZone();
		}
		wlg.addLink()
			.setTitle(getString("babyprofile:Consolidated.TimeZone"))
			.setValue(TimeZoneEx.getDisplayString(tz, getLocale()))
			.setURL(getPageURL(TimeZonePage.COMMAND));

		wlg.render();
		
		// Logout and close account
		write("<br><br><table><tr><td>");
		writeFormOpen("GET", LogoutPage.COMMAND);
		writeButton(getString("babyprofile:Consolidated.Logout"));
		writeFormClose();
		write("</td><td>");
		writeFormOpen("GET", CloseAccountPage.COMMAND);
		new ButtonInputControl(this, null)
			.setStrong(true)
			.setValue(getString("babyprofile:Consolidated.Unsubscribe"))
			.render();
		writeFormClose();
		write("</td></tr></table>");
	}
}
