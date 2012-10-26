package baby.pages.scrapbook;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterMap;
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
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Photo.Title", (this.curEntry != null) ? this.curEntry.getCreated() : "");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		if (this.curEntry != null && this.curEntry.isHasPhoto())
		{
			write("<div class=\"LargeImage\">");
			
			writeImage(this.curEntry.getPhoto(), BabyConsts.IMAGESIZE_BOX_800X800, null, null);
			
			if (this.prevID != null)
			{
				write("<div class=\"PrevWrapper\">");
				write("<a class=\"Prev\" href=\"");
				write(getPageURL(COMMAND, new ParameterMap(PARAM_ID, this.prevID.toString())));
				write("\">");
				write("</a>");
				write("</div>");
			}
			
			if (this.nextID != null)
			{
				write("<div class=\"NextWrapper\">");
				write("<a class=\"Next\" href=\"");
				write(getPageURL(COMMAND, new ParameterMap(PARAM_ID, this.nextID.toString())));
				write("\">");
				write("</a>");
				write("</div>");
			}
			
			write("</div>");
		}
	}
}
