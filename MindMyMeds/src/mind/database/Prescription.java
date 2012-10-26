package mind.database;

import java.util.*;

import samoyan.database.DataBean;

public class Prescription extends DataBean
{
	public final static int MAXSIZE_NICKNAME = 32;
	public final static int MAXSIZE_PURPOSE = 64;
	public final static int MAXSIZE_INSTRUCTIONS = 32;
	public final static int MAXSIZE_REGIMEN_SUMMARY = 128;
	public final static int MAXSIZE_DOCTOR_NAME = 128;
	public final static int MAXSIZE_DOSE_INFO = 32;
	
	public UUID getDrugID()
	{
		return (UUID) get("DrugID");
	}
	public void setDrugID(UUID drugID)
	{
		set("DrugID", drugID);
	}

	public UUID getPatientID()
	{
		return (UUID) get("PatientID");
	}
	public void setPatientID(UUID patientID)
	{
		set("PatientID", patientID);
	}

	public UUID getDoctorID()
	{
		return (UUID) get("DoctorID");
	}
	public void setDoctorID(UUID doctorID)
	{
		set("DoctorID", doctorID);
	}

	public String getNickname()
	{
		return (String) get("Nickname");
	}
	public void setNickname(String nickname)
	{
		set("Nickname", nickname);
	}

	public String getPurpose()
	{
		return (String) get("Purpose");
	}
	public void setPurpose(String purpose)
	{
		set("Purpose", purpose);
	}

	public String getInstructions()
	{
		return (String) get("Instructions");
	}
	public void setInstructions(String instructions)
	{
		set("Instructions", instructions);
	}

	public String getDoseInfo()
	{
		return (String) get("DoseInfo");
	}
	public void setDoseInfo(String doseInfo)
	{
		set("DoseInfo", doseInfo);
	}

	public Integer getDosesRemaining()
	{
		return (Integer) get("DosesRemaining");
	}
	public void setDosesRemaining(Integer doses)
	{
		set("DosesRemaining", doses);
	}
	
	public Date getNextDoseDate()
	{
		return (Date) get("NextDoseDate");
	}
	public void setNextDoseDate(Date nextDate)
	{
		set("NextDoseDate", nextDate);
	}
	
	public void progressNextDoseDate(TimeZone tz)
	{
		Date last = getNextDoseDate();
		if (last==null)
		{
			last = new Date();
		}
		
		Calendar cal = Calendar.getInstance(tz);
		cal.setTime(last);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		QuarterHourBitSet bitmap = getQuarterHourBitmap();
		
		for (int d=0; d<2; d++)
		{
			for (int h=0; h<24; h++)
			{
				for (int m=0; m<60; m+=15)
				{
					if (bitmap.get(h, m))
					{
						cal.set(Calendar.HOUR_OF_DAY, h);
						cal.set(Calendar.MINUTE, m);
						if (cal.getTime().after(last))
						{
							setNextDoseDate(cal.getTime());
							return;
						}
					}
				}
			}
			
			cal.add(Calendar.DATE, getFreqDays());
		}
	}
	
	public String getDoctorName()
	{
		return (String) get("DoctorName");
	}
	public void setDoctorName(String doctorName)
	{
		set("DoctorName", doctorName);
	}
	
	public int getFreqDays()
	{
		Integer result = (Integer) get("FreqDays");
		return (result==null? 1 : result);
	}
	public void setFreqDays(int days)
	{
		set("FreqDays", days);
	}
	
	public QuarterHourBitSet getQuarterHourBitmap()
	{
		return new QuarterHourBitSet((byte[]) get("QuarterHourBitmap"));
	}
	
	public void setQuarterHourBitmap(QuarterHourBitSet bitmap)
	{
		set("QuarterHourBitmap", bitmap.getBitmap());
	}
}
