package baby.pages.content;

import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import baby.database.ArticleStore;
import baby.database.ChecklistStore;
import baby.database.MeasureStore;
import baby.pages.BabyPage;

public final class ContentHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT;
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:Home.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Articles
		List<String> sections = ArticleStore.getInstance().getSections();
		for (String section : sections)
		{			
			List<UUID> articleIDs = ArticleStore.getInstance().queryBySection(section);
			twoCol.writeRow(section);
			twoCol.writeEncodeLong(articleIDs.size());
		}
		
		if (sections.size()>0)
		{
			twoCol.writeSpaceRow();
		}
		
		// Checklists
		twoCol.writeRow(getString("content:Home.Checklists"));
		twoCol.writeEncodeLong(ChecklistStore.getInstance().getAll().size());
		
		twoCol.writeSpaceRow();

		// Measures
		twoCol.writeRow(getString("content:Home.Measures"));
		twoCol.writeEncodeLong(MeasureStore.getInstance().getAll().size());
		
		twoCol.render();
	}
}
