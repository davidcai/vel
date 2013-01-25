package baby.controls;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
import baby.pages.journey.JournalPage;
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

		public Map<UUID, List<MeasureRecord>> getBabyRecords()
		{
			if (babyRecords == null)
			{
				babyRecords = new LinkedHashMap<UUID, List<MeasureRecord>>();
			}
			return babyRecords;
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
	private int maxSize = 5;
	private Day from;
	private String showMoreCommand;
	private String fromParamName;
	
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
	public JournalListControl(WebPage out, List<UUID> entryIDs, List<UUID> recordIDs, Day from, int maxSize)
	{
		this.out = out;
		this.entryIDs = entryIDs;
		this.recordIDs = recordIDs;
		this.from = from;
		this.maxSize = maxSize;
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
	
	public int getMaxSize()
	{
		return maxSize;
	}
	public JournalListControl setMaxSize(int maxSize)
	{
		this.maxSize = maxSize;
		return this;
	}
	
	public Day getFrom()
	{
		return from;
	}
	public JournalListControl setFrom(Day from)
	{
		this.from = from;
		return this;
	}
	
	public String getShowMoreCommand()
	{
		return showMoreCommand;
	}
	public JournalListControl setShowMoreCommand(String showMoreCommand)
	{
		this.showMoreCommand = showMoreCommand;
		return this;
	}
	
	public String getFromParamName()
	{
		return fromParamName;
	}
	public JournalListControl setFromParamName(String fromParamName)
	{
		this.fromParamName = fromParamName;
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
				// Late dates come first
				return - d1.compareTo(d2);
			}
		});
		
		TimeZone tz = out.getTimeZone();
		Date dtFrom = (from != null ? from.getDayEnd(tz) : null);
		
		if ((entryIDs != null && entryIDs.isEmpty() == false) || (recordIDs != null && recordIDs.isEmpty() == false))
		{
			// Journal entries
			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				Date date = entry.getCreated();
				
				// Skip dates
				if (dtFrom != null && date.after(dtFrom))
				{
					continue;
				}
				
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
				Date date = rec.getCreatedDate();
				
				// Skip previous dates
				if (dtFrom != null && date.after(dtFrom))
				{
					continue;
				}
				
				// Skip records w/o values
				if (rec.getValue() == null)
				{
					continue;
				}
				
				// Skip orphan records
				Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
				if (m == null)
				{
					continue;
				}
				
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
			} //-- for
			
		} //-- if
		
		// 
		// Render list items
		//
		
		if (itemsByDate.isEmpty() == false)
		{
			Day today = new Day(tz, new Date());
			boolean phone = out.getContext().getUserAgent().isSmartPhone();
			DateFormat dfDow = DateFormatEx.getSimpleInstance(phone ? "EEE" : "EEEE", out.getLocale(), tz);
			DateFormat dfDate = DateFormatEx.getLongDateInstance(out.getLocale(), tz);
			
			out.write("<div class=\"JournalList\">");
			
			int size = 0;
			Day prevDay = null;
			Day nextFrom = null;
			
			for (Date date : itemsByDate.keySet())
			{
				// Date heading
				Day day = new Day(tz, date);
				if (day.equals(prevDay) == false)
				{
					// Stop rendering list items if the number of items exceeds the maximum size
					if (size >= maxSize)
					{
						nextFrom = day;
						break;
					}
					
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
				
				// Edit button
				out.write("<div class=\"EditButton\">");
//				out.writeImage("baby/edit.png", out.getString("journey:Journal.Edit"), 
//						out.getPageURL(JournalPage.COMMAND_EDIT, new ParameterMap(JournalPage.PARAM_TIMESTAMP, date.getTime())));
				out.write("<small><a href=\"");
				out.writeEncode(out.getPageURL(JournalPage.COMMAND_EDIT, new ParameterMap(JournalPage.PARAM_TIMESTAMP, date.getTime())));
				out.write("\">");
				out.writeImage("icons/standard/pencil-16.png", out.getString("journey:Journal.Edit"));
				out.write(" ");
				out.writeEncode(out.getString("journey:Journal.Edit"));
				out.write("</a></small>");
				out.write("</div>");
				
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
				
				size++;
				
			} //-- for
			
			out.write("</div>"); //-- .JournalList
			
			// Show more button
			if (nextFrom != null)
			{
				if (showMoreCommand == null || fromParamName == null)
				{
					throw new IllegalStateException("showMoreCommand and fromParamName must be specified.");
				}
				
				String btnID = "ShowMore-" + nextFrom.getYear() + "-" + nextFrom.getMonth() + "-" + nextFrom.getDay();
				String url = out.getPageURL(showMoreCommand, new ParameterMap(fromParamName, nextFrom.toString()));
				
				out.write("<div class=\"JournalShowMoreButton\" id=\"");
				out.write(btnID);
				out.write("\">");
				
				out.write("<a href=\"javascript:void(0);\">");
				out.writeEncode(out.getString("baby:JournalListCtrl.ShowMore"));
				out.write("</a>");
				
				out.write("<script>");
				out.write("$('#"); out.write(btnID); out.write(" A').on('click', function(e) {");
				out.write("  e.preventDefault();");
				out.write("  $.ajax({");
				out.write("    url: '"); out.write(url); out.write("', ");
				out.write("    dataType: 'html', ");
				out.write("    context: this, ");
				out.write("    success: function(data, textStatus, jqXHR) {");
				out.write("      $(this).parent().replaceWith(data);"); // Replace .JournalShowMoreButton with returned HTML
				out.write("    }"); //-- success: function
				out.write("  });"); //-- $.ajax({
				out.write("});"); //-- on('click', ...)
				out.write("</script>");
				
				out.write("</div>"); //-- .JournalShowMoreButton
			}
			
		} //-- if
	}
	
	private void writeMeasureRecords(TwoColFormControl twoCol, List<MeasureRecord> records, boolean metric) throws Exception
	{
		boolean first = true;
		for (MeasureRecord rec : records)
		{
			Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
			
			String strVal = null;
			Float val = MeasureRecordsPageHelper.getMeasureRecordValue(rec, metric);
			if (val != null)
			{
				boolean hasDecimals = (Math.round(val * 1000) % 1000) != 0;
				if (hasDecimals)
				{
					strVal = String.valueOf(val);
				}
				else
				{
					strVal = String.valueOf(val.intValue());
				}
			}
			
			String unit = metric ? m.getMetricUnit() : m.getImperialUnit();
			
			if (first == false)
			{
				twoCol.writeEncode(out.getString("journey:Journal.Comma"));
			}
			
			twoCol.writeEncode(out.getString("journey:Journal.MeasureRecord", 
				m.getLabel(), strVal, unit));
			
			first = false;
		}
	}
}
