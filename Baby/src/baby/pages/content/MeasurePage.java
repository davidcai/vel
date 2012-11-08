package baby.pages.content;

import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.database.Measure;
import baby.database.MeasureStore;
import baby.pages.BabyPage;

public class MeasurePage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure";
	
	public static final String PARAM_ID = "id";
	public static final String PARAM_LABEL = "label";
	public static final String PARAM_FOR_MOM = "forMom";
	public static final String PARAM_PRECONCEPTION = "preconception";
	public static final String PARAM_PREGNANCY = "pregnancy";
	public static final String PARAM_INFANCY = "infancy";
	public static final String PARAM_METRIC_UNIT = "metricUnit";
	public static final String PARAM_IMPERIAL_UNIT = "imperialUnit";
	public static final String PARAM_METRIC_MIN = "metricMin";
	public static final String PARAM_METRIC_MAX = "metricMax";
	public static final String PARAM_IMPERIAL_MIN = "imperialMin";
	public static final String PARAM_IMPERIAL_MAX = "imperialMax";
	public static final String PARAM_METRIC_TO_IMPERIAL_ALPHA = "metricToImperialAlpha";
	public static final String PARAM_METRIC_TO_IMPERIAL_BETA = "metricToImperialBeta";
	
	public static final String PARAM_SAVE = "save";
	public static final String PARAM_REMOVE = "remove";
	
	private Measure measure;
	
	@Override
	public void init() throws Exception
	{
		if (isParameter(PARAM_ID)) 
		{
			this.measure = MeasureStore.getInstance().open(getParameterUUID(PARAM_ID));
		}
		
		if (this.measure == null)
		{
			this.measure = new Measure();
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			validateParameterString(PARAM_LABEL, 1, Measure.MAXSIZE_LABEL);
			validateParameterString(PARAM_METRIC_UNIT, 1, Measure.MAXSIZE_UNIT);
			validateParameterString(PARAM_IMPERIAL_UNIT, 1, Measure.MAXSIZE_UNIT);
			
			validateParameterDecimal(PARAM_METRIC_MIN, 0f, Measure.MAXVAL_MINMAX);
			validateParameterDecimal(PARAM_METRIC_MAX, 0f, Measure.MAXVAL_MINMAX);
			
			if (getParameterDecimal(PARAM_METRIC_MAX) <= getParameterDecimal(PARAM_METRIC_MIN))
			{
				throw new WebFormException(PARAM_METRIC_MAX, getString("content:Measure.MinGreaterThanMax"));
			}
			
			validateParameterDecimal(PARAM_IMPERIAL_MIN, 0f, Measure.MAXVAL_MINMAX);
			validateParameterDecimal(PARAM_IMPERIAL_MAX, 0f, Measure.MAXVAL_MINMAX);
			
			if (getParameterDecimal(PARAM_IMPERIAL_MAX) <= getParameterDecimal(PARAM_IMPERIAL_MIN))
			{
				throw new WebFormException(PARAM_IMPERIAL_MAX, getString("content:Measure.MinGreaterThanMax"));
			}
			
			String alpha = getParameterString(PARAM_METRIC_TO_IMPERIAL_ALPHA);
			if (Util.isEmpty(alpha))
			{
				throw new WebFormException(PARAM_METRIC_TO_IMPERIAL_ALPHA, getString("common:Errors.MissingField"));
			}
			
			try
			{
				Float.parseFloat(alpha);
			}
			catch (NumberFormatException e)
			{
				throw new WebFormException(PARAM_METRIC_TO_IMPERIAL_ALPHA, getString("common:Errors.InvalidValue"));
			}
			
			String beta = getParameterString(PARAM_METRIC_TO_IMPERIAL_BETA);
			if (Util.isEmpty(beta))
			{
				throw new WebFormException(PARAM_METRIC_TO_IMPERIAL_BETA, getString("common:Errors.MissingField"));
			}
			
			try
			{
				Float.parseFloat(beta);
			}
			catch (NumberFormatException e)
			{
				throw new WebFormException(PARAM_METRIC_TO_IMPERIAL_BETA, getString("common:Errors.InvalidValue"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			this.measure.setLabel(getParameterString(PARAM_LABEL));
			this.measure.setForMother(isParameter(PARAM_FOR_MOM));
			this.measure.setForPreconception(isParameter(PARAM_PRECONCEPTION));
			this.measure.setForPregnancy(isParameter(PARAM_PREGNANCY));
			this.measure.setForInfancy(isParameter(PARAM_INFANCY));
			this.measure.setMetricUnit(getParameterString(PARAM_METRIC_UNIT));
			this.measure.setImperialUnit(getParameterString(PARAM_IMPERIAL_UNIT));
			this.measure.setMetricMin(getParameterDecimal(PARAM_METRIC_MIN));
			this.measure.setMetricMax(getParameterDecimal(PARAM_METRIC_MAX));
			this.measure.setImperialMin(getParameterDecimal(PARAM_IMPERIAL_MIN));
			this.measure.setImperialMax(getParameterDecimal(PARAM_IMPERIAL_MAX));
			this.measure.setMetricToImperialAlpha(Float.parseFloat(getParameterString(PARAM_METRIC_TO_IMPERIAL_ALPHA)));
			this.measure.setMetricToImperialBeta(Float.parseFloat(getParameterString(PARAM_METRIC_TO_IMPERIAL_BETA)));
			
			MeasureStore.getInstance().save(this.measure);
		}
		else if (isParameter(PARAM_REMOVE))
		{
			MeasureStore.getInstance().remove(this.measure.getID());
		}
		
		throw new RedirectException(MeasureListPage.COMMAND, null);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("content:Measure.Label"));
		twoCol.writeTextInput(PARAM_LABEL, this.measure.getLabel(), 32, Measure.MAXSIZE_LABEL);
		twoCol.write("<br>");
		twoCol.writeCheckbox(PARAM_FOR_MOM, getString("content:Measure.ForMom"), this.measure.isForMother());
		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("content:Measure.Stages"));
		twoCol.writeCheckbox(PARAM_PRECONCEPTION, getString("content:Measure.Preconception"), this.measure.isForPreconception());
		twoCol.writeCheckbox(PARAM_PREGNANCY, getString("content:Measure.Pregnancy"), this.measure.isForPregnancy());
		twoCol.writeCheckbox(PARAM_INFANCY, getString("content:Measure.Infancy"), this.measure.isForInfancy());
		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("content:Measure.MetricUnit"));
		twoCol.writeTextInput(PARAM_METRIC_UNIT, this.measure.getMetricUnit(), 16, Measure.MAXSIZE_UNIT);
		
		twoCol.writeRow(getString("content:Measure.ImperialUnit"));
		twoCol.writeTextInput(PARAM_IMPERIAL_UNIT, this.measure.getImperialUnit(), 16, Measure.MAXSIZE_UNIT);
		
		twoCol.writeRow(getString("content:Measure.MetricFormula"));
		twoCol.writeTextInput(PARAM_METRIC_TO_IMPERIAL_BETA, this.measure.getMetricToImperialBeta(), 7, Measure.MAXSIZE_METRIC_TO_IMPERIAL);
		twoCol.write(" * " );
		twoCol.writeEncode(getString("content:Measure.MetricValue"));
		twoCol.write(" + ");
		twoCol.writeTextInput(PARAM_METRIC_TO_IMPERIAL_ALPHA, this.measure.getMetricToImperialAlpha(), 7, Measure.MAXSIZE_METRIC_TO_IMPERIAL);

		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("content:Measure.MetricMin"));
		twoCol.writeDecimalInput(PARAM_METRIC_MIN, this.measure.getMetricMin(), 7, 0f, Measure.MAXVAL_MINMAX);

		twoCol.writeRow(getString("content:Measure.MetricMax"));
		twoCol.writeDecimalInput(PARAM_METRIC_MAX, this.measure.getMetricMax(), 7, 0f, Measure.MAXVAL_MINMAX);

		twoCol.writeRow(getString("content:Measure.ImperialMin"));
		twoCol.writeDecimalInput(PARAM_IMPERIAL_MIN, this.measure.getImperialMin(), 7, 0f, Measure.MAXVAL_MINMAX);

		twoCol.writeRow(getString("content:Measure.ImperialMax"));
		twoCol.writeDecimalInput(PARAM_IMPERIAL_MAX, this.measure.getImperialMax(), 7, 0f, Measure.MAXVAL_MINMAX);
				
		twoCol.render();
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, this.measure);
		
		if (this.measure.isSaved())
		{
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
			
			writeHiddenInput(PARAM_ID, this.measure.getID().toString());
		}
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:Measure.Title");
	}
}
