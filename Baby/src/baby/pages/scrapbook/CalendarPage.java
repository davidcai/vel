package baby.pages.scrapbook;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import baby.controls.BadgedCalendarControl;
import baby.controls.BadgedCalendarControl.Badge;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.pages.BabyPage;

public class CalendarPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/calendar";

	@Override
	public void renderHTML() throws Exception
	{
		// Get base date
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		Integer yyyy = getParameterInteger("y");
		if (yyyy == null)
		{
			yyyy = cal.get(Calendar.YEAR);
		}
		Integer mm = getParameterInteger("m");
		if (mm == null)
		{
			mm = cal.get(Calendar.MONTH) + 1; // 0-based to 1-based
		}
		Integer dd = getParameterInteger("d");
		if (dd == null)
		{
			dd = cal.get(Calendar.DAY_OF_MONTH);
		}
		
		// Calendar
		BadgedCalendarControl calCtrl = new BadgedCalendarControl(this);
		calCtrl.setDay(yyyy, mm, dd);

		// Journal entries
		cal.set(yyyy, mm - 1, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();
		
		UUID userID = getContext().getUserID();
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByDate(userID, from, to);
		for (UUID entryID : entryIDs)
		{
			JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(entry.getCreated());
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(entry.isHasPhoto() ? Badge.Photo : Badge.Text);
		}
		
		// Measure records
		List<UUID> recordIDs = MeasureRecordStore.getInstance().getByDate(userID, from, to);
		for (UUID recordID : recordIDs)
		{
			MeasureRecord record = MeasureRecordStore.getInstance().load(recordID);
			
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(record.getCreatedDate());
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(Badge.MeasureRecord);
		}
		
		// TODO: Checklists
		//ChecklistStore.getInstance().queryBySectionAndTimeline(section, lowStage, highStage);
		
		// TODO: Appointments
		
		calCtrl.render();
		
		// TODO: Legend
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Calendar.Title");
	}
}
