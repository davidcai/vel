package baby.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;
import samoyan.database.Image;

public class JournalEntry extends DataBean
{
	public final static int MAXSIZE_TEXT = 140;
	public final static int MAXWIDTH_PHOTO = 800;
	public final static int MAXHEIGHT_PHOTO = 600;
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}

	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	public String getText()
	{
		return (String) get("Text");
	}

	public void setText(String text)
	{
		set("Text", text);
	}

	public Date getCreated()
	{
		return (Date) get("Created");
	}

	public void setCreated(Date created)
	{
		set("Created", created);
	}

	public Image getPhoto()
	{
		return (Image) get("Photo");
	}

	public void setPhoto(Image photo)
	{
		set("Photo", photo);
	}
	
	public boolean isHasPhoto()
	{
		return (Boolean) get("HasPhoto", false);
	}
	
	public void setHasPhoto(boolean hasPhoto)
	{
		set("HasPhoto", hasPhoto);
	}
}
