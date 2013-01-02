package baby.pages.journey;

import java.text.DateFormat;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TabControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;
import baby.pages.scrapbook.JournalEntryPage;
import baby.pages.scrapbook.PhotoPage;

public class JournalPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/journal";

	@Override
	public void renderHTML() throws Exception
	{
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("journey:Journal.Title"), getPageURL(JournalPage.COMMAND))
//				.addTab(ChartsPage.COMMAND, getString("scrapbook:Charts.Title"), getPageURL(ChartsPage.COMMAND))
//				.addTab(GalleryPage.COMMAND, getString("scrapbook:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}
		
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(getContext().getUserID());
		if (entryIDs.isEmpty() == false)
		{
			write("<ul class=\"Journal\">");

			DateFormat dfDateTime = DateFormatEx.getDateTimeInstance(getLocale(), getTimeZone());

			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);

				write("<li>");

				write("<div class=\"EntryCreated\">");
				writeLink(
						dfDateTime.format(entry.getCreated()),
						getPageURL(JournalEntryPage.COMMAND,
								new ParameterMap(JournalEntryPage.PARAM_ID, entryID.toString())));
				write("</div>");

				if (Util.isEmpty(entry.getText()) == false)
				{
					write("<div class=\"EntryText\">");
					writeEncode(entry.getText());
					write("</div>");
				}

				if (entry.getPhoto() != null)
				{
					write("<div class=\"EntryPhoto\">");
					write("<a href=\"");
					writeEncode(getPageURL(PhotoPage.COMMAND, new ParameterMap(PhotoPage.PARAM_ID, entry.getID().toString())));
					write("\">");
					writeImage(entry.getPhoto(), Image.SIZE_THUMBNAIL, null, null);
					write("</a>");
					write("</div>");
				}

				write("</li>");
			}

			write("</ul>");
		}
		else
		{
			write("<div class=\"PaddedPageContent\">");
			writeEncode(getString("journey:Journal.NoEntry"));
			write("</div>");
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Journal.Title");
	}
}
