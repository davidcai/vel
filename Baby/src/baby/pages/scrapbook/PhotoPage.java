package baby.pages.scrapbook;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ImageControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.PageNotFoundException;
import baby.app.BabyConsts;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;

public class PhotoPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/photo";
	public final static String PARAM_ID = "id";
	
	private JournalEntry curEntry;
	private UUID prevID;
	private UUID nextID;
	
	@Override
	public void init() throws Exception
	{
		UUID entryID = getParameterUUID(PARAM_ID);
		if (entryID != null)
		{
			this.curEntry = JournalEntryStore.getInstance().load(entryID);
			
			// Figure out prev and next IDs
			List<UUID> entryIDs = JournalEntryStore.getInstance().getPhotosByUser(getContext().getUserID());
			Iterator<UUID> it = entryIDs.iterator();
			while (it.hasNext())
			{
				UUID id = it.next();
				if (id.equals(entryID))
				{
					if (it.hasNext()) 
					{
						this.nextID = it.next();
					}
					
					break;
				}
				
				this.prevID = id;
			}
		}
		
		if (this.curEntry == null || this.curEntry.isHasPhoto()==false)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Photo.Title", (this.curEntry != null) ? this.curEntry.getCreated() : "");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		UserAgent ua = getContext().getUserAgent();
		
		String imgSz = BabyConsts.IMAGESIZE_BOX_800X800;
		if (ua.getScreenWidth() <= 400)
		{
			imgSz = BabyConsts.IMAGESIZE_BOX_400X400;
		}
		
		write("<div class=\"LargeImage\">");
		
		ImageControl img = new ImageControl(this).img(this.curEntry.getPhoto(), imgSz);
		if (ua.isSmartPhone())
		{
			img.width(ua.getScreenWidth()-2).height(0); // 2 pixels margin, auto-adjust height
		}
		img.render();
		
		if (this.prevID != null)
		{
			write("<div class=\"PrevWrapper\">");
			write("<a class=\"Prev\" href=\"");
			writeEncode(getPageURL(COMMAND, new ParameterMap(PARAM_ID, this.prevID.toString())));
			write("\">");
			write("</a>");
			write("</div>");
		}
		
		if (this.nextID != null)
		{
			write("<div class=\"NextWrapper\">");
			write("<a class=\"Next\" href=\"");
			writeEncode(getPageURL(COMMAND, new ParameterMap(PARAM_ID, this.nextID.toString())));
			write("\">");
			write("</a>");
			write("</div>");
		}
		
		write("</div>");
		
		if (!Util.isEmpty(this.curEntry.getText()))
		{
//			write("<div align=center>");
			write("<br>");
			writeEncode(this.curEntry.getText());
//			write("</div>");
		}
	}
}
