package baby.controls;

import baby.database.Stage;

import samoyan.controls.ImageControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.WebPage;

public class TimelineSliderControl
{
	private WebPage out;
	private Stage stage;
	private String stageParamName = "stage";
	
	// The stages in the timeline are:
	// Label					Range
	// -----					-----
	// Preconception			*
	// Pregnancy week 1-6		1-5
	// Pregnancy week 6-10		6-9
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
	// Infancy month 6			6-8
	// Infancy month 9			9-11
	// Infancy month 12			12
	private final static String[] labels = {
		"Preconception",
		"Week1",
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
		"Month9",
		"Month12"
	};
	private final static int[] ranges = {
		1, 1,
		101, 105,
		106, 109,
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
		206, 208,
		209, 211,
		212, 212
	};
	
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

	public TimelineSliderControl(WebPage outputPage, Stage stage, String stageParamName)
	{
		this.out = outputPage;
		this.stage = stage;
		this.stageParamName = stageParamName;
	}
		
	public void render() throws Exception
	{
		int start;
		int end;
		if (this.stage.isPregnancy())
		{
			start = 100;
			end = 200;
		}
		else if (this.stage.isInfancy())
		{
			start = 200;
			end = 300;
		}
		else if (this.stage.isPreconception())
		{
			start = 0;
			end = 100;
		}
		else
		{
			return;
		}
		int stageInt = stage.toInteger();
		boolean phone = out.getContext().getUserAgent().isSmartPhone();
		
		int width = phone? out.getContext().getUserAgent().getScreenWidth()-10 : 600;
		out.write("<table class=Slider style=\"width:");
		out.write(width);
		out.write("px\">");

		// ROW 1 - Stage arrows + Title
		String title = null;
		int stops = 0;
		String prev = null;
		String next = null;
		for (int i=0; i<labels.length; i++)
		{
			int low = ranges[i*2];
			int hi = ranges[i*2+1];
			if (low<start)
			{
				prev = low + "-" + hi;
				continue;
			}
			if (hi>end)
			{
				if (next==null)
				{
					next = low + "-" + hi;
				}
				continue;
			}
			stops ++;
			if (stageInt>=low && stageInt<=hi)
			{
				title = this.out.getString("baby:TimelineCtrl."+labels[i]);
			}
		}
		out.write("<tr><td colspan=");
		out.write(2+stops);
		out.write(" align=center>");
		out.write("<table class=Title><tr><td align=left width=\"33%\" class=Prev>");
			String linkText = null;
			if (phone)
			{
				linkText = "\u00a0"; // &nbsp;
			}
			else if (stage.isPregnancy())
			{
				linkText = this.out.getString("baby:TimelineCtrl.PreconceptionStage");
			}
			else if (stage.isInfancy())
			{
				linkText = this.out.getString("baby:TimelineCtrl.PregnancyStage");
			}
			if (linkText!=null)
			{
				out.writeLink(linkText, this.out.getPageURL(this.out.getContext().getCommand(), new ParameterMap(this.stageParamName, prev)));
			}
		out.write("</td><td align=center width=\"33%\">");
			out.write("<h2>");
			out.writeEncode(title);
			out.write("</h2>");
		out.write("</td><td align=right width=\"33%\" class=Next>");
			linkText = null;
			if (phone)
			{
				linkText = "\u00a0"; // &nbsp;
			}
			else if (stage.isPregnancy())
			{
				linkText = this.out.getString("baby:TimelineCtrl.InfancyStage");
			}
			else if (stage.isPreconception())
			{
				linkText = this.out.getString("baby:TimelineCtrl.PregnancyStage");
			}
			if (linkText!=null)
			{
				out.writeLink(linkText, this.out.getPageURL(this.out.getContext().getCommand(), new ParameterMap(this.stageParamName, next)));
			}
		out.write("</td></tr></table>");
		out.write("</td></tr>");
		
		if (stage.isPregnancy() || stage.isInfancy())
		{
			// ROW 2 - labels for bar
			if (!phone)
			{
				out.write("<tr class=BarLabels><td></td>");
		
				for (int i=0; i<labels.length; i++)
				{
					String shortLabel = this.out.getString("baby:TimelineCtrl."+labels[i]+"Short");
					int low = ranges[i*2];
					int hi = ranges[i*2+1];
					if (low<start || hi>end) continue;
					boolean current = stageInt>=low && stageInt<=hi;
					
					out.write("<td");
					if (current)
					{
						out.write(" class=Current");
					}
					out.write(">");
					if (phone || current)
					{
						out.writeEncode(shortLabel);
					}
					else
					{
						out.writeLink(shortLabel, out.getPageURL(out.getContext().getCommand(), new ParameterMap(this.stageParamName, low + "-" + hi)));
					}
					out.write("</td>");
				}
	
				out.write("<td></td></tr>");
			}
			
			// ROW 3 - bar
			out.write("<tr class=Bar><td class=LeftEnd></td>");
			
			for (int i=0; i<labels.length; i++)
			{
	//			String label = this.out.getString("baby:TimelineCtrl."+labels[i]);
				int low = ranges[i*2];
				int hi = ranges[i*2+1];
				if (low<start || hi>end) continue;
				
				out.write("<td class=Stop href=\"");
				out.writeEncode(out.getPageURL(out.getContext().getCommand(), new ParameterMap(this.stageParamName, low + "-" + hi)));
				out.write("\" style=\"width:");
				out.write((width-20 + stops-1) / stops);
				out.write("px\"");
				if (!phone)
				{
					out.write(" onclick=\"window.location.href=$(this).attr('href');\"");
				}
				out.write(">");
				new ImageControl(this.out).resource("baby/timeline-thumb.png").addCssClass(stageInt>=low && stageInt<=hi?null:"Hide").render();
				out.write("</td>");
			}
					
			out.write("<td class=RightEnd></td></tr>");
		}
		
		out.write("</table>");
	}
}
