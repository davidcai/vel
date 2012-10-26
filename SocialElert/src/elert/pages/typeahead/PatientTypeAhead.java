package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import elert.database.UserEx;
import elert.database.UserExStore;
import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;

public final class PatientTypeAhead extends WebPage
{
	public static final String COMMAND = "patient.typeahead";
	
	public PatientTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				List<UUID> ids = UserStore.getInstance().searchByName(query);
				for(UUID id : ids)
				{
					User patient = UserStore.getInstance().load(id);
					UserEx patientEx = UserExStore.getInstance().loadByUserID(id);
										
					StringBuilder html = new StringBuilder();
					html.append(patient.getDisplayName());
					if (!Util.isEmpty(patientEx.getMRN()))
					{
						html.append("<small class=Faded> (").append(patientEx.getMRN()).append(")</small>");
					}
					
					addOption(patient.getID(), patient.getDisplayName(), html.toString());
				}
			}
		});
	}	
}
