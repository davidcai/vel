package baby.pages.journey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.database.UserStore;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.RedirectException;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.database.Measure;
import baby.database.MeasureRecord;
import baby.database.MeasureRecordStore;
import baby.database.MeasureStore;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class MeasureRecordsPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/measurerecords";
	
	public final static String PARAM_DATE = "dt";
	
	private final static String PARAM_VALUE_PREFIX = "value_";
	private final static String PARAM_ID_PREFIX = "id_";
	private final static String PARAM_SAVE = "save";
	private final static String PARAM_REMOVE = "remove";
	
	private Mother mom;
	private List<MeasureRecord> momRecords = new ArrayList<MeasureRecord>();
	private Map<UUID, List<MeasureRecord>> babyRecords = new LinkedHashMap<UUID, List<MeasureRecord>>();
	private boolean newRecords;
	
	@Override
	public void init() throws Exception
	{
		UUID userID = getContext().getUserID();
		this.mom = MotherStore.getInstance().loadByUserID(userID);
		
		Date date = getParameterDate(PARAM_DATE);
		if (date != null)
		{
			//
			// Edit existing records
			//
			
			this.newRecords = false;
			
			List<UUID> recordIDs = MeasureRecordStore.getInstance().getByDate(userID, date);
			for (UUID recordID : recordIDs)
			{
				MeasureRecord rec = MeasureRecordStore.getInstance().open(recordID);
				Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
				if (m.isForMother())
				{
					// Mother records
					this.momRecords.add(rec);
				}
				else
				{
					// Baby records
					UUID babyID = rec.getBabyID();
					List<MeasureRecord> records = this.babyRecords.get(babyID);
					if (records == null)
					{
						records = new ArrayList<MeasureRecord>();
						this.babyRecords.put(babyID, records);
					}
					records.add(rec);
				}
			}
		}
		else
		{
			//
			// New records
			//
			
			this.newRecords = true;
			
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			date = cal.getTime();
			
			List<UUID> babyIDs = BabyStore.getInstance().getByUser(userID); // Sorted by baby names
			
			Stage stage = this.mom.getEstimatedPregnancyStage(date, cal.getTimeZone());
			List<UUID> measureIDs = filterByStage(MeasureStore.getInstance().getAll(), stage);
			for (UUID measureID : measureIDs)
			{
				// Mother records
				
				Measure m = MeasureStore.getInstance().load(measureID);
				if (m.isForMother())
				{
					MeasureRecord rec = new MeasureRecord();
					rec.setUserID(userID);
					rec.setMeasureID(measureID);
					rec.setCreatedDate(date);
					
					// By default, use the unit system defined in mother's profile
					rec.setMetric(this.mom.isMetric());
					
					this.momRecords.add(rec);
				}
				else
				{
					// Duplicate measure records for each baby
					
					for (UUID babyID : babyIDs)
					{
						MeasureRecord rec = new MeasureRecord();
						rec.setUserID(userID);
						rec.setBabyID(babyID);
						rec.setMeasureID(measureID);
						rec.setCreatedDate(date);

						// By default, use unit system defined in mother's profile
						rec.setMetric(this.mom.isMetric());
						
						List<MeasureRecord> records = this.babyRecords.get(babyID);
						if (records == null)
						{
							records = new ArrayList<MeasureRecord>();
							this.babyRecords.put(babyID, records);
						}
						records.add(rec);
					}
				}
			}
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		validateMeasureRecords(this.momRecords);
		for (List<MeasureRecord> records : this.babyRecords.values())
		{
			validateMeasureRecords(records);
		}
	}
	
	private void validateMeasureRecords(List<MeasureRecord> records) throws Exception
	{
		for (MeasureRecord rec : records)
		{
			if (isParameterNotEmpty(getFieldKey(PARAM_VALUE_PREFIX, rec)))
			{
				Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
				Float min = this.mom.isMetric() ? m.getMetricMin() : m.getImperialMin();
				Float max = this.mom.isMetric() ? m.getMetricMax() : m.getImperialMax();
				validateParameterDecimal(getFieldKey(PARAM_VALUE_PREFIX, rec), min, max);
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		commitMeasureRecords(this.momRecords);
		for (List<MeasureRecord> records : this.babyRecords.values())
		{
			commitMeasureRecords(records);
		}
		
		throw new RedirectException(ChartsPage.COMMAND, null);
	}
	
	private void commitMeasureRecords(List<MeasureRecord> records) throws Exception
	{
		for (MeasureRecord rec : records)
		{
			rec.setValue(getParameterDecimal(getFieldKey(PARAM_VALUE_PREFIX, rec)));
			
			// Unit system defined in mother's profile always triumph over record's unit system.
			rec.setMetric(this.mom.isMetric());
			
			MeasureRecordStore.getInstance().save(rec);
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		UserAgent ua = getContext().getUserAgent();
		
		if (ua.isSmartPhone() == false)
		{
			writeEncode(getString("journey:MeasureRecords.Help"));
			write("<br><br>");
		}
		
		if (this.momRecords.isEmpty() == false || this.babyRecords.isEmpty() == false)
		{
			writeFormOpen();
			writeMeasureRecords();
			writeHiddenInput(PARAM_DATE, getParameterString(PARAM_DATE));
			write("<br>");
			writeSaveButton(PARAM_SAVE, 
				this.momRecords.isEmpty() ? this.babyRecords.values().iterator().next().get(0) : this.momRecords.get(0));
			if (this.newRecords == false)
			{
				write("&nbsp;");
				writeRemoveButton(PARAM_REMOVE);
			}
			writeFormClose();
		}
		else
		{
			write("<br>");
			writeEncode(getString("journey:MeasureRecords.NoRecords"));
		}
	}
	
	private void writeMeasureRecords() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Mother records
		if (this.momRecords.isEmpty() == false)
		{
			twoCol.writeSubtitleRow(UserStore.getInstance().load(this.mom.getUserID()).getDisplayName());
			
			for (MeasureRecord rec : this.momRecords)
			{
				writeMeasureRecord(rec, twoCol);
			}
		}
		
		// Baby records
		if (this.babyRecords.isEmpty() == false)
		{
			for (UUID babyID : this.babyRecords.keySet())
			{
				Baby baby = BabyStore.getInstance().load(babyID);
				
				String name = (Util.isEmpty(baby.getName())) ? getString("journey:MeasureRecords.Anonymous") : baby.getName();
				twoCol.writeSubtitleRow(name);
				
				List<MeasureRecord> records = this.babyRecords.get(babyID);
				for (MeasureRecord rec : records)
				{
					writeMeasureRecord(rec, twoCol);
				}
			}
		}
		
		twoCol.render();
	}
	
	private void writeMeasureRecord(MeasureRecord rec, TwoColFormControl twoCol) throws Exception
	{
		Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
		
		twoCol.writeRow(m.getLabel());

		Float min = this.mom.isMetric() ? m.getMetricMin() : m.getImperialMin();
		Float max = this.mom.isMetric() ? m.getMetricMax() : m.getImperialMax();
		Float val = getMeasureRecordValue(rec);
		
		if (m.isForMother())
		{
			rec.getBabyID();
		}
		
		twoCol.writeDecimalInput(getFieldKey(PARAM_VALUE_PREFIX, rec), val, 16, min, max);
		twoCol.write("&nbsp;");
		twoCol.writeEncode(this.mom.isMetric() ? m.getMetricUnit() : m.getImperialUnit());
		twoCol.writeHiddenInput(getFieldKey(PARAM_ID_PREFIX, rec), rec.getID().toString());
	}
	
	/**
	 * Gets measure record value that is normalized by mother's preferred unit system.
	 * 
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private Float getMeasureRecordValue(MeasureRecord rec) throws Exception
	{
		Float val = rec.getValue();
		if (val != null)
		{
			Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
			
			// Convert value from record's current unit system to mother's unit system
			if (this.mom.isMetric() && rec.isMetric() == false)
			{
				val = m.toMetric(val);
			}
			else if (this.mom.isMetric() == false && rec.isMetric())
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
	private List<UUID> filterByStage(List<UUID> measureIDs, Stage stage) throws Exception
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
	 * Field key = prefix + user ID + measure ID.
	 * 
	 * @param prefix
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	private String getFieldKey(String prefix, MeasureRecord rec) throws Exception
	{
		StringBuilder sb = new StringBuilder(prefix);
		
		Measure m = MeasureStore.getInstance().load(rec.getMeasureID());
		if (m.isForMother())
		{
			sb.append(this.mom.getUserID().toString());
		}
		else
		{
			sb.append(rec.getBabyID().toString());
		}
		
		sb.append(rec.getMeasureID());
		
		return sb.toString();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:MeasureRecords.Title");
	}
}
