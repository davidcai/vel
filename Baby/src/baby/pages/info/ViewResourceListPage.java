package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.controls.SelectInputControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.Setup;
import samoyan.servlet.UserAgent;
import baby.app.BabyConsts;
import baby.database.Article;
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
		
		write("<table width=\"100%\">");
		for (UUID articleID : articleIDs)
		{
			Article article = ArticleStore.getInstance().load(articleID);
			
			write("<tr><td>");
			writeLink(article.getTitle(), getPageURL(ViewArticlePage.COMMAND, new ParameterMap(ViewArticlePage.PARAM_ID, article.getID().toString())));
			if (!Util.isEmpty(article.getSubSection()))
			{
				write(" <span class=Faded>(");
				writeEncode(article.getSubSection());
				write(")</span>");
			}
			write("<br>");
			String summary = article.getSummary();
			if (Util.isEmpty(summary))
			{
				summary = article.getPlainText();
			}
			writeEncode(Util.getTextAbstract(summary, Article.MAXSIZE_SUMMARY));
			write("</td></tr>");
			write("<tr><td>&nbsp;</td></tr>");
		}
		write("</table>");
		
		// Additional resources
		write("<br><hr><br>");
		writeEncode(getString("information:Resources.AdditionalResources"));
		write("<br><ul>");
		write("<li>");
		writeLink(getString("information:Resources.OnlinePregnancyCenter"), "https://healthy.kaiserpermanente.org/health/poc?uri=center:pregnancy&article=EEA2B18C-B19C-11E0-B461-CB58EEF22C59");
		write("</li>");
		UserAgent ua = getContext().getUserAgent();
		// !$! Confirm links are correct
		// If app already installed on the device, have link to open the app directly
		String ios = "https://itunes.apple.com/us/app/kaiser-permanente/id493390354?mt=8";
		String android = "https://play.google.com/store/apps/details?id=org.kp.m";
		if (!ua.isAndroid())
		{
			write("<li>");
			writeLink(getString("information:Resources.IOS", Setup.getAppOwner(getLocale())), ios);
			write("</li>");
		}
		if (!ua.isIOS())
		{
			write("<li>");
			writeLink(getString("information:Resources.Android", Setup.getAppOwner(getLocale())), android);
			write("</li>");
		}
		write("</ul>");
	}
}
