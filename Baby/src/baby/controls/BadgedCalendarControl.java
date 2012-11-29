package baby.controls;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import samoyan.controls.BigCalendarControl;
import samoyan.servlet.WebPage;

public class BadgedCalendarControl extends BigCalendarControl
{
	public enum BadgeType
	{
		JournalEntry, Checklist, Appointment
	}
	
	public static class Badge
	{
		private BadgeType type;
		private Object value;
		
		public Badge() 
		{
			// Do nothing
		}
		
		public Badge(BadgeType type, Object value)
		{
			setType(type);
			setValue(value);
		}

		public BadgeType getType()
		{
			return type;
		}

		public void setType(BadgeType type)
		{
			this.type = type;
		}

		public Object getValue()
		{
			return value;
		}

		public void setValue(Object value)
		{
			this.value = value;
		}
	}

	public static final String KEY_DELIM = "-";

	private WebPage out;
	private Map<String, List<Badge>> dayToBadges = new LinkedHashMap<String, List<Badge>>();

	public BadgedCalendarControl(WebPage outputPage)
	{
		super(outputPage);
		this.out = outputPage;
	}

	@Override
	protected void renderCell(int yyyy, int mm, int dd)
	{
		super.renderCell(yyyy, mm, dd);

		List<Badge> badges = getBadges(yyyy, mm + 1, dd); // 0-based to 1-based month
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
				
				out.write("<span class=\"CalendarBadge");
				if (badge.getType() != null)
				{
					out.write(" " + badge.getType());
				}
				out.write("\">");
				out.writeEncode(badge.getValue());
				out.write("</span>");
				
				first = false;
			}

			out.write("</div>");
		}
	}

	/**
	 * Returns a list of badges for the date. If no badge exists, returns an empty list. 
	 * All date arguments are 1-based. 
	 * 
	 * @param yyyy
	 * @param mm
	 * @param dd
	 * @return
	 */
	public List<Badge> getBadges(int yyyy, int mm, int dd)
	{
		String key = yyyy + KEY_DELIM + mm + KEY_DELIM + dd;
		List<Badge> badges = dayToBadges.get(key);
		if (badges == null)
		{
			badges = new ArrayList<Badge>();
			dayToBadges.put(key, badges);
		}

		return badges;
	}
}
