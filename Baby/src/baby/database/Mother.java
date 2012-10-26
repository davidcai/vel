package baby.database;

import java.util.Calendar;
import java.util.Date;
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
	
	public Stage getPregnancyStage()
	{
		Date due = getDueDate();
		if (due!=null)
		{
			int week = Stage.MAX_WEEKS+1;
			Calendar now = Calendar.getInstance(TimeZoneEx.GMT);
			while (now.getTime().before(due) && week>1)
			{
				week--;
				now.add(Calendar.DATE, 7);
			}
			if (week>Stage.MAX_WEEKS) week = Stage.MAX_WEEKS;
			return Stage.pregnancy(week);
		}
		
		Date delivery = getBirthDate();
		if (delivery!=null)
		{
			int month = 0;
			Calendar now = Calendar.getInstance(TimeZoneEx.GMT);
			while (now.getTime().after(delivery) && month<Stage.MAX_MONTHS)
			{
				month++;
				now.add(Calendar.MONTH, -1);
			}
			if (month==0) month = 0;
			return Stage.infancy(month);
		}
		
		return Stage.preconception();
	}
}
