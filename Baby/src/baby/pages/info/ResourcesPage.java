package baby.pages.info;

import java.util.List;
import java.util.UUID;

import samoyan.controls.SelectInputControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;
import baby.pages.profile.MedicalCenterPage;

public class ResourcesPage extends BabyPage
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

		writeHorizontalNav(ResourcesPage.COMMAND);

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
		List<UUID> articleIDs = ArticleStore.getInstance().queryByMedicalCenter(mother.getRegion(), medCenter);
		
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
		
		for (UUID articleID : articleIDs)
		{
			Article article = ArticleStore.getInstance().load(articleID);
			
			writeLink(article.getTitle(), getPageURL(ArticlePage.COMMAND, new ParameterMap(ArticlePage.PARAM_ID, article.getID().toString())));
			write(" <span class=Faded>(");
			writeEncode(article.getSection());
			write(")</span>");
			write("<br>");
			String summary = article.getSummary();
			if (Util.isEmpty(summary))
			{
				summary = article.getPlainText();
			}
			writeEncode(Util.getTextAbstract(summary, Article.MAXSIZE_SUMMARY));
			write("<br><br>");
		}
		
		// Additional resources
		write("<br><hr><br>");
		writeEncode(getString("information:Resources.AdditionalResources"));
		write("<br><ul>");
		write("<li>");
		writeLink(getString("information:Resources.OnlinePregnancyCenter"), "https://healthy.kaiserpermanente.org/health/poc?uri=center:pregnancy&article=EEA2B18C-B19C-11E0-B461-CB58EEF22C59");
		write("</li>");
		write("<li>");
		writeLink(getString("information:Resources.MedicalRecords"), ""); // !$! Need link from KP
		write("</li>");
		write("</ul>");
	}
}
