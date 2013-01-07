package samoyan.servlet.exc;

import java.util.Map;

import samoyan.apps.system.GoBackPage;
import samoyan.core.ParameterMap;
import samoyan.servlet.RequestContext;

/**
 * Redirects the user after a form is committed.
 * Will either redirect to self with a "Saved" message, or will redirect to the previous page using
 * the go-back mechanism.
 * @author brianwillis
 *
 */
public class AfterCommitRedirectException extends RedirectException
{
	public AfterCommitRedirectException()
	{
		this(null);
	}
	
	public AfterCommitRedirectException(Map<String, String> params)
	{
		RequestContext ctx = RequestContext.getCurrent();
		if (ctx.getParameter(RequestContext.PARAM_GO_BACK_ON_SAVE)!=null)
		{
			setCommand(GoBackPage.COMMAND);
		}
		else
		{
			setCommand(ctx.getCommand());
			setParameters(new ParameterMap(RequestContext.PARAM_SAVED, "").plus(params));
		}
	}
}
