package mind.database;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.core.ParameterList;
import samoyan.core.Util;
import samoyan.database.*;

public class DoseStore extends DataBeanStore<Dose>
{
	private static DoseStore instance = new DoseStore();

	protected DoseStore()
	{
	}
	public final static DoseStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Dose> getBeanClass()
	{
		return Dose.class;
	}	
	
	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Doses");
		
		td.defineCol("PatientID", UUID.class).invariant().ownedBy("Patients");
		td.defineCol("PrescriptionID", UUID.class).invariant().ownedBy("Prescriptions");
		td.defineCol("TakeDate", Date.class);
		td.defineCol("ResolutionDate", Date.class);
		td.defineCol("Resolution", Short.class);
		td.defineCol("SkipReason", String.class).size(0, Dose.MAXSIZE_SKIP_REASON);

		// Deprecated columns
		td.defineCol("SentToMobile", String.class).size(0, User.MAXSIZE_PHONE);
		td.defineCol("ShortCode", Integer.class);
		
		return td;
	}

	// - - -
		
	public List<UUID> getByPrescriptionID(UUID rxID) throws Exception
	{
		return queryByColumn("PrescriptionID", rxID, "TakeDate", true);
	}

	public List<UUID> getByPatient(UUID patientID, Date from, Date to, UUID rxID) throws Exception
	{
		String sql = "SELECT ID FROM Doses WHERE PatientID=?";
		ParameterList params = new ParameterList(patientID);
		if (from!=null)
		{
			sql += " AND TakeDate>?";
			params.plus(from.getTime());
		}
		if (to!=null)
		{
			sql += " AND TakeDate<=?";
			params.plus(to.getTime());
		}
		if (rxID!=null)
		{
			sql += " AND PrescriptionID=?";
			params.plus(Util.uuidToBytes(rxID));
		}
		sql += " ORDER BY TakeDate ASC";
		
		return Query.queryListUUID(sql, params);
	}	
}
