package baby.pages.info;

import java.util.List;
import java.util.UUID;

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
		StringBuilder link = new StringBuilder();
		link.append("<a href=\"");
		link.append(getPageURL(MedicalCenterPage.COMMAND));
		link.append("\">");
		link.append("$text$");
		link.append("</a>");
		
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		if (Util.isEmpty(mother.getMedicalCenter()))
		{
			String pattern = Util.textToHtml(getString("information:Resources.NoMedicalCenter", "$link$"));
			pattern = Util.strReplace(pattern, "$link$", link.toString());
			pattern = Util.strReplace(pattern, "$text$", Util.textToHtml(getString("information:Resources.ChooseMedicalCenter")));
			write(pattern);
			return;
		}
		
		List<UUID> articleIDs = ArticleStore.getInstance().queryByMedicalCenter(mother.getRegion(), mother.getMedicalCenter());
		
		String pattern = Util.textToHtml(getString("information:Resources.FoundResources", articleIDs.size(), "$link$"));
		pattern = Util.strReplace(pattern, "$link$", link.toString());
		pattern = Util.strReplace(pattern, "$text$", mother.getMedicalCenter());
		write(pattern);
		write("<br><br>");
		
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
