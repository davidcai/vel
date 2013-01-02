package samoyan.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import samoyan.core.Cache;
import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.core.image.ImageSizer;
import samoyan.core.image.JaiImage;

public final class ImageStore
{
	private static ImageStore instance = new ImageStore();

	protected ImageStore()
	{
	}
	public final static ImageStore getInstance()
	{
		return instance;
	}	

	// - - -

	private Map<String, ImageSizer> sizers = new ConcurrentHashMap<String, ImageSizer>();
	
	public void bindSizer(String key, ImageSizer sizer)
	{
		this.sizers.put(key, sizer);
	}
	
	public ImageSizer lookupSizer(String key)
	{
		return this.sizers.get(key);
	}
	
	// - - -
	
	public void save(Image img) throws SQLException
	{
		Query q = new Query();
		try
		{
			ResultSet rs = q.updatableSelect("SELECT ID,Width,Height,Version,LengthBytes,Bytes,MimeType,LinkedID,Name,OriginalID,Size FROM Images WHERE ID=?", new ParameterList(img.getID()));
			boolean insert = !rs.next();
			if (insert)
			{
				rs.moveToInsertRow();
			}
			else
			{
				img.setVersion(rs.getInt("Version") + 1);
			}
			
			rs.updateBytes("ID", Util.uuidToBytes(img.getID()));
			rs.updateInt("Width", img.getWidth());
			rs.updateInt("Height", img.getHeight());
			rs.updateInt("Version", img.getVersion());
			rs.updateInt("LengthBytes", img.getLengthBytes());
			rs.updateBytes("Bytes", img.getBytes());
			rs.updateString("MimeType", img.getMimeType());
			rs.updateBytes("LinkedID", Util.uuidToBytes(img.getLinkedID()));
			rs.updateString("Name", img.getName());
			rs.updateBytes("OriginalID", Util.uuidToBytes(img.getOriginalID()));
			rs.updateString("Size", img.getSize());
			
			if (insert)
			{
				rs.insertRow();
			}
			else
			{
				rs.updateRow();
			}
		}
		finally
		{
			q.close();
		}

		// Cache the result for future use
		Cache.insert("img:" + img.getID().toString(), img);

		// Also delete resized versions of this image
		removeResizedCopies(img.getID());
	}
	
	private void removeResizedCopies(UUID originalID) throws SQLException
	{
		Query q = new Query();
		try
		{
			ResultSet rs = q.updatableSelect("SELECT ID,Size FROM Images WHERE OriginalID=? AND ID<>?", new ParameterList(originalID).plus(originalID));
			while (rs.next())
			{
				UUID copyID = Util.bytesToUUID(rs.getBytes(1));
				String size = rs.getString(2);
				rs.deleteRow();
				Cache.invalidate("img:" + copyID.toString());
				Cache.invalidate("img:" + originalID.toString() + "." + size);
			}
		}
		finally
		{
			q.close();
		}
	}
	
	public Image load(UUID imgID) throws SQLException
	{
		// Check cache
		String cacheKey = "img:" + imgID.toString();
		Image cached = (Image) Cache.get(cacheKey);
		if (cached!=null)
		{
			return cached;
		}
		
		Query q = new Query();
		try
		{
			ResultSet rs = q.select("SELECT * FROM Images WHERE ID=?", new ParameterList(imgID));
			if (!rs.next())
			{
				return null;
			}
			
			Image img = new Image();
			img.setID(imgID);
			img.setWidth(rs.getInt("Width"));
			img.setHeight(rs.getInt("Height"));
			img.setVersion(rs.getInt("Version"));
			img.setBytes(rs.getBytes("Bytes"));
			img.setMimeType(rs.getString("MimeType"));
			img.setLinkedID(Util.bytesToUUID(rs.getBytes("LinkedID")));
			img.setName(rs.getString("Name"));
			img.setOriginalID(Util.bytesToUUID(rs.getBytes("OriginalID")));
			img.setSize(rs.getString("Size"));
			
			// Cache the result for future use
			Cache.insert(cacheKey, img);
			
			return img;
		}
		finally
		{
			q.close();
		}
	}
	
	public Image loadAndResize(UUID imgID, String size, float pixelRatio) throws Exception
	{
		String pixelRatioStr = String.valueOf(pixelRatio);
		if (pixelRatioStr.endsWith(".0"))
		{
			pixelRatioStr = pixelRatioStr.substring(0, pixelRatioStr.length()-2);
		}
				
		String cacheKey = "img:" + imgID.toString() + "." + size + "X" + pixelRatioStr;
		if (size.equalsIgnoreCase(Image.SIZE_FULL)==false)
		{
			UUID cachedID = (UUID) Cache.get(cacheKey);
			if (cachedID!=null)
			{
				return load(cachedID);
			}

			// Look up the resized image in the database
			Query q = new Query();
			try
			{
				ResultSet rs = q.select("SELECT ID FROM Images WHERE OriginalID=? AND Size=?", new ParameterList(imgID).plus(size + "X" + pixelRatioStr));
				if (rs.next())
				{
					UUID resizedID = Util.bytesToUUID(rs.getBytes("ID"));
									
					// Cache the query result for future use
					Cache.insert(cacheKey, resizedID);
				
					Image resizedImg = load(resizedID);
					if (resizedImg!=null)
					{
						return resizedImg;
					}
				}
			}
			finally
			{
				q.close();
			}
		}
		
		Image img = load(imgID);
		if (img==null)
		{
			return null;
		}
		if (size.equalsIgnoreCase(Image.SIZE_FULL))
		{
			return img;
		}
		
		
		
		ImageSizer sizer = lookupSizer(size);
		if (sizer==null)
		{
			return null;
		}
		
		JaiImage jai = new JaiImage(img.getBytes());
		jai = sizer.process(jai, pixelRatio);
		Image processed = new Image(jai);
		processed.setOriginalID(imgID);
		processed.setLinkedID(imgID);
		processed.setSize(size + "X" + pixelRatioStr);
		processed.setName(size + "X" + pixelRatioStr);
		processed.setVersion(img.getVersion());
		save(processed);
		
		// Cache the query result for future use
		Cache.insert(cacheKey, processed.getID());

		return processed;
	}
	
	public void remove(UUID imgID) throws SQLException
	{
		Query q = new Query();
		try
		{
			int res = q.update("DELETE FROM Images WHERE ID=?", new ParameterList(imgID));
			if (res==0)
			{
				return;
			}
		}
		finally
		{
			q.close();
		}
		
		// Clear cache
		Cache.invalidate("img:" + imgID.toString());

		// Also delete resized versions of this image
		removeResizedCopies(imgID);
	}
	
	public void removeByLinkedID(UUID linkedID) throws SQLException
	{
		Query q = new Query();
		try
		{
			ResultSet rs = q.updatableSelect("SELECT ID FROM Images WHERE LinkedID=?", new ParameterList(linkedID));
			while (rs.next())
			{
				remove(Util.bytesToUUID(rs.getBytes(1)));				
			}
		}
		finally
		{
			q.close();
		}
	}
}
