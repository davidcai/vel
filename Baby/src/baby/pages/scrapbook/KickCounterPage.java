package baby.pages.scrapbook;

import baby.pages.BabyPage;

public class KickCounterPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/kickcounter";
	
	public final static String PARAM_START = "start";
	public final static String PARAM_STOP = "stop";
	public final static String PARAM_COUNT = "count";
	public final static String PARAM_SAVE = "save";
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<div id=\"KickCounter\">");
		
		write("<div id=\"BigLED\">");
		
		// #Countdown
		write("<div id=\"Countdown\">");
		write("1:00");
		write("</div>");
		
		// #Kicks
		write("<div id=\"Kicks\">");
		writeEncode(getString("scrapbook:KickCounter.CountLabel"));
		write("<span>0</span>");
		write("</div>");
		
		write("</div>");
		
		// #Buttons
		write("<div id=\"Buttons\">");
		writeButton(PARAM_START, getString("scrapbook:KickCounter.Start"));
		writeButtonRed(PARAM_STOP, getString("scrapbook:KickCounter.Stop"));
		writeButton(PARAM_COUNT, getString("scrapbook:KickCounter.Count"));
		writeButton(PARAM_SAVE, getString("scrapbook:KickCounter.Save"));
		write("</div>");
		
		write("</div>");
		
		writeIncludeJS("baby/kickcounter.js");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:KickCounter.Title");
	}
}
