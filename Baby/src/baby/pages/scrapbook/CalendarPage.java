package baby.pages.scrapbook;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import baby.controls.BadgedCalendarControl;
import baby.controls.BadgedCalendarControl.Badge;
import baby.controls.BadgedCalendarControl.BadgeType;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
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

		cal.set(yyyy, mm - 1, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();

		// Journal entries
		Map<Calendar, Integer> dayToBadgeCount = new HashMap<Calendar, Integer>();
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByDate(getContext().getUserID(), from, to);
		for (UUID entryID : entryIDs)
		{
			JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
			
			Calendar c = Calendar.getInstance(getTimeZone());
			c.clear();
			c.setTime(entry.getCreated());
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			
			Integer count = dayToBadgeCount.get(c);
			dayToBadgeCount.put(c, (count == null ? 0 : count) + 1);
		}
		
		// TODO: Checklists
		
		// TODO: Appointments
		
		// Calendar
		BadgedCalendarControl calCtrl = new BadgedCalendarControl(this);
		calCtrl.setDay(yyyy, mm, dd);
		for (Calendar c : dayToBadgeCount.keySet())
		{
			List<Badge> badges = calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
			badges.add(new Badge(BadgeType.JournalEntry, dayToBadgeCount.get(c)));
			
			// Dummy data
			badges.add(new Badge(BadgeType.Checklist, dayToBadgeCount.get(c) + 1));
			badges.add(new Badge(BadgeType.Appointment, dayToBadgeCount.get(c) + 10));
			//-- Dummy data
		}
		
		calCtrl.render();
		
		// TODO: Legend
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:Calendar.Title");
	}
}
