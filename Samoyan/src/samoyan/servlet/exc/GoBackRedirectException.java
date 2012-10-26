package samoyan.servlet.exc;

import samoyan.apps.system.GoBackPage;
import samoyan.core.ParameterMap;

/**
 * Allows redirecting the client using back in its history list.
 * @author brian
 *
 */
public class GoBackRedirectException extends RedirectException
{
	private int steps = 1;
	
	public GoBackRedirectException()
	{
		this(1);
	}
	
	/**
	 * 
	 * @param steps The number of steps to go back in the user's history. Must be a positive number.
	 * Typically, 1 or 2 should be used.
	 */
	public GoBackRedirectException(int steps)
	{
		super(GoBackPage.COMMAND, new ParameterMap(GoBackPage.PARAM_STEPS, String.valueOf(steps)));
	}
}
