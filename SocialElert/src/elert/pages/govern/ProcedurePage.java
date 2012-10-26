package elert.pages.govern;

import elert.pages.ElertPage;

public class ProcedurePage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_GOVERN + "/procedure";
	
	public ProcedurePage()
	{
		setChild(new elert.pages.common.CommonProcedurePage()
		{
			@Override
			protected boolean isCustomProcedure()
			{
				return false;
			}

			@Override
			protected String getRedirectCommand()
			{
				return ProceduresPage.COMMAND;
			}
		});
	}
}
