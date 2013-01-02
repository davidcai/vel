package samoyan.database;

import java.util.Date;
import java.util.UUID;

import samoyan.core.Util;
import samoyan.servlet.Setup;

public final class AuthToken extends DataBean
{
	public final static int MAXSIZE_SIGNATURE = 64; // 64 characters to hold 32 bytes of SHA-256 hash in hex
	public final static int MAXSIZE_APPLE_PUSH_TOKEN = 32;
	
	public AuthToken()
	{
		init("Life", Setup.getSessionLength());
		init("LastAccessed", new Date());
	}
	
	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}
	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}
	
	public long getLife()
	{
		return (Long) get("Life");
	}
	public void setLife(long millis)
	{
		set("Life", millis);
	}

	public Date getLastAccessed()
	{
		return (Date) get("LastAccessed");
	}
	public void setLastAccessed(Date dt)
	{
		set("LastAccessed", dt);
	}

	public boolean isExpired(Date dt)
	{
		return getLastAccessed().getTime() + getLife() < dt.getTime();
	}
	
	public boolean isUserAgentSignature(String sig)
	{
		String storedSig = (String) get("UserAgentSignature");
		
		try
		{
			return storedSig==null || Util.objectsEqual(Util.hashSHA256(sig), storedSig);
		}
		catch (Exception e)
		{
			// Should not happen
			return false;
		}
	}
	/**
	 * Limits this auth token to a particular user agent string.
	 * @param sig
	 */
	public void setUserAgentSignature(String sig)
	{
		if (sig==null)
		{
			set("UserAgentSignature", null);
		}
		else
		{
			try
			{
				set("UserAgentSignature", Util.hashSHA256(sig));
			}
			catch (Exception e)
			{
				// Should not happen
			}
		}
	}
	
	public String getApplePushToken()
	{
		byte[] bytes = (byte[]) get("ApplePushToken");
		return bytes==null? null : Util.byteArrayToHexString(bytes);
	}
	public void setApplePushToken(String token)
	{
		set("ApplePushToken", token==null? null : Util.hexStringToByteArray(token));
	}
}
