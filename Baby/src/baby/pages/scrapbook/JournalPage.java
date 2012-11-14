package baby.pages.scrapbook;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;


public class JournalPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/journal";
	public final static String PARAM_POST = "post";
	public final static String PARAM_DATE = "date";

	private JournalEntry entry = null;

	@Override
	public void init() throws Exception
	{
		this.entry = new JournalEntry();
	}

	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_POST))
		{
			validateParameterString("text", 0, JournalEntry.MAXSIZE_TEXT);

			// Show errors if both text and photo have no inputs
			if (isParameterNotEmpty("text") == false && getParameterImage("photo") == null)
			{
				throw new WebFormException(new String[] {"text", "photo"}, getString("scrapbook:JournalEntry.NoInput"));
			}
		}
	}

	@Override
	public void commit() throws Exception
	{
		this.entry.setUserID(getContext().getUserID());
		this.entry.setText(getParameterString("text"));
		
		Image photo = getParameterImage("photo");
		this.entry.setHasPhoto(photo != null);
		this.entry.setPhoto(photo);
		
		this.entry.setCreated(Calendar.getInstance(getTimeZone()).getTime());

		JournalEntryStore.getInstance().save(this.entry);

		// Redirect to itself
		throw new RedirectException(COMMAND, null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Journal.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		DateFormat df = DateFormatEx.getSimpleInstance("MMMMM d, yyyy", getLocale(), getTimeZone());

		// Date param
		Date date = null;
		if (isParameter(PARAM_DATE))
		{
			try
			{
				date = DateFormatEx.getSimpleInstance("MM-dd-yyyy", getLocale(), getTimeZone())
					.parse(getParameterString(PARAM_DATE));
			}
			catch (ParseException e)
			{
				date = null;
			}
		}
		
		write("<h2>");
		writeEncode(df.format((date != null) ? date : Calendar.getInstance(getTimeZone()).getTime()));
		//writeEncodeDate((date != null) ? date : new Date());
		write("</h2>");

		// What's on your mind?
		writeEncode(getString("scrapbook:Journal.WhatIsOnYourMind"));
		writeFormOpen();
		write("<br>");
		writeTextAreaInput("text", "", 80, 3, JournalEntry.MAXSIZE_TEXT);
		write("<br><br>");
		writeImageInput("photo", null);
		write("<br>");
		writeButton(PARAM_POST, getString("scrapbook:Journal.Post"));
		if (isParameter(PARAM_DATE))
		{
			writeHiddenInput(PARAM_DATE, Util.htmlEncode(getParameterString(PARAM_DATE)));
		}
		writeFormClose();
		
		// Entry list
		List<UUID> entryIDs = null;
		if (date != null)
		{
			Calendar cal = Calendar.getInstance(getTimeZone());
			cal.setTime(date);

			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date from = cal.getTime();
			
			cal.set(Calendar.HOUR_OF_DAY, 24);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date to = cal.getTime();
			
			entryIDs = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), from, to);
		}
		else
		{
			entryIDs = JournalEntryStore.getInstance().getByUserID(getContext().getUserID());
			if (entryIDs.size()>10) // !$!
			{
				entryIDs = entryIDs.subList(0, 10);
			}
		}
		
		write("<ul class=\"Journal\">");
		
		if (entryIDs != null && entryIDs.isEmpty() == false)
		{
			df = DateFormatEx.getSimpleInstance("MMMMM d, yyyy h:mm a", getLocale(), getTimeZone());

			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);

				write("<li>");

				write("<div class=\"EntryCreated\">");
				writeLink(
						df.format(entry.getCreated()),
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
					writeImage(entry.getPhoto(), Image.SIZE_THUMBNAIL, null, null);
					write("</div>");
				}

				write("</li>");
			}
		}
		else
		{
			write("<li>");
			writeEncode(getString("scrapbook:Journal.NoResults"));
			write("</li>");
		}

		write("</ul>");
	}
}
