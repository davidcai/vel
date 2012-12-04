package baby.pages.scrapbook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
import samoyan.database.Image;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;

public class GalleryPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/gallery";
	
	public final static int SCREENSIZE_WIDTH_THRESHOLD = 450;
	public final static String PARAM_POST = "post";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_POST) && getParameterImage("photo") == null)
		{
			throw new WebFormException("photo", getString("scrapbook:Gallery.NoInput"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter(PARAM_POST))
		{
			Image photo = getParameterImage("photo");
			if (photo != null)
			{
				JournalEntry entry = new JournalEntry();
				entry.setUserID(getContext().getUserID());
				entry.setCreated(new Date());
				entry.setPhoto(photo);
				entry.setHasPhoto(true);
				
				JournalEntryStore.getInstance().save(entry);
			}
		}

		// Redirect to itself
		throw new RedirectException(COMMAND, null);
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Gallery.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeHorizontalNav(GalleryPage.COMMAND);
		
		writeFormOpen();
		writeImageInput("photo", null);
		write("<br>");
		writeButton(PARAM_POST, getString("scrapbook:Gallery.Post"));
		writeFormClose();
		
		write("<br>");
		
		// Get entries with photos
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(getContext().getUserID());
		List<JournalEntry> entries = new ArrayList<JournalEntry>();
		for (UUID entryID : entryIDs)
		{
			JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
			if (entry.isHasPhoto())
			{
				entries.add(entry);
			}
		}
		
		if (entries.isEmpty() == false)
		{
			int entryCount = entries.size();
			int colCountMax = getContext().getUserAgent().getScreenWidth() / 150; // BabyConsts.IMAGESIZE_THUMB_150X150
			int rowCount = (int) Math.ceil((entryCount + 0d) / colCountMax);
			
			write("<table class=\"PhotoGrid\">");
			
			for (int r = 0; r < rowCount; r++)
			{
				write("<tr>");
				
				for (int c = 0; c < colCountMax; c++)
				{
					int index = r * colCountMax + c;
					if (index < entryCount)
					{
						JournalEntry entry = entries.get(index);
						
						write("<td>");
						write("<a href=\"");
						writeEncode(getPageURL(PhotoPage.COMMAND, 
							new ParameterMap(PhotoPage.PARAM_ID, entry.getID().toString())));
						write("\">");
						writeImage(entry.getPhoto(), BabyConsts.IMAGESIZE_THUMB_150X150, null, null);
						write("</a>");
						write("<br>");
						writeEncodeDate(entry.getCreated());
						write("</td>");
					}
				}
				
				write("</tr>");
			}
			
			write("</table>");
		}
		else
		{
			writeEncode(getString("scrapbook:Gallery.NoPhoto"));
		}
	}
}
