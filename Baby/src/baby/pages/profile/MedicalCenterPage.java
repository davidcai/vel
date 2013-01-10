package baby.pages.profile;

import java.util.List;

import samoyan.apps.profile.ProfilePage;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.AfterCommitRedirectException;
import samoyan.servlet.exc.WebFormException;

import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public class MedicalCenterPage extends BabyPage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/medical-center";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:MedicalCenter.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		
		// Help
		writeEncode(getString("babyprofile:MedicalCenter.Help", Setup.getAppTitle(getLocale())));
		write("<br><br>");
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Dropdown of regions
		twoCol.writeRow(getString("babyprofile:MedicalCenter.Region"));
		
		List<String> regions = ArticleStore.getInstance().getRegions();
		
		SelectInputControl select = new SelectInputControl(twoCol, "region");
		select.addOption("", "");
		for (String region : regions)
		{
			select.addOption(region, region);
		}
		select.setInitialValue(mother.getRegion()==null? "" : mother.getRegion());
		select.setAttribute("onchange", "$('#x'+this.selectedIndex).removeClass('NoShow').siblings().addClass('NoShow');");
		select.render();
		
		String currentRegion = getParameterString("region");
		if (currentRegion==null)
		{
			currentRegion = mother.getRegion();
		}
		
		twoCol.writeSpaceRow();
		
		// Dropdowns for medical regions
		twoCol.writeRow(getString("babyprofile:MedicalCenter.MedicalCenter"));

		twoCol.write("<div>");
		twoCol.write("<span id=x0 class=\"");
		if (currentRegion!=null)
		{
			twoCol.write("NoShow");
		}
		twoCol.write("\">");
		twoCol.write(getString("babyprofile:MedicalCenter.NoRegion"));
		twoCol.write("</span>");
		int i = 1;
		for (String region : regions)
		{
			List<String> centers = ArticleStore.getInstance().getMedicalCenters(region);
			
			select = new SelectInputControl(twoCol, region);
			select.addOption("", "");
			for (String center : centers)
			{
				select.addOption(center, center);
			}
			select.setInitialValue(mother.getMedicalCenter()==null? "" : mother.getMedicalCenter());
			if (currentRegion==null || currentRegion.equals(region)==false)
			{
				select.addCssClass("NoShow");
			}
			select.setAttribute("id", "x"+i);
			select.render();
			
			i++;
		}
		twoCol.write("</div>");
		
		twoCol.render();
		write("<br>");
				
		writeSaveButton(mother);
		writeFormClose();
	}
	
	@Override
	public void validate() throws Exception
	{
		String region = validateParameterString("region", 1, -1);
		List<String> regions = ArticleStore.getInstance().getRegions();
		if (regions.contains(region)==false)
		{
			throw new WebFormException("region", getString("common:Errors.InvalidValue"));
		}
		
		String center = validateParameterString(region, 1, -1);
		List<String> centers = ArticleStore.getInstance().getMedicalCenters(region);
		if (centers.contains(center)==false)
		{
			throw new WebFormException(region, getString("common:Errors.InvalidValue"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		String region = getParameterString("region");
		String center = getParameterString(region);
		
		Mother mother = MotherStore.getInstance().openByUserID(getContext().getUserID());
		mother.setRegion(region);
		mother.setMedicalCenter(center);
		MotherStore.getInstance().save(mother);
		
		// Redirect to parent
		progressGuidedSetup();
		throw new AfterCommitRedirectException();
	}
}
