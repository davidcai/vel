package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.controls.SelectInputControl;
import samoyan.core.Util;
import samoyan.servlet.Setup;
import samoyan.servlet.UserAgent;
import baby.app.BabyConsts;
import baby.controls.ArticleListControl;
import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;
import baby.pages.profile.MedicalCenterPage;

public class ViewResourceListPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/resources";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Resources.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		List<String> medicalCenters = ArticleStore.getInstance().getMedicalCenters(mother.getRegion());

//		writeHorizontalNav(ViewResourceListPage.COMMAND);

		if (Util.isEmpty(mother.getMedicalCenter()) || medicalCenters.contains(mother.getMedicalCenter())==false)
		{
			StringBuilder link = new StringBuilder();
			link.append("<a href=\"");
			link.append(getPageURL(MedicalCenterPage.COMMAND));
			link.append("\">");
			link.append("$text$");
			link.append("</a>");

			String pattern = Util.textToHtml(getString("information:Resources.NoMedicalCenter", "$link$"));
			pattern = Util.strReplace(pattern, "$link$", link.toString());
			pattern = Util.strReplace(pattern, "$text$", Util.textToHtml(getString("information:Resources.ChooseMedicalCenter")));
			write(pattern);
			return;
		}
		
		String medCenter = getParameterString("center");
		if (Util.isEmpty(medCenter))
		{
			medCenter = mother.getMedicalCenter();
		}
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySectionAndMedicalCenter(BabyConsts.SECTION_RESOURCE, mother.getRegion(), medCenter);
		
		// Medical center drop down
		writeFormOpen("GET", null);
		write("<table><tr valign=middle><td>");
		writeEncode(getString("information:Resources.FoundResources", articleIDs.size()));
		write("</td><td>");
		SelectInputControl select = new SelectInputControl(this, "center");
		for (String m : medicalCenters)
		{
			select.addOption(m, m);
		}
		select.setInitialValue(medCenter);
		select.setAutoSubmit(true);
		select.render();
		write("</td></tr></table><br>");
		writeFormClose();
		
		// Render resources
		new ArticleListControl(this, articleIDs).showImages(false).showRegion(false).showSummary(!phone).render();
		
		// Additional resources
		write("<br><hr><br>");
		writeEncode(getString("information:Resources.AdditionalResources"));
		write("<br><ul>");
		write("<li>");
		write("<a target=_blank href=\"");
		writeEncode("https://healthy.kaiserpermanente.org/health/poc?uri=center:pregnancy&article=EEA2B18C-B19C-11E0-B461-CB58EEF22C59");
		write("\">");
		writeEncode(getString("information:Resources.OnlinePregnancyCenter"));
		write("</a>");
//		writeLink(getString("information:Resources.OnlinePregnancyCenter"), "https://healthy.kaiserpermanente.org/health/poc?uri=center:pregnancy&article=EEA2B18C-B19C-11E0-B461-CB58EEF22C59");
		write("</li>");
		UserAgent ua = getContext().getUserAgent();
		// !$! Confirm with customer that these links are correct
		// If app already installed on the device, have link to open the app directly
		String ios = "https://itunes.apple.com/us/app/kaiser-permanente/id493390354?mt=8";
		String android = "https://play.google.com/store/apps/details?id=org.kp.m";
		if (!ua.isAndroid())
		{
			write("<li>");
			write("<a target=_blank href=\"");
			writeEncode(ios);
			write("\">");
			writeEncode(getString("information:Resources.IOS", Setup.getAppOwner(getLocale())));
			write("</a>");
//			writeLink(getString("information:Resources.IOS", Setup.getAppOwner(getLocale())), ios);
			write("</li>");
		}
		if (!ua.isIOS())
		{
			write("<li>");
			write("<a target=_blank href=\"");
			writeEncode(android);
			write("\">");
			writeEncode(getString("information:Resources.Android", Setup.getAppOwner(getLocale())));
			write("</a>");
//			writeLink(getString("information:Resources.Android", Setup.getAppOwner(getLocale())), android);
			write("</li>");
		}
		write("</ul>");
	}
}
