package baby.pages.scrapbook;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TabControl;
import samoyan.controls.TextAreaInputControl;
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
	public final static String PARAM_YYYY = "yyyy";
	public final static String PARAM_M = "m";
	public final static String PARAM_D = "d";

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
		// Horizontal nav bar
		if (getContext().getUserAgent().isSmartPhone())
		{
			new TabControl(this)
				.addTab(JournalPage.COMMAND, getString("scrapbook:Journal.Title"), getPageURL(JournalPage.COMMAND))
				.addTab(ChartsPage.COMMAND, getString("scrapbook:Charts.Title"), getPageURL(ChartsPage.COMMAND))
				.addTab(GalleryPage.COMMAND, getString("scrapbook:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
				.setCurrentTab(getContext().getCommand())
				.setStyleButton()
				.setAlignStretch()
				.render();
		}

		// Date param
		Date date = null;
		if (isParameterNotEmpty(PARAM_YYYY) && isParameterNotEmpty(PARAM_M) && isParameterNotEmpty(PARAM_D))
		{
			Integer yyyy = getParameterInteger(PARAM_YYYY);
			Integer m = getParameterInteger(PARAM_M);
			Integer d = getParameterInteger(PARAM_D);
			
			if (yyyy != null && m != null & d != null)
			{
				Calendar cal = Calendar.getInstance(getTimeZone());
				cal.set(yyyy, m - 1, d, 0, 0, 0);
				cal.set(Calendar.MILLISECOND, 0);
				
				date = cal.getTime();
			}
		}
		
		write("<h2>");
		DateFormat dfDate = DateFormatEx.getDateInstance(getLocale(), getTimeZone());
		writeEncode(dfDate.format((date != null) ? date : Calendar.getInstance(getTimeZone()).getTime()));
		//writeEncodeDate((date != null) ? date : new Date());
		write("</h2>");

		// What's on your mind?
//		writeEncode(getString("scrapbook:Journal.WhatIsOnYourMind"));
		writeFormOpen();
//		write("<br>");
		new TextAreaInputControl(this, "text")
			.setRows(3).setCols(80)
			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
			.setPlaceholder(getString("scrapbook:Journal.WhatIsOnYourMind"))
			.render();
//		writeTextAreaInput("text", "", 80, 3, JournalEntry.MAXSIZE_TEXT);
		write("<br><br>");
		writeImageInput("photo", null);
		write("<br>");
		writeButton(PARAM_POST, getString("scrapbook:Journal.Post"));
		
		// Date post back
		if (date != null)
		{
			Calendar cal = Calendar.getInstance(getTimeZone());
			cal.setTime(date);
			int yyyy = cal.get(Calendar.YEAR);
			int m = cal.get(Calendar.MONTH) + 1;
			int d = cal.get(Calendar.DAY_OF_MONTH);
			writeHiddenInput(PARAM_YYYY, yyyy); 
			writeHiddenInput(PARAM_M, m); 
			writeHiddenInput(PARAM_D, d);
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
			cal.add(Calendar.DATE, 1);
			Date to = cal.getTime();
			
			entryIDs = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), from, to);
		}
		else
		{
			entryIDs = JournalEntryStore.getInstance().getByUserID(getContext().getUserID());
			// !$! Show only the top 10?
			if (entryIDs.size()>10) 
			{
				entryIDs = entryIDs.subList(0, 10);
			}
		}
		
		write("<ul class=\"Journal\">");
		
		if (entryIDs != null && entryIDs.isEmpty() == false)
		{
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
