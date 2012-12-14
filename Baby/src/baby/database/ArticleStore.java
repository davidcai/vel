package baby.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.core.Util;
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
		td.defineCol("TimelineFrom", Integer.class);
		td.defineCol("TimelineTo", Integer.class);
		td.defineCol("UpdatedDate", Date.class);
		td.defineCol("Region", String.class).size(0, Article.MAXSIZE_REGION);
		td.defineCol("MedicalCenter", String.class).size(0, Article.MAXSIZE_MEDICAL_CENTER);
		td.defineCol("Priority", Integer.class);

		td.defineProp("Photo", Image.class);
		td.defineProp("SubSection", String.class).size(0, Article.MAXSIZE_SUBSECTION);

		return td;
	}

	// - - -
	
	public Article loadBySourceURL(String url) throws Exception
	{
		byte[] urlHash = Util.hexStringToByteArray(Util.hashSHA256(url));
		return loadByColumn("SourceURLHash", urlHash);
	}
	
	public Article openBySourceURL(String url) throws Exception
	{
		byte[] urlHash = Util.hexStringToByteArray(Util.hashSHA256(url));
		return openByColumn("SourceURLHash", urlHash);
	}

	public List<UUID> queryBySectionAndMedicalCenter(String section, String region, String medicalCenter) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Articles WHERE Section=? AND Region=? AND MedicalCenter=? ORDER BY Priority DESC, Title ASC", new ParameterList(section).plus(region).plus(medicalCenter));
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
	 * Removes articles that were last updated before the given date.
	 * @param updatedBefore
	 * @throws SQLException 
	 */
	public void removeStaleArticles(String section, Date updatedBefore) throws Exception
	{
		removeMany( Query.queryListUUID("SELECT ID FROM Articles WHERE Section=? AND UpdatedDate<?", new ParameterList(section).plus(updatedBefore)) );
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
	
	public List<String> getRegions() throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT Region FROM Articles WHERE NOT Region IS NULL ORDER BY Region", null);
	}
	
	public List<String> getMedicalCenters(String region) throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT MedicalCenter FROM Articles WHERE Region=? ORDER BY MedicalCenter ASC", new ParameterList(region));
	}

	public List<String> getSections() throws SQLException
	{
		return Query.queryListString("SELECT DISTINCT Section FROM Articles ORDER BY Section ASC", null);
	}

	public List<UUID> getAll() throws Exception
	{
		return super.queryAll();
	}
}
