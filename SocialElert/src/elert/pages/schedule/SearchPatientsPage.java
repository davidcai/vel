package elert.pages.schedule;

import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.servlet.exc.RedirectException;
import elert.pages.ElertPage;
import elert.pages.typeahead.PatientTypeAhead;

public final class SearchPatientsPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/search-patients";

	@Override
	public void commit() throws Exception
	{
		Pair<String, String> kvp = getParameterTypeAhead("q");
		if (kvp!=null && Util.isUUID(kvp.getKey()))
		{
			throw new RedirectException(PatientProfilePage.COMMAND, new ParameterMap(PatientProfilePage.PARAM_ID, kvp.getKey()));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:SearchPatients.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("schedule:SearchPatients.Help"));
		write("<br><br>");
		writeFormOpen();
		writeTypeAheadInput("q", null, null, 40, User.MAXSIZE_NAME, getPageURL(PatientTypeAhead.COMMAND));
		write(" ");
		writeButton(getString("controls:Button.Search"));
		writeFormClose();
		
		if (isParameter("q"))
		{
			write("<br>");
			writeEncode(getString("schedule:SearchPatients.NoResults"));
		}
	}
}
