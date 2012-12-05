package baby.pages.profile;

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
import samoyan.apps.profile.ProfilePage;
import samoyan.apps.profile.RealNamePage;
import samoyan.apps.profile.TimeZonePage;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import baby.database.BabyStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public class ConsolidatedProfilePage extends BabyPage
{
	public final static String COMMAND = ProfilePage.COMMAND;
			
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
		
		if (getContext().getUserAgent().isSmartPhone())
		{
			new LinkToolbarControl(this)
				.addLink(getString("babyprofile:Consolidated.Logout"), getPageURL(LogoutPage.COMMAND), "icons/basic1/key_16.png")
				.addLink(getString("babyprofile:Consolidated.Unsubscribe"), getPageURL(CloseAccountPage.COMMAND), "icons/basic1/delete_16.png")
				.render();
		}
		
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
		
		// Stage
		twoCol.writeRow(getString("babyprofile:Consolidated.Stage"));
		if (mother.getDueDate()!=null)
		{
			twoCol.writeEncode(getString("babyprofile:Consolidated.Pregnancy", mother.getDueDate()));
		}
		else if (mother.getBirthDate()!=null)
		{
			twoCol.writeEncode(getString("babyprofile:Consolidated.Infancy", mother.getBirthDate()));
		}
		else
		{
			twoCol.writeEncode(getString("babyprofile:Consolidated.Preconception"));
		}
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(StagePage.COMMAND));
		twoCol.write("</small>");
		
		// Babies
		List<UUID> babyIDs = BabyStore.getInstance().getByUser(userID);
		twoCol.writeRow(getString("babyprofile:Consolidated.Babies", babyIDs.size()));
		twoCol.writeEncodeLong(babyIDs.size());
		// !$! TODO: print "Twins (male, female)" or "David, Melissa" or Unspecified
		// Always return at least 1 baby from BabyStore?
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(BabiesPage.COMMAND));
		twoCol.write("</small>");
		
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
				MobileCarrier mc = MobileCarrierStore.getInstance().load(user.getMobileCarrierID());
				if (mc!=null)
				{
					write(" ");
					writeEncode(mc.getName());
				}
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
		TimeZone tz = user.getTimeZone();
		if (tz==null)
		{
			tz = getTimeZone();
		}
		twoCol.writeRow(getString("babyprofile:Consolidated.TimeZone"));
		twoCol.writeEncode(TimeZoneEx.getDisplayString(tz, getLocale()));
		twoCol.write(" <small>");
		twoCol.writeLink(getString("babyprofile:Consolidated.Edit"), getPageURL(TimeZonePage.COMMAND));
		twoCol.write("</small>");

		twoCol.render();
	}
}
