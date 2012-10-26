package baby.database;

import java.util.Date;

import samoyan.core.Util;
import samoyan.database.DataBean;
import samoyan.database.Image;

public final class Article extends DataBean
{
	public static final int MAXSIZE_TITLE = 128;
	public static final int MAXSIZE_SUMMARY = 256;
	public static final int MAXSIZE_SECTION = 64;
	public static final int MAXSIZE_REGION = 64;
	public static final int MAXSIZE_MEDICAL_CENTER = 64;
	public static final int MAXSIZE_SOURCE_URL = 2048;
	
	// Known sections
	public static final String SECTION_HEALTHY_BEGINNINGS = "Healthy Beginnings";
	
	public String getHTML()
	{
		return (String) get("HTML");
	}
	public void setHTML(String html)
	{
		set("HTML", html);
		set("PlainText", html==null? null : Util.htmlToText(html));
	}
	/**
	 * Returns the content of the article without any HTML tags. Used as basis for text-searches.
	 * @return
	 */
	public String getPlainText()
	{
		return (String) get("PlainText");
	}
	
	public String getTitle()
	{
		return (String) get("Title");
	}
	public void setTitle(String title)
	{
		set("Title", title);
	}
	
	public String getSummary()
	{
		return (String) get("Summary");
	}
	public void setSummary(String summary)
	{
		set("Summary", summary);
	}
	
	public String getSourceURL()
	{
		return (String) get("SourceURL");
	}
	public void setSourceURL(String url)
	{
		set("SourceURL", url);
		set("SourceURLHash", url==null? null : Util.hexStringToByteArray(Util.hashSHA256(url)));
	}

	public String getSection()
	{
		return (String) get("Section");
	}
	public void setSection(String section)
	{
		set("Section", section);
	}

	public int getTimelineFrom()
	{
		return (Integer) get("TimelineFrom", 0);
	}
	public void setTimelineFrom(int phase)
	{
		set("TimelineFrom", phase);
	}
	
	public int getTimelineTo()
	{
		return (Integer) get("TimelineTo", 0);
	}
	public void setTimelineTo(int phase)
	{
		set("TimelineTo", phase);
	}

	public Date getUpdatedDate()
	{
		return (Date) get("UpdatedDate");
	}
	public void setUpdatedDate(Date date)
	{
		set("UpdatedDate", date);
	}

	public String getRegion()
	{
		return (String) get("Region");
	}
	public void setRegion(String region)
	{
		set("Region", region);
	}
	
	public String getMedicalCenter()
	{
		return (String) get("MedicalCenter");
	}
	public void setMedicalCenter(String city)
	{
		set("MedicalCenter", city);
	}

	public Image getPhoto()
	{
		return (Image) get("Photo");
	}
	public void setPhoto(Image img)
	{
		set("Photo", img);
	}
	
	public int getPriority()
	{
		return (Integer) get("Priority", 0);
	}
	public void setPriority(int priority)
	{
		set("Priority", priority);
	}
}
