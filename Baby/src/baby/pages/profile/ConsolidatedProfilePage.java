package baby.pages.profile;

import java.util.UUID;

import samoyan.apps.profile.ChangeLoginNamePage;
import samoyan.apps.profile.ChangePasswordPage;
import samoyan.apps.profile.EmailPage;
import samoyan.apps.profile.MobilePage;
import samoyan.apps.profile.PhonePage;
import samoyan.apps.profile.ProfilePage;
import samoyan.apps.profile.RealNamePage;
import samoyan.apps.profile.TimeZonePage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
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
	public final static String COMMAND = ProfilePage.COMMAND + "/consolidated";
			
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
				
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Real name
		twoCol.writeRow(getString("babyprofile:Consolidated.Name"));
		if (!Util.isEmpty(user.getName()))
		{
			twoCol.writeEncode(user.getName());
		}
		else
		{
			twoCol.write("<span class=Faded>");
			twoCol.writeEncode(getString("babyprofile:Consolidated.EmptyField"));
			twoCol.write("</span>");
		}
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(RealNamePage.COMMAND));
		twoCol.write("</small>");
		
		// Login name
		twoCol.writeRow(getString("babyprofile:Consolidated.LoginName"));
		twoCol.writeEncode(user.getLoginName());
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(ChangeLoginNamePage.COMMAND));
		twoCol.write("</small>");
		
		// Password
		twoCol.writeRow(getString("babyprofile:Consolidated.Password"));
		twoCol.writeEncode("********");
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(ChangePasswordPage.COMMAND));
		twoCol.write("</small>");
		
		twoCol.writeSpaceRow();
		
		// Email
		twoCol.writeRow(getString("babyprofile:Consolidated.Email"));
		twoCol.writeEncode(user.getEmail());
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(EmailPage.COMMAND));
		twoCol.write("</small>");
		
		// Mobile
		if (fed.isChannelEnabled(Channel.SMS))
		{
			twoCol.writeRow(getString("babyprofile:Consolidated.Mobile"));
			if (!Util.isEmpty(user.getMobile()))
			{
				twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(user.getMobile()));
			}
			else
			{
				twoCol.write("<span class=Faded>");
				twoCol.writeEncode(getString("babyprofile:Consolidated.EmptyField"));
				twoCol.write("</span>");
			}
			twoCol.write(" <small>");
			twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(MobilePage.COMMAND));
			twoCol.write("</small>");
		}
		
		// Phone
		if (fed.isChannelEnabled(Channel.VOICE))
		{
			twoCol.writeRow(getString("babyprofile:Consolidated.Phone"));
			if (!Util.isEmpty(user.getPhone()))
			{
				twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(user.getPhone()));
			}
			else
			{
				twoCol.write("<span class=Faded>");
				twoCol.writeEncode(getString("babyprofile:Consolidated.EmptyField"));
				twoCol.write("</span>");
			}
			twoCol.write(" <small>");
			twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(PhonePage.COMMAND));
			twoCol.write("</small>");
		}

		twoCol.writeSpaceRow();
		
		// Medical center
		twoCol.writeRow(getString("babyprofile:Consolidated.MedicalCenter"));
		if (!Util.isEmpty(mother.getMedicalCenter()))
		{
			twoCol.writeEncode(mother.getMedicalCenter());
		}
		else
		{
			twoCol.write("<span class=Faded>");
			twoCol.writeEncode(getString("babyprofile:Consolidated.EmptyField"));
			twoCol.write("</span>");
		}
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(MedicalCenterPage.COMMAND));
		twoCol.write("</small>");
		
		twoCol.writeSpaceRow();
		
		// Units
		twoCol.writeRow(getString("babyprofile:Consolidated.Units"));
		if (mother.isMetric()==false)
		{
			twoCol.writeEncode(getString("babyprofile:Consolidated.Imperial"));
		}
		else
		{
			twoCol.writeEncode(getString("babyprofile:Consolidated.Metric"));
		}
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(UnitsPage.COMMAND));
		twoCol.write("</small>");

		// Time zone
		twoCol.writeRow(getString("babyprofile:Consolidated.TimeZone"));
		twoCol.writeEncode(TimeZoneEx.getDisplayString(user.getTimeZone(), getLocale()));
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(TimeZonePage.COMMAND));
		twoCol.write("</small>");

		twoCol.render();
	}
}
