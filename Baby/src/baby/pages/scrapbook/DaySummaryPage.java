package baby.pages.scrapbook;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.UserStore;
import baby.controls.ChecklistControl;
import baby.database.Appointment;
import baby.database.AppointmentStore;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;
import baby.pages.todo.AppointmentsPage;

public class DaySummaryPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/daysummary";
	
	@Override
	public void renderHTML() throws Exception
	{
		UUID userID = getContext().getUserID();
		Mother mother = MotherStore.getInstance().loadByUserID(userID);
		DateFormat dfDate = DateFormatEx.getDateInstance(getLocale(), getTimeZone());
		DateFormat dfTime = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
		boolean empty = true;
		
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
		
		cal.set(yyyy, mm - 1, dd, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		Date from = cal.getTime();
		cal.add(Calendar.DATE, 1);
		Date to = cal.getTime();
		
		write("<h2>");
		writeEncode(dfDate.format(from));
		write("</h2>");
		
		//
		// Delivery due
		//
		
		Date delivery = mother.getDueDate(getTimeZone());
		if (delivery == null)
		{
			delivery = mother.getBirthDate(getTimeZone());
		}
		if (delivery != null && delivery.before(from) == false && delivery.before(to))
		{
			// TODO: Fix this
			if (mother.getEstimatedPregnancyStage(from, getTimeZone()).isInfancy())
			{
				writeEncode(getString("scrapbook:DaySummary.GaveBirth"));
			}
			else
			{
				writeEncode(getString("scrapbook:DaySummary.Expecting"));
			}
			
			write("<br>");
			write("<br>");
			
			empty = false;
		}
		
		//
		// Journals
		//
		
		List<UUID> entryIDs = JournalEntryStore.getInstance().getByDate(userID, from, to);
		if (entryIDs.isEmpty() == false)
		{
			write("<h2>");
			writeEncode(getString("scrapbook:DaySummary.Journal"));
			write("</h2>");
			
			write("<ul class=\"Journal\">");
			
			for (UUID entryID : entryIDs)
			{
				JournalEntry entry = JournalEntryStore.getInstance().load(entryID);
				write("<li>");
	
				write("<div class=\"EntryCreated\">");
				writeLink(dfTime.format(entry.getCreated()),
					getPageURL(JournalEntryPage.COMMAND, new ParameterMap(JournalEntryPage.PARAM_ID, entryID.toString())));
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
			
			write("</ul>");
			write("<br>");
			
			empty = false;
		}
		
		//
		// Measures
		//
		
		List<UUID> recordIDs = MeasureRecordStore.getInstance().getByDate(userID, from, to);
		if (recordIDs.isEmpty() == false)
		{
			write("<h2>");
			writeEncode(getString("scrapbook:DaySummary.Measures"));
			write("</h2>");
			write("<hr>");
			
			TwoColFormControl twoCol = new TwoColFormControl(this);
			for (UUID recordID : recordIDs)
			{
				MeasureRecord rec = MeasureRecordStore.getInstance().load(recordID);
				
				String name = null;
				Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
				if (measure.isForMother()) 
				{
					String momName = UserStore.getInstance().load(mother.getUserID()).getDisplayName(); 
					if (momName.equals(name) == false)
					{
						name = momName;
						twoCol.writeTextRow(momName);
					}
				}
				else
				{
					Baby baby = BabyStore.getInstance().load(rec.getBabyID());
					if (baby != null)
					{
						String babyName = baby.getName();
						if (babyName.equals(name) == false)
						{
							name = babyName;
							twoCol.writeTextRow(name);
						}
					}
				}
				
				writeMeasureRecord(twoCol, rec, mother.isMetric());
			}
			
			twoCol.render();
			
			write("<br>");
			writeLink(getString("scrapbook:DaySummary.MoreDetails"), getPageURL(
				ChartsPage.COMMAND, new ParameterMap(ChartsPage.PARAM_DATE, mm + "-" + dd + "-" + yyyy)));
			write("<br>");
			
			empty = false;
		}
		
		//
		// Checklist dues
		//
		
		Stage lowStage = mother.getEstimatedPregnancyStage(from, getTimeZone());
		Stage highStage = mother.getEstimatedPregnancyStage(to, getTimeZone());
		
		List<UUID> checklistIDs = ChecklistStore.getInstance().queryByTimeline(lowStage.toInteger(), highStage.toInteger());
		if (checklistIDs.isEmpty() == false)
		{
			boolean first = true;
			
			for (UUID checklistID : checklistIDs)
			{
				Checklist checklist = ChecklistStore.getInstance().load(checklistID);
				Date checklistDue = mother.calcDateOfStage(checklist.getTimelineTo());
				if (checklistDue != null && checklistDue.before(from) == false && checklistDue.before(to))
				{
					if (first)
					{
						write("<h2>");
						writeEncode(getString("scrapbook:DaySummary.Checklists"));
						write("</h2>");
						write("<hr>");
					}
					
					new ChecklistControl(this, getContext().getUserID(), checklistID).render();
					
					first = false;
				}
			}
			
			if (first == false)
			{
				write("<br>");
				
				empty = false;
			}
		}
		
		//
		// Appointment dues
		//
		
		List<UUID> appointmentIDs = AppointmentStore.getInstance().getByDate(userID, from, to);
		if (appointmentIDs.isEmpty() == false)
		{
			write("<h2>");
			writeEncode(getString("scrapbook:DaySummary.Appointments"));
			write("</h2>");
			write("<hr>");
			
			write("<table>");
			for (UUID appointmentID : appointmentIDs)
			{
				Appointment appointment = AppointmentStore.getInstance().load(appointmentID);
				
				write("<tr>");
				
				write("<td style=\"text-align: right;\">");
				writeEncodeTime(appointment.getDateTime());
				write("</td>");
				
				write("<td>");
				String caption = getString("todo:Appointments.DescAndType", appointment.getDescription(), appointment.getType());
				writeLink(caption, getPageURL(AppointmentsPage.COMMAND, new ParameterMap(AppointmentsPage.PARAM_ID, appointment.getID().toString())));
				write("</td>");
				
				write("</tr>");
			}
			write("</table>");
			write("<br>");
			
			empty = false;
		}
		
		if (empty)
		{
			writeEncode(getString("scrapbook:DaySummary.Empty"));
		}
	}
	
	private void writeMeasureRecord(TwoColFormControl twoCol, MeasureRecord rec, boolean metric) throws Exception
	{
		Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
		Float val = getMeasureRecordValue(rec, metric);
		
		twoCol.writeRow(measure.getLabel());
		twoCol.writeEncode(val);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(metric ? measure.getMetricUnit() : measure.getImperialUnit());
	}
	
	/**
	 * Gets measure record value that is normalized by mother's preferred unit system.
	 * 
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private Float getMeasureRecordValue(MeasureRecord rec, boolean metric) throws Exception
	{
		Float val = rec.getValue();
		if (val != null)
		{
			Measure measure = MeasureStore.getInstance().load(rec.getMeasureID());
			
			// Convert value from record's current unit system to mother's unit system
			if (metric && rec.isMetric() == false)
			{
				val = measure.toMetric(val);
			}
			else if (metric == false && rec.isMetric())
			{
				val = measure.toImperial(val);
			}
		}
		
		return val;
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:DaySummary.Title");
	}
}
