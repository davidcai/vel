package elert.pages.schedule;

import elert.pages.ElertPage;

public class ProcedurePage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_SCHEDULE + "/procedure";
	
	public ProcedurePage()
	{
		setChild(new elert.pages.common.CommonProcedurePage()
		{
			@Override
			protected boolean isCustomProcedure()
			{
				return true;
			}

			@Override
			protected String getRedirectCommand()
			{
				return ProceduresPage.COMMAND_STANDARD;
			}
		});
	}	
}
