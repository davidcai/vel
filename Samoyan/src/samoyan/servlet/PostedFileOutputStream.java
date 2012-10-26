package samoyan.servlet;

import java.io.*;

public final class PostedFileOutputStream extends OutputStream
{
	File file = null;
	FileOutputStream fileStream = null;
	long size = 0;
	boolean inited = false;
	
	private void init() throws IOException
	{
		if (this.inited) return;
		
		// Create a temporary file
		this.file = File.createTempFile("upstream", ".tmp");
//		Debug.println("Upstream file " + this.file.getAbsolutePath() + " created");
		
		// Init the stream
		this.fileStream = new FileOutputStream(this.file);
		
		this.inited = true;
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			if (this.fileStream!=null)
			{
				this.fileStream.close();
			}
			if (this.file!=null)
			{
				this.file.delete();
//				Debug.println("Upstream file " + this.file.getAbsolutePath() + " deleted");
			}
		}
		finally
		{
			super.finalize();
		}
	}

	public File getFile()
	{
		return this.file;
	}
	
	public long size()
	{
		return this.size;
	}
	
	@Override
	public void close() throws IOException
	{
		if (this.fileStream!=null)
		{
			this.fileStream.close();
		}
	}

	@Override
	public void flush() throws IOException
	{
		if (this.fileStream!=null)
		{
			this.fileStream.flush();
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (len==0) return;
		if (this.inited==false) init(); // Just in time stream init
		
		this.fileStream.write(b, off, len);
		this.size += len;
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		if (b.length==0) return;
		if (this.inited==false) init(); // Just in time stream init

		this.fileStream.write(b);
		this.size += b.length;
	}

	@Override
	public void write(int b) throws IOException
	{
		if (this.inited==false) init(); // Just in time stream init

		this.fileStream.write(b);
		this.size ++;
	}
}
