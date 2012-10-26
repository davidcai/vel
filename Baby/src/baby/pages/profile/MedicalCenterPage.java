package baby.pages.profile;

import java.util.List;

import samoyan.apps.profile.ProfilePage;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

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
		String current = null;
		if (!Util.isEmpty(mother.getMedicalCenter()))
		{
			current = mother.getRegion() + "|" + mother.getMedicalCenter();
		}
		
		int COLS = 5;
		if (getContext().getUserAgent().isSmartPhone())
		{
			COLS = 2;
		}
		
		writeFormOpen();
		
		List<String> regions = ArticleStore.getInstance().getRegions();
		for (String region : regions)
		{
			write("<b>");
			writeEncode(region);
			write("</b><br>");
			
			List<String> centers = ArticleStore.getInstance().getMedicalCenters(region);
			write("<table width=\"100%\">");
			for (int i=0; i<COLS; i++)
			{
				write("<col width=\"");
				write(100/COLS);
				write("%\">");
			}
			for (int i=0; i<centers.size(); i++)
			{
				if (i%COLS==0)
				{
					write("<tr>");
				}
				write("<td>");
				
				String center = centers.get(i);
				writeRadioButton("radio", center, region + "|" + center, current);
				
				write("</td>");
				if (i%COLS==COLS-1)
				{
					write("</tr>");
				}
			}
			if (centers.size()%COLS!=0)
			{
				write("<td colspan=");
				write(COLS-centers.size()%COLS);
				write(">");
				write("&nbsp;</td></tr>");
			}
			write("</table><br>");
		}
		
		writeSaveButton(mother);
		writeFormClose();
	}
	
	@Override
	public void commit() throws Exception
	{
		String radio = getParameterString("radio");
		int p = radio.indexOf("|");
		String region = radio.substring(0, p);
		String center = radio.substring(p+1);
		
		Mother mother = MotherStore.getInstance().openByUserID(getContext().getUserID());
		mother.setRegion(region);
		mother.setMedicalCenter(center);
		MotherStore.getInstance().save(mother);
		
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}
}
