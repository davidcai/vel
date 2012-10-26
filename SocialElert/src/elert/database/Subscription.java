package elert.database;

import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;

public final class Subscription extends DataBean
{
	public static final int MAXSIZE_NICKNAME = 64;
	public static final int MAX_ADVANCE_NOTICE = Math.max(30, Procedure.MAX_LEAD); // days
	public static final int MAXSIZE_REASON = 256;
	public static final int MAX_DURATION = 1440; // 24 hours, in minutes
	public static final int MAX_AVAILABILITY_MONTHS = 6;

	public Subscription()
	{
		init("Urgent", false);
		init("CreatedDate", new Date());
		init("Duration", 0);
	}
	
	/**
	 * The user ID of the patient.
	 * @return
	 */
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	/**
	 * The ID of the service area where the patient is scheduled to undergo the procedure.
	 * Cannot be <code>null</code>.
	 * @return
	 */
	public UUID getServiceAreaID()
	{
		return (UUID) get("AreaID");
	}
	public void setServiceAreaID(UUID areaID)
	{
		set("AreaID", areaID);
	}
		
	public int getAdvanceNotice()
	{
		return (Integer) get("AdvanceNotice", 0);
	}
	public void setAdvanceNotice(int days)
	{
		set("AdvanceNotice", days);
	}

	public int getDuration()
	{
		return (Integer) get("Duration", 0);
	}
	public void setDuration(int minutes)
	{
		set("Duration", minutes);
	}

	public Date getOriginalDate()
	{
		return (Date) get("OriginalDate");
	}
	public void setOriginalDate(Date dt)
	{
		set("OriginalDate", dt);
	}

	public boolean isAcceptOtherPhysician()
	{
		return (Boolean) get("AcceptOtherPhysician", false);
	}
	public void setAcceptOtherPhysician(boolean b)
	{
		set("AcceptOtherPhysician", b);
	}
	
	public String getReason()
	{
		return (String) get("Reason");
	}
	public void setReason(String reason)
	{
		set("Reason", reason);
	}
	
	/**
	 * The user ID of the scheduler who verified this subscription.
	 * @return
	 */
	public UUID getVerifiedByUserID()
	{
		return (UUID) get("VerifiedBy");
	}
	public void setVerifiedBy(UUID userID)
	{
		set("VerifiedBy", userID);
	}
	
	/**
	 * The date that this subscription was verified.
	 * @return
	 */
	public Date getVerifiedDate()
	{
		return (Date) get("VerifiedDate");
	}
	public void setVerifiedDate(Date dt)
	{
		set("VerifiedDate", dt);
	}
	
	/**
	 * Indicates if the patient has no scheduling restrictions and is always available for the procedure.
	 * @return
	 */
	public boolean isAlwaysAvailable()
	{
		return (Boolean) get("AlwaysAvailable", false);
	}
	public void setAlwaysAvailable(boolean always)
	{
		set("AlwaysAvailable", always);
	}

	public BitSet getAvailable(int yyyy, int mm)
	{
		String name = String.valueOf(yyyy);
		if (mm<10)
		{
			name += "0";
		}
		name += String.valueOf(mm);
		
		return (BitSet) get("Available" + name);
	}
	public void setAvailable(int yyyy, int mm, BitSet bs)
	{
		String name = String.valueOf(yyyy);
		if (mm<10)
		{
			name += "0";
		}
		name += String.valueOf(mm);
		
		set("Available" + name, bs);
	}

	/**
	 * The date that this subscription was originally created by the patient.
	 * @return
	 */
	public Date getCreatedDate()
	{
		return (Date) get("CreatedDate");
	}
	public void setCreatedDate(Date dt)
	{
		set("CreatedDate", dt);
	}
	
	public boolean isUrgent()
	{
		return (Boolean) get("Urgent", false);
	}
	public void setUrgent(boolean urgent)
	{
		set("Urgent", urgent);
	}

	/**
	 * Indicates if the patient was chosen as the finalist of an opening, i.e. if they got the appointment.
	 * @return
	 */
	public boolean isFinalized()
	{
		return (Boolean) get("Finalized", false);
	}
	public void setFinalized(boolean finalized)
	{
		set("Finalized", finalized);
	}

	/**
	 * Indicates if the patient has removed (unsubscribed from) this eLert.
	 * @return
	 */
	public boolean isRemoved()
	{
		return (Boolean) get("Removed", false);
	}
	public void setRemoved(boolean removed)
	{
		set("Removed", removed);
	}
	
	public boolean isExpired()
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(getCreatedDate());
		cal.add(Calendar.MONTH, MAX_AVAILABILITY_MONTHS);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return System.currentTimeMillis() >= cal.getTimeInMillis();
	}
}
