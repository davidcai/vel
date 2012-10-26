package mind.pages.patient.coaching;

import java.util.*;

import mind.database.*;
import samoyan.controls.NavTreeControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.RequestContext;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class CoachingTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		RequestContext ctx = outputPage.getContext();

		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("mind:Nav.Learn"));
		navCtrl.addPage(BotChatPage.COMMAND, null);
		navCtrl.addPage(SearchDrugsPage.COMMAND, null);
		navCtrl.addPage(EquipmentsPage.COMMAND, null);

		// My drugs
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		List<UUID> rxIDs = PrescriptionStore.getInstance().getByPatientID(patient.getID());
		List<Drug> drugs = new ArrayList<Drug>(rxIDs.size());
		for (UUID rxID : rxIDs)
		{
			Prescription rx = PrescriptionStore.getInstance().load(rxID);
			if (rx!=null)
			{
				Drug drug = DrugStore.getInstance().load(rx.getDrugID());
				if (drug!=null && drug.getPatientID()==null)
				{
					drugs.add(drug);
				}
			}
		}
		if (drugs.size()>0)
		{
			Collections.sort(drugs);			
			navCtrl.addHeader(outputPage.getString("mind:Nav.MyDrugs"));
			for (Drug drug : drugs)
			{
				navCtrl.addLink(drug.getName(), outputPage.getPageURL(DrugInfoPage.COMMAND, new ParameterMap(DrugInfoPage.PARAM_ID, drug.getID().toString())));
			}
		}		
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return CoachingPage.COMMAND;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("mind:Tab.Coaching");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "mind/tab-coaching.png";
	}
}
