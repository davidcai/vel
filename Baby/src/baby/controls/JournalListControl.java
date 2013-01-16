package baby.controls;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
import samoyan.core.Day;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;
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
import baby.pages.journey.MeasureRecordsPageHelper;
import baby.pages.journey.PhotoPage;

public class JournalListControl
{
	private static class ListItem implements Comparable<ListItem>
	{
		private Date date;
		private JournalEntry entry;
		private List<MeasureRecord> momRecords;
		private Map<UUID, List<MeasureRecord>> babyRecords;
		
		public Date getDate()
		{
			return date;
		}
		public void setDate(Date date)
		{
			this.date = date;
		}
		
		public JournalEntry getEntry()
		{
			return entry;
		}
		public void setEntry(JournalEntry entry)
		{
			this.entry = entry;
		}
		
		public List<MeasureRecord> getMomRecords()
		{
			if (momRecords == null)
			{
				momRecords = new ArrayList<MeasureRecord>();
			}
			return momRecords;
		}
		public void setMomRecords(List<MeasureRecord> momRecords)
		{
			this.momRecords = momRecords;
		}

		public Map<UUID, List<MeasureRecord>> getBabyRecords()
		{
			if (babyRecords == null)
			{
				babyRecords = new LinkedHashMap<UUID, List<MeasureRecord>>();
			}
			return babyRecords;
		}
		public void setBabyRecords(Map<UUID, List<MeasureRecord>> babyRecords)
		{
			this.babyRecords = babyRecords;
		}
		
		@Override
		public int compareTo(ListItem that)
		{
			// Late dates go first
			Date d1 = this.getDate();
			Date d2 = (that == null) ? null : that.getDate();
			return d2 == null ? -1 :  - d1.compareTo(d2);
		}
	}
	
	private WebPage out;
	private List<UUID> entryIDs;
	private List<UUID> recordIDs;
	private Map<Date, ListItem> itemsByDate;
	
	public JournalListControl(WebPage out)
	{
		this.out = out;
	}
	public JournalListControl(WebPage out, List<UUID> entryIDs, List<UUID> recordIDs)
	{
		this.out = out;
		this.entryIDs = entryIDs;
		this.recordIDs = recordIDs;
	}
	
	public List<UUID> getEntryIDs()
	{
		if (this.entryIDs == null)
		{
			this.entryIDs = new ArrayList<UUID>();
		}
		return this.entryIDs;
	}
	public JournalListControl setEntryIDs(List<UUID> entryIDs)
	{
		this.entryIDs = entryIDs;
		return this;
	}
	
	public List<UUID> getRecordIDs()
	{
		if (this.recordIDs == null)
		{
			this.recordIDs = new ArrayList<UUID>();
		}
		return this.recordIDs;
	}
	public JournalListControl setRecordIDs(List<UUID> recordIDs)
	{
		this.recordIDs = recordIDs;
		return this;
	}
	
	public void render() throws Exception
	{
		//
		// Prepare list items
		//
		
		itemsByDate = new TreeMap<Date, ListItem>(new Comparator<Date>() {

			@Override
			public int compare(Date d1, Date d2)
			{
				return - d1.compareTo(d2);
			}
		});
		
		if ((entryIDs != null && entryIDs.isEmpty() == false) || (recordIDs != null && recordIDs.isEmpty() == false))
		{
			// Journal entries
			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				Date date = entry.getCreated();
				
				ListItem item = itemsByDate.get(date);
				if (item == null)
				{
					item = new ListItem();
					itemsByDate.put(date, item);
				}
				item.setDate(date);
				item.setEntry(entry);
			}
			
			// Measure records
			for (UUID recordID : recordIDs)
			{
				MeasureRecord rec = MeasureRecordStore.getInstance().load(recordID);
				if (rec.getValue() != null)
				{
					Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
					if (m != null)
					{
						Date date = rec.getCreatedDate();
						
						ListItem item = itemsByDate.get(date);
						if (item == null)
						{
							item = new ListItem();
							itemsByDate.put(date, item);
						}
						item.setDate(date);
						
						UUID babyID = rec.getBabyID();
						if (babyID == null)
						{
							// Mother
							item.getMomRecords().add(rec);
						}
						else
						{
							// Babies
							Baby baby = BabyStore.getInstance().load(babyID);
							if (baby != null)
							{
								List<MeasureRecord> records = item.getBabyRecords().get(babyID);
								if (records == null)
								{
									records = new ArrayList<MeasureRecord>();
									item.getBabyRecords().put(babyID, records);
								}
								records.add(rec);
							}
						}
					}
				}
			}
			
		} //-- if
		
		// 
		// Render list items
		//
		
		if (itemsByDate.isEmpty() == false)
		{
			Day today = new Day(out.getTimeZone(), new Date());
			boolean phone = out.getContext().getUserAgent().isSmartPhone();
			DateFormat dfDow = DateFormatEx.getSimpleInstance(phone ? "EEE" : "EEEE", out.getLocale(), out.getTimeZone());
			DateFormat dfDate = DateFormatEx.getLongDateInstance(out.getLocale(), out.getTimeZone());
			
			out.write("<div class=\"JournalList\">");
			
			Day prevDay = null;
			for (Date date : itemsByDate.keySet())
			{
				// Date heading
				Day day = new Day(out.getTimeZone(), date);
				if (day.equals(prevDay) == false)
				{
					StringBuilder dateStr = new StringBuilder();
					if (day.equals(today))
					{
						dateStr.append(out.getString("journey:Journal.Today"));
					}
					else
					{
						dateStr.append(dfDow.format(date));
					}
					dateStr.append(out.getString("journey:Journal.Comma"));
					dateStr.append(dfDate.format(date));
					
					out.write("<div class=\"DateHeading\">");
					out.writeEncode(dateStr.toString());
					out.write("</div>");
				}
				prevDay = day;
				
				out.write("<div class=\"ListItem\">");
				
				ListItem item = itemsByDate.get(date);
				JournalEntry entry = item.getEntry();
				List<MeasureRecord> momRecords = item.getMomRecords();
				Map<UUID, List<MeasureRecord>> babyRecords = item.getBabyRecords();
				
				// Photo
				if (entry != null && entry.isHasPhoto())
				{
					out.write("<div class=\"Photo\">");
					out.writeImage(entry.getPhoto(), BabyConsts.IMAGESIZE_THUMB_100X100, null, 
						out.getPageURL(PhotoPage.COMMAND, 
							new ParameterMap(PhotoPage.PARAM_ID, entry.getID().toString())));
					out.write("</div>");
				}
				
				out.write("<div class=\"RecordsAndText\">");
				
				// Records
				if (momRecords.isEmpty() == false || babyRecords.isEmpty() == false)
				{
					Mother mom = MotherStore.getInstance().loadByUserID(out.getContext().getUserID());
					
					out.write("<div class=\"Records\">");
					TwoColFormControl twoCol = new TwoColFormControl(out);
					
					// Mother
					if (momRecords.isEmpty() == false)
					{
						User user = UserStore.getInstance().load(out.getContext().getUserID());
						
						twoCol.writeRow(user.getDisplayName());
						writeMeasureRecords(twoCol, momRecords, mom.isMetric());
					}
					
					// Babies
					if (babyRecords.isEmpty() == false)
					{
						for (UUID babyID : babyRecords.keySet())
						{
							List<MeasureRecord> records = babyRecords.get(babyID);
							if (records.isEmpty() == false)
							{
								Baby baby = BabyStore.getInstance().load(babyID);
								String name = Util.isEmpty(baby.getName()) ? out.getString(
									"journey:Journal.Anonymous") : baby.getName();
									
								twoCol.writeRow(name);
								writeMeasureRecords(twoCol, babyRecords.get(babyID), mom.isMetric());
							}
						}
					}
					
					twoCol.render();
					out.write("</div>"); //-- .Records
				}
				
				// Text
				if (entry != null && Util.isEmpty(entry.getText()) == false)
				{
					out.write("<div class=\"Text\">");
					out.writeEncode(entry.getText());
					out.write("</div>");
				}
				
				out.write("</div>"); //-- .RecordsAndText
				
				out.write("</div>"); //-- .ListItem
				
			} //-- for
			
			out.write("</div>"); //-- .JournalList
		}
	}
	
	private void writeMeasureRecords(TwoColFormControl twoCol, List<MeasureRecord> records, boolean metric) throws Exception
	{
		boolean first = true;
		for (MeasureRecord rec : records)
		{
			Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
			
			Float val = MeasureRecordsPageHelper.getMeasureRecordValue(rec, metric);
			String unit = metric ? m.getMetricUnit() : m.getImperialUnit();
			
			if (first == false)
			{
				twoCol.writeEncode(out.getString("journey:Journal.Comma"));
			}
			
			twoCol.writeEncode(out.getString("journey:Journal.MeasureRecord", 
				m.getLabel(), String.valueOf(val), unit));
			
			first = false;
		}
	}
}
