package baby.pages.journey;

import baby.pages.BabyPage;

public class PhotoPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/photo";
	
	public final static String PARAM_ID = "id";
	
	@Override
	public void renderHTML() throws Exception
	{
		writeEncode("Photo");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Photo.Title");
	}
}
