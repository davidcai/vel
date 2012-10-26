package elert.database;

import java.util.UUID;

import samoyan.database.DataBean;

/**
 * Keeps application-specific properties on the User object.
 * @author brian
 *
 */
public class UserEx extends DataBean
{
	public final static int MAXSIZE_MRN = 12;
	public final static int SIZE_NUID = 7;
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	public String getMRN()
	{
		return (String) get("MRN");
	}
	public void setMRN(String mrn)
	{
		set("MRN", mrn);
	}

	/**
	 * National User ID. One letter and 6 digits, e.g. "K123456"
	 * @return
	 */
	public String getNUID()
	{
		return (String) get("NUID");
	}
	public void setNUID(String nuid)
	{
		set("NUID", nuid);
	}	
}
