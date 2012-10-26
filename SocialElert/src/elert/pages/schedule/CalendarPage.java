package elert.pages.schedule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.SelectInputControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.ProcedureOpeningLinkStore;
import elert.database.ResourceProcedureLinkStore;
import elert.pages.ElertPage;

public final class CalendarPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/calendar";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:Calendar.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		// Get base date
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		Integer yyyy = getParameterInteger("y");
		if (yyyy==null) yyyy = cal.get(Calendar.YEAR);
		Integer mm = getParameterInteger("m");
		if (mm==null) mm = cal.get(Calendar.MONTH) + 1; // 0-based to 1-based
		Integer dd = getParameterInteger("d");
		if (dd==null) dd = cal.get(Calendar.DAY_OF_MONTH);

		// Query
		cal.set(yyyy, mm-1, 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date from = cal.getTime();
		int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();
		
		List<UUID> openingIDs = OpeningStore.getInstance().queryOpenings(from, to, FacilityStore.getInstance().queryByUser(getContext().getUserID()));
		
		// Bucketize
		List<Opening> todaysOpenings = new ArrayList<Opening>();
		int[] count = new int[cal.getMaximum(Calendar.DAY_OF_MONTH) + 1];
		
		for (UUID openingID : openingIDs)
		{
			Opening opening = OpeningStore.getInstance().load(openingID);
			cal.setTime(opening.getDateTime());
			int day = cal.get(Calendar.DAY_OF_MONTH);
			count[day] ++;
			if (day==dd)
			{
				todaysOpenings.add(opening);
			}
		}
		
		// Help
		writeEncode(getString("schedule:Calendar.Help"));
		write("<br><br>");
		
		// Render calendar
		writeMonth(yyyy, mm, dd, count);
		
		// Render today's openings
		if (todaysOpenings.size()>0)
		{
			write("<br><br>");
			writeOpenings(todaysOpenings);
			
			writeLegend();
		}
	}

	private void writeOpenings(List<Opening> group) throws Exception
	{
		final boolean phone = getContext().getUserAgent().isSmartPhone(); 
		final DateFormat miniDateTime = DateFormatEx.getMiniDateTimeInstance(getLocale(), getTimeZone());
		final Date now = new Date();
		
		new DataTableControl<Opening>(this, "openings", group.iterator())
		{			
			@Override
			protected void defineColumns() throws Exception
			{
				column(getString("schedule:Calendar.Opening"));
				column(getString("schedule:Calendar.Duration"));
				column(getString("schedule:Calendar.Rank"));
				
				if (!phone)
				{
					column(getString("elert:Legend.Accepted")).align("center").alignHeader("center").image("elert/circle-v.png").width(1);
					column(getString("elert:Legend.Declined")).align("center").alignHeader("center").image("elert/circle-x.png").width(1);
					column(getString("elert:Legend.DidNotReply")).align("center").alignHeader("center").image("elert/circle-q.png").width(1);
				}
				else
				{
					column(getString("schedule:Calendar.Elert"));
				}
				
				column(getString("schedule:Calendar.Status"));
			}

			@Override
			protected void renderRow(Opening opening) throws Exception
			{
				Facility facility = FacilityStore.getInstance().load(opening.getFacilityID());
				List<UUID> procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(opening.getID());
								
				// Name and link
				cell();
				writeLink(	facility.getCode() + " " + miniDateTime.format(opening.getDateTime()),
							getPageURL(OpeningPage.COMMAND, new ParameterMap(OpeningPage.PARAM_ID, opening.getID().toString())));
								
				// Duration
				cell();
				writeEncodeLong(opening.getDuration());
				if (opening.getOriginalDuration()!=opening.getDuration())
				{
					write("<span class=Faded>/");
					writeEncodeLong(opening.getOriginalDuration());
					write("</span>");
				}
				write(" ");
				writeEncode(getString("schedule:Calendar.Minutes"));

				// rank
				cell();
				if (procIDs.size()>0)
				{
					int resourceRank = 0;
					for (UUID procID : procIDs)
					{
						resourceRank += ResourceProcedureLinkStore.getInstance().getTotalRankForProcedure(procID);
					}
					writeEncodeLong(resourceRank);
				}
				else
				{
					write("<span class=Faded>");
					writeEncode(getString("schedule:Calendar.NotApplicable"));
					write("</span>");
				}
				
				// eLert statuses
				if (phone)
				{
					cell();
				}
				List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
				int countAccepted = 0;
				int countDeclined = 0;
				for (int e=0; e<elertIDs.size(); e++)
				{
					Elert eLert = ElertStore.getInstance().load(elertIDs.get(e));
					if (eLert.getReply()==Elert.REPLY_ACCEPTED)
					{
						countAccepted++;
					}
					else if (eLert.getReply()==Elert.REPLY_DECLINED)
					{
						countDeclined++;
					}
				}
				if (!phone)
				{
					cell();
				}
				else
				{
					write("&nbsp;");
				}
				if (elertIDs.size()>0)
				{
					write("<div class=\"ElertCount Accepted\">");
					writeEncodeLong(countAccepted);
					write("</div>");
				}
				
				if (!phone)
				{
					cell();
				}
				else
				{
					write("&nbsp;");
				}
				if (elertIDs.size()>0)
				{
					write("<div class=\"ElertCount Declined\">");
					writeEncodeLong(countDeclined);
					write("</div>");
				}

				if (!phone)
				{
					cell();
				}
				else
				{
					write("&nbsp;");
				}
				if (elertIDs.size()>0)
				{
					write("<div class=\"ElertCount DidNotReply\">");
					writeEncodeLong(elertIDs.size() - countAccepted - countDeclined);
					write("</div>");
				}
				
				// Status
				cell();
				if (opening.isClosed() || opening.getDateTime().before(now))
				{
					writeEncode(getString("schedule:Calendar.StatusClosed"));
				}
				else
				{
					writeEncode(getString("schedule:Calendar.StatusOpen"));
				}
			}
		}.render();			
	}

	private void writeMonth(int yyyy, int mm, int dd, int[] count)
	{
		mm--; // 1-based to 0-based
		
		DateFormat df;
		
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.clear();
		cal.set(yyyy, mm, 1, 12, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		// The frame
		write("<table class=BigCalendar>");

		// Month header
		ParameterMap params = new ParameterMap();
		write("<tr valign=middle>");
		write("<td class=Month>");
			cal.set(yyyy, mm, 1, 12, 0, 0);
			cal.add(Calendar.MONTH, -1);
			df = new SimpleDateFormat("MMM");
			df.setTimeZone(cal.getTimeZone());
			params.plus("y", String.valueOf(cal.get(Calendar.YEAR)))
				.plus("m", String.valueOf(cal.get(Calendar.MONTH)+1))
				.plus("d", String.valueOf(Math.min(dd, cal.getActualMaximum(Calendar.DAY_OF_MONTH))));
			write("<a href=\"");
			writeEncode(getPageURL(getContext().getCommand(), params));
			write("\">");
			writeEncode("< ");
			writeEncode(df.format(cal.getTime()));
			write("</a>");
		write("</td><td class=Month colspan=5>");
//			df = new SimpleDateFormat("MMMM yyyy");
//			df.setTimeZone(cal.getTimeZone());
//			writeEncode(df.format(cal.getTime()));

			// Nav form
			df = new SimpleDateFormat("MMMM");
			df.setTimeZone(cal.getTimeZone());
			writeFormOpen("GET", getContext().getCommand());
				
				SelectInputControl combo = new SelectInputControl(this, "m");
				combo.setInitialValue(mm+1);
				for (int i=1; i<=12; i++)
				{
					cal.set(yyyy, i-1, 1);
					combo.addOption(df.format(cal.getTime()), i);
				}
				combo.setAutoSubmit(true);
				combo.render();
				write(" ");
				combo = new SelectInputControl(this, "y");
				combo.setInitialValue(yyyy);
				for (int i=cal.get(Calendar.YEAR)+1; i>cal.get(Calendar.YEAR)+1-10; i--)
				{
					combo.addOption(String.valueOf(i), i);
				}
				combo.setAutoSubmit(true);
				combo.render();
//				write(" ");
//				writeButton(getString("controls:Button.Go"));
				writeHiddenInput("d", dd);

			writeFormClose();
	
			
		write("</td><td class=Month>");
			cal.set(yyyy, mm, 1, 12, 0, 0);
			cal.add(Calendar.MONTH, 1);
			df = new SimpleDateFormat("MMM");
			df.setTimeZone(cal.getTimeZone());
			params.plus("y", String.valueOf(cal.get(Calendar.YEAR)))
				.plus("m", String.valueOf(cal.get(Calendar.MONTH)+1))
				.plus("d", String.valueOf(Math.min(dd, cal.getActualMaximum(Calendar.DAY_OF_MONTH))));
			write("<a href=\"");
			writeEncode(getPageURL(getContext().getCommand(), params));
			write("\">");
			writeEncode(df.format(cal.getTime()));
			writeEncode(" >");
			write("</a>");
		write("</td></tr>");
		
		// Days of week
		cal.set(yyyy, mm, 1, 12, 0, 0);
		int firstDow = cal.getFirstDayOfWeek(); // Typically Sunday or Monday
		cal.set(Calendar.DAY_OF_WEEK, firstDow);
		
		df = new SimpleDateFormat("EEE");
		write("<tr>");
		for (int i=0; i<7; i++)
		{
			write("<td class=DOW>");
			writeEncode(df.format(cal.getTime()));
			write("</td>");
			cal.add(Calendar.DATE, 1);
		}
		write("</tr>");
		
		// Days
		cal.set(Calendar.DAY_OF_MONTH, 1);
		if (firstDow!=cal.get(Calendar.DAY_OF_WEEK))
		{
			cal.set(Calendar.DAY_OF_WEEK, firstDow);
		}
		else
		{
			cal.add(Calendar.DATE, -7);
		}
		
		// Always print 6 weeks
		for (int week=0; week<6; week++)
		{
			write("<tr>");
			
			for (int day=0; day<7; day++)
			{
				int m = cal.get(Calendar.MONTH);
				int d = cal.get(Calendar.DAY_OF_MONTH);
				write("<td class=\"Day");
				if (m!=mm)
				{
					write(" Disabled");
				}
				else if (d==dd)
				{
					write(" Selected");
				}
				write("\">");
				if (m==mm && d!=dd)
				{
					params.plus("y", String.valueOf(yyyy));
					params.plus("m", String.valueOf(mm+1));
					params.plus("d", String.valueOf(d));
					write("<a href=\"");
					writeEncode(getPageURL(getContext().getCommand(), params));
					write("\">");
				}
				else
				{
					write("<div>");
				}
				write("<div class=Number>");
				write(d);
				write("</div>");

				int x = count[d];
				if (m==mm && x>0)
				{
					writeEncodeLong(x);
				}
				else
				{
					write("&nbsp;");
				}

				if (m==mm && d!=dd)
				{
					write("</a>");
				}
				else
				{
					write("</div>");
				}
				write("</td>");
				
				cal.add(Calendar.DATE, 1);
			}
			
			write("</tr>");
		}
		
		// Close the frame
		write("</table>");
	}
}
