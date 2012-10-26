package mind.pages.patient;

import samoyan.servlet.WebPage;

/**
 * Base class for patient pages.
 * @author brian
 *
 */
public abstract class PatientPage extends WebPage
{	
	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		return getContext().getUserID()!=null;
	}
}
