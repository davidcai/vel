package mind.tasks;

import samoyan.tasks.RecurringTask;
import mind.database.DrugStore;

/**
 * Remove patient defined drugs that are no longer attached to any prescription.
 * @author brian
 *
 */
public class RemoveOrphanedDrugsRecurringTask implements RecurringTask
{
	public void work() throws Exception
	{
		DrugStore.getInstance().removeOrphanedDrugs();
	}
	
	@Override
	public long getInterval()
	{
		return 24L*60L*60L*1000L; // 24 hrs
	}
}
