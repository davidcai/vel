package baby.pages.info;

import baby.pages.BabyPage;
import samoyan.servlet.exc.RedirectException;

public class InformationHomePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION;
	
	@Override
	public void init() throws Exception
	{
		throw new RedirectException(ViewArticleListPage.COMMAND, null);
	}
}
