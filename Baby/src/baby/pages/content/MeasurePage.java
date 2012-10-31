package baby.pages.content;

import baby.pages.BabyPage;

public class MeasurePage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure";

	@Override
	public void renderHTML() throws Exception
	{
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:Measure.Title");
	}
}
