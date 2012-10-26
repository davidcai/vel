package samoyan.tasks;

import java.io.File;

import samoyan.servlet.Setup;

/**
 * Delete files from the temp folder that are older than 4 sessions.
 * There include user uploaded file streams, image manipulation files, etc.
 * @author brian
 *
 */
public class CleanTempFilesRecurringTask implements RecurringTask
{
	@Override
	public void work() throws Exception
	{
		long twoSessionsAgo = System.currentTimeMillis() - 4L*Setup.getSessionLength();

		File dir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = dir.listFiles();
		for (File file : files)
		{
			if (file.lastModified() < twoSessionsAgo)
			{
				file.delete();
			}
		}
	}

	@Override
	public long getInterval()
	{
		return Setup.getSessionLength();
	}
}
