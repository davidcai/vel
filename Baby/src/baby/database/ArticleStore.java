package baby.database;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import baby.app.BabyConsts;

import samoyan.core.DateFormatEx;
import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.core.image.JaiImage;
import samoyan.database.DataBeanStore;
import samoyan.database.Image;
import samoyan.database.Query;
import samoyan.database.TableDef;

public final class ArticleStore extends DataBeanStore<Article>
{
	private static ArticleStore instance = new ArticleStore();

	protected ArticleStore()
	{
	}

	public final static ArticleStore getInstance()
	{
		return instance;
	}

	@Override
	protected Class<Article> getBeanClass()
	{
		return Article.class;
	}

	@Override
	public TableDef defineMapping()
	{
		TableDef td = createTableDef("Articles");

		td.defineProp("HTML", String.class);
		td.defineProp("PlainText", String.class);
		td.defineProp("Summary", String.class).size(0, Article.MAXSIZE_SUMMARY);
		td.defineProp("SourceURL", String.class).size(0, Article.MAXSIZE_SOURCE_URL);
		
		td.defineCol("Title", String.class).size(0, Article.MAXSIZE_TITLE);
		td.defineCol("SourceURLHash", byte[].class);
		td.defineCol("Section", String.class).size(0, Article.MAXSIZE_SECTION);
		td.defineCol("SubSection", String.class).size(0, Article.MAXSIZE_SUBSECTION);
		td.defineCol("TimelineFrom", Integer.class);
		td.defineCol("TimelineTo", Integer.class);
		td.defineCol("UpdatedDate", Date.class);
		td.defineCol("Region", String.class).size(0, Article.MAXSIZE_REGION);
		td.defineCol("MedicalCenter", String.class).size(0, Article.MAXSIZE_MEDICAL_CENTER);
		td.defineCol("Priority", Integer.class);
		td.defineCol("ByCrawler", Boolean.class);

		td.defineProp("Photo", Image.class);
		td.defineProp("YouTube", String.class);

		return td;
	}

	// - - -
	
	public Article loadBySourceURL(String url) throws Exception
	{
		if (url==null) return null;
		byte[] urlHash = Util.hexStringToByteArray(Util.hashSHA256(url));
		return loadByColumn("SourceURLHash", urlHash);
	}
	
	public Article openBySourceURL(String url) throws Exception
	{
		if (url==null) return null;
		byte[] urlHash = Util.hexStringToByteArray(Util.hashSHA256(url));
		return openByColumn("SourceURLHash", urlHash);
	}

	/**
	 * Queries the database for the latest date of stored resource articles.
	 * @return
	 * @throws SQLException 
	 */
	public Date queryLastUpdated(String section) throws SQLException
	{
		Query q = new Query();
		try
		{
			ResultSet rs = q.select("SELECT MAX(UpdatedDate) FROM Articles WHERE Section=?", new ParameterList(section));
			if (rs.next())
			{
				return new Date(rs.getLong(1));
			}
			else
			{
				return null;
			}
		}
		finally
		{
			q.close();
		}
	}
	
	/**
	 * Removes articles that were created by the crawler, and last updated before the given date.
	 * @param updatedBefore
	 * @throws SQLException 
	 */
	public void removeStaleCrawledArticles(String section, Date updatedBefore) throws Exception
	{
		removeMany( Query.queryListUUID("SELECT ID FROM Articles WHERE Section=? AND UpdatedDate<? AND ByCrawler<>0", new ParameterList(section).plus(updatedBefore)) );
	}
	
	public List<UUID> queryBySection(String section) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Articles WHERE Section=? ORDER BY Priority DESC, Title ASC", new ParameterList(section));
	}

	/**
	 * 
	 * @param section
	 * @param stage The integer representation of the pregnancy stage, as returned from {@link Stage#toInteger()}.
	 * @return
	 * @throws SQLException
	 */
	public List<UUID> queryBySectionAndTimeline(String section, int lowStage, int highStage) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Articles WHERE Section=? AND " +
				"((TimelineFrom>=? AND TimelineFrom<=?) OR (TimelineTo>=? AND TimelineTo<=?) OR (TimelineFrom<? AND TimelineTo>?))" +
				"ORDER BY Priority DESC, Title ASC",
				new ParameterList(section).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage));
	}
	
	public List<UUID> queryBySectionAndMedicalCenter(String section, String subsection, String region, String medicalCenter) throws SQLException
	{
		StringBuilder sql = new StringBuilder();
		ParameterList params = new ParameterList();
		
		sql.append("SELECT ID FROM Articles WHERE 1=1");
		if (!Util.isEmpty(section))
		{
			sql.append(" AND Section=?");
			params.plus(section);
		}
		if (!Util.isEmpty(subsection))
		{
			sql.append(" AND SubSection=?");
			params.plus(subsection);
		}
		if (!Util.isEmpty(region))
		{
			sql.append(" AND Region=?");
			params.plus(region);
		}
		if (!Util.isEmpty(medicalCenter))
		{
			sql.append(" AND MedicalCenter=?");
			params.plus(medicalCenter);
		}
		sql.append(" ORDER BY Priority DESC, Title ASC");
		
		return Query.queryListUUID(sql.toString(), params);
	}
	
//	/**
//	 * Query the article store according to the optional parameters.
//	 * @param section The section. <code>null</code> for all sections.
//	 * @param lowStage Pregnancy stage integer value, <code>Stage.invalid().toInteger()</code> to ignore.
//	 * @param highStage Pregnancy stage integer value, <code>Stage.invalid().toInteger()</code> to ignore.
//	 * @param region The region. <code>null</code> for all regions.
//	 * @param medicalCenter The medical center. <code>null</code> for all medical centers.
//	 * @return
//	 * @throws SQLException 
//	 */
//	public List<UUID> query(String section, int lowStage, int highStage, String region, String medicalCenter) throws SQLException
//	{
//		StringBuilder sql = new StringBuilder();
//		ParameterList params = new ParameterList();
//		
//		sql.append("SELECT ID FROM Articles WHERE 1=1");
//		if (section!=null)
//		{
//			sql.append(" AND Section=?");
//			params.plus(section);
//		}
//		if (lowStage!=Stage.invalid().toInteger() && highStage!=Stage.invalid().toInteger())
//		{
//			sql.append(" AND ((TimelineFrom>=? AND TimelineFrom<=?) OR (TimelineTo>=? AND TimelineTo<=?) OR (TimelineFrom<? AND TimelineTo>?))");
//			params.plus(lowStage).plus(highStage).plus(lowStage).plus(highStage).plus(lowStage).plus(highStage);
//		}
//		if (region!=null)
//		{
//			sql.append(" AND Region=?");
//			params.plus(region);
//		}
//		if (medicalCenter!=null)
//		{
//			sql.append(" AND MedicalCenter=?");
//			params.plus(medicalCenter);
//		}
//		sql.append(" ORDER BY Priority DESC, Title ASC");
//		
//		return Query.queryListUUID(sql.toString(), params);
//	}
	
	public List<String> getRegions() throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT Region FROM Articles WHERE NOT Region IS NULL AND NOT Region='' ORDER BY Region", null);
	}
	
	public List<String> getMedicalCenters(String region) throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT MedicalCenter FROM Articles WHERE NOT MedicalCenter IS NULL AND NOT MedicalCenter='' AND Region=? ORDER BY MedicalCenter ASC", new ParameterList(region));
	}

	public List<String> getMedicalCenters() throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT MedicalCenter FROM Articles WHERE NOT MedicalCenter IS NULL AND NOT MedicalCenter='' ORDER BY MedicalCenter ASC", null);
	}

	public List<String> getSections() throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT Section FROM Articles ORDER BY Section ASC", null);
	}

	public List<String> getSubSections(String section) throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT SubSection FROM Articles WHERE Section=? ORDER BY SubSection ASC", new ParameterList(section));
	}

	public List<UUID> getAll() throws Exception
	{
		return super.queryAll();
	}
	
	public List<UUID> searchByText(String queryStr, String section, String region) throws SQLException
	{
		try
		{
			ParameterList params = new ParameterList();
			String sql = "SELECT p.LinkedID FROM Props AS p, Articles AS a " +
					"WHERE a.ID=p.LinkedID AND p.Name='PlainText' AND FREETEXT(p.*, ?)";
			params.add(queryStr);
			if (!Util.isEmpty(section))
			{
				sql += " AND a.Section=?";
				params.add(section);
			}
			if (!Util.isEmpty(region))
			{
				sql += " AND a.Region=?";
				params.add(region);
			}
			return Query.queryListUUID(sql, params);
		}
		catch (SQLException exc)
		{
			// If FREETEXT is not supported, default to using LIKE %q%
			ParameterList params = new ParameterList();
			String sql = "SELECT p.LinkedID FROM Props AS p, Articles AS a " +
					"WHERE a.ID=p.LinkedID AND p.Name='PlainText' AND (p.Val LIKE ? OR p.ValText LIKE ?)";
			params.add("%" + queryStr + "%");
			params.add("%" + queryStr + "%");
			if (!Util.isEmpty(section))
			{
				sql += " AND a.Section=?";
				params.add(section);
			}
			if (!Util.isEmpty(region))
			{
				sql += " AND a.Region=?";
				params.add(region);
			}
			return Query.queryListUUID(sql, params);
		}
	}
	
	/**
	 * Create an article from the given input streams and add it to the database.
	 * @param text A stream pointing to HTML text in the proper format. Must be a UTF-8 encoded stream.
	 * @param img A stream pointing to an image file to associate with the article, may be <code>null</code>.
	 * @return The persisted article, or <code>null</code>.
	 */
	public Article importFromStream(InputStream text, InputStream img) throws Exception
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
		if (type!=null && type.equalsIgnoreCase("Article")==false)
		{
			return null;
		}
		
		String section = getMetaTagValue(html, "Section");
		if (section==null) section = BabyConsts.SECTION_INFO;
		String subsection = getMetaTagValue(html, "Subsection");
		String uri = getMetaTagValue(html, "URI");
		String desc = getMetaTagValue(html, "Description");
		String pinned = getMetaTagValue(html, "Pinned");
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
		String youTube = getMetaTagValue(html, "YouTube");
		
		JaiImage jai = null;
		if (img!=null)
		{
			jai = new JaiImage(Util.inputStreamToBytes(img));
		}

		Article article = openBySourceURL(uri);
		if (article==null)
		{
			article = new Article();
		}
//		else if (!article.getUpdatedDate().before(updated))
//		{
//			return article;
//		}
		
		article.setSection(section);
		article.setSubSection(subsection);
		article.setSourceURL(uri);
		article.setTitle(title);
		article.setSummary(desc);
		article.setHTML(body);
		article.setPriority(pinned!=null && pinned.equalsIgnoreCase("true")? 100 : 0);
		article.setTimelineFrom(from.toInteger());
		article.setTimelineTo(to.toInteger());
		article.setUpdatedDate(updated);
		if (jai!=null)
		{
			article.setPhoto(new Image(jai));
		}
		article.setYouTubeVideoID(youTube);
		
		save(article);

		return article;
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
