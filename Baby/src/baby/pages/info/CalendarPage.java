package baby.pages.info;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import baby.controls.BadgedCalendarControl;
import baby.controls.BadgedCalendarControl.Badge;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;
import baby.pages.scrapbook.DaySummaryPage;

public class CalendarPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/calendar";

	@Override
	public void renderHTML() throws Exception
	{
//		writeHorizontalNav(CalendarPage.COMMAND);
		
		UUID userID = getContext().getUserID();
		Mother mother = MotherStore.getInstance().loadByUserID(userID);
		
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
		
		cal.set(yyyy, mm - 1, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();

		//
		// Journal entries
		//
		
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByDate(userID, from, to);
		for (UUID entryID : entryIDs)
		{
			JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(entry.getCreated());
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(entry.isHasPhoto() ? Badge.Photo : Badge.Text);
		}
		
		//
		// Measure records
		//
		
		List<UUID> recordIDs = MeasureRecordStore.getInstance().getByDate(userID, from, to);
		for (UUID recordID : recordIDs)
		{
			MeasureRecord record = MeasureRecordStore.getInstance().load(recordID);
			
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(record.getCreatedDate());
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(Badge.MeasureRecord);
		}
		
		//
		// Checklist dues
		//
		
		Calendar calChecklist = Calendar.getInstance(getTimeZone());
		calChecklist.set(yyyy, mm - 1, dd, 0, 0, 0);
		calChecklist.set(Calendar.MILLISECOND, 0);
		
		Stage lowStage = mother.getEstimatedPregnancyStage(calChecklist.getTime(), getTimeZone());
		calChecklist.add(Calendar.MONTH, 1);
		Stage highStage = mother.getEstimatedPregnancyStage(calChecklist.getTime(), getTimeZone());
		
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryByTimeline(lowStage.toInteger(), highStage.toInteger());
		for (UUID checklistID : checklistIDs)
		{
			Checklist checklist = ChecklistStore.getInstance().load(checklistID);
			
			Date checklistDue = mother.calcDateOfStage(checklist.getTimelineTo(), getTimeZone());
			if (checklistDue != null)
			{
				calChecklist.setTime(checklistDue);
				calCtrl.getBadges(
					calChecklist.get(Calendar.YEAR), calChecklist.get(Calendar.MONTH) + 1, calChecklist.get(Calendar.DAY_OF_MONTH))
					.add(Badge.ChecklistDue);
			}
		}
		
		//
		// Appointment dues
		//
		
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getByDate(userID, from, to);
		for (UUID appointmentID : appointmentIDs)
		{
			Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
			
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(appointment.getDateTime());
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(Badge.AppointmentDue);			
		}
		
		//
		// Delivery due
		//
		
		Date due = mother.getDueDate(getTimeZone());
		if (due == null)
		{
			due = mother.getBirthDate(getTimeZone());
		}
		if (due != null)
		{
			Calendar c = Calendar.getInstance(getTimeZone());
			c.setTime(due);
			calCtrl.getBadges(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
				.add(Badge.DeliveryDue);		
		}
		
		write("<div class=\"BigCalendarContainer\">");
		
		calCtrl.setCommand(DaySummaryPage.COMMAND, null);
		calCtrl.render();
		
		//
		// Legend
		//
		
		write("<div class=\"BigCalendarLegend\">");
		for (Badge badge : Badge.values())
		{
			write("<span class=\"CalendarBadge ");
			write(badge.toString());
			write("\"></span>");
			writeEncode(getString("information:Calendar." + badge.toString()));
			write("<br>");
		}
		write("</div>");
		
		write("</div>");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("information:Calendar.Title");
	}
}
