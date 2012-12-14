package mind.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import samoyan.core.*;
import samoyan.database.*;

public class DrugStore extends DataBeanStore<Drug>
{
	private static DrugStore instance = new DrugStore();

	protected DrugStore()
	{
	}
	public final static DrugStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Drug> getBeanClass()
	{
		return Drug.class;
	}	

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Drugs");
		
		td.defineCol("Name", String.class).size(0, Drug.MAXSIZE_NAME);
		td.defineCol("GenericName", String.class).size(0, Drug.MAXSIZE_GENERIC_NAME);
		td.defineCol("PatientID", UUID.class).invariant().ownedBy("Patients");
		
		td.defineProp("Desc", String.class);
		td.defineProp("Info", String.class);
		td.defineProp("YouTubeVideoID", String.class);
		
		return td;
	}
	
	// - - -
	
	public List<UUID> searchByName(String queryString) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";
		
		return Query.queryListUUID(
				"SELECT ID FROM Drugs WHERE (Name LIKE ? OR GenericName LIKE ?) AND (PatientID IS NULL)", 
				new ParameterList(queryString).plus(queryString));
	}

	public List<UUID> searchPrivateByName(String queryString, UUID patientID) throws SQLException
	{
		if (queryString.length()>=3)
		{
			queryString = "%" + queryString;
		}
		queryString += "%";
		
		return Query.queryListUUID(
				"SELECT ID FROM Drugs WHERE (Name LIKE ? OR GenericName LIKE ?) AND PatientID=?",
				new ParameterList(queryString).plus(queryString).plus(patientID));
	}

	public Drug loadByName(String drugName, UUID patientID) throws Exception
	{
		Query q = new Query();
		try
		{
			ParameterList params = new ParameterList(drugName);
			String sql = "SELECT ID FROM Drugs WHERE Name=? AND (PatientID IS NULL";
			if (patientID!=null)
			{
				params.plus(Util.uuidToBytes(patientID));
				sql += " OR PatientID=?";
			}
			sql += ")";
			ResultSet rs = q.select(sql, params);
			if (rs.next())
			{
				return load(Util.bytesToUUID(rs.getBytes(1)));
			}
			else
			{
				// Try loading from drugs.com
				return createFromWeb(drugName);
			}
		}
		finally
		{
			q.close();
		}
	}
	
	/**
	 * Returns the drugs that were created by the given patient.
	 * This is NOT the list of drugs that a patient is prescribed to.
	 * @param patientID
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByPatientID(UUID patientID) throws Exception
	{
		return queryByColumn("PatientID", patientID, "Name", true);
	}
	
	/**
	 * Remove patient defined drugs that are no longer attached to any prescription.
	 * @throws Exception 
	 */
	public void removeOrphanedDrugs() throws Exception
	{
		removeMany(Query.queryListUUID("SELECT ID FROM Drugs WHERE NOT PatientID IS NULL AND NOT ID IN (SELECT DISTINCT DrugID FROM Prescriptions)", null));
	}
	
	/**
	 * Downloads drug information from drugs.com and creates a new Drug object.
	 * @param drugName
	 * @return The newly created, unsaved, Drug object, or <code>null</code> if the drug was not found on drugs.com.
	 * @throws Exception
	 */
	public Drug createFromWeb(String drugName) throws Exception
	{
		if (Util.isEmpty(drugName)) return null;
		drugName = drugName.trim();
		
		// Search HTML in cache
		String cacheKey = "drugs.com:" + drugName;
		String html = (String) Cache.get(cacheKey);
		if (html==null)
		{
			WebBrowser wb = new WebBrowser();
			wb.setUserAgent(WebBrowser.AGENT_FIREFOX);
			wb.setUseCache(true);
			
			// First, try fetching as brand name
			wb.get("http://www.drugs.com/" + Util.urlSafe(drugName).toLowerCase(Locale.US) + ".html?printable=1");
			int response = wb.getResponseCode();
			
			if (response!=HttpServletResponse.SC_OK)
			{
				// Try fetching as generic name
				wb.get("http://www.drugs.com/mtm/" + Util.urlSafe(drugName).toLowerCase(Locale.US) + ".html", new ParameterMap("printable", "1"));
				response = wb.getResponseCode();
			}
			
			if (response!=HttpServletResponse.SC_OK)
			{
				Cache.insert(cacheKey, "NULL");
				return null;
			}
			
			// Scrape the HTML
			html = wb.getContent();
		
			Cache.insert(cacheKey, html);
		}
		else if (html.equals("NULL"))
		{
			return null;
		}
		
		String h1 = getBetween(html, "<h1", "/h1>");
		h1 = getBetween(h1, ">", "<");
		
		String genericName = getBetween(html, "Generic Name: ", "<br />\nBrand Names:");
		if (genericName==null)
		{
			genericName = getBetween(html, "Generic Name: ", "</p>");
		}
		if (genericName==null)
		{
			return null;
		}
		int p = genericName.lastIndexOf("(");
		if (p>=0)
		{
			genericName = genericName.substring(0, p);
		}
		genericName = genericName.trim();
		if (genericName.indexOf("<")>=0)
		{
			genericName = getBetween(genericName, ">", "<");
		}
		
		String main = getBetween(html, "Generic Name: ", "<h2>Where can I get more information?</h2>");
		if (main==null)
		{
			main = getBetween(html, "Generic Name: ", "</div>\n\n<script");
		}
		main = "<p>Generic Name: " + main;
		main = Util.strReplace(main, "<a ", "<span ");
		main = Util.strReplace(main, "</a>", "</span>");
		p = main.indexOf("<div class=\"dotline\"></div>");
		if (p>=0)
		{
			int q = main.indexOf("<h2>", p);
			if (q>=0)
			{
				main = main.substring(0, p) + main.substring(q);
			}
		}
		
		String whatis = getBetween(html, "<h2>What is", "/p>");
		whatis = getBetween(whatis, "<p>", "<");
		
		if (Util.isEmpty(h1) || Util.isEmpty(main) || Util.isEmpty(genericName))
		{
			return null;
		}
		
		// Create the drug
		Drug drug = new Drug();
		drug.setName(h1);
		drug.setGenericName(genericName);
		drug.setDescription(whatis);
		drug.setInformation(main);
		
		save(drug);
		
		return drug;
	}
	
	private String getBetween(String html, String open, String close)
	{
		if (html==null) return null;
		
		int p = html.indexOf(open);
		if (p<0) return null;
		p += open.length();
		int q = html.indexOf(close, p);
		if (q<0) return null;
		return html.substring(p, q);
	}

	/**
	 * Checks whether two drugs have interactions.
	 * @param drugID1
	 * @param drugID2
	 * @return
	 * @throws Exception 
	 */
	public boolean isInteraction(UUID drugID1, UUID drugID2) throws Exception
	{
		if (drugID1.equals(drugID2))
		{
			// Drug does not interact with itself
			return false;
		}
			
		Drug d1 = load(drugID1);
		Drug d2 = load(drugID2);
		
		if (d1.getPatientID()!=null || d2.getPatientID()!=null)
		{
			// User defined drugs have no interactions
			return false;
		}
		
		String info1 = d1.getDrugInteractionInformation();
		if (info1!=null)
		{
			if (info1.indexOf(d2.getName())>=0 || info1.indexOf(d2.getGenericName())>=0)
			{
				return true;
			}
		}
		
		String info2 = d2.getDrugInteractionInformation();
		if (info2!=null)
		{
			if (info2.indexOf(d1.getName())>=0 || info2.indexOf(d1.getGenericName())>=0)
			{
				return true;
			}
		}

		return false;
	}
}
