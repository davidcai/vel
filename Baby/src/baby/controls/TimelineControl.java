package baby.controls;

import java.util.Map;

import baby.database.Stage;
import samoyan.core.ParameterMap;
import samoyan.servlet.WebPage;

public class TimelineControl
{
	private WebPage out;
	private Stage stage;
	private String command = null;
	private String stageParamName = "stage";
	private Map<String, String> params = null;
	
	public TimelineControl(WebPage outputPage, Stage stage)
	{
		this.out = outputPage;
		this.stage = stage;
		this.command = outputPage.getContext().getCommand();
	}
		
	public TimelineControl setCommand(String command, Map<String, String> params)
	{
		this.command = command;
		this.params = params;
		return this;
	}
	
	public TimelineControl setStageParamName(String stageParamName)
	{
		this.stageParamName = stageParamName;
		return this;
	}
	
	public void render()
	{
		ParameterMap urlParams = new ParameterMap();
		if (this.params!=null)
		{
			urlParams.putAll(this.params);
		}
		
		boolean smartPhone = out.getContext().getUserAgent().isSmartPhone();
		if (stage.isPreconception())
		{
			// Nothing to show
		}
		else if (stage.isPregnancy())
		{
			// 40 weeks
			out.write("<div class=TimelineBar>");
			for (int i=1; i<=Stage.MAX_WEEKS; i++)
			{
				out.write("<a href=\"");
				out.write(out.getPageURL(this.command, urlParams.plus(this.stageParamName, String.valueOf(Stage.pregnancy(i).toInteger()))));
				out.write("\"");
				if (i==stage.getPregnancyWeek())
				{
					out.write(" class=Current");
				}
				out.write(">");
				out.writeEncodeLong(i);
				out.write("</a>");
				if (smartPhone && (i==13 || i==26))
				{
					out.write("<br>");
				}
			}
			out.write("</div>");
		}
		else if (stage.isInfancy())
		{
			// 12 months
			out.write("<div class=TimelineBar>");
			for (int i=1; i<=Stage.MAX_MONTHS; i++)
			{
				out.write("<a href=\"");
				out.write(out.getPageURL(this.command, urlParams.plus(this.stageParamName, String.valueOf(Stage.infancy(i).toInteger()))));
				out.write("\"");
				if (i==stage.getInfancyMonth())
				{
					out.write(" class=Current");
				}
				out.write(">");
				out.writeEncodeLong(i);
				out.write("</a>");
			}
			out.write("</div>");
		}
	}
}
