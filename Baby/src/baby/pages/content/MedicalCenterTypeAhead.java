package baby.pages.content;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import baby.database.ArticleStore;
import baby.pages.BabyPage;
import samoyan.apps.system.TypeAhead;

public class MedicalCenterTypeAhead extends TypeAhead
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/medical-centers.typeahed";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		List<String> medCenters = ArticleStore.getInstance().getMedicalCenters();
		for (String m : medCenters)
		{
			if (m.toLowerCase(Locale.US).indexOf(q.toLowerCase(Locale.US))>=0)
			{
				addOption(m);
			}
		}
	}
}
