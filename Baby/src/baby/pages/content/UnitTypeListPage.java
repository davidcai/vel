package baby.pages.content;

import java.util.List;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.servlet.exc.RedirectException;
import baby.database.UnitType;
import baby.database.UnitTypeStore;
import baby.pages.BabyPage;

public class UnitTypeListPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/unit-type-list";
	
	public static final String PARAM_IDS = "utIDs";
	public static final String PARAM_ID_PREFIX = "utID_";
	public static final String PARAM_METRIC_PREFIX = "utMetric_";
	public static final String PARAM_IMPERIAL_PREFIX = "utImperial_";
	public static final String PARAM_SAVE = "save";
	
	@Override
	public void validate() throws Exception
	{
		int count = getParameterInteger(PARAM_IDS);
		for (int i = 0; i < count; i++)
		{
			if (isParameter(PARAM_ID_PREFIX + i))
			{
				validateParameterString(PARAM_IMPERIAL_PREFIX + i, 1, UnitType.MAXSIZE_LABEL);
				validateParameterString(PARAM_METRIC_PREFIX + i, 1, UnitType.MAXSIZE_LABEL);
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		int count = getParameterInteger(PARAM_IDS);
		List<UUID> toDelete = UnitTypeStore.getInstance().getAll();
		for (int i = 0; i < count; i++)
		{
			if (isParameter(PARAM_ID_PREFIX + i))
			{
				UUID id = getParameterUUID(PARAM_ID_PREFIX + i);
				
				UnitType ut = null;
				if (id != null)
				{
					ut = UnitTypeStore.getInstance().open(id);
				}
				
				if (ut == null)
				{
					ut = new UnitType();
				}
				else
				{
					toDelete.remove(id);
				}
				
				ut.setImperialLabel(getParameterString(PARAM_IMPERIAL_PREFIX + i));
				ut.setMetricLabel(getParameterString(PARAM_METRIC_PREFIX + i));
				
				UnitTypeStore.getInstance().save(ut);
			}
		}
		
		UnitTypeStore.getInstance().removeMany(toDelete);
		
		throw new RedirectException(COMMAND, null);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		List<UUID> unitTypeIDs = UnitTypeStore.getInstance().getAll();
		
		writeFormOpen();
		
		write("<br>");
		new ControlArray<UUID>(this, PARAM_IDS, unitTypeIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID unitTypeID) throws Exception
			{
				UnitType ut = UnitTypeStore.getInstance().load(unitTypeID);
				writeEncode(getString("content:UnitTypeList.ImperialLabel"));
				write("&nbsp;");
				writeTextInput(PARAM_IMPERIAL_PREFIX + rowNum, 
					ut == null ? null : ut.getImperialLabel(), 16, UnitType.MAXSIZE_LABEL);
				write("&nbsp;");
				writeEncode(getString("content:UnitTypeList.MetricLabel"));
				write("&nbsp;");
				writeTextInput(PARAM_METRIC_PREFIX + rowNum, 
					ut == null ? null : ut.getMetricLabel(), 16, UnitType.MAXSIZE_LABEL);
				writeHiddenInput(PARAM_ID_PREFIX + rowNum, 
					ut == null ? "" : ut.getID().toString());
			}
		}.render();
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:UnitTypeList.Title");
	}
}
