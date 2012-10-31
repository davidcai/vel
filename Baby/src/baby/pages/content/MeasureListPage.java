package baby.pages.content;

import java.util.List;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.servlet.exc.WebFormException;
import baby.database.Measure;
import baby.database.MeasureStore;
import baby.database.UnitType;
import baby.database.UnitTypeStore;
import baby.pages.BabyPage;

public class MeasureListPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure-list";
	
	public static final String PARAM_IDS = "mIDs";
	public static final String PARAM_ID_PREFIX = "mID_";
	public static final String PARAM_LABEL_PREFIX = "mLabel_";
	public static final String PARAM_UNIT_TYPE_PREFIX = "mUnitType_";
	public static final String PARAM_FOR_MOM_PREFIX = "mForMom_";
	public static final String PARAM_PRECONCEPTION_PREFIX = "mPreconception_";
	public static final String PARAM_PREGNANCY_PREFIX = "mPregnacy_";
	public static final String PARAM_INFANCY_PREFIX = "mInfancy_";
	public static final String PARAM_MIN_VALUE_PREFIX = "mMinValue_";
	public static final String PARAM_MAX_VALUE_PREFIX = "mMaxValue_";
	public static final String PARAM_DEF_VALUE_PREFIX = "mDefValue_";
	public static final String PARAM_SAVE = "save";
	
	@Override
	public void validate() throws Exception
	{
		int count = getParameterInteger(PARAM_IDS);
		for (int i = 0; i < count; i++)
		{
			if (isParameter(PARAM_ID_PREFIX + i))
			{
				validateParameterString(PARAM_LABEL_PREFIX + i, 1, Measure.MAXSIZE_LABEL);
				if (Util.isEmpty(getParameterTypeAhead(PARAM_UNIT_TYPE_PREFIX + i).getKey()))
				{
					throw new WebFormException(PARAM_UNIT_TYPE_PREFIX + i, getString("common:Errors.InvalidValue"));
				}
			}
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write("<br>");
		writeFormOpen();
		
		List<UUID> mIDs = MeasureStore.getInstance().getAll();
		new ControlArray<UUID>(this, PARAM_IDS, mIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID id) throws Exception
			{
				TwoColFormControl twoCol = new TwoColFormControl(this);
				
				Measure m = MeasureStore.getInstance().load(id);
				
				// Label
				twoCol.writeRow(getString("content:MeasureList.Measure"));
				twoCol.writeTextInput(PARAM_LABEL_PREFIX + rowNum, 
					m == null ? null : m.getLabel(), 20, Measure.MAXSIZE_LABEL);
				
				// For mom
				twoCol.write("&nbsp;");
				twoCol.writeCheckbox(PARAM_FOR_MOM_PREFIX + rowNum, getString("content:MeasureList.ApplyToMom"), 
					m == null ? false : m.isForMother());
				
				// Stages
				twoCol.write("&nbsp;");
				twoCol.writeCheckbox(PARAM_PRECONCEPTION_PREFIX + rowNum, getString("content:MeasureList.Preconception"), 
					m == null ? false : m.isForPreconception());
				twoCol.write("&nbsp;");
				twoCol.writeCheckbox(PARAM_PREGNANCY_PREFIX + rowNum, getString("content:MeasureList.Pregnancy"), 
					m == null ? false : m.isForPregnancy());
				twoCol.write("&nbsp;");
				twoCol.writeCheckbox(PARAM_INFANCY_PREFIX + rowNum, getString("content:MeasureList.Infancy"), 
					m == null ? false : m.isForInfancy());
				
								
				// Unit type and values
				twoCol.writeRow(getString("content:MeasureList.Units"));
				UUID utID = null;
				String utLabel = null;
				if (m != null)
				{
					utID = m.getUnitTypeID();
					if (utID != null)
					{
						UnitType ut = UnitTypeStore.getInstance().load(utID);
						if (ut != null)
						{
							utLabel = ut.getImperialLabel() + "/" + ut.getMetricLabel();
						}
					}
				}
				twoCol.writeTypeAheadInput(PARAM_UNIT_TYPE_PREFIX + rowNum, 
					utID, utLabel, 10, UnitType.MAXSIZE_LABEL, getPageURL(UnitTypeTypeAhead.COMMAND));
				twoCol.write("&nbsp;");
				twoCol.writeEncode(getString("content:MeasureList.MinValue"));
				twoCol.write("&nbsp;");
				twoCol.writeNumberInput(PARAM_MIN_VALUE_PREFIX + rowNum, 
					m == null ? Measure.MINVALUE : m.getMinValue(), 5, Measure.MINVALUE, Measure.MAXVALUE);
				twoCol.write("&nbsp;");
				twoCol.writeEncode(getString("content:MeasureList.MaxValue"));
				twoCol.write("&nbsp;");
				twoCol.writeNumberInput(PARAM_MAX_VALUE_PREFIX + rowNum, 
					m == null ? Measure.MAXVALUE : m.getMaxValue(), 5, Measure.MINVALUE, Measure.MAXVALUE);
				twoCol.write("&nbsp;");
				twoCol.writeEncode(getString("content:MeasureList.DefValue"));
				twoCol.write("&nbsp;");
				twoCol.writeNumberInput(PARAM_DEF_VALUE_PREFIX + rowNum, 
					m == null ? Measure.MINVALUE : m.getDefValue(), 5, Measure.MINVALUE, Measure.MAXVALUE);
				twoCol.write("&nbsp;");
				
				
				twoCol.writeHiddenInput(PARAM_ID_PREFIX + rowNum, 
					m == null ? "" : m.getID().toString());
				
				twoCol.writeSpaceRow();
				
				twoCol.render();
			}
		}.render();
		write("<br>");
		
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:MeasureList.Title");
	}
}
