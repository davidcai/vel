package elert.database;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import samoyan.core.Util;
import samoyan.database.DataBean;

public class Procedure extends DataBean
{
	/**
	 * Sorts procedure by their offical name (not the display name).
	 * @author brian
	 *
	 */
	public static class SortByName implements Comparator<Procedure>
	{
		private Collator collator;

		public SortByName(Locale locale)
		{
			this.collator = Collator.getInstance(locale);
		}

		public int compare(Procedure proc1, Procedure proc2)
		{
			CollationKey key1 = collator.getCollationKey(proc1.getName());
			CollationKey key2 = collator.getCollationKey(proc2.getName());

			return key1.compareTo(key2);
		}
	}

	/**
	 * Sorts procedure by their offical name (not the display name).
	 * @author brian
	 *
	 */
	public static class SortByDisplayName implements Comparator<Procedure>
	{
		private Collator collator;

		public SortByDisplayName(Locale locale)
		{
			this.collator = Collator.getInstance(locale);
		}

		public int compare(Procedure proc1, Procedure proc2)
		{
			CollationKey key1 = collator.getCollationKey(proc1.getDisplayName());
			CollationKey key2 = collator.getCollationKey(proc2.getDisplayName());

			return key1.compareTo(key2);
		}
	}

	public static final int MAXSIZE_NAME = 64;
	public static final int MAXSIZE_TYPE = 64;
	public static final int MAXSIZE_SHORT_DESCRIPTION = 256;
	public static final int MAXSIZE_COMMON_NAME = 64;
	
	public static final int MAX_DURATION = 1440;  //24 hours (in minutes)
	public static final int MAX_LEAD = 30;  //in days

	public String getName()
	{
		return (String) get("Name");
	}
	
	public void setName(String name)
	{
		set("Name", name);
	}
	
	public UUID getTypeID()
	{
		return (UUID) get("TypeID");
	}
	
	public void setTypeID(UUID typeID)
	{
		
		set("TypeID", typeID);
	}
	
	public int getDuration()
	{
		return (Integer) get("Duration", 0);
	}
	
	public void setDuration(int duration)
	{
		set("Duration", duration);
	}
	
	public int getLead()
	{
		return (Integer) get("Lead", 0);
	}
	
	public void setLead(int lead)
	{
		set("Lead",  lead);
	}
	
	public boolean isCustom()
	{
		return (Boolean)get("Custom");
	}
	
	public void setCustom(boolean isCustom)
	{
		set("Custom", isCustom);
	}
	
	public String getShortDescription()
	{
		return (String)get("ShortDesc");
	}
	
	public void setShortDescription(String description)
	{
		set("ShortDesc",  description);
	}
	
	public String getCommonName()
	{
		return (String)get("CommonName");
	}

	public void setCommonName(String commonName)
	{
		set("CommonName", commonName);
	}
	
	public String getDisplayName()
	{
		if (!Util.isEmpty(getCommonName()))
		{
			return getCommonName();
		}
		else
		{
			return getName();
		}
	}
	
	public String getDefinition()
	{
		return (String) get("Definition");
	}
	
	public void setDefinition(String definition)
	{
		set("Definition",  definition);
	}
	
	public String getInstructions()
	{
		return (String) get("Instructions");
	}
	
	public void setInstructions(String instructions)
	{
		set("Instructions",  instructions);
	}
	
	public String getNotes()
	{
		return (String) get("Notes");
	}
	
	public void setNotes(String notes)
	{
		set("Notes",  notes);
	}

	public String getVideo()
	{
		return (String) get("Video");
	}
	
	public void setVideo(String video)
	{
		set("Video",  video);
	}		
}
