package elert.pages.patient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;

import samoyan.controls.NavTreeControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class PatientTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("patient:Nav.MyElerts"));
		navCtrl.addPage(WallPage.COMMAND, null);
		navCtrl.addPage(SubscriptionsPage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("patient:Nav.InfoCenter"));
		navCtrl.addPage(ProcedureSearchPage.COMMAND, null);
		
		// Get all procedures that this patient subscribes to
		Set<UUID> procIDs = new HashSet<UUID>();
		List<UUID> subIDs = SubscriptionStore.getInstance().getByUserID(outputPage.getContext().getUserID());
		for (UUID subID : subIDs)
		{
			procIDs.addAll(SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(subID));
			if (procIDs.size()>=6) // Show 6 (or a bit more) procedures
			{
				break;
			}
		}
		List<Procedure> procs = new ArrayList<Procedure>(procIDs.size());
		for (UUID procID : procIDs)
		{
			procs.add(ProcedureStore.getInstance().load(procID));
		}
		Collections.sort(procs, new Procedure.SortByName(outputPage.getLocale()));
		
		for (Procedure proc : procs)
		{
			navCtrl.addLink(
				proc.getName(),
				outputPage.getPageURL(ProcedureInfoPage.COMMAND, new ParameterMap(ProcedureInfoPage.PARAM_ID, proc.getID().toString())));
		}
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return ElertPage.COMMAND_PATIENT;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("patient:Tab.Title");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "elert/tab-patient.png";
	}
}
