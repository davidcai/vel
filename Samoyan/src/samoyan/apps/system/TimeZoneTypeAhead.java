package samoyan.apps.system;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import samoyan.core.DateFormatEx;
import samoyan.core.TimeZoneEx;
import samoyan.core.Util;

public class TimeZoneTypeAhead extends TypeAhead
{
	public final static String COMMAND = "timezone.typeahead";
	
	@Override
	protected void doQuery(String q) throws SQLException, Exception
	{
		q = q.toLowerCase(getLocale());
		
		DateFormat tf = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());
		Date now = new Date();
		
		// Get all time zones
		String[] tzIDs = TimeZoneEx.getPrimaryIDs();
		for (String tzID : tzIDs)
		{
			TimeZone tz = TimeZone.getTimeZone(tzID);
			
			// Perform search
			String lcTzID = tzID.toLowerCase(getLocale()).replace('_', ' ');
			int p = lcTzID.lastIndexOf("/");
			if (p>=0)
			{
				lcTzID = lcTzID.substring(p+1);
			}
			String lcDesc = tz.getDisplayName(getLocale()).toLowerCase(getLocale());
			
			if (lcDesc.indexOf(q)>=0 || lcTzID.indexOf(q)>=0)
			{
				String desc = TimeZoneEx.getDisplayString(tz, getLocale());
				tf.setTimeZone(tz);
				addOption(tz.getID(), desc, Util.htmlEncode(desc) + "<br><small class=Faded>" + Util.htmlEncode(getString("system:TZTypeAhead.CurrentTime", tf.format(now))) + "</small></br>");
			}
		}
	}
}
