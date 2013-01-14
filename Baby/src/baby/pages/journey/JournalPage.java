package baby.pages.journey;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import samoyan.controls.DecimalInputControl;
import samoyan.controls.ImageInputControl;
import samoyan.controls.TabControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
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
	
	public final static String PARAM_TIMESTAMP = "t";
	
	private final static String PARAM_RECORD_VALUE_PREFIX = "value_";
	private final static String PARAM_RECORD_ID_PREFIX = "id_";
	private final static String PARAM_TEXT = "text";
	private final static String PARAM_PHOTO = "photo";
	private final static String PARAM_POST = "post";
	private final static String PARAM_REMOVE = "remvoe";
	
	private Date date;
	private Mother mom;
	private List<MeasureRecord> momRecords;
	private Map<UUID, List<MeasureRecord>> babyRecords;
	
	@Override
	public void init() throws Exception
	{
		UUID userID = getContext().getUserID();
		this.mom = MotherStore.getInstance().loadByUserID(userID);
		
		// Get date
		Long time = getParameterLong(PARAM_TIMESTAMP);
		if (time != null)
		{
			try
			{
				this.date = new Date(time);
			}
			catch (Exception e)
			{
				this.date = null;
			}
		}
		
		// Prepare measure records
		if (this.date != null)
		{
			//
			// Editing
			//
			
			this.momRecords = new ArrayList<MeasureRecord>();
			this.babyRecords = new LinkedHashMap<UUID, List<MeasureRecord>>();
			
			List<UUID> recordIDs = MeasureRecordStore.getInstance().getByDate(userID, this.date);
			if (recordIDs.isEmpty() == false)
			{
				for (UUID recordID : recordIDs)
				{
					MeasureRecord rec = MeasureRecordStore.getInstance().open(recordID);
					Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
					if (m.isForMother())
					{
						// Mother records
						this.momRecords.add(rec);
					}
					else
					{
						// Baby records
						UUID babyID = rec.getBabyID();
						List<MeasureRecord> records = this.babyRecords.get(babyID);
						if (records == null)
						{
							records = new ArrayList<MeasureRecord>();
							this.babyRecords.put(babyID, records);
						}
						records.add(rec);
					}
				}
			}
			else
			{
				Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
				cal.setTime(this.date);
				this.momRecords = MeasureRecordsPageHelper.createMeasureRecordsForMom(this, this.mom, cal);
				this.babyRecords = MeasureRecordsPageHelper.createMeasureRecordsForBabies(this, this.mom, cal);
			}
		}
		else
		{
			//
			// New
			//
			
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			this.date = cal.getTime(); 

			this.momRecords = MeasureRecordsPageHelper.createMeasureRecordsForMom(this, this.mom, cal);
			this.babyRecords = MeasureRecordsPageHelper.createMeasureRecordsForBabies(this, this.mom, cal);
		}
	}
	
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_POST))
		{
			boolean hasRecParams = false;
			List<String> recParamNames = getMeasureRecordFieldNames();
			for (String paramName : recParamNames)
			{
				if (isParameterNotEmpty(paramName))
				{
					hasRecParams = true;
					break;
				}
			}

			// Show errors if text, photo, and measure records have no inputs
			if (hasRecParams == false && 
				isParameterNotEmpty(PARAM_TEXT) == false && 
				getParameterImage(PARAM_PHOTO) == null)
			{
				String[] paramNames = recParamNames.toArray(new String[recParamNames.size() + 2]);
				paramNames[paramNames.length - 2] = PARAM_TEXT;
				paramNames[paramNames.length - 1] = PARAM_PHOTO;
				throw new WebFormException(paramNames, getString("journey:Journal.NoInput"));
			}

			// Text
			validateParameterString(PARAM_TEXT, 0, JournalEntry.MAXSIZE_TEXT);

			// Measure records
			validateMeasureRecords(this.momRecords);
			for (List<MeasureRecord> records : this.babyRecords.values())
			{
				validateMeasureRecords(records);
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		// Measure records
		commitMeasureRecords(this.momRecords);
		for (List<MeasureRecord> records : this.babyRecords.values())
		{
			commitMeasureRecords(records);
		}
		
		// Journal entry
		if (isParameter(PARAM_POST))
		{
			JournalEntry entry = null;
			if (isParameterNotEmpty(PARAM_TIMESTAMP))
			{
				UUID entryID = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), this.date);
				if (entryID != null)
				{
					entry = JournalEntryStore.getInstance().open(entryID);
				}
			}
			if (entry == null)
			{
				entry = new JournalEntry();
				entry.setCreated(this.date);
			}
			
			entry.setUserID(getContext().getUserID());
			entry.setText(getParameterString(PARAM_TEXT));
			
			Image photo = getParameterImage(PARAM_PHOTO);
			entry.setHasPhoto(photo != null);
			entry.setPhoto(photo);
			
			JournalEntryStore.getInstance().save(entry);
		}
		else if (isParameter(PARAM_REMOVE))
		{
			UUID entryID = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), this.date);
			if (entryID != null)
			{
				JournalEntryStore.getInstance().remove(entryID);
			}
		}
		
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
		
		JournalEntry entry = null;
		if (isParameterNotEmpty(PARAM_TIMESTAMP))
		{
			UUID entryID = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), this.date);
			if (entryID != null)
			{
				entry = JournalEntryStore.getInstance().open(entryID);
			}
		}
		
		//
		// Expandable input form
		//
		
		writeFormOpen();
		
		// Placeholder
		new TextInputControl(this, null)
			.setSize(80)
			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
			.setPlaceholder(getString("journey:Journal.WhatIsOnYourMind"))
			.setAutoFocus(false)
			.setID("EntryPlaceHolder")
			.render();
		
		write("<div id=\"EntryInputs\"");
		if (isParameter(PARAM_POST) || isParameterNotEmpty(PARAM_TIMESTAMP))
		{
			write(" class=\"Show\"");
		}
		write(">");
		
		write("<div id=\"EntryInputsHelp\">");
		writeEncode(getString("journey:Journal.Help"));
		write("</div>");
		write("<br>");
		
		// Measure records
		writeMeasureRecords();
		write("<br>");
		
		// Text
		new TextAreaInputControl(this, "text")
			.setRows(3).setCols(80)
			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
			.setPlaceholder(getString("journey:Journal.Text.Placeholder"))
			.setInitialValue(entry != null ? entry.getText() : null)
			.render();
		write("<br>");
		
		// Photo
		new ImageInputControl(this, "photo").showThumbnail(false).render();
		write("<br>");
		
		// Postback
		writeHiddenInput(PARAM_TIMESTAMP, getParameterString(PARAM_TIMESTAMP));
		
		// Buttons
		if (isParameterNotEmpty(PARAM_TIMESTAMP))
		{
			writeButton(PARAM_POST, getString("journey:Journal.Save"));
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
		}
		else
		{
			writeButton(PARAM_POST, getString("journey:Journal.Post"));
		}
		write("</div>"); //-- #EntryInputs
		
		writeFormClose();
		write("<br>");
		
		//
		// Entries
		//
		
		if (isParameterNotEmpty(PARAM_TIMESTAMP) == false)
		{
			writeEntries();
		}
		
		writeIncludeJS("baby/journal.js");
	}


	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:Journal.Title");
	}


	private List<String> getMeasureRecordFieldNames() throws Exception
	{
		List<String> names = new ArrayList<String>();
		for (MeasureRecord rec : this.momRecords)
		{
			names.add(getFieldName(PARAM_RECORD_VALUE_PREFIX, rec));
		}
		for (List<MeasureRecord> records : this.babyRecords.values())
		{
			for (MeasureRecord rec : records)
			{
				names.add(getFieldName(PARAM_RECORD_VALUE_PREFIX, rec));
			}
		}
		
		return names;
	}
	
	private void validateMeasureRecords(List<MeasureRecord> records) throws Exception
	{
		for (MeasureRecord rec : records)
		{
			if (isParameterNotEmpty(getFieldName(PARAM_RECORD_VALUE_PREFIX, rec)))
			{
				Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
				Float min = this.mom.isMetric() ? m.getMetricMin() : m.getImperialMin();
				Float max = this.mom.isMetric() ? m.getMetricMax() : m.getImperialMax();
				validateParameterDecimal(getFieldName(PARAM_RECORD_VALUE_PREFIX, rec), min, max);
			}
		}
	}
	
	private void commitMeasureRecords(List<MeasureRecord> records) throws Exception
	{
		for (MeasureRecord rec : records)
		{
			if (isParameter(PARAM_POST))
			{
				rec.setValue(getParameterDecimal(getFieldName(PARAM_RECORD_VALUE_PREFIX, rec)));
				
				// Unit system defined in mother's profile always triumph over record's unit system.
				rec.setMetric(this.mom.isMetric());
				
				MeasureRecordStore.getInstance().save(rec);
			}
			else if (isParameter(PARAM_REMOVE))
			{
				MeasureRecordStore.getInstance().remove(rec.getID());
			}
		}
	}

	private void writeEntries() throws Exception
	{
		UUID userID = getContext().getUserID();
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(userID);
		List<UUID> recordIDs = MeasureRecordStore.getInstance().getByUserID(userID);
		if (entryIDs.isEmpty() == false || recordIDs.isEmpty() == false)
		{
			write("<div id=\"EntriesList\">");
			
			//
			// Group entries by dates. Entries include journal entries and measure records. 
			//
			
			Map<Day, List<Object>> entriesByDates = new TreeMap<Day, List<Object>>(new Comparator<Day>()
			{
				@Override
				public int compare(Day d1, Day d2)
				{
					return - d1.compareTo(d2);
				}
		
			});
			
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
								.setURL(getPageURL(JournalPage.COMMAND, 
									new ParameterMap(JournalPage.PARAM_TIMESTAMP, entry.getCreated().getTime())));
						
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
								Float val = MeasureRecordsPageHelper.getMeasureRecordValue(rec, mom.isMetric());
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
											.setURL(getPageURL(JournalPage.COMMAND, new ParameterMap(JournalPage.PARAM_TIMESTAMP, 
												String.valueOf(rec.getCreatedDate().getTime()))));
										prevLink = wl;
									}
									
									if (Util.isEmpty(wl.getExtra()) == false)
									{
										title = wl.getExtra() + ", " + title;
									}
									wl.setExtra(title);
									
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
	}
	
	private void writeMeasureRecords() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Mother records
		if (this.momRecords.isEmpty() == false)
		{
			twoCol.writeRow(UserStore.getInstance().load(this.mom.getUserID()).getDisplayName());
			
			boolean first = true;
			for (MeasureRecord rec : this.momRecords)
			{
				if (first == false)
				{
					twoCol.write("&nbsp;");
				}
				
				writeMeasureRecord(rec, twoCol);
				first = false;
			}
		}
		
		// Baby records
		if (this.babyRecords.isEmpty() == false)
		{
			for (UUID babyID : this.babyRecords.keySet())
			{
				Baby baby = BabyStore.getInstance().load(babyID);
				if (baby != null)
				{
					String name = (Util.isEmpty(baby.getName())) ? getString("journey:MeasureRecords.Anonymous") : baby.getName();
					twoCol.writeRow(name);
					
					List<MeasureRecord> records = this.babyRecords.get(babyID);
					boolean first = true;
					for (MeasureRecord rec : records)
					{
						if (first == false)
						{
							twoCol.write("&nbsp;");
						}
						
						writeMeasureRecord(rec, twoCol);
						first = false;
					}
				}
			}
		}
		
		twoCol.render();
	}
	
	private void writeMeasureRecord(MeasureRecord rec, TwoColFormControl twoCol) throws Exception
	{
		Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
		
		Float min = this.mom.isMetric() ? m.getMetricMin() : m.getImperialMin();
		Float max = this.mom.isMetric() ? m.getMetricMax() : m.getImperialMax();
		Float val = MeasureRecordsPageHelper.getMeasureRecordValue(rec, this.mom.isMetric());
		
		new DecimalInputControl(twoCol, getFieldName(PARAM_RECORD_VALUE_PREFIX, rec))
			.setMinValue(min)
			.setMaxValue(max)
//			.setSize(20)
			.setPlaceholder(getString("journey:Journal.MeasureRecord.Placeholder", m.getLabel(), this.mom.isMetric() ? m.getMetricUnit() : m.getImperialUnit()))
			.setInitialValue(val)
			.render();
		
		twoCol.writeHiddenInput(getFieldName(PARAM_RECORD_ID_PREFIX, rec), rec.getID().toString());
	}
	
	/**
	 * Field key = prefix + user ID + measure ID.
	 * 
	 * @param prefix
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private String getFieldName(String prefix, MeasureRecord rec) throws Exception
	{
		StringBuilder sb = new StringBuilder(prefix);
		
		Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
		if (m.isForMother())
		{
			sb.append(this.mom.getUserID().toString());
		}
		else
		{
			sb.append(rec.getBabyID().toString());
		}
		
		sb.append(rec.getMeasureID());
		
		return sb.toString();
	}
}
