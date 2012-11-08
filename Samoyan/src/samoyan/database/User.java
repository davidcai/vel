package samoyan.database;

import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import samoyan.core.BCrypt;
import samoyan.core.Util;
import samoyan.servlet.Channel;

public final class User extends DataBean
{
	public static final int MINSIZE_LOGINNAME = 5;
	public static final int MAXSIZE_LOGINNAME = 64;
	public static final int MAXSIZE_PASSWORD = 64; // Must be at least 60 chars to accomodate BCrypted passwords
	public static final int MINSIZE_PASSWORD = 8;
	public static final int MAXSIZE_EMAIL = 256;
	public static final int MINSIZE_NAME = 5;
	public static final int MAXSIZE_NAME = 128;
	public static final int MAXSIZE_PHONE = 24;
	public final static int MAXSIZE_FACEBOOK = 64;
	public final static int MAXSIZE_TWITTER = 15;
			
	public static final String GENDER_MALE = "M";
	public static final String GENDER_FEMALE = "F";
	
	public User()
	{
		init("Joined", new Date());
		init("Password", Util.randomPassword(MINSIZE_PASSWORD));
	}
		
	public Date getLastActive()
	{
		return (Date) get("LastActive");
	}
	public void setLastActive(Date lastActive)
	{
		set("LastActive", lastActive);
	}

	// - - -
	
	public String getLoginName()
	{
		return (String) get("LoginName");
	}
	public void setLoginName(String loginName)
	{
		if (loginName==null)
		{
			set("LoginName", null);
		}
		else
		{
			set("LoginName", loginName.toLowerCase(Locale.US));
		}
	}
	
	public boolean isPassword(String pw)
	{
		String password = (String) get("Password");
		return (pw.equals(password) || BCrypt.checkpw(pw, password));
	}
	public void setPassword(String pw)
	{
		String hashed = BCrypt.hashpw(pw);
		set("Password", hashed);
	}
	
	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String name)
	{
		set("Name", name);
	}
	public String getDisplayName()
	{
		String name = getName();
		if (Util.isEmpty(name))
		{
			name = getLoginName();
		}
		return name;
	}
		
	public String getEmail()
	{
		return (String) get("Email");
	}
	public void setEmail(String email)
	{
		set("Email", email);
	}

	public Date getDateJoined()
	{
		return (Date) get("Joined");
	}
	public void setDateJoined(Date joined)
	{
		set("Joined", joined);
	}

	public boolean isSuspended()
	{
		return (Boolean) get("Suspended", false);
	}
	public void setSuspended(boolean b)
	{
		set("Suspended", b);
	}
	
	public boolean isTerminated()
	{
		return (Boolean) get("Terminated", false);
	}
	public void setTerminated(boolean b)
	{
		set("Terminated", b);
	}

	// - - -

	/**
	 * The user's designated time zone. May be <code>null</code> for new users.
	 * @return
	 */
	public TimeZone getTimeZone()
	{
		return (TimeZone) get("TimeZone");
	}
	public void setTimeZone(TimeZone tz)
	{
		set("TimeZone", tz);
	}	

	/**
	 * The user's designated locale. May be <code>null</code> for new users.
	 * @return
	 */
	public Locale getLocale()
	{
		return (Locale) get("Locale");
	}
	public void setLocale(Locale loc)
	{
		set("Locale", loc);
	}	

	// - - -
	
	public String getPhone()
	{
		return (String) get("Phone");
	}
	public void setPhone(String phone)
	{
		set("Phone", phone);
	}
	public String getPhoneVerificationCode()
	{
		return (String) get("PhoneVerifyCode");
	}
	public void setPhoneVerificationCode(String mobile)
	{
		set("PhoneVerifyCode", mobile);
	}
	
	public String getMobile()
	{
		return (String) get("Mobile");
	}
	public void setMobile(String mobile)
	{
		set("Mobile", mobile);
	}	
	public UUID getMobileCarrierID()
	{
		return (UUID) get("MobileCarrierID");
	}
	public void setMobileCarrierID(UUID mobileCarrierID)
	{
		set("MobileCarrierID", mobileCarrierID);
	}	
	public String getMobileVerificationCode()
	{
		return (String) get("MobileVerifyCode");
	}
	public void setMobileVerificationCode(String mobile)
	{
		set("MobileVerifyCode", mobile);
	}

	public String getTwitter()
	{
		return (String) get("Twitter");
	}
	public void setTwitter(String tw)
	{
		set("Twitter", tw);
	}
	
	public boolean isChannelActive(String channel)
	{
		if (channel.equalsIgnoreCase(Channel.EMAIL))
		{
			return Util.isValidEmailAddress(getEmail());
		}
		else if (channel.equalsIgnoreCase(Channel.SMS))
		{
			return !Util.isEmpty(getMobile());
		}
		else if (channel.equalsIgnoreCase(Channel.VOICE))
		{
			return !Util.isEmpty(getPhone());
		}
		else if (channel.equalsIgnoreCase(Channel.INSTANT_MESSAGE))
		{
			return !Util.isEmpty(getXMPP());
		}
		else if (channel.equalsIgnoreCase(Channel.FACEBOOK_MESSSAGE))
		{
			return !Util.isEmpty(getFacebook());
		}
		else if (channel.equalsIgnoreCase(Channel.TWITTER))
		{
			return !Util.isEmpty(getTwitter());
		}
		else
		{
			return false;
		}
	}
	

	public String getFacebook()
	{
		return (String) get("Facebook");
	}
	public void setFacebook(String fb)
	{
		set("Facebook", fb);
	}

	public String getXMPP()
	{
		return (String) get("XMPP");
	}
	public void setXMPP(String xmppID)
	{
		set("XMPP", xmppID);
	}

	public BitSet getTimeline(String channel)
	{
		return (BitSet) get("Timeline." + channel);
	}
	public void setTimeline(String channel, BitSet bits)
	{
		set("Timeline." + channel, bits);
	}

	public String getGender()
	{
		return (String) get("Gender");
	}
	public void setGender(String gender)
	{
		if (gender!=null && !gender.equals(GENDER_MALE) && !gender.equals(GENDER_FEMALE))
		{
			gender = null;
		}
		set("Gender", gender);
	}
	public boolean isMale()
	{
		String gender = getGender();
		return gender!=null && gender.equals(GENDER_MALE);
	}
	public boolean isFemale()
	{
		String gender = getGender();
		return gender!=null && gender.equals(GENDER_FEMALE);
	}
	
	public Date getBirthday()
	{
		return (Date) get("Birthday");
	}
	public void setBirthday(Date birthday)
	{
		set("Birthday", birthday);
	}	

	public Image getAvatar()
	{
		return (Image) get("Avatar");
	}
	public void setAvatar(Image avatar)
	{
		set("Avatar", avatar);
	}
	
	public String getPasswordResetCode()
	{
		return (String) get("PasswordResetCode");
	}
	public void setPasswordResetCode(String code)
	{
		set("PasswordResetCode", code);
	}
	public Date getPasswordResetDate()
	{
		return (Date) get("PasswordResetDate");
	}
	public void setPasswordResetDate(Date date)
	{
		set("PasswordResetDate", date);
	}
	
	public List<String> getGuidedSetupPages()
	{
		String s = (String) get("GuidedSetupPages");
		if (Util.isEmpty(s))
		{
			return null;
		}
		else
		{
			return Util.tokenize(s, ";");
		}
	}
	/**
	 * Sets the commands of the pages of the guided setup.
	 * @param commands
	 */
	public void setGuidedSetupPages(List<String> commands)
	{
		if (commands==null || commands.size()==0)
		{
			clear("GuidedSetupPages");
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<commands.size(); i++)
		{
			if (i>0)
			{
				builder.append(";");
			}
			builder.append(commands.get(i));
		}
		set("GuidedSetupPages", builder.toString());
	}
	public int getGuidedSetupStep()
	{
		return (Integer) get("GuidedSetupStep", -1);
	}
	public void setGuidedSetupStep(int step)
	{
		set("GuidedSetupStep", step);
	}
	/**
	 * Indicates if the user is currently undergoing the guided setup.
	 * @return
	 */
	public boolean isGuidedSetup()
	{
		List<String> pages = getGuidedSetupPages();
		int step = getGuidedSetupStep();
		return pages!=null && step<pages.size();
	}
}
