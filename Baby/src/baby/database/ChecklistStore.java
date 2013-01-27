package baby.database;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import baby.app.BabyConsts;

import samoyan.core.DateFormatEx;
import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public final class ChecklistStore extends DataBeanStore<Checklist>
{
	private static ChecklistStore instance = new ChecklistStore();

	protected ChecklistStore()
	{
	}

	public final static ChecklistStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Checklist> getBeanClass()
	{
		return Checklist.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = createTableDef("Checklists");
		
		td.defineCol("Title", String.class).size(0, Checklist.MAXSIZE_TITLE);
		td.defineCol("Description", String.class).size(0, Checklist.MAXSIZE_DESCRIPTION);
		td.defineCol("Section", String.class).size(0, Checklist.MAXSIZE_SECTION);
		td.defineCol("SubSection", String.class).size(0, Checklist.MAXSIZE_SUBSECTION);
		td.defineCol("TimelineFrom", Integer.class);
		td.defineCol("TimelineTo", Integer.class);
		td.defineCol("UserID", UUID.class).ownedBy("Users");
		td.defineCol("UpdatedDate", Date.class);
		td.defineCol("SourceURLHash", byte[].class);

		td.defineProp("SourceURL", String.class).size(0, Checklist.MAXSIZE_SOURCE_URL);

		return td;
	}

	// - - -

	public Checklist loadBySourceURL(String url) throws Exception
	{
		if (url==null) return null;
		byte[] urlHash = Util.hexStringToByteArray(Util.hashSHA256(url));
		return loadByColumn("SourceURLHash", urlHash);
	}
	
	public Checklist openBySourceURL(String url) throws Exception
	{
		if (url==null) return null;
		byte[] urlHash = Util.hexStringToByteArray(Util.hashSHA256(url));
		return openByColumn("SourceURLHash", urlHash);
	}

	/**
	 * Returns the IDs of checklists that are defined by the content manager, i.e. excluding checklists created by users.
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> getAllStandard() throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Checklists WHERE UserID IS NULL ORDER BY TimelineFrom ASC, TITLE ASC", null);
	}

	public List<UUID> queryBySectionAndTimeline(String section, int lowStage, int highStage) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Checklists WHERE Section=? AND " +
				"((TimelineFrom>=? AND TimelineFrom<=?) OR (TimelineTo>=? AND TimelineTo<=?) OR (TimelineFrom<? AND TimelineTo>?))" +
				"ORDER BY TimelineFrom DESC, Title ASC",
				new ParameterList(section).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage));
	}
	
	public List<UUID> queryByTimeline(int lowStage, int highStage) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Checklists WHERE " +
				"(TimelineFrom>=? AND TimelineFrom<=?) OR (TimelineTo>=? AND TimelineTo<=?) OR (TimelineFrom<? AND TimelineTo>?)" +
				"ORDER BY TimelineFrom DESC, Title ASC",
				new ParameterList().plus(lowStage).plus(highStage).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage));
	}
	
	public Checklist loadPersonalChecklist(UUID userID) throws Exception
	{
		if (userID==null) return null;
		
		Checklist cl = loadByColumn("UserID", userID);
		if (cl==null)
		{
			cl = new Checklist();
			cl.setTitle(userID.toString());
			cl.setUserID(userID);
			cl.setTimelineFrom(Stage.preconception().toInteger());
			cl.setTimelineTo(Stage.infancy(Stage.MAX_MONTHS).toInteger());
			cl.setUpdatedDate(new Date());
			save(cl);
		}
		return cl;
	}

	public List<UUID> getAll() throws Exception
	{
		return super.queryAll();
	}

	/**
	 * Create an checklist from the given input streams and add it to the database.
	 * @param text A stream pointing to HTML text in the proper format. Must be a UTF-8 encoded stream.
	 * @return The persisted checklist, or <code>null</code>.
	 */
	public Checklist importFromStream(InputStream text) throws Exception
	{
		String title = "";
		String body = "";
		
		String html = Util.inputStreamToString(text, "UTF-8");
		int p = html.indexOf("<title>");
		if (p>=0)
		{
			p += 7;
			int q = html.indexOf("</title>", p);
			if (q>=0)
			{
				title = Util.htmlDecode(html.substring(p, q));
			}
		}
		
		p = html.indexOf("<body>");
		if (p>=0)
		{
			p += 6;
			int q = html.indexOf("</body>", p);
			if (q>=0)
			{
				body = html.substring(p, q);
			}
		}
		
		String type = getMetaTagValue(html, "Type");
		if (type!=null && type.equalsIgnoreCase("Checklist")==false)
		{
			return null;
		}
		
		String section = getMetaTagValue(html, "Section");
		if (section==null) section = BabyConsts.SECTION_INFO;
		String subsection = getMetaTagValue(html, "Subsection");
		String uri = getMetaTagValue(html, "URI");
		String desc = getMetaTagValue(html, "Description");
		Stage from = parseTimeline(getMetaTagValue(html, "From"));
		if (from==null) from = Stage.preconception();
		Stage to = parseTimeline(getMetaTagValue(html, "To"));
		if (to==null) to = Stage.pregnancy(Stage.MAX_MONTHS);
		Date updated;
		try
		{
			updated = DateFormatEx.getISO8601Instance().parse(getMetaTagValue(html, "Updated"));
		}
		catch (Exception e)
		{
			updated = new Date();
		}
		
		// Write checklist
		Checklist checklist = openBySourceURL(uri);
		if (checklist==null)
		{
			checklist = new Checklist();
		}
//		else if (!checklist.getUpdatedDate().before(updated))
//		{
//			return checklist;
//		}
		
		checklist.setSection(section);
		checklist.setSubSection(subsection);
		checklist.setSourceURL(uri);
		checklist.setTitle(title);
		checklist.setDescription(desc);
		checklist.setTimelineFrom(from.toInteger());
		checklist.setTimelineTo(to.toInteger());
		checklist.setUpdatedDate(updated);
					
		save(checklist);
		
		// Write checkitems
		List<String> texts = new ArrayList<String>();
		List<String> links = new ArrayList<String>();
		p = 0;
		while (true)
		{
			p = body.indexOf("<li>", p);
			if (p<0) break;
			p += 4;
			int q = body.indexOf("</li>", p);
			if (q<0) break;
			
			String li = body.substring(p, q);
			int xx = li.indexOf(">");
			int yy = li.lastIndexOf("<");
			if (xx>=0 && yy>xx)
			{
				texts.add(li.substring(xx+1, yy));
				int hh = li.toLowerCase(Locale.US).indexOf("href=");
				if (hh>=0)
				{
					int ws = li.indexOf(" ", hh);
					if (ws<0 || ws>xx)
					{
						ws = xx;
					}
					String href = li.substring(hh+5, ws);
					if (href.startsWith("\"") || href.startsWith("'"))
					{
						href = href.substring(1);
					}
					if (href.endsWith("\"") || href.endsWith("'"))
					{
						href = href.substring(0, href.length()-1);
					}
					links.add(href);
				}
				else
				{
					links.add(null);
				}
			}
			else
			{
				texts.add(li);
				links.add(null);
			}
			p = q + 5;
		}

		List<UUID> checkitemIDs = CheckItemStore.getInstance().getByChecklistID(checklist.getID());
		for (UUID id : checkitemIDs)
		{
			CheckItem ci = CheckItemStore.getInstance().load(id);
			
			boolean found = false;
			for (int k=0; k<texts.size(); k++)
			{
				if (texts.get(k)!=null && texts.get(k).equalsIgnoreCase(ci.getText()))
				{
					// Update checkitem
					ci = (CheckItem) ci.clone(); // Open for writing
					ci.setOrderSequence(k);
					ci.setLink(links.get(k));
					CheckItemStore.getInstance().save(ci);

					texts.set(k, null);
					links.set(k, null);
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				// Checkitem was removed
				CheckItemStore.getInstance().remove(id);
			}
		}
		
		for (int k=0; k<texts.size(); k++)
		{
			if (texts.get(k)!=null)
			{
				CheckItem ci = new CheckItem();
				ci.setChecklistID(checklist.getID());
				ci.setText(texts.get(k));
				ci.setLink(links.get(k));
				ci.setOrderSequence(k);
				CheckItemStore.getInstance().save(ci);
			}
		}
		
		return checklist;
	}
	
	private String getMetaTagValue(String html, String name)
	{
		String lcHTML = html.toLowerCase(Locale.US);
		int p = lcHTML.indexOf("<meta name=\"" + name.toLowerCase(Locale.US) + "\"");
		if (p>=0)
		{
			int q = lcHTML.indexOf("content=\"", p);
			if (q>=0)
			{
				q += 9;
				int r = lcHTML.indexOf("\"", q);
				if (r>=0)
				{
					return html.substring(q, r);
				}
			}
		}
		return null;
	}
	
	private Stage parseTimeline(String s)
	{
		if (s==null)
		{
			return null;
		}
		s = s.toLowerCase(Locale.US);
		if (s.startsWith("week "))
		{
			return Stage.pregnancy(Integer.parseInt(s.substring(5).trim()));
		}
		else if (s.startsWith("pregnancy "))
		{
			return Stage.pregnancy(Integer.parseInt(s.substring(10).trim()));
		}
		else if (s.startsWith("month "))
		{
			return Stage.infancy(Integer.parseInt(s.substring(6).trim()));
		}
		else if (s.startsWith("infancy "))
		{
			return Stage.infancy(Integer.parseInt(s.substring(8).trim()));
		}
		else if (s.startsWith("pre"))
		{
			return Stage.preconception();
		}
		else
		{
			return null;
		}
	}
}
