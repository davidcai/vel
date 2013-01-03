package baby.pages.info;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.TabControl;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import baby.pages.BabyPage;

public class AppointmentsBasePage extends BabyPage
{
	protected void writeTabs()
	{
		TabControl tc = new TabControl(this)
			.addTab(AppointmentsListPage.COMMAND, getString("information:AppointmentsBase.Tab.List"), getPageURL(AppointmentsListPage.COMMAND))
			.addTab(AppointmentsCalendarPage.COMMAND, getString("information:AppointmentsBase.Tab.Calendar"), getPageURL(AppointmentsCalendarPage.COMMAND))
			.setCurrentTab(getContext().getCommand());

		if (getContext().getUserAgent().isSmartPhone())
		{
			tc.setStyleButton();
			tc.setAlignStretch();
		}
		
		tc.render();
	}
	
	protected void writeAddButton() throws Exception
	{
		if (getContext().getUserAgent().isSmartPhone())
		{
			writeFormOpen("GET", EditAppointmentPage.COMMAND);
			new ButtonInputControl(this, null)
				.setValue(getString("information:AppointmentsBase.AddHotButton"))
				.setMobileHotAction(true)
				.setAttribute("class", "NoShow")
				.render();
			writeFormClose();
		}
		else
		{
			new LinkToolbarControl(this)
				.addLink(getString("information:AppointmentsBase.AddLink"), getPageURL(EditAppointmentPage.COMMAND), "icons/basic1/pencil_16.png")
				.render();
		}
	}
	
	/**
	 * Returns a string representing the name of the day, e.g. Today, Tomorrow, Thursday, and Sunday. 
	 * Returns null if the date is 7-day later than today.
	 *  
	 * @param cal
	 * @return
	 */
	protected String getDescriptiveWeekDay(Calendar cal)
	{
		String weekDay = null;
		
		cal = clearHours(cal);
		Date dt = cal.getTime();
		
		Calendar calToday = Calendar.getInstance(getTimeZone(), getLocale());
		calToday = clearHours(calToday);
		
		if (cal.equals(calToday))
		{
			// Today
			weekDay = getString("information:AppointmentsBase.Today");
		}
		else
		{
			cal.add(Calendar.DATE, -1);
			if (cal.equals(calToday)) 
			{
				// Tomorrow
				weekDay = getString("information:AppointmentsBase.Tomorrow");
			}
			else
			{
				cal.add(Calendar.DATE, -5);
				if (cal.after(calToday) == false)
				{
					// Day within a week
					DateFormat dfDay = DateFormatEx.getSimpleInstance("EEE", getLocale(), getTimeZone());
					weekDay = dfDay.format(dt);
				}
			}
		}
		
		return weekDay;
	}
	
	protected String getDescriptiveDate(Calendar cal)
	{
		StringBuilder sb = new StringBuilder();
		
		String weekDay = getDescriptiveWeekDay(cal);
		if (weekDay != null)
		{
			sb.append(weekDay);
			sb.append(getString("information:AppointmentsBase.Comma"));
		}
		
		DateFormat dfDate = DateFormatEx.getMediumDateInstance(getLocale(), getTimeZone());
		sb.append(dfDate.format(cal.getTime()));
		
		return sb.toString();
	}
	
	protected Calendar clearHours(Calendar cal)
	{
		Calendar c = (Calendar) cal.clone();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		return c;
	}
	
	protected String getAppointmentPageURL(UUID appointmentID)
	{
		return getPageURL(getAppointmentPageCommand(), getAppointmentPageParams(appointmentID));
	}
	
	protected String getAppointmentPageCommand()
	{
		String cmd;
		if (getContext().getUserAgent().isSmartPhone())
		{
			cmd = AppointmentPage.COMMAND;
		}
		else
		{
			cmd = EditAppointmentPage.COMMAND;
		}
		
		return cmd;
	}
	
	protected ParameterMap getAppointmentPageParams(UUID appointmentID)
	{
		ParameterMap params = new ParameterMap();
		if (getContext().getUserAgent().isSmartPhone())
		{
			params.plus(AppointmentPage.PARAM_ID, appointmentID.toString());
		}
		else
		{
			params.plus(EditAppointmentPage.PARAM_ID, appointmentID.toString());
		}
		
		return params;
	}
}
