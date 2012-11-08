package samoyan.servlet.exc;

import samoyan.apps.system.GoBackPage;

/**
 * Allows redirecting the client using back in its history list.
 * @author brian
 *
 */
public class GoBackRedirectException extends RedirectException
{
	public GoBackRedirectException()
	{
		super(GoBackPage.COMMAND, null);
	}
}
