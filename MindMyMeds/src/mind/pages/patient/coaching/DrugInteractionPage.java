package mind.pages.patient.coaching;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import mind.database.Drug;
import mind.database.DrugStore;
import mind.pages.patient.PatientPage;

public class DrugInteractionPage extends PatientPage
{
	public final static String COMMAND = CoachingPage.COMMAND + "/interaction";

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:DrugInteraction.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		// Load drugs indicated in parameters
		List<Drug> drugs = new ArrayList<Drug>();
		for (int i=0; ; i++)
		{
			UUID drugID = getParameterUUID("d"+i);
			if (drugID==null) break;
			Drug drug = DrugStore.getInstance().load(drugID);
			if (drug!=null)
			{
				drugs.add(drug);
			}
			
		}
		
		// Help string
		StringBuffer base = new StringBuffer();
		StringBuffer rest = new StringBuffer();
		for (int i=0; i<drugs.size(); i++)
		{
			if (i==0)
			{
				base.append("<a href=\"");
				base.append(getPageURL(DrugInfoPage.COMMAND, new ParameterMap("id", drugs.get(i).getID().toString())));
				base.append("\">");
				base.append(drugs.get(i).getName());
				base.append("</a>");
			}
			else
			{
				if (i>1)
				{
					if (i==drugs.size()-1)
					{
						rest.append(" ");
						rest.append(getString("mind:DrugInteraction.And"));
						rest.append(" ");
					}
					else
					{
						rest.append(", ");
					}
				}
				
				rest.append("<a href=\"");
				rest.append(getPageURL(DrugInfoPage.COMMAND, new ParameterMap("id", drugs.get(i).getID().toString())));
				rest.append("\">");
				rest.append(drugs.get(i).getName());
				rest.append("</a>");				
			}
		}
				
		String pattern = Util.htmlEncode(getString("mind:DrugInteraction.Help", "$base$", "$rest$"));
		pattern = Util.strReplace(pattern, "$base$", base.toString());
		pattern = Util.strReplace(pattern, "$rest$", rest.toString());
		write(pattern);
		write("<br><br>");
		
		for (int i=0; i<drugs.size(); i++)
		{
			Drug di = drugs.get(i);
			String info = di.getDrugInteractionInformation();

			for (int j=0; j<drugs.size(); j++)
			{
				if (j==i) continue;
				Drug dj = drugs.get(j);
				
				info = Util.strReplace(info, dj.getName(), "<b class=Marker>" + dj.getName() + "</b>");
				info = Util.strReplace(info, dj.getGenericName(), "<b class=Marker>" + dj.getGenericName() + "</b>");
			}
			
			write("<h2>");
			writeEncode(getString("mind:DrugInteraction.DrugSection", di.getName()));
			write("</h2>");
			write(info);
			write("<br>");
		}
	}
	
	
}
