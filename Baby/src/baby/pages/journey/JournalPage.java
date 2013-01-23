package baby.pages.journey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.DecimalInputControl;
import samoyan.controls.ImageInputControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.UserStore;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.controls.JournalListControl;
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
	public final static String COMMAND_LIST = BabyPage.COMMAND_JOURNEY + "/journal";
	public final static String COMMAND_EDIT = BabyPage.COMMAND_JOURNEY + "/edit-journal";
	public final static String COMMAND_RECORD = BabyPage.COMMAND_JOURNEY + "/add-record";
	
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
			
			if (JournalPage.COMMAND_RECORD.equals(getContext().getCommand()))
			{
				// For add record page, show errors if measure records have no inputs
				if (hasRecParams == false)
				{
					throw new WebFormException(recParamNames, getString("journey:Journal.RequireRecord"));
				}
			}
			else
			{
				// For list and edit pages, show errors if none of text, photo, or measure records have inputs
				if (hasRecParams == false && 
					isParameterNotEmpty(PARAM_TEXT) == false && 
					getParameterImage(PARAM_PHOTO) == null)
				{
					String[] paramNames = recParamNames.toArray(new String[recParamNames.size() + 2]);
					paramNames[paramNames.length - 2] = PARAM_TEXT;
					paramNames[paramNames.length - 1] = PARAM_PHOTO;
					throw new WebFormException(paramNames, getString("journey:Journal.RequireInput"));
				}

				// Text
				validateParameterString(PARAM_TEXT, 0, JournalEntry.MAXSIZE_TEXT);
			}

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
		
		// Commit journal entry only if the current page is a list or edit page
		String cmd = getContext().getCommand();
		if (JournalPage.COMMAND_LIST.equals(cmd) || JournalPage.COMMAND_EDIT.equals(cmd))
		{
			if (isParameter(PARAM_POST))
			{
				JournalEntry entry = null;
				UUID entryID = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), this.date);
				if (entryID != null)
				{
					entry = JournalEntryStore.getInstance().open(entryID);
				}
				if (entry == null)
				{
					entry = new JournalEntry();
					entry.setCreated(this.date);
				}
				
				entry.setUserID(getContext().getUserID());
				entry.setText(getParameterString(PARAM_TEXT));
				
				if (isParameterNotEmpty(PARAM_PHOTO))
				{
					Image photo = getParameterImage(PARAM_PHOTO);
					entry.setHasPhoto(photo != null);
					entry.setPhoto(photo);
				}
				
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
		}
		
		// Redirect to itself
		throw new RedirectException(JournalPage.COMMAND_LIST, null);
	}


	@Override
	public void renderHTML() throws Exception
	{
		UUID userID = getContext().getUserID();
		boolean phone = getContext().getUserAgent().isSmartPhone();
		String cmd = getContext().getCommand();
		
//		// Horizontal nav bar
//		if (phone)
//		{
//			new TabControl(this)
//				.addTab(JournalPage.COMMAND_LIST, getString("journey:Journal.Title"), getPageURL(JournalPage.COMMAND_LIST))
//				.addTab(GalleryPage.COMMAND, getString("journey:Gallery.Title"), getPageURL(GalleryPage.COMMAND))
//				.addTab(ChartsPage.COMMAND, getString("journey:Charts.Title"), getPageURL(ChartsPage.COMMAND))
//				.setCurrentTab(JournalPage.COMMAND_LIST)
//				.setStyleButton()
//				.setAlignStretch()
//				.render();
//		}
		
		// Edit mode button for smart phones
		if (phone && JournalPage.COMMAND_LIST.equals(cmd))
		{
			writeFormOpen("GET", JournalPage.COMMAND_LIST);
			new ButtonInputControl(this, null)
				.setValue(getString("journey:Journal.Edit"))
				.setMobileHotAction(true)
				.setID("Edit")
				.setAttribute("class", "NoShow")
				.setAttribute("labeledit", getString("journey:Journal.Edit"))
				.setAttribute("labelcancel", getString("journey:Journal.Cancel"))
				.render();
			writeFormClose();
		}
		
		JournalEntry entry = null;
		UUID entryID = JournalEntryStore.getInstance().getByDate(userID, this.date);
		if (entryID != null)
		{
			entry = JournalEntryStore.getInstance().open(entryID);
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
			.setID("EntryPlaceholder")
			.render();
		
		// Show entry input fields if the request is a post, or the current page is either edit or record page
		write("<div id=\"EntryInputs\"");
		if (isParameter(PARAM_POST) || JournalPage.COMMAND_EDIT.equals(cmd) || JournalPage.COMMAND_RECORD.equals(cmd))
		{
			write(" class=\"Show\"");
		}
		write(">");
		
		// Measure records
		writeMeasureRecords();
		write("<br>");
		
		// Show text and photo fields if the current page is either list or edit pages
		if (JournalPage.COMMAND_LIST.equals(cmd) || JournalPage.COMMAND_EDIT.equals(cmd))
		{
			// Text
			new TextAreaInputControl(this, "text")
				.setRows(3).setCols(80)
				.setMaxLength(JournalEntry.MAXSIZE_TEXT)
				.setPlaceholder(getString("journey:Journal.WriteSomething"))
				.setInitialValue(entry != null ? entry.getText() : null)
				.render();
			write("<br>");
			
			// Photo
			new ImageInputControl(this, "photo").showThumbnail(false).render();
			write("<br>");
		}
		
		// Postback
		writeHiddenInput(PARAM_TIMESTAMP, getParameterString(PARAM_TIMESTAMP));
		
		// Buttons
		if (JournalPage.COMMAND_EDIT.equals(cmd))
		{
			new ButtonInputControl(this, PARAM_POST)
				.setValue(getString("journey:Journal.Save"))
				.setMobileHotAction(phone)
				.render();
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
		}
		else
		{
			writeButton(PARAM_POST, getString("journey:Journal.Post"));
		}
		write("</div>"); //-- #EntryInputs
		
		writeFormClose();
		
		//
		// Entries
		//
		
		if (JournalPage.COMMAND_LIST.equals(cmd))
		{
			List<UUID> entryIDs = JournalEntryStore.getInstance().getByUserID(userID);
			List<UUID> recordIDs = MeasureRecordStore.getInstance().getByUserID(userID);
			
			new JournalListControl(this)
				.setEntryIDs(entryIDs)
				.setRecordIDs(recordIDs)
				.render();
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
					String name = (Util.isEmpty(baby.getName())) ? getString("journey:Journal.Anonymous") : baby.getName();
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
