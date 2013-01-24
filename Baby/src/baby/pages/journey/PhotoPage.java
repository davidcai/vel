package baby.pages.journey;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ImageControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.core.image.JaiImage;
import samoyan.database.Image;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.AfterCommitRedirectException;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import baby.app.BabyConsts;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;

public class PhotoPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/photo";
	
	public final static String PARAM_ID = "id";
	
	private JournalEntry curEntry;
	private UUID prevID;
	private UUID nextID;
	
	@Override
	public void init() throws Exception
	{
		this.curEntry = JournalEntryStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.curEntry == null || this.curEntry.getUserID().equals(getContext().getUserID()) == false || this.curEntry.isHasPhoto() == false)
		{
			throw new PageNotFoundException();
		}
		
		// Figure out prev and next IDs
		List<UUID> entryIDs = JournalEntryStore.getInstance().getPhotosByUser(getContext().getUserID());
		Iterator<UUID> it = entryIDs.iterator();
		while (it.hasNext())
		{
			UUID id = it.next();
			if (id.equals(this.curEntry.getID()))
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
	
	@Override
	public void renderHTML() throws Exception
	{
		// Perform rotation
		String rotate = getParameterString("rotate");
		String sid = getParameterString("sid");
		if (!Util.isEmpty(rotate) && sid.equals(getContext().getSessionID().toString()))
		{
			JaiImage jai = new JaiImage(this.curEntry.getPhoto().getBytes());
			if (rotate.equals("left"))
			{
				jai.rotate(270);
			}
			else if (rotate.equals("right"))
			{
				jai.rotate(90);
			}
			JournalEntry entry = (JournalEntry) this.curEntry.clone();
			entry.setPhoto(new Image(jai));
			JournalEntryStore.getInstance().save(entry);
			
			throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, this.curEntry.getID()));
		}
		
		UserAgent ua = getContext().getUserAgent();
		
		String imgSz = BabyConsts.IMAGESIZE_BOX_600X600;
		if (ua.getScreenWidth() <= 400)
		{
			imgSz = BabyConsts.IMAGESIZE_BOX_400X400;
		}
		
		write("<div class=\"LargeImage\">");
		
		ImageControl img = new ImageControl(this).img(this.curEntry.getPhoto(), imgSz);
		if (ua.isSmartPhone())
		{
			img.width(ua.getScreenWidth()-10).height(0); // 10 pixels margin, auto-adjust height
		}
		img.render();
		
		if (this.prevID != null)
		{
			write("<div class=\"PrevWrapper\">");
			write("<a class=\"Prev\" href=\"");
			writeEncode(getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ID, this.prevID)));
			write("\">");
			write("</a>");
			write("</div>");
		}
		
		if (this.nextID != null)
		{
			write("<div class=\"NextWrapper\">");
			write("<a class=\"Next\" href=\"");
			writeEncode(getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ID, this.nextID)));
			write("\">");
			write("</a>");
			write("</div>");
		}
		
		write("</div>");
		write("<br><br>");
		
		// Rotate
		if (ua.isSmartPhone())
		{
			write("<div align=center>");
		}
		write("<table><tr><td>");
		String url = getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ID, this.curEntry.getID()).plus("rotate", "left").plus("sid", getContext().getSessionID()));
		writeImage("baby/rotate-left.png", getString("journey:Photo.RotateLeft"), url);
		write("</td><td>");
		writeEncode(getString("journey:Photo.Rotate"));
		write("</td><td>");
		url = getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ID, this.curEntry.getID()).plus("rotate", "right").plus("sid", getContext().getSessionID()));
		writeImage("baby/rotate-right.png", getString("journey:Photo.RotateRight"), url);
		write("</td></tr></table>");
		if (ua.isSmartPhone())
		{
			write("</div>");
		}
		write("<br>");
		
		writeFormOpen();
		new TextAreaInputControl(this, "text")
			.setRows(3).setCols(80)
			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
			.setPlaceholder(getString("journey:Photo.ImageDescription"))
			.setInitialValue(this.curEntry.getText())
			.render();
		write("<br><br>");
		writeSaveButton("save", this.curEntry);
		write("&nbsp;");
		writeRemoveButton("remove");
				
		writeHiddenInput(PARAM_ID, null); // Postback
		writeFormClose();	
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Photo.Title");
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter("save"))
		{
			validateParameterString("text", 0, JournalEntry.MAXSIZE_TEXT);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("save"))
		{
			JournalEntry entry = (JournalEntry) this.curEntry.clone();
			entry.setText(getParameterString("text"));
			JournalEntryStore.getInstance().save(entry);
			
			throw new AfterCommitRedirectException(new ParameterMap(PARAM_ID, this.curEntry.getID()));
		}
		
		if (isParameter("remove"))
		{
			JournalEntryStore.getInstance().remove(this.curEntry.getID());
			if (this.nextID != null)
			{
				throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, this.nextID));
			}
			else if (this.prevID != null)
			{
				throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, this.prevID));
			}
			else
			{
				throw new RedirectException(GalleryPage.COMMAND, null);
			}
		}
	}
}
