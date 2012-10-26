package samoyan.tasks;

import samoyan.database.AuthTokenStore;
import samoyan.servlet.Setup;

public final class DeleteExpiredAuthTokensRecurringTask implements RecurringTask
{
	@Override
	public void work() throws Exception
	{
		AuthTokenStore.getInstance().removeExpired();
	}

	@Override
	public long getInterval()
	{
		return Setup.getSessionLength();
	}
}
