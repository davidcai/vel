package mind.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mind.database.*;
import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;

/**
 * A typeahead for selecint drugs. The key is the UUID of the drug, the value is the name of the drug.
 * @author brian
 *
 */
public class DrugChooserTypeAhead extends TypeAhead
{
	public final static String COMMAND = "drugchooser.typeahead";

	@Override
	protected void doQuery(String q) throws Exception
	{
		RequestContext ctx = getContext();
		Patient patient = PatientStore.getInstance().loadByUserID(ctx.getUserID());
		
		List<UUID> drugIDs = new ArrayList<UUID>();
		drugIDs.addAll(DrugStore.getInstance().searchByName(q));
		if (patient!=null)
		{
			drugIDs.addAll(DrugStore.getInstance().searchPrivateByName(q, patient.getID()));
		}

		for (UUID drugID : drugIDs)
		{
			Drug drug = DrugStore.getInstance().load(drugID);
			if (drug!=null)
			{
				String html = drug.getName();
				if (!Util.isEmpty(drug.getGenericName()) && drug.getName().equals(drug.getGenericName())==false)
				{
//					html += "<br><small class=Faded>";
					html += " (";
					html += drug.getGenericName();
					html += ")";
//					html += "</small>";
				}
//				if (!Util.isEmpty(drug.getDescription()))
//				{
//					html += "<br><small class=Faded>";
//					html += drug.getDescription();
//					html += "</small>";
//				}

				addOption(drug.getID(), drug.getName(), html);
			}
		}
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		return PatientStore.getInstance().loadByUserID(getContext().getUserID())!=null;
	}
}
