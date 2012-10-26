package elert.pages.patient;

import samoyan.core.Util;
import samoyan.servlet.exc.PageNotFoundException;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.pages.ElertPage;

public class ProcedureInfoPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/procedure";
	public final static String PARAM_ID = "id";
	
	private Procedure proc;
	
	
	@Override
	public void init() throws Exception
	{
		this.proc = ProcedureStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.proc==null)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return this.proc.getDisplayName();
	}

	@Override
	public void renderHTML() throws Exception
	{
		boolean isPhone = getContext().getUserAgent().isSmartPhone();

		// Video
		if (!Util.isEmpty(this.proc.getVideo()))
		{
			if (isPhone==false)
			{
				writeEmbedVideo(this.proc.getVideo(), 400, 300);
			}
			else
			{
				writeEmbedVideo(this.proc.getVideo(), getContext().getUserAgent().getScreenWidth()-10, (getContext().getUserAgent().getScreenWidth()-10)*3/4);
			}
			write("<br><br>");
		}
		
		if (!Util.isEmptyHTML(this.proc.getDefinition()) ||
			!Util.isEmpty(this.proc.getShortDescription()) ||
			this.proc.getDuration()>0)
		{
			write("<h2>");
			writeEncode(getString("patient:ProcedureInfo.GeneralInfo"));
			write("</h2>");

			// Definition or short description
			if (!Util.isEmptyHTML(this.proc.getDefinition()))
			{
				write(this.proc.getDefinition());
				write("<br><br>");
			}
			else if (!Util.isEmpty(this.proc.getShortDescription()))
			{
				writeEncode(this.proc.getShortDescription());
				write("<br><br>");
			}
			
			// Duration
			if (this.proc.getDuration()>0)
			{
				writeEncode(getString("patient:ProcedureInfo.Duration", this.proc.getDuration()));
				write("<br><br>");
			}
		}
		
		// Instructions
		if (!Util.isEmptyHTML(this.proc.getInstructions()))
		{
			write("<h2>");
			writeEncode(getString("patient:ProcedureInfo.BeforeTheProcedure"));
			write("</h2>");
			write(this.proc.getInstructions());
		}
	}
}
