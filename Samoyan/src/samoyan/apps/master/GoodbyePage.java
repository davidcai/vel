package samoyan.apps.master;

import samoyan.apps.master.RootPage;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

/**
 * Non-admin users are redirected to this page after logging out.
 * By default, this page redirects to the root page. Implementations might want to override this behavior.
 * @author brian
 *
 */
public class GoodbyePage extends WebPage
{
	public final static String COMMAND = "goodbye";

	@Override
	public void renderHTML() throws Exception
	{
		throw new RedirectException(RootPage.COMMAND, null);
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}
}
