package baby.pages.journey;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TabControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.servlet.RequestContext;
import baby.database.BabyStore;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
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
		
		writeStageInfo();
		
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

	private void writeStageInfo() throws Exception
	{
		RequestContext ctx = getContext();
		Mother mother = MotherStore.getInstance().loadByUserID(ctx.getUserID());
		Stage stage = mother.getPregnancyStage();
		Date now = new Date();

		// Stage status
		String status = null;
		if (stage.isPreconception())
		{
			status = getString("journey:Journal.StatusPreconception");
		}
		else if (stage.isPregnancy())
		{
			Date due = mother.getDueDate();
			long days = (due.getTime() - now.getTime()) / (24L*60L*60L*1000L) + 1;
			if (days<=1L)
			{
				// Overdue
				status = getString("journey:Journal.StatusImminent");
			}
			else
			{
				status = getString("journey:Journal.StatusPregnancy", stage.getPregnancyWeek(), days);
			}
		}
		else if (stage.isInfancy())
		{
			String names = null;
			List<UUID> babyIDs = BabyStore.getInstance().getAtLeastOneBaby(ctx.getUserID());
			if (babyIDs.size()==1)
			{
				names = BabyStore.getInstance().load(babyIDs.get(0)).getName();
			}
			if (Util.isEmpty(names))
			{
				names = getString("journey:Journal.BabyCountName." + (babyIDs.size()<=8 ? babyIDs.size() : "N"));
			}
			
			Date birth = mother.getBirthDate();
			long weeks = (now.getTime() - birth.getTime()) / (7L*24L*60L*60L*1000L) + 1;
			if (weeks<=18)
			{
				status = getString("journey:Journal.StatusInfancyWeeks", names, babyIDs.size(), weeks);
			}
			else
			{
				status = getString("journey:Journal.StatusInfancyMonths", names, babyIDs.size(), stage.getInfancyMonth());
			}
		}
		
		write("<div align=center>");
		write("<h2>");
		writeEncode(status);
		write("</h2>");
		write("</div>");
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Journal.Title");
	}
}
