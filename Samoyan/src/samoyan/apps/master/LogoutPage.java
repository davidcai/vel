package samoyan.apps.master;

import java.util.UUID;

import samoyan.apps.master.GoodbyePage;
import samoyan.database.AuthTokenStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

public class LogoutPage extends WebPage
{
	public final static String COMMAND = "logout";
	
	@Override
	public void renderHTML() throws Exception
	{
		// Clear cookie and redirect to goodbye page
		String auth = getContext().getCookie(RequestContext.COOKIE_AUTH);
		if (auth!=null)
		{
			AuthTokenStore.getInstance().remove(UUID.fromString(auth));
		}
		setCookie(RequestContext.COOKIE_AUTH, "");
		throw new RedirectException(GoodbyePage.COMMAND, null);
	}
}
