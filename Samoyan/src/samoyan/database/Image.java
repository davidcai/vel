package samoyan.database;

import java.io.IOException;
import java.util.UUID;

import samoyan.core.Util;
import samoyan.core.image.JaiImage;

public final class Image
{
	public final static String SIZE_FULL = "full";
	public final static String SIZE_THUMBNAIL = "thumbnail";

	public final static int JPEG_QUALITY = 85;
	public static final int MAX_WIDTH = 2048;
	public static final int MAX_HEIGHT = 2048;

	private UUID id = UUID.randomUUID();
	private int width = 0;
	private int height = 0;
	private int version = 0;
	private int lenBytes = 0;
	private UUID origID = null;
	private String size = SIZE_FULL;
	private UUID linkedID = null;
	private String name = "";
	private byte[] bytes = null;
	private String mimeType = "";
	
	public Image()
	{
	}
	
	public Image(JaiImage jai) throws IOException
	{
		setWidth(jai.getWidth());
		setHeight(jai.getHeight());
		if (jai.hasAlpha())
		{
			setBytes(jai.encodePNG());
			setMimeType("image/png");
		}
		else
		{
			setBytes(jai.encodeJPEG(JPEG_QUALITY));
			setMimeType("image/jpeg");
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Image)
		{
			Image that = (Image) obj;
			return	this.getWidth()==that.getWidth() &&
					this.getHeight()==that.getHeight() &&
					this.getLengthBytes()==that.getLengthBytes() &&
					Util.objectsEqual(this.getMimeType(), that.getMimeType()) &&
					Util.objectsEqual(this.getBytes(), that.getBytes());
		}
		else
		{
			return false;
		}
	}
	
	public UUID getID()
	{
		return this.id;
	}
	void setID(UUID id)
	{
		this.id = id;
	}

	UUID getOriginalID()
	{
		return this.origID!=null? this.origID : this.id;
	}
	void setOriginalID(UUID id)
	{
		this.origID = id;
	}
	String getSize()
	{
		return this.size;
	}
	void setSize(String sz)
	{
		this.size = sz;
	}
	
	UUID getLinkedID()
	{
		return this.linkedID;
	}
	void setLinkedID(UUID id)
	{
		this.linkedID = id;
	}
	String getName()
	{
		return this.name;
	}
	void setName(String name)
	{
		this.name = name;
	}

	public int getWidth()
	{
		return this.width;
	}
	void setWidth(int width)
	{
		this.width = width;
	}
	
	public int getHeight()
	{
		return this.height;
	}
	void setHeight(int height)
	{
		this.height = height;
	}
	
	public int getVersion()
	{
		return this.version;
	}
	void setVersion(int v)
	{
		this.version = v % 10000;
	}
	
	public String getMimeType()
	{
		return this.mimeType ;
	}
	void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}
	
	public byte[] getBytes()
	{
		return this.bytes;
	}
	void setBytes(byte[] bytes)
	{
		this.bytes = bytes;
	}
	public int getLengthBytes()
	{
		return this.bytes==null? 0 : this.bytes.length;
	}
}
