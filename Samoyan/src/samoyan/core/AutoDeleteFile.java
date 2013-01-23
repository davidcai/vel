package samoyan.core;

import java.io.File;

/**
 * Encloses a file and automatically deletes it when the object is garbage collected.
 * @author brianwillis
 *
 */
public class AutoDeleteFile extends File
{
	public AutoDeleteFile(File f)
	{
		super(f.getAbsolutePath());
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		super.delete();
		super.finalize();
	}
}
