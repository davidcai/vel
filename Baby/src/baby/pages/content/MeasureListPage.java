package baby.pages.content;

import java.util.List;
import java.util.UUID;

import baby.database.MeasureStore;
import baby.pages.BabyPage;

public class MeasureListPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure-list";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:MeasureList.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<h2>");
		writeEncode(getString("content:MeasureList.MomMeasures"));
		write("</h2>");
		
		//new LinkToolbarControl(this).addLink(getString(""), url, icon).render();
		
		List<UUID> momMeasureIDs = MeasureStore.getInstance().getMeasures(true);
		if (momMeasureIDs != null && momMeasureIDs.isEmpty() == false)
		{
			
		}
		else
		{
			writeEncode(getString("content:MeasureList.NoMeasures"));
		}
		
		write("<h2>");
		writeEncode(getString("content:MeasureList.BabyMeasures"));
		write("</h2>");
		
		List<UUID> babyMeasureIDs = MeasureStore.getInstance().getMeasures(false);
		if (babyMeasureIDs != null && babyMeasureIDs.isEmpty() == false)
		{
			
		}
		else
		{
			writeEncode(getString("content:MeasureList.NoMeasures"));
		}		
	}
}
