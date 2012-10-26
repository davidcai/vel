package samoyan.notif;

import java.util.Date;
import java.util.UUID;

import samoyan.database.LogEntryStore;
import samoyan.database.Notification;
import samoyan.database.NotificationStore;

class ScheduleFutureNotifs implements Runnable
{
	public static final long INTERVAL = 60L*1000L; // 1 min

	@Override
	public void run()
	{
		try
		{
			// Scheduled notifs whose time has come
			Date cutoff = new Date(System.currentTimeMillis() + INTERVAL);

			for (UUID notifID : NotificationStore.getInstance().query(null, cutoff, null, null, null, Notification.STATUS_UNSENT))
			{
				Notifier.send(notifID);
			}
			
//			// Unsent notifs that fell through the cracks
//			Date fiveMinAgo = new Date(now.getTime() - 5L*60L*1000L);
//			for (UUID notifID : NotificationStore.getUnsent(now))
//			{
//				Notifier.doSend(notifID);
//			}
		}
		catch (Exception e)
		{
			// Log exception
			LogEntryStore.log(e);
		}
	}
}
