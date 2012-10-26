package baby.pages.todo;

import samoyan.servlet.exc.RedirectException;
import baby.pages.BabyPage;

public final class TodoHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_TODO;

	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(ChecklistPage.COMMAND, null);
	}
}
