package elert.pages.schedule;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.BigCalendarControl;
import samoyan.controls.DataTableControl;
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
//		int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		cal.add(Calendar.MONTH, 1);
		Date to = cal.getTime();
		
		List<UUID> openingIDs = OpeningStore.getInstance().queryOpenings(from, to, FacilityStore.getInstance().queryByUser(getContext().getUserID()));
		
		// Bucketize
		List<Opening> todaysOpenings = new ArrayList<Opening>();
		final int[] count = new int[cal.getMaximum(Calendar.DAY_OF_MONTH) + 1];
		
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
		final int currentMonth = mm;
		new BigCalendarControl(this)
		{
			protected void renderCell(int yyyy, int mm, int dd)
			{
				if (mm==currentMonth)
				{
					writeEncodeLong(count[dd]);
				}
			}
		}
		.setDay(yyyy, mm, dd)
		.render();
		
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
}
