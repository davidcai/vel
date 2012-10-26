package samoyan.tasks;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.database.LogEntryStore;
import samoyan.database.LogType;
import samoyan.database.LogTypeStore;

public class DeleteOldLogEntriesRecurringTask implements RecurringTask
{
	@Override
	public void work() throws Exception
	{
		long now = System.currentTimeMillis();
		Set<String> typeSet = new HashSet<String>(1);
		
		List<String> typeNames = LogTypeStore.getInstance().getNames();
		for (String t : typeNames)
		{
			LogType type = LogTypeStore.getInstance().loadByName(t);
			if (type.getLife()<0)
			{
				continue;
			}

			Date cutOff = new Date(now - type.getLife() * 24L*60L*60L*1000L); 
			typeSet.clear();
			typeSet.add(type.getName());
			
			List<UUID> logIDs = LogEntryStore.getInstance().queryLog(null, cutOff, null, null, null, null, typeSet);
			for (UUID id : logIDs)
			{
				LogEntryStore.getInstance().remove(id);
			}
		}
	}

	@Override
	public long getInterval()
	{
		return 15L*60L*1000L; // 15 min
	}
}
