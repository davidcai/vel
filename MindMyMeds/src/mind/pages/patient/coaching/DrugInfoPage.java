package mind.pages.patient.coaching;

import java.util.UUID;

import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import mind.database.Drug;
import mind.database.DrugStore;
import mind.pages.patient.PatientPage;

public class DrugInfoPage extends PatientPage
{
	public final static String COMMAND = CoachingPage.COMMAND + "/drug-info";
	
	public final static String PARAM_ID = "id";

	private Drug drug;
	
	@Override
	public String getTitle() throws Exception
	{
		return this.drug.getName();
	}

	@Override
	public void init() throws Exception
	{
		RequestContext ctx = getContext();
		
		UUID drugID = getParameterUUID(PARAM_ID);
		if (drugID==null)
		{
			throw new PageNotFoundException();
			
		}
		
		this.drug = DrugStore.getInstance().load(drugID);
		if (this.drug==null)
		{
			throw new PageNotFoundException();
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		if (!Util.isEmpty(this.drug.getYouTubeVideoID()) && ctx.getUserAgent().isBlackBerry()==false)
		{
			int width = 400;
			if (ctx.getUserAgent().isSmartPhone() && width>ctx.getUserAgent().getScreenWidth()-25)
			{
				width = ctx.getUserAgent().getScreenWidth()-25;
			}
			writeYouTubeVideo(this.drug.getYouTubeVideoID(), width, width*3/4);
			write("<br><br>");
		}
		
		if (!Util.isEmpty(this.drug.getInformation()))
		{
			write(this.drug.getInformation());
		}
	}
}
