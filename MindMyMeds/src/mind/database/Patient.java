package mind.database;

import java.util.UUID;

import samoyan.database.DataBean;

public class Patient extends DataBean
{
	public final static int SIZE_MRN = 12;
	
	public UUID getLoginID()
	{
		return (UUID) get("LoginID");
	}
	public void setLoginID(UUID loginID)
	{
		set("LoginID", loginID);
	}
	public Long getMRN()
	{
		return (Long) get("MRN");
	}
	public void setMRN(Long mrn)
	{
		set("MRN", mrn);
	}
}
