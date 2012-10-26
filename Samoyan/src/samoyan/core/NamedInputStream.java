package samoyan.core;

import java.io.IOException;
import java.io.InputStream;

public class NamedInputStream extends InputStream
{
	private String fileName = "";
	private String mimeType = "application/octet-stream";
	private InputStream in = null;

	public NamedInputStream(InputStream stm)
	{
		this.in = stm;
	}
	
	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	@Override
	public int read() throws IOException
	{
		return in.read();
	}

	@Override
	public int available() throws IOException
	{
		return in.available();
	}

	@Override
	public void close() throws IOException
	{
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit)
	{
		in.mark(readlimit);
	}

	@Override
	public boolean markSupported()
	{
		return in.markSupported();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		return in.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		return in.read(b);
	}

	@Override
	public synchronized void reset() throws IOException
	{
		in.reset();
	}

	@Override
	public long skip(long n) throws IOException
	{
		return in.skip(n);
	}
}
