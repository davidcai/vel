package mind.pages.patient.coaching;

import java.util.*;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import mind.database.*;
import mind.pages.DrugChooserTypeAhead;
import mind.pages.patient.PatientPage;

public class SearchDrugsPage extends PatientPage
{
	public final static String COMMAND = CoachingPage.COMMAND + "/search";

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:SearchDrugs.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		
		String q = getParameterString("q");
		
		writeEncode(getString("mind:SearchDrugs.Help", getString("mind:App.Title")));
		write("<br><br>");

		writeFormOpen("GET", null);
		write("<table><tr valign=middle><td>");
		writeTypeAheadInput("q", q, q, 40, Drug.MAXSIZE_NAME, getPageURL(DrugChooserTypeAhead.COMMAND));
		write("</td><td>");
		writeButton(getString("mind:SearchDrugs.Search"));
		write("</td></tr></table>");
		writeFormClose();
		write("<br>");
		
		if (q==null)
		{
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
				
				List<UUID> drugIDs = new ArrayList<UUID>(drugs.size());
				for (Drug drug : drugs)
				{
					drugIDs.add(drug.getID());
				}
			
				writeEncode(getString("mind:SearchDrugs.YourPrescriptions"));
				write("<br><br>");
				
				renderDrugList(drugIDs);
			}
		}
		else
		{
			Drug drug = DrugStore.getInstance().loadByName(q, null);
			if (drug!=null)
			{
				throw new RedirectException(DrugInfoPage.COMMAND, new ParameterMap("id", drug.getID().toString()));
			}
			
			List<UUID> drugIDs = DrugStore.getInstance().searchByName(q);
			if (drugIDs.size()==0)
			{
				writeEncode(getString("mind:SearchDrugs.NoResults"));
				return;
			}
			else
			{
				renderDrugList(drugIDs);
			}
		}
	}
	
	private void renderDrugList(List<UUID> drugIDs) throws Exception
	{
		for (UUID drugID : drugIDs)
		{
			Drug drug = DrugStore.getInstance().load(drugID);
			if (drug==null) continue;
			
			writeLink(drug.getDisplayName(), getPageURL(DrugInfoPage.COMMAND, new ParameterMap("id", drug.getID().toString())));
			write("<br>");
			if (!Util.isEmpty(drug.getDescription()))
			{
				writeEncode(drug.getDescription());
				write("<br>");
			}
			write("<br>");
		}
	}
}
