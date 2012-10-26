package elert.pages.patient;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.pages.ElertPage;
import samoyan.servlet.exc.RedirectException;

public class PatientHomePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT;
	
	@Override
	public void renderHTML() throws Exception
	{
		List<UUID> elertIDs = ElertStore.getInstance().queryByPatientID(getContext().getUserID());
		if (elertIDs.size()>0)
		{
			Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L*24L*60L*60L*1000L);
			Elert latestElert = ElertStore.getInstance().load(elertIDs.get(0));
			if (latestElert.getDateSent().after(thirtyDaysAgo))
			{
				throw new RedirectException(WallPage.COMMAND, null);
			}
		}

		throw new RedirectException(SubscriptionsPage.COMMAND, null);
	}
}
