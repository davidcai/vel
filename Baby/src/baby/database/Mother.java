package baby.database;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import samoyan.core.TimeZoneEx;
import samoyan.database.DataBean;

public class Mother extends DataBean
{
	/**
	 * The date the mother is expected to deliver her baby.
	 * @return
	 */
	public Date getDueDate()
	{
		return (Date) get("DueDate");
	}
	public void setDueDate(Date date)
	{
		set("DueDate", date);
	}
	public Date getDueDate(TimeZone tz)
	{
		Date orig = getDueDate();
		if (orig != null)
		{
			return new Date(orig.getTime() - tz.getOffset(orig.getTime()));
		}
		
		return null;
	}
	
	/**
	 * The date that the mother gave birth (not to be confused with the mother's birthday).
	 * @return
	 */
	public Date getBirthDate()
	{
		return (Date) get("BirthDate");
	}
	public void setBirthDate(Date date)
	{
		set("BirthDate", date);
	}
	public Date getBirthDate(TimeZone tz)
	{
		Date orig = getBirthDate();
		if (orig != null)
		{
			return new Date(orig.getTime() - tz.getOffset(orig.getTime()));
		}
		
		return null;
	}
	
	public boolean isMetric()
	{
		return (Boolean) get("Metric", false);
	}
	public void setMetric(boolean b)
	{
		set("Metric", b);
	}
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}
	
	public String getRegion()
	{
		return (String) get("Region");
	}
	public void setRegion(String region)
	{
		set("Region", region);
	}

	public String getMedicalCenter()
	{
		return (String) get("MedicalCenter");
	}
	public void setMedicalCenter(String city)
	{
		set("MedicalCenter", city);
	}
	
	public boolean isChecklistViewAll()
	{
		return (Boolean) get("ChecklistViewAll", false);
	}
	public void setChecklistViewAll(boolean b)
	{
		set("ChecklistViewAll", b);
	}

	public Stage getPregnancyStage()
	{
		Date due = getDueDate();
		if (due != null)
		{
			int week = Stage.MAX_WEEKS + 1;
			Calendar cal = Calendar.getInstance(TimeZoneEx.GMT);
			while (cal.getTime().before(due) && week > 1)
			{
				week--;
				cal.add(Calendar.DATE, 7);
			}
			if (week > Stage.MAX_WEEKS)
			{
				week = Stage.MAX_WEEKS;
			}
			
			return Stage.pregnancy(week);
		}

		Date delivery = getBirthDate();
		if (delivery != null)
		{
			int month = 0;
			Calendar cal = Calendar.getInstance(TimeZoneEx.GMT);
			while (cal.getTime().after(delivery) && month < Stage.MAX_MONTHS)
			{
				month++;
				cal.add(Calendar.MONTH, -1);
			}
			if (month == 0)
			{
				month = 1;
			}
			
			return Stage.infancy(month);
		}

		return Stage.preconception();
	}

	/**
	 * @param target The date in which to calculate the stage of the mother.
	 * @param tz Target time zone.
	 * @return
	 */
	public Stage getEstimatedPregnancyStage(Date target, TimeZone tz)
	{
		Date birthday = getDueDate(tz) == null ? getBirthDate(tz) : getDueDate(tz);
		if (birthday == null)
		{
			return Stage.preconception();
		}
		
		Calendar cal = Calendar.getInstance(tz);
		cal.setTime(birthday);
		cal.add(Calendar.DATE, - Stage.MAX_WEEKS * 7);
		Date conception = cal.getTime();
		
		if (target.before(conception))
		{
			return Stage.preconception();
		}
		
		if (target.after(birthday))
		{
			int month = 0;
			cal.setTime(target);
			while (cal.getTime().after(birthday) && month < Stage.MAX_MONTHS)
			{
				month++;
				cal.add(Calendar.MONTH, -1);
			}
			if (month == 0)
			{
				month = 1;
			}
			
			return Stage.infancy(month);
		}

		int week = Stage.MAX_WEEKS + 1;
		cal.setTime(target);
		while (cal.getTime().before(birthday) && week > 1)
		{
			week--;
			cal.add(Calendar.DATE, 7);
		}
		if (week > Stage.MAX_WEEKS)
		{
			week = Stage.MAX_WEEKS;
		}
		
		return Stage.pregnancy(week);
	}
	
	public Date calcDateOfStage(int timelineTo, TimeZone tz)
	{
		Stage presentStage = getPregnancyStage();
		Stage dueStage = Stage.fromInteger(timelineTo);
		
		if (presentStage.isPreconception())
		{
			// We can't estimate dates
			return null;
		}
		else if (presentStage.isPregnancy())
		{
			Calendar cal = Calendar.getInstance(tz);
			cal.setTime(getDueDate(tz));

			if (dueStage.isPreconception())
			{
				cal.add(Calendar.DATE, -7 * 40);
			}
			else if (dueStage.isPregnancy())
			{
				cal.add(Calendar.DATE, -7 * 40);
				cal.add(Calendar.DATE, 7 * dueStage.getPregnancyWeek());
			}
			else if (dueStage.isInfancy())
			{
				cal.add(Calendar.MONTH, dueStage.getInfancyMonth());
			}
			return cal.getTime();
		}
		else if (presentStage.isInfancy())
		{
			Calendar cal = Calendar.getInstance(tz);
			cal.setTime(getBirthDate(tz));
			
			if (dueStage.isPreconception())
			{
				cal.add(Calendar.DATE, -7 * 40);
			}
			else if (dueStage.isPregnancy())
			{
				cal.add(Calendar.DATE, -7 * 40);
				cal.add(Calendar.DATE, 7 * dueStage.getPregnancyWeek());
			}
			else if (dueStage.isInfancy())
			{
				cal.add(Calendar.MONTH, dueStage.getInfancyMonth());
			}
			return cal.getTime();
		}		
		else
		{
			return null;
		}
	}
}