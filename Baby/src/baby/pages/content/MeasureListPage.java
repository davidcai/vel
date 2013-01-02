package baby.pages.content;

import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.exc.RedirectException;
import baby.database.Measure;
import baby.database.MeasureStore;
import baby.pages.BabyPage;

public class MeasureListPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_CONTENT + "/measure-list";
	
	public static final String PARAM_ID = "id";
	
	@Override
	public void validate() throws Exception
	{
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		new LinkToolbarControl(this)
			.addLink(getString("content:MeasureList.NewMeasure"), getPageURL(MeasurePage.COMMAND), "icons/standard/pencil-16.png")
			.render();
		
		List<UUID> measureIDs = MeasureStore.getInstance().getAll();
		if (measureIDs.isEmpty())
		{
			writeEncode(getString("content:MeasureList.NoMeasuresDefined"));
			return;
		}
		
		writeFormOpen();
		
		new DataTableControl<UUID>(this, "measures", measureIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1); // checkbox
				column(getString("content:MeasureList.Label"));
				column(getString("content:MeasureList.Unit"));
				column(getString("content:MeasureList.ForMom"));
				column(getString("content:MeasureList.Preconception"));
				column(getString("content:MeasureList.Pregnancy"));
				column(getString("content:MeasureList.Infancy"));
			}

			@Override
			protected void renderRow(UUID id) throws Exception
			{
				Measure m = MeasureStore.getInstance().load(id);
				
				cell();
				writeCheckbox("chk_" + m.getID().toString(), null, false);

				cell();
				writeLink(m.getLabel(), getPageURL(MeasurePage.COMMAND, new ParameterMap(PARAM_ID, m.getID().toString())));
				
				cell();
				writeEncode(getString("content:MeasureList.MetricImperial", m.getMetricUnit(), m.getImperialUnit()));

				cell();
				if (m.isForMother())
				{
					writeEncode(getString("content:MeasureList.Yes"));
				}
				
				cell();
				if (m.isForPreconception())
				{
					writeEncode(getString("content:MeasureList.Yes"));
				}
				
				cell();
				if (m.isForPregnancy())
				{
					writeEncode(getString("content:MeasureList.Yes"));
				}
				
				cell();
				if (m.isForInfancy())
				{
					writeEncode(getString("content:MeasureList.Yes"));
				}
			}
		}.render();
		
		write("<br>");
		writeRemoveButton();
		
		writeFormClose();
	}
	
	@Override
	public void commit() throws Exception
	{
		for (String p : getContext().getParameterNamesThatStartWith("chk_"))
		{
			MeasureStore.getInstance().remove(UUID.fromString(p.substring(4)));
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("content:MeasureList.Title");
	}
}
