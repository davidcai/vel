package mind.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;

public class Dose extends DataBean
{
	public final static int MAXSIZE_SKIP_REASON = 128;
	public final static int SHORT_CODE_MODULO = 10000;
	
	public final static short UNRESOLVED = 0;
	public final static short TAKEN = 1;
	public final static short SKIPPED = -1;
	public final static short UNKNOWN = -100;
	
	public UUID getPrescriptionID()
	{
		return (UUID) get("PrescriptionID");
	}
	public void setPrescriptionID(UUID rxID)
	{
		set("PrescriptionID", rxID);
	}
	
	public UUID getPatientID()
	{
		return (UUID) get("PatientID");
	}
	public void setPatientID(UUID patientID)
	{
		set("PatientID", patientID);
	}
	
	public Date getTakeDate()
	{
		return (Date) get("TakeDate");
	}
	public void setTakeDate(Date takeDate)
	{
		set("TakeDate", takeDate);
	}
	
	public Date getResolutionDate()
	{
		return (Date) get("ResolutionDate");
	}
	public void setResolutionDate(Date resolutionDate)
	{
		set("ResolutionDate", resolutionDate);
	}
	
	public short getResolution()
	{
		return (Short) get("Resolution", UNRESOLVED);
	}
	public void setResolution(short resolution)
	{
		set("Resolution", resolution);
	}
	
	public String getSkipReason()
	{
		return (String) get("SkipReason");
	}
	public void setSkipReason(String skipReason)
	{
		set("SkipReason", skipReason);
	}
}
