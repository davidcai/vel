package baby.pages.journey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.Stage;

public class MeasureRecordsPageHelper
{
	private static MeasureRecordsPageHelper instance = new MeasureRecordsPageHelper();

	protected MeasureRecordsPageHelper()
	{
	}

	public final static MeasureRecordsPageHelper getInstance()
	{
		return instance;
	}
	
	/**
	 * Gets measure record value that is normalized by either metric or imperial unit system. 
	 * 
	 * @param rec
	 * @param metric
	 * @return
	 * @throws Exception
	 */
	public Float getMeasureRecordValue(MeasureRecord rec, boolean metric) throws Exception
	{
		Float val = rec.getValue();
		if (val != null)
		{
			Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
			
			// Convert value from record's current unit system to mother's unit system
			if (metric && rec.isMetric() == false)
			{
				val = m.toMetric(val);
			}
			else if (metric == false && rec.isMetric())
			{
				val = m.toImperial(val);
			}
		}
		
		return val;
	}
	
	/**
	 * Returns measures that are suitable for the specified stage.
	 * 
	 * @param measureIDs
	 * @param stage
	 * @return
	 * @throws Exception
	 */
	public List<UUID> filterMeasuresByStage(List<UUID> measureIDs, Stage stage) throws Exception
	{
		List<UUID> filteredMeasureIDs = new ArrayList<UUID>();
		
		for (UUID measureID : measureIDs)
		{
			Measure m = MeasureStore.getInstance().load(measureID);
			
			if ((m.isForPreconception() && stage.isPreconception()) || 
				(m.isForPregnancy() && stage.isPregnancy()) || 
				(m.isForInfancy() && stage.isInfancy())) 
			{
				filteredMeasureIDs.add(measureID);
			}
		}
		
		return filteredMeasureIDs;
	}
	
	/**
	 * Creates a list of eligible measure records for the mother.
	 * 
	 * @param page
	 * @param mom
	 * @param cal
	 * @return
	 * @throws Exception
	 */
	public List<MeasureRecord> createMeasureRecordsForMom(WebPage page, Mother mom, Calendar cal) throws Exception
	{
		List<MeasureRecord> records = new ArrayList<MeasureRecord>();
		
		UUID userID = page.getContext().getUserID();
		Stage stage = mom.getEstimatedPregnancyStage(cal.getTime(), cal.getTimeZone());
		List<UUID> measureIDs = filterMeasuresByStage(MeasureStore.getInstance().getAll(true), stage);
		for (UUID measureID : measureIDs)
		{
			Measure m = MeasureStore.getInstance().load(measureID);
			MeasureRecord rec = new MeasureRecord();
			rec.setUserID(userID);
			rec.setMeasureID(measureID);
			rec.setCreatedDate(cal.getTime());
			
			// By default, use the unit system defined in mother's profile
			rec.setMetric(mom.isMetric());
			
			records.add(rec);
		}
		
		return records;
	}
	
	/**
	 * Creates a map of baby IDs and associated measure records. 
	 * 
	 * @param page
	 * @param mom
	 * @param cal
	 * @return
	 * @throws Exception
	 */
	public Map<UUID, List<MeasureRecord>> createMeasureRecordsForBabies(WebPage page, Mother mom, Calendar cal) throws Exception
	{
		Map<UUID, List<MeasureRecord>> recordsByBaby = new LinkedHashMap<UUID, List<MeasureRecord>>();
		
		UUID userID = page.getContext().getUserID();
		List<UUID> babyIDs = BabyStore.getInstance().getByUser(userID); // Sorted by baby names
		Stage stage = mom.getEstimatedPregnancyStage(cal.getTime(), cal.getTimeZone());
		List<UUID> measureIDs = filterMeasuresByStage(MeasureStore.getInstance().getAll(false), stage);
		for (UUID measureID : measureIDs)
		{
			// Duplicate measure records for each baby
			for (UUID babyID : babyIDs)
			{
				MeasureRecord rec = new MeasureRecord();
				rec.setUserID(userID);
				rec.setBabyID(babyID);
				rec.setMeasureID(measureID);
				rec.setCreatedDate(cal.getTime());

				// By default, use unit system defined in mother's profile
				rec.setMetric(mom.isMetric());
				
				List<MeasureRecord> records = recordsByBaby.get(babyID);
				if (records == null)
				{
					records = new ArrayList<MeasureRecord>();
					recordsByBaby.put(babyID, records);
				}
				records.add(rec);
			}
		}
		
		return recordsByBaby;
	}
	
	public void writeMeasureRecords(WebPage page, Mother mom, 
		List<MeasureRecord> momRecords, Map<UUID, List<MeasureRecord>> babyRecords, String paramVal, String paramID) throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(page);
		
		// Mother records
		if (momRecords.isEmpty() == false)
		{
			twoCol.writeSubtitleRow(UserStore.getInstance().load(mom.getUserID()).getDisplayName());
			
			for (MeasureRecord rec : momRecords)
			{
				writeMeasureRecord(twoCol, rec, mom.isMetric(), paramVal, paramID);
			}
		}
		
		// Baby records
		if (babyRecords.isEmpty() == false)
		{
			for (UUID babyID : babyRecords.keySet())
			{
				Baby baby = BabyStore.getInstance().load(babyID);
				if (baby != null)
				{
					String name = (Util.isEmpty(baby.getName())) ? page.getString("journey:MeasureRecords.Anonymous") : baby.getName();
					twoCol.writeSubtitleRow(name);
					
					List<MeasureRecord> records = babyRecords.get(babyID);
					for (MeasureRecord rec : records)
					{
						writeMeasureRecord(twoCol, rec, mom.isMetric(), paramVal, paramID);
					}
				}
			}
		}
		
		twoCol.render();
	}
	
	public void writeMeasureRecord(TwoColFormControl twoCol, MeasureRecord rec, boolean metric, String paramVal, String paramID) throws Exception
	{
		Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
		
		twoCol.writeRow(m.getLabel());

		Float min = metric ? m.getMetricMin() : m.getImperialMin();
		Float max = metric ? m.getMetricMax() : m.getImperialMax();
		Float val = getMeasureRecordValue(rec, metric);
		
		twoCol.writeDecimalInput(getMeasureRecordFieldKey(paramVal, rec), val, 16, min, max);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(metric ? m.getMetricUnit() : m.getImperialUnit());
		twoCol.writeHiddenInput(getMeasureRecordFieldKey(paramID, rec), rec.getID().toString());
	}
	
	/**
	 * Field key = prefix + user ID + measure ID.
	 * 
	 * @param prefix
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private String getMeasureRecordFieldKey(String prefix, MeasureRecord rec) throws Exception
	{
		StringBuilder sb = new StringBuilder(prefix);
		
		Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
		if (m.isForMother())
		{
			sb.append(rec.getUserID().toString());
		}
		else
		{
			sb.append(rec.getBabyID().toString());
		}
		
		sb.append(rec.getMeasureID());
		
		return sb.toString();
	}
}
