package baby.controls;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import samoyan.controls.BigCalendarControl;
import samoyan.servlet.WebPage;

public class BadgedCalendarControl extends BigCalendarControl
{
	public enum Badge
	{
		Photo, Text, MeasureRecord, ChecklistDue, AppointmentDue
	}

	public static final String KEY_DELIM = "-";

	private WebPage out;
	private Map<String, Set<Badge>> dayToBadges = new LinkedHashMap<String, Set<Badge>>();

	public BadgedCalendarControl(WebPage outputPage)
	{
		super(outputPage);
		this.out = outputPage;
	}

	@Override
	protected void renderCell(int yyyy, int mm, int dd)
	{
		super.renderCell(yyyy, mm, dd);

		Set<Badge> badges = getBadges(yyyy, mm + 1, dd); // 0-based to 1-based month
		if (badges != null && badges.isEmpty() == false)
		{
			out.write("<div class=\"CalendarBadges\">");

			boolean first = true;
			for (Badge badge : badges)
			{
				if (first == false)
				{
					out.write(" ");
				}
				
				out.write("<span class=\"CalendarBadge ");
				out.write(badge);
				out.write("\">");
				out.write("</span>");
				
				first = false;
			}

			out.write("</div>");
		}
	}

	/**
	 * Returns a set of badges for the date. If no badge exists, returns an empty list. 
	 * All date arguments are 1-based. 
	 * 
	 * @param yyyy
	 * @param mm
	 * @param dd
	 * @return
	 */
	public Set<Badge> getBadges(int yyyy, int mm, int dd)
	{
		String key = yyyy + KEY_DELIM + mm + KEY_DELIM + dd;
		Set<Badge> badges = dayToBadges.get(key);
		if (badges == null)
		{
			badges = EnumSet.noneOf(Badge.class);
			dayToBadges.put(key, badges);
		}

		return badges;
	}
}
