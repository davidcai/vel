package baby.pages.todo;

import baby.pages.BabyPage;

public final class ChecklistPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_TODO + "/checklist";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("todo:Checklist.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("To be implemented..."); // !$!
	}
}
