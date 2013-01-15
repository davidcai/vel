package baby.pages.journey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.servlet.WebPage;
import baby.database.BabyStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.Stage;

public class MeasureRecordsPageHelper
{
	/**
	 * Gets measure record value that is normalized by either metric or imperial unit system. 
	 * 
	 * @param rec
	 * @param metric
	 * @return
	 * @throws Exception
	 */
	public static Float getMeasureRecordValue(MeasureRecord rec, boolean metric) throws Exception
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
	public static List<UUID> filterMeasuresByStage(List<UUID> measureIDs, Stage stage) throws Exception
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
	public static List<MeasureRecord> createMeasureRecordsForMom(WebPage page, Mother mom, Calendar cal) throws Exception
	{
		List<MeasureRecord> records = new ArrayList<MeasureRecord>();
		
		UUID userID = page.getContext().getUserID();
		Stage stage = mom.getEstimatedPregnancyStage(cal.getTime(), cal.getTimeZone());
		List<UUID> measureIDs = filterMeasuresByStage(MeasureStore.getInstance().getAll(true), stage);
		for (UUID measureID : measureIDs)
		{
//			Measure m = MeasureStore.getInstance().load(measureID);
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
	public static Map<UUID, List<MeasureRecord>> createMeasureRecordsForBabies(WebPage page, Mother mom, Calendar cal) throws Exception
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
}
