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
		DeliveryDue, AppointmentDue, ChecklistDue, MeasureRecord, Photo, Text
	}

	public static final String KEY_DELIM = "-";
	public static final int MAX_BADGES_NORMAL = 6;
	public static final int MAX_BADGES_MOBILE = 2;

	private WebPage out;
	private Map<String, Set<Badge>> dayToBadges = new LinkedHashMap<String, Set<Badge>>();

	public BadgedCalendarControl(WebPage outputPage)
	{
		super(outputPage);
		this.out = outputPage;
	}

	@Override
	protected boolean isCellEnabled(int yyyy, int mm, int dd)
	{
		Set<Badge> badges = getBadges(yyyy, mm + 1, dd); // 0-based to 1-based month
		return (badges != null && badges.isEmpty() == false);
	}
	
	@Override
	protected void renderCell(int yyyy, int mm, int dd)
	{
		super.renderCell(yyyy, mm, dd);

		Set<Badge> badges = getBadges(yyyy, mm + 1, dd); // 0-based to 1-based month
		if (badges != null && badges.isEmpty() == false)
		{
			final int maxBadges = getContext().getUserAgent().isMobile() ? MAX_BADGES_MOBILE : MAX_BADGES_NORMAL;
			
			out.write("<div class=\"CalendarBadges\">");

			int i = 1;
			for (Badge badge : badges)
			{
				if (i > maxBadges)
				{
					break;
				}
				
				if (i > 1)
				{
					out.write(" ");
				}
				
				out.write("<span class=\"CalendarBadge ");
				out.write(badge);
				out.write("\">");
				out.write("</span>");
				i++;
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
