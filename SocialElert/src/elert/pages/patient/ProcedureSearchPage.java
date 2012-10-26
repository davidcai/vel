package elert.pages.patient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import elert.pages.typeahead.ProcedureTypeAhead;

public class ProcedureSearchPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_PATIENT + "/info-search";
	
	@Override
	public void validate() throws Exception
	{
		Pair<String, String> procedureKvp = getParameterTypeAhead("q");
		if (Util.isEmpty(procedureKvp.getValue()))
		{
			throw new WebFormException("q", getString("common:Errors.Missingfield"));
		}
		if (Util.isEmpty(procedureKvp.getKey()))
		{
			throw new WebFormException("q", getString("patient:ProcedureSearch.NoSuchProcedure", procedureKvp.getValue()));
		}
		Procedure proc = ProcedureStore.getInstance().load(UUID.fromString(procedureKvp.getKey()));
		if (proc==null)
		{
			throw new WebFormException("q", getString("patient:ProcedureSearch.NoSuchProcedure", procedureKvp.getValue()));
		}
	}

	@Override
	public void commit() throws Exception
	{
		Pair<String, String> procedureKvp = getParameterTypeAhead("q");
		UUID procedureID = UUID.fromString(procedureKvp.getKey());
		throw new RedirectException(ProcedureInfoPage.COMMAND, new ParameterMap(ProcedureInfoPage.PARAM_ID, procedureID.toString()));
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("patient:ProcedureSearch.Help"));
		write("<br><br>");

		writeFormOpen();
		writeTypeAheadInput("q", null, null, 40, Procedure.MAXSIZE_NAME, getPageURL(ProcedureTypeAhead.COMMAND));
		write(" ");
		writeButton(getString("controls:Button.Search"));
		write("<br><br>");
		writeFormClose();
		
		// Get all procedures that this patient subscribes to
		Set<UUID> procIDs = new HashSet<UUID>();
		List<UUID> subIDs = SubscriptionStore.getInstance().getByUserID(getContext().getUserID());
		for (UUID subID : subIDs)
		{
			procIDs.addAll(SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(subID));
		}
		List<Procedure> procs = new ArrayList<Procedure>(procIDs.size());
		for (UUID procID : procIDs)
		{
			procs.add(ProcedureStore.getInstance().load(procID));
		}
		Collections.sort(procs, new Procedure.SortByName(getLocale()));

//		if (procs.size()>0)
//		{
//			write("<h2>");
//			writeEncode(getString("patient:ProcedureSearch.MyProcedures"));
//			write("</h2>");
//		}
		for (Procedure proc : procs)
		{
			writeLink(proc.getDisplayName(), getPageURL(ProcedureInfoPage.COMMAND, new ParameterMap(ProcedureInfoPage.PARAM_ID, proc.getID().toString())));
			if (!Util.isEmpty(proc.getShortDescription()))
			{
				write(" - ");
				writeEncode(proc.getShortDescription());
			}
			write("<br><br>");
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:ProcedureSearch.Title");
	}
}
