package baby.pages.content;

import java.util.List;

import samoyan.servlet.exc.RedirectException;

import baby.database.ArticleStore;
import baby.database.ChecklistStore;
import baby.pages.BabyPage;

public class DeleteAllPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/delete-all";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:DeleteAll.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("content:DeleteAll.Help"));
		write("<br><br>");
		
		writeFormOpen();
		
		List<String> sections = ArticleStore.getInstance().getSections();
		for (String s : sections)
		{
			int count = ArticleStore.getInstance().queryBySection(s).size();
			writeCheckbox("a_" + s, s, false);
			write(" <span class=Faded>(");
			writeEncodeLong(count);
			write(")</span>");
			write("<br>");
		}
		
		int count = ChecklistStore.getInstance().getAllStandard().size();
		writeCheckbox("c", getString("content:DeleteAll.Checklists"), false);
		write(" <span class=Faded>(");
		writeEncodeLong(count);
		write(")</span>");
		write("<br>");
		
		write("<br>");
		writeRemoveButton();
		
		writeFormClose();
	}
	
	@Override
	public void commit() throws Exception
	{
		List<String> sections = ArticleStore.getInstance().getSections();
		for (String s : sections)
		{
			if (isParameter("a_" + s))
			{
				ArticleStore.getInstance().removeMany( ArticleStore.getInstance().queryBySection(s) );
			}
		}
		if (isParameter("c"))
		{
			ChecklistStore.getInstance().removeMany( ChecklistStore.getInstance().getAllStandard() );
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), null);
	}
}
