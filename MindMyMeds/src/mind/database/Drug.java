package mind.database;

import java.util.UUID;

import samoyan.database.DataBean;

public class Drug extends DataBean implements Comparable<Drug>
{
	public static final int MAXSIZE_NAME = 256;
	public static final int MAXSIZE_GENERIC_NAME = 256;
	public static final int MAXSIZE_DESCRIPTION = 256;

	@Override
	public int compareTo(Drug that)
	{
		return this.getName().compareTo(that.getName());
	}

	public String getName()
	{
		return (String) get("Name");
	}
	public void setName(String name)
	{
		set("Name", name);
	}
	
	public String getGenericName()
	{
		return (String) get("GenericName");
	}
	public void setGenericName(String genericName)
	{
		set("GenericName", genericName);
	}
	
	public String getDisplayName()
	{
		String dispName = getName();
		String generic = getGenericName();
		if (dispName.equals(generic)==false)
		{
			dispName += " (" + generic + ")";
		}
		return dispName;
	}
	
	public UUID getPatientID()
	{
		return (UUID) get("PatientID");
	}
	public void setPatientID(UUID patientID)
	{
		set("PatientID", patientID);
	}
	
	public String getDescription()
	{
		return (String) get("Desc");
	}
	public void setDescription(String description)
	{
		set("Desc", description);
	}
	
	public String getYouTubeVideoID()
	{
		return (String) get("YouTubeVideoID");
	}
	public void setYouTubeVideoID(String youTubeVideoID)
	{
		set("YouTubeVideoID", youTubeVideoID);
	}
	
	public String getInformation()
	{
		return (String) get("Info");
	}
	public void setInformation(String information)
	{
		set("Info", information);
	}

	public String getDrugInteractionInformation()
	{
		String info = getInformation();
		if (info==null)
		{
			return null;
		}
		int p = info.indexOf("<h2>What other drugs");
		if (p<0)
		{
			p = info.lastIndexOf("<h2>");
		}
		if (p<0)
		{
			return null;
		}
		p = info.indexOf("</h2>", p);
		if (p<0)
		{
			return null;
		}
		p += 5;
		return info.substring(p);
	}
	public String getSideEffectsInformation()
	{
		String info = getInformation();
		if (info==null)
		{
			return null;
		}
		int p = info.indexOf("side effects</h2>");
		if (p<0)
		{
			return null;
		}
		p += "side effects</h2>".length();
		
		int q = info.indexOf("<h2>", p);
		if (q<0)
		{
			q = info.length();
		}
		
		return info.substring(p, q);
	}
}
