package baby.database;

import java.util.Date;

import baby.app.BabyConsts;

import samoyan.core.Util;
import samoyan.database.DataBean;
import samoyan.database.Image;

public final class Article extends DataBean
{
	public static final int MAXSIZE_TITLE = 128;
	public static final int MAXSIZE_SUMMARY = 256;
	public static final int MAXSIZE_SECTION = 64;
	public static final int MAXSIZE_SUBSECTION = 64;
	public static final int MAXSIZE_REGION = 64;
	public static final int MAXSIZE_MEDICAL_CENTER = 64;
	public static final int MAXSIZE_SOURCE_URL = 2048;
	public static final int MAXSIZE_YOUTUBE = 16;
	
	public Article()
	{
		init("TimelineFrom", Stage.preconception().toInteger());
		init("TimelineTo", Stage.infancy(Stage.MAX_MONTHS).toInteger());
		init("UpdatedDate", new Date());
	}
	
	public String getHTML()
	{
		return (String) get("HTML");
	}
	public void setHTML(String html)
	{
		set("HTML", html);
		setPlainText();
	}
	
	public String getTitle()
	{
		return (String) get("Title");
	}
	public void setTitle(String title)
	{
		set("Title", title);
		setPlainText();
	}
	
	private void setPlainText()
	{
		String html = getHTML();
		String title = getTitle();
		
		StringBuilder bld = new StringBuilder();
		if (title!=null)
		{
			bld.append(title);
			bld.append(". ");
		}
		if (html!=null)
		{
			bld.append(Util.htmlToText(html));
		}
		set("PlainText", bld.toString());
	}
	
	public String getSummary()
	{
		String summary = getSummaryRaw();
		if (Util.isEmpty(summary))
		{
			summary = Util.htmlToText(getHTML());
			summary = Util.getTextAbstract(summary, Article.MAXSIZE_SUMMARY);
		}
		return summary;
	}
	public String getSummaryRaw()
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
		return (String) get("Section", BabyConsts.SECTION_INFO);
	}
	public void setSection(String section)
	{
		set("Section", section);
	}

	public String getSubSection()
	{
		return (String) get("SubSection");
	}
	public void setSubSection(String subSection)
	{
		set("SubSection", subSection);
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
	
	public String getYouTubeVideoID()
	{
		return (String) get("YouTube");
	}
	public void setYouTubeVideoID(String youTubeVideID)
	{
		set("YouTube", youTubeVideID);
	}
	
	/**
	 * Indicates if this article was created by a crawler. In this case, it will not be editable by the content manager.
	 * @return
	 */
	public boolean isByCrawler()
	{
		return (Boolean) get("ByCrawler", false);
	}
	public void setByCrawler(boolean byCrawler)
	{
		set("ByCrawler", byCrawler);
	}
}
