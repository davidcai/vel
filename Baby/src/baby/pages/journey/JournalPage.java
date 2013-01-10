package baby.pages.journey;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TabControl;
import samoyan.controls.WideLinkGroupControl;
import samoyan.controls.WideLinkGroupControl.WideLink;
import samoyan.core.DateFormatEx;
import samoyan.core.Day;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.UserStore;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public class JournalPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/journal";
	
	private final static String PARAM_POST = "post";
	private final static String PARAM_TEXT = "text";
	private final static String PARAM_PHOTO = "photo";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_POST))
		{
			validateParameterString(PARAM_TEXT, 0, JournalEntry.MAXSIZE_TEXT);

			// Show errors if both text and photo have no inputs
			if (isParameterNotEmpty(PARAM_TEXT) == false && getParameterImage(PARAM_PHOTO) == null)
			{
				throw new WebFormException(new String[] {PARAM_TEXT, PARAM_PHOTO}, getString("journey:Journal.NoInput"));
			}
		}
	}
	@Override
	public void commit() throws Exception
	{
		JournalEntry entry = new JournalEntry();
		entry.setUserID(getContext().getUserID());
		entry.setText(getParameterString(PARAM_TEXT));
		
		Image photo = getParameterImage(PARAM_PHOTO);
		entry.setHasPhoto(photo != null);
		entry.setPhoto(photo);
		
		entry.setCreated(Calendar.getInstance(getTimeZone()).getTime());

		JournalEntryStore.getInstance().save(entry);

		// Redirect to itself
		throw new RedirectException(JournalPage.COMMAND, null);
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
//		write("<div id=\"NewJournalEntryPanel\"");
//		if (isParameter(PARAM_POST))
//		{
//			write(" class=\"Expanded\"");
//		}
//		write(">");
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
		UUID userID = getContext().getUserID();
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(userID);
		List<UUID> recordIDs = MeasureRecordStore.getInstance().getByUserID(userID);
		if (entryIDs.isEmpty() == false || recordIDs.isEmpty() == false)
		{
			write("<div id=\"EntriesList\">");
			
			//
			// Group entries by dates. Entries include journal entries and measure records. 
			//
			
			Map<Day, List<Object>> entriesByDates = new LinkedHashMap<Day, List<Object>>();
			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				Day day = new Day(getTimeZone(), entry.getCreated());
				
				List<Object> entries = entriesByDates.get(day);
				if (entries == null)
				{
					entries = new ArrayList<Object>();
					entriesByDates.put(day, entries);
				}
				entries.add(entry);
			}
			for (UUID recordID : recordIDs)
			{
				MeasureRecord rec = MeasureRecordStore.getInstance().load(recordID);
				if (rec.getValue() != null)
				{
					Day day = new Day(getTimeZone(), rec.getCreatedDate());
					
					List<Object> entries = entriesByDates.get(day);
					if (entries == null)
					{
						entries = new ArrayList<Object>();
						entriesByDates.put(day, entries);
					}
					entries.add(rec);
				}
			}
			
			//
			// Render entries
			//
			
			Day today = new Day(getTimeZone(), new Date());
			boolean phone = getContext().getUserAgent().isSmartPhone();
			DateFormat dfDow = DateFormatEx.getSimpleInstance(phone ? "EEE" : "EEEE", getLocale(), getTimeZone());
			DateFormat dfDate = DateFormatEx.getLongDateInstance(getLocale(), getTimeZone());
			Mother mom = MotherStore.getInstance().loadByUserID(userID);
			String momName = UserStore.getInstance().load(userID).getDisplayName();
		
			for (Day day : entriesByDates.keySet())
			{
				// Date header
				Date date = day.getMidDay(getTimeZone(), 0, 0, 0);
				
				StringBuilder dateStr = new StringBuilder();
				if (day.equals(today))
				{
					dateStr.append(getString("journey:Journal.Today"));
				}
				else
				{
					dateStr.append(dfDow.format(date));
				}
				dateStr.append(getString("journey:Journal.Comma"));
				dateStr.append(dfDate.format(date));
				
				write("<div class=\"Date\">");
				writeEncode(dateStr.toString());
				write("</div>");
				
				// Entry list
				WideLinkGroupControl wlg = new WideLinkGroupControl(this);
				WideLink prevLink = null;
				MeasureRecord prevRec = null;
				List<Object> entries = entriesByDates.get(day);
				for (Object obj : entries)
				{
					// Journal entry
					if (obj instanceof JournalEntry)
					{
						JournalEntry entry = (JournalEntry) obj;
						WideLink wl = wlg.addLink()
								.setTitle(entry.getText())
								.setURL(getPageURL(JournalEntryPage.COMMAND, 
									new ParameterMap(JournalEntryPage.PARAM_ID, entry.getID().toString())));
						
						String cssClass = "JournalEntry";
						
						// Photo
						if (entry.isHasPhoto())
						{
							Image photo = entry.getPhoto();
							if (photo != null)
							{
								wl.setImage(photo, BabyConsts.IMAGESIZE_THUMB_50X50, entry.getText());
								cssClass += " PhotoEntry";
							}
						}
						
						wl.setCSSClass(cssClass);
					}
					// Measure record
					else if (obj instanceof MeasureRecord)
					{
						MeasureRecord rec = (MeasureRecord) obj;
						Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
						
						if (m != null)
						{
							// Person name
							String name = null;
							if (rec.getBabyID() != null)
							{
								Baby baby = BabyStore.getInstance().load(rec.getBabyID());
								if (baby != null)
								{
									// Use baby's name since this record is a baby record.
									name = Util.isEmpty(baby.getName()) ? getString("journey:Journal.Anonymous") : baby.getName();
								}
							}
							else
							{
								name = momName;
							}
							
							if (Util.isEmpty(name) == false)
							{
								Float val = getMeasureRecordValue(rec, mom.isMetric());
								if (val != null)
								{
									String label = m.getLabel();
									String unit = mom.isMetric() ? m.getMetricUnit() : m.getImperialUnit();
									String title = getString("journey:Journal.MeasureRecord", label, name, val, unit);
									
									WideLink wl = prevLink;
									if (prevRec == null || rec.getCreatedDate().equals(prevRec.getCreatedDate()) == false)
									{
										wl = wlg.addLink()
											.setCSSClass("MeasureRecord")
											.setURL(getPageURL(MeasureRecordsPage.COMMAND, new ParameterMap(MeasureRecordsPage.PARAM_TIMESTAMP, 
												String.valueOf(rec.getCreatedDate().getTime()))));
										prevLink = wl;
									}
									
									if (Util.isEmpty(wl.getTitle()) == false)
									{
										title = wl.getTitle() + (phone ? Util.textToHtml("\r\n") : ", ") + title;
									}
									wl.setTitle(title);
									
									prevRec = rec;
								}
							}
						}
						
					} //-- else if (obj instanceof MeasureRecord)
					
				} //-- for entries
				
				wlg.render();
				
			} //-- for date groups
			
			write("</div>"); //-- #EntriesList
		}
		else
		{
			writeEncode(getString("journey:Journal.NoEntry"));
		}
		
		writeIncludeJS("baby/journal.js");
	}
	
	/**
	 * Gets measure record value that is normalized by the specified metric flag.
	 * 
	 * @param rec
	 * @param metric
	 * @return
	 * @throws Exception
	 */
	private Float getMeasureRecordValue(MeasureRecord rec, boolean metric) throws Exception
	{
		Float val = rec.getValue();
		if (val != null)
		{
			Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
			
			// Convert value from record's current unit system to mother's unit system
			if (metric && rec.isMetric() == false)
			{
				val = m.toMetric(val);
			}
			else if (metric == false && rec.isMetric())
			{
				val = m.toImperial(val);
			}
		}
		
		return val;
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Journal.Title");
	}
}
