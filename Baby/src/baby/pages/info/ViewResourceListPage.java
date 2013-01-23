package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
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
		List<String> categories = ArticleStore.getInstance().getSubSections(BabyConsts.SECTION_RESOURCE);
		
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
		String category = getParameterString("category");
		
		List<UUID> articleIDs = ArticleStore.getInstance().queryBySectionAndMedicalCenter(BabyConsts.SECTION_RESOURCE, category, mother.getRegion(), medCenter);

		// Search box
		write("<div class=ArticleSearchBox>");
		writeFormOpen("GET", SearchResourcesPage.COMMAND);
		writeTextInput(SearchResourcesPage.PARAM_QUERY, null, 30, 128);
		write(" ");
		new ButtonInputControl(this, null)
			.setValue(getString("controls:Button.Search"))
			.setMobileHotAction(true)
			.render();
		writeFormClose();
		write("</div>");
		
		// Medical center + sub-sections drop downs
		writeFormOpen("GET", null);
		write("<table><tr valign=middle><td>");
		if (!phone)
		{
			writeEncode(getString("information:Resources.FoundResources", articleIDs.size()));
		}
		else
		{
			writeEncode(getString("information:Resources.MedicalCenter"));
		}
		write("</td><td>");
		SelectInputControl select = new SelectInputControl(this, "center");
		for (String m : medicalCenters)
		{
			select.addOption(m, m);
		}
		select.setInitialValue(medCenter);
		select.setAutoSubmit(true);
		select.render();
		write("</td>");
		if (phone)
		{
			write("</tr><tr valign=middle>");
		}
		write("<td>");
		writeEncode(getString("information:Resources.Categories", articleIDs.size()));
		write("</td><td>");
		select = new SelectInputControl(this, "category");
		select.addOption(getString("information:Resources.AllCategories"), "");
		for (String m : categories)
		{
			select.addOption(m, m);
		}
		select.setInitialValue(category);
		select.setAutoSubmit(true);
		select.render();
		write("</td></tr></table>");
		writeFormClose();
		
		write("<br>");

		// Render resources
		if (articleIDs.size()>0)
		{
			new ArticleListControl(this, articleIDs).showImages(false).showRegion(false).showSummary(!phone).render();
		}
		else
		{
			writeEncode(getString("information:Resources.NoResults"));
			write("<br>");
		}
		
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
