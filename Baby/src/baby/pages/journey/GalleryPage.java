package baby.pages.journey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ImageInputControl;
import samoyan.controls.TabControl;
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
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/album";
	
	private final static String PARAM_POST = "post";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_POST) && getParameterImage("photo") == null)
		{
			throw new WebFormException("photo", getString("journey:Gallery.NoInput"));
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
		return getString("journey:Gallery.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("journey:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("journey:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("journey:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}
		
		writeFormOpen();
		new ImageInputControl(this, "photo").showThumbnail(false).render();
		write("<br>");
		writeButton(PARAM_POST, getString("journey:Gallery.Post"));
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
			int COLS = 4;
			String imgSize = BabyConsts.IMAGESIZE_THUMB_150X150;
			if (getContext().getUserAgent().isSmartPhone())
			{
				COLS = getContext().getUserAgent().getScreenWidth() / 75;
				imgSize = Image.SIZE_THUMBNAIL;
			}
			if (COLS>5)
			{
				COLS = 5;
			}
						
			write("<table class=\"PhotoGrid\">");
			
			for (int i=0; i<entries.size(); i++)
			{
				if (i%COLS==0)
				{
					write("<tr>");
				}
				write("<td>");
				
				JournalEntry entry = entries.get(i);
				writeImage(entry.getPhoto(), imgSize, null,
					getPageURL(PhotoPage.COMMAND, new ParameterMap(PhotoPage.PARAM_ID, entry.getID().toString())));
//				write("<br>");
//				writeEncodeDate(entry.getCreated());

				write("</td>");
				if (i%COLS==COLS-1)
				{
					write("</tr>");
				}
			}
			if (entries.size()%COLS!=0)
			{
				write("<td colspan=");
				write(COLS-entries.size()%COLS);
				write(">&nbsp;</td></tr>");
			}
						
			write("</table>");
		}
		else
		{
			writeEncode(getString("journey:Gallery.NoPhoto"));
		}
	}
}
