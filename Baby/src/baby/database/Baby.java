package baby.database;

import java.util.UUID;

import samoyan.database.DataBean;

public class Baby extends DataBean
{
	public static final int MAXSIZE_NAME = 64;

	public static enum Gender
	{
		UNDETERMINED, MALE, FEMALE;

		@Override
		public String toString()
		{
			return name().toLowerCase();
		}

		public static Gender fromString(String str)
		{
			Gender ret = UNDETERMINED;

			for (Gender gender : Gender.values())
			{
				if (gender.toString().equalsIgnoreCase(str))
				{
					ret = gender;
					break;
				}
			}

			return ret;
		}
	}

	public UUID getUserID()
	{
		return (UUID) get("UserID");
	}

	public void setUserID(UUID userID)
	{
		set("UserID", userID);
	}

	public String getName()
	{
		return (String) get("Name");
	}

	public void setName(String name)
	{
		set("Name", name);
	}

	public Gender getGender()
	{
		Gender g = Gender.UNDETERMINED;

		Object oMale = get("Male");
		if (oMale != null)
		{
			Boolean male = (Boolean) oMale;
			g = (male) ? Gender.MALE : Gender.FEMALE;
		}

		return g;
	}

	public void setGender(Gender gender)
	{
		if (gender == null || Gender.UNDETERMINED == gender)
		{
			set("Male", null);
		}
		else
		{
			set("Male", Gender.MALE == gender);
		}
	}
}
