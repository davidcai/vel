package baby.pages.profile;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.WideLinkGroupControl;
import samoyan.controls.WideLinkGroupControl.WideLink;
import samoyan.core.Day;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;
import baby.database.BabyStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public final class PregnancyProfilePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_PROFILE;
	
	private Date gmtToLocalTimeZone(Date date)
	{
		return new Day(TimeZoneEx.GMT, date).getDayStart(getTimeZone());
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Pregnancy.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		UUID userID = getContext().getUserID();
		Mother mother = MotherStore.getInstance().loadByUserID(userID);
		
		writeHorizontalNav(PregnancyProfilePage.COMMAND);

		WideLinkGroupControl wlg = new WideLinkGroupControl(this);
		
		// Stage
		WideLink wl = wlg.addLink()
			.setTitle(getString("babyprofile:Pregnancy.Stage"))
			.setURL(getPageURL(StagePage.COMMAND));
		if (mother.getDueDate()!=null)
		{
			wl.setValue(getString("babyprofile:Pregnancy.Pregnancy", gmtToLocalTimeZone(mother.getDueDate())));
		}
		else if (mother.getBirthDate()!=null)
		{
			wl.setValue(getString("babyprofile:Pregnancy.Infancy", gmtToLocalTimeZone(mother.getBirthDate())));
		}
		else
		{
			wl.setValue(getString("babyprofile:Pregnancy.Preconception"));
		}

		// Babies
		// !$! TODO: print "Twins (male, female)" or "David, Melissa" or Unspecified
		// Always return at least 1 baby from BabyStore?
		List<UUID> babyIDs = BabyStore.getInstance().getAtLeastOneBaby(userID);
		wlg.addLink()
			.setTitle(getString("babyprofile:Pregnancy.Babies", babyIDs.size()))
			.setValue(String.valueOf(babyIDs.size()))
			.setURL(getPageURL(BabiesPage.COMMAND));
					
		// Medical center
		wlg.addLink()
			.setTitle(getString("babyprofile:Pregnancy.MedicalCenter"))
			.setValue(!Util.isEmpty(mother.getMedicalCenter()) ? mother.getMedicalCenter() : getString("babyprofile:Pregnancy.EmptyField"))
			.setURL(getPageURL(MedicalCenterPage.COMMAND));
		
		wlg.render();
	}
}
