package mind.database;

import java.sql.SQLException;
import java.util.*;

import samoyan.core.ParameterList;
import samoyan.database.DataBeanStore;
import samoyan.database.Query;
import samoyan.database.TableDef;

public class PrescriptionStore extends DataBeanStore<Prescription>
{
	private static PrescriptionStore instance = new PrescriptionStore();

	protected PrescriptionStore()
	{
	}
	public final static PrescriptionStore getInstance()
	{
		return instance;
	}
	
	@Override
	protected Class<Prescription> getBeanClass()
	{
		return Prescription.class;
	}

	@Override
	protected TableDef defineMapping()
	{
		TableDef td = createTableDef("Prescriptions");
		
		td.defineCol("DrugID", UUID.class).ownedBy("Drugs");
		td.defineCol("PatientID", UUID.class).invariant().ownedBy("Patients");
		td.defineCol("DoctorID", UUID.class);
		td.defineCol("DoctorName", String.class).size(0, Prescription.MAXSIZE_DOCTOR_NAME);
		td.defineCol("Nickname", String.class).size(0, Prescription.MAXSIZE_NICKNAME);
		td.defineCol("Purpose", String.class).size(0, Prescription.MAXSIZE_PURPOSE);
		td.defineCol("Instructions", String.class).size(0, Prescription.MAXSIZE_INSTRUCTIONS);
		td.defineCol("DosesRemaining", Integer.class);
		td.defineCol("NextDoseDate", Date.class);
		td.defineCol("FreqDays", Integer.class);
		td.defineCol("QuarterHourBitmap", byte[].class);
		td.defineCol("DoseInfo", String.class).size(0, Prescription.MAXSIZE_DOSE_INFO);
		
		return td;
	}

	// - - -
	
	/**
	 * Gets the list of prescriptions for the given patient.
	 * @param patientID
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByPatientID(UUID patientID) throws Exception
	{
		return queryByColumn("PatientID", patientID, "NextDoseDate", true);
	}

	/**
	 * Gets the list of prescriptions for the given drug.
	 * @param patientID
	 * @return
	 * @throws Exception 
	 */
	public List<UUID> getByDrugID(UUID drugID) throws Exception
	{
		return queryByColumn("DrugID", drugID, "NextDoseDate", true);
	}

	/**
	 * Returns prescriptions that have doses remaining, and whose next dose date is on or before the indicated date.
	 * @param now The cutoff date, inclusive.
	 * @return
	 * @throws SQLException 
	 */
	public List<UUID> getByDoseDue(Date now) throws SQLException
	{
		return Query.queryListUUID("SELECT ID FROM Prescriptions WHERE NextDoseDate<=? AND DosesRemaining>0", new ParameterList(now));
	}
}
