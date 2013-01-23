package samoyan.tasks;

import java.io.File;

import samoyan.core.image.JaiImage;
import samoyan.servlet.PostedFileOutputStream;
import samoyan.servlet.Setup;

/**
 * Delete files from the temp folder that are older than a session.
 * There include user uploaded file streams, image manipulation files, etc.
 * @author brian
 *
 */
public class CleanTempFilesRecurringTask implements RecurringTask
{
	@Override
	public void work() throws Exception
	{
		long sessionsAgo = System.currentTimeMillis() - Setup.getSessionLength();

		File dir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = dir.listFiles();
		for (File file : files)
		{
			if (file.getName().startsWith(JaiImage.RENDER_IMAGE_FILENAME) ||
				file.getName().startsWith(PostedFileOutputStream.UPSTREAM_FILENAME) ||
				file.getName().startsWith("imageio"))
			{
				if (file.lastModified() < sessionsAgo)
				{
					file.delete();
				}
			}
		}
	}

	@Override
	public long getInterval()
	{
		return Setup.getSessionLength() / 4;
	}
}
