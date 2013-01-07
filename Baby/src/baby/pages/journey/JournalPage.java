package baby.pages.journey;

import java.text.DateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TabControl;
import samoyan.controls.WideLinkGroupControl;
import samoyan.controls.WideLinkGroupControl.WideLink;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.database.Image;
import baby.app.BabyConsts;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.database.MeasureStore;
import baby.pages.BabyPage;

public class JournalPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/journal";
	
	private final static String PARAM_POST = "post";

	@Override
	public void renderHTML() throws Exception
	{
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("journey:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("journey:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("journey:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}
		
		// Add button
		if (getContext().getUserAgent().isSmartPhone())
		{
			writeFormOpen("GET", JournalEntryPage.COMMAND);
			new ButtonInputControl(this, JournalEntryPage.PARAM_EDIT)
				.setValue(getString("journey:Journal.AddHotButton"))
				.setMobileHotAction(true)
				.setAttribute("class", "NoShow")
				.render();
			writeFormClose();
		}
		else
		{
			new LinkToolbarControl(this)
				.addLink(getString("journey:Journal.AddLink"), getPageURL(JournalEntryPage.COMMAND), "icons/standard/pencil-16.png")
				.render();
		}
		
//		writeFormOpen();
//		
//		new TextInputControl(this, "NewJournalEntryPlaceHolder")
//			.setSize(80)
//			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
//			.setPlaceholder(getString("journey:Journal.WhatIsOnYourMind"))
//			.setAutoFocus(false)
//			.setID("NewJournalEntryPlaceHolder")
//			.render();
//		
//		write("<div id=\"NewJournalEntryPanel\">");
//		new TextAreaInputControl(this, "text")
//			.setRows(3).setCols(80)
//			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
//			.setPlaceholder(getString("journey:Journal.WhatIsOnYourMind"))
//			.render();
//		write("<br>");
//		new ImageInputControl(this, "photo").showThumbnail(false).render();
//		write("<br>");
//		writeButton(PARAM_POST, getString("journey:Journal.Post"));
//		write("</div>"); //-- #NewJournalEntryPanel
//		
//		writeFormClose();
//		write("<br>");
		
		// Entries
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(getContext().getUserID());
		if (entryIDs.isEmpty() == false)
		{
			DateFormat dfDate = DateFormatEx.getMiniDateInstance(getLocale(), getTimeZone());
			
			WideLinkGroupControl wlg = new WideLinkGroupControl(this);
			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				
				WideLink wl = wlg.addLink()
					.setTitle(entry.getText())
					.setValue(dfDate.format(entry.getCreated()))
					.setURL(getPageURL(JournalEntryPage.COMMAND, 
						new ParameterMap(JournalEntryPage.PARAM_ID, entryID.toString())));

				// Photo
				if (entry.isHasPhoto())
				{
					Image photo = entry.getPhoto();
					if (photo != null)
					{
						wl.setImage(photo).setImageSizeSpec(BabyConsts.IMAGESIZE_THUMB_50X50);
					}
				}

				// Measure records
				List<UUID> recordIDs = MeasureRecordStore.getInstance().getByJournalEntryID(entryID);
				if (recordIDs.isEmpty() == false)
				{
					Set<UUID> measureIDs = new LinkedHashSet<UUID>();
					for (UUID recordID : recordIDs)
					{
						MeasureRecord record = MeasureRecordStore.getInstance().load(recordID);
						measureIDs.add(record.getMeasureID());
					}
					
					StringBuilder sb = new StringBuilder(getString("journey:Journal.MeasureRecords"));
					int i = 0;
					for (UUID measureID : measureIDs)
					{
						Measure measure = MeasureStore.getInstance().load(measureID);
						
						if (i > 0)
						{
							if (i < measureIDs.size() - 1)
							{
								sb.append(getString("journey:Journal.Comma"));
							}
							else
							{
								if (i > 1)
								{
									sb.append(getString("journey:Journal.Comma"));
								}
								sb.append(getString("journey:Journal.And"));
							}
						}
						
						sb.append(measure.getLabel());
						
						i++;
					}
					
					wl.setExtra(sb.toString());
				}
			}
			wlg.render();
		}
		else
		{
			writeEncode(getString("journey:Journal.NoEntry"));
		}
		
		writeIncludeJS("baby/journal.js");
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Journal.Title");
	}
}
