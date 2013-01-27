package baby.pages.journey;

import java.util.List;
import java.util.UUID;

import samoyan.core.Day;
import baby.controls.JournalListControl;
import baby.database.JournalEntryStore;
import baby.database.MeasureRecordStore;
import baby.pages.BabyPage;

public class JournalAJAXPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/journal.ajax";

	public final static String PARAM_FROM = "from";

	@Override
	public void renderHTML() throws Exception
	{
		// Get from date
		Day from = null;
		try
		{
			if (isParameterNotEmpty(PARAM_FROM))
			{
				from = new Day(getParameterString(PARAM_FROM));
			}
		}
		catch (Exception e)
		{
			from = null;
		}
		
		UUID userID = getContext().getUserID();
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(userID);
		List<UUID> recordIDs = MeasureRecordStore.getInstance().getByUserID(userID);
		
		new JournalListControl(this)
			.setEntryIDs(entryIDs)
			.setRecordIDs(recordIDs)
			.setFrom(from)
			.render();
	}

	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
	}
}
