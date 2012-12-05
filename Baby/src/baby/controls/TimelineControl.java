package baby.controls;

import java.util.Map;

import baby.database.Stage;
import samoyan.controls.SelectInputControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.WebPage;

public class TimelineControl
{
	private WebPage out;
	private Stage stage;
	private String stageParamName = "stage";
	private Map<String, String> params = null;
	
	// The stages in the timeline are:
	// Label					Range
	// -----					-----
	// Preconception			*
	// Pregnancy week 6-10		1-9
	// Pregnancy week 10-12		10-15
	// Pregnancy week 16-20		16-19
	// Pregnancy week 20-24		20-23
	// Pregnancy week 24-28		24-29
	// Pregnancy week 30-32		30-35
	// Pregnancy week 36		36-37
	// Pregnancy week 38		38
	// Pregnancy week 39-40		39-40
	// Infancy postpartum		1
	// Infancy month 2			2-3
	// Infancy month 4			4-5
	// Infancy month 6			6-10
	// Infancy month 12			11-12
	private final static String[] labels = {
		"Preconception",
		"Week6",
		"Week10",
		"Week16",
		"Week20",
		"Week24",
		"Week30",
		"Week36",
		"Week38",
		"Week39",
		"Month1",
		"Month2",
		"Month4",
		"Month6",
		"Month12"
	};
	private final static int[] ranges = {
		1, 1,
		101, 109,
		110, 115,
		116, 119,
		120, 123,
		124, 129,
		130, 135,
		136, 137,
		138, 138,
		139, 140,
		201, 201,
		202, 203,
		204, 205,
		206, 210,
		211, 212
	};

	public TimelineControl(WebPage outputPage, Stage stage, String stageParamName)
	{
		this.out = outputPage;
		this.stage = stage;
		this.stageParamName = stageParamName;
	}
	
	public static int getLowRange(int stageInt)
	{
		for (int i=0; i<labels.length; i++)
		{
			int low = ranges[i*2];
			int hi = ranges[i*2+1];
			
			if (stageInt>=low && stageInt<=hi)
			{
				return low;
			}
		}
		return 0;
	}
	public static int getHighRange(int stageInt)
	{
		for (int i=0; i<labels.length; i++)
		{
			int low = ranges[i*2];
			int hi = ranges[i*2+1];
			
			if (stageInt>=low && stageInt<=hi)
			{
				return hi;
			}
		}
		return 0;
	}
	
	public void render()
	{
		ParameterMap urlParams = new ParameterMap();
		if (this.params!=null)
		{
			urlParams.putAll(this.params);
		}
				
		this.out.writeFormOpen("GET", null);
		
		int stageInt = stage.toInteger();
		SelectInputControl select = new SelectInputControl(this.out, this.stageParamName);
		for (int i=0; i<labels.length; i++)
		{
			String label = this.out.getString("baby:TimelineCtrl."+labels[i]);
			int low = ranges[i*2];
			int hi = ranges[i*2+1];
			
			select.addOption(label, low + "-" + hi);
			if (stageInt>=low && stageInt<=hi)
			{
				select.setInitialValue(low + "-" + hi);
			}
		}
		select.setAutoSubmit(true);
		select.render();
		
		if (this.params!=null)
		{
			for (String k : this.params.keySet())
			{
				this.out.writeHiddenInput(k, this.params.get(k));
			}
		}
		
		this.out.writeFormClose();		
	}
}
