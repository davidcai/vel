package samoyan.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class GoogleGraph
{
	public final static String STRING = "string";
	public final static String NUMBER = "number";
	
	public final static String LINE = "line";
	public final static String BARS = "bars";
	public final static String AREA = "area";

	public final static String AREA_CHART = "AreaChart";
	public final static String BAR_CHART = "BarChart";
	public final static String CANDLESTICK_CHART = "CandlestickChart";
	public final static String COLUMN_CHART = "ColumnChart";
	public final static String COMBO_CHART = "ComboChart";
	public final static String GAUGE_CHART = "GaugeChart";
	public final static String LINE_CHART = "LineChart";
	public final static String PIE_CHART = "PieChart";
	public final static String SCATTER_CHART = "ScatterChart";
	
	public final static String RIGHT = "right";
	public final static String TOP = "top";
	public final static String BOTTOM = "bottom";
	public final static String IN = "in";
	public final static String NONE = "none";

	public class SliceDefinition
	{
		private String color;
		public SliceDefinition()
		{
		}
		
		public String getColor()
		{
			return color;
		}

		public void setColor(String color)
		{
			this.color = color;
		}
	}
	private List<SliceDefinition> sliceDefinitions = null;
	
	public class ColumnDefinition
	{
		private String dataType;
		private String label;
		private int lineWidth = -1;
		private int pointSize = -1;
		private String color;
		private boolean oppositeAxis = false;
		private boolean visibleInLegend = true;
		private String type;
		private float areaOpacity = 1.0F;
		
		public ColumnDefinition(String dataType, String label)
		{
			this.dataType = dataType;
			this.label = label;
		}

		public String getDataType()
		{
			return dataType;
		}

		/**
		 * 
		 * @param type STRING or NUMBER
		 */
		public ColumnDefinition setDataType(String type)
		{
			this.dataType = type;
			return this;
		}

		public String getLabel()
		{
			return label;
		}

		public ColumnDefinition setLabel(String label)
		{
			this.label = label;
			return this;
		}

		public int getLineWidth()
		{
			return lineWidth;
		}

		public ColumnDefinition setLineWidth(int lineWidth)
		{
			this.lineWidth = lineWidth;
			return this;
		}

		public int getPointSize()
		{
			return pointSize;
		}

		public ColumnDefinition setPointSize(int pointSize)
		{
			this.pointSize = pointSize;
			return this;
		}

		public String getColor()
		{
			return color;
		}

		public ColumnDefinition setColor(String color)
		{
			this.color = color;
			return this;
		}

		public boolean isOppositeAxis()
		{
			return oppositeAxis;
		}

		public ColumnDefinition setOppositeAxis(boolean oppositeAxis)
		{
			this.oppositeAxis = oppositeAxis;
			return this;
		}

		public boolean isVisibleInLegend()
		{
			return visibleInLegend;
		}

		public ColumnDefinition setVisibleInLegend(boolean visibleInLegend)
		{
			this.visibleInLegend = visibleInLegend;
			return this;
		}

		public String getType()
		{
			return type;
		}

		/**
		 * 
		 * @param type LINE, BARS or AREA
		 */
		public ColumnDefinition setType(String type)
		{
			this.type = type;
			return this;
		}

		public float getAreaOpacity()
		{
			return areaOpacity;
		}

		public ColumnDefinition setAreaOpacity(float areaOpacity)
		{
			this.areaOpacity = areaOpacity;
			return this;
		}
		
		
	}
	private List<ColumnDefinition> columnDefinitions = null;
	
	private List<List<Object>> rows = null;
	
	public class ChartArea
	{
		private int left = 50;
		private int right = 50;
		private int top = 50;
		private int bottom = 50;
		
		ChartArea()
		{
		}
		public int getLeft()
		{
			return left;
		}
		public void setLeft(int left)
		{
			this.left = left;
		}
		public int getRight()
		{
			return right;
		}
		public void setRight(int right)
		{
			this.right = right;
		}
		public int getTop()
		{
			return top;
		}
		public void setTop(int top)
		{
			this.top = top;
		}
		public int getBottom()
		{
			return bottom;
		}
		public void setBottom(int bottom)
		{
			this.bottom = bottom;
		}
		public void set(int top, int right, int bottom, int left)
		{
			this.top = top;
			this.right = right;
			this.bottom = bottom;
			this.left = left;
		}
	}
	private ChartArea chartArea = new ChartArea();
	
	private int width = 800;
	private int height = 400;
	private String title = "";
	private String chartType = LINE_CHART;
	private String legend = "";
	private int lineWidth = -1;
	private int pointSize = -1;
	private float areaOpacity = 1.0F;
	private boolean runScriptOnLoad = true;
	private static AtomicInteger graphCount = new AtomicInteger(0);
	
	/**
	 * 
	 * @param type The data type of the columns, STRING or NUMBER
	 * @param label The label of the column
	 */
	public ColumnDefinition addColumn(String type, String label)
	{
		if (this.columnDefinitions==null)
		{
			this.columnDefinitions = new ArrayList<ColumnDefinition>();
		}
		ColumnDefinition colDef = new ColumnDefinition(type, label);
		this.columnDefinitions.add(colDef);
		return colDef;
	}
	
	public SliceDefinition addSlice()
	{
		if (this.sliceDefinitions==null)
		{
			this.sliceDefinitions = new ArrayList<SliceDefinition>();
		}
		SliceDefinition sliceDef = new SliceDefinition();
		this.sliceDefinitions.add(sliceDef);
		return sliceDef;
	}

	public void addRows(int rowCount)
	{
		if (this.rows==null)
		{
			this.rows = new ArrayList<List<Object>>(rowCount);
		}
	}
	
	public void addRow(List<Object> rowData)
	{
		if (this.rows==null)
		{
			this.rows = new ArrayList<List<Object>>();
		}
		this.rows.add(rowData);
	}
	
	public void addRow(String labelColumn, Number[] numericColumns)
	{
		List<Object> rowData = new ArrayList<Object>(1 + numericColumns.length);
		rowData.add(labelColumn);
		for (Number n : numericColumns)
		{
			rowData.add(n);
		}
		addRow(rowData);
	}
	public void addRow(Number[] numericColumns)
	{
		List<Object> rowData = new ArrayList<Object>(numericColumns.length);
		for (Number n : numericColumns)
		{
			rowData.add(n);
		}
		addRow(rowData);
	}
	
	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public String getChartType()
	{
		return chartType;
	}

	public void setChartType(String chartType)
	{
		this.chartType = chartType;
	}
	
	public String getLegend()
	{
		return legend;
	}

	public ChartArea getChartArea()
	{
		return chartArea;
	}

	/**
	 * 
	 * @param legend RIGHT, TOP, BOTTOM, IN or NONE.
	 */
	public void setLegend(String legend)
	{
		this.legend = legend;
	}

	public int getLineWidth()
	{
		return lineWidth;
	}

	public void setLineWidth(int lineWidth)
	{
		this.lineWidth = lineWidth;
	}

	public int getPointSize()
	{
		return pointSize;
	}

	public void setPointSize(int pointSize)
	{
		this.pointSize = pointSize;
	}

	public float getAreaOpacity()
	{
		return areaOpacity;
	}

	public void setAreaOpacity(float areaOpacity)
	{
		this.areaOpacity = areaOpacity;
	}

	public boolean isRunScriptOnLoad()
	{
		return runScriptOnLoad;
	}

	/**
	 * Indicates if to defer the executrion of the graph generating script until after the page loads,
	 * or if to run it immediately. The latter is needed for AJAX embedding. 
	 * @param runScriptOnLoad <code>true</code> to use <code>google.setOnLoadCallback</code> to defer execution,
	 * <code>false</code> to run immediately.
	 */
	public void setRunScriptOnLoad(boolean runScriptOnLoad)
	{
		this.runScriptOnLoad = runScriptOnLoad;
	}

	// - - - - -
	
	private WebPage page;
	
	public GoogleGraph(WebPage page)
	{
		this.page = page;
	}
	
	public void render()
	{
		int renderCount = graphCount.incrementAndGet();
		if (renderCount>Integer.MAX_VALUE/2)
		{
			graphCount.set(0);
		}
		
		int w = this.width;
		int h = this.height;
		if (w > this.page.getContext().getUserAgent().getScreenWidth() - 16)
		{
			w = this.page.getContext().getUserAgent().getScreenWidth() - 16;
//			h = h * w / this.width;
		}
		
		page.write("<div id=\"googlegraph");
		page.write(renderCount);
		page.write("\" style=\"width:");
		page.write(w);
		page.write("px;height:");
		page.write(h);
		page.write("px;\"></div>");
		
		if (this.runScriptOnLoad && page.getEphemeral("GoogleGraph:corechart")==null)
		{
			page.write("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>");
			page.write("<script type=\"text/javascript\">");
			page.write("google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});");
			page.write("</script>");
			
			page.setEphemeral("GoogleGraph:corechart", "1");
		}
		
		page.write("<script type=\"text/javascript\">");
		if (this.runScriptOnLoad)
		{
			page.write("google.setOnLoadCallback(drawChart");
			page.write(renderCount);
			page.write(");");
			page.write("function drawChart");
			page.write(renderCount);
			page.write("()");
		}
		page.write("{");
		page.write("var data = new google.visualization.DataTable();");
		
		int c = 0;
		int numCols = this.columnDefinitions.size();
		boolean[] isStringColumn = new boolean[numCols];
		for (ColumnDefinition cd : this.columnDefinitions)
		{
			page.write("data.addColumn('");
			page.write(jsEncode(cd.getDataType()));
			page.write("','");
			if (!Util.isEmpty(cd.getLabel()))
			{
				page.write(jsEncode(cd.getLabel()));
			}
			page.write("');");
			
			isStringColumn[c++] = cd.getDataType().equals(STRING);
		}
		page.write("data.addRows([");
		boolean firstRow = true;
		for (List<Object> r : this.rows)
		{
			if (firstRow==false)
			{
				page.write(",");
			}
			firstRow = false;
			page.write("[");
			for (c=0; c<numCols; c++)
			{
				if (c>0)
				{
					page.write(",");
				}
				if (isStringColumn[c])
				{
					page.write("'");
				}
				if (r.get(c)!=null)
				{
					page.write(r.get(c));
				}
				else if (c==numCols-1 && isStringColumn[c]==false)
				{
					page.write("null");
				}
				if (isStringColumn[c])
				{
					page.write("'");
				}
			}
			
			page.write("]");
		}
		page.write("]);");

		page.write("var chart = new google.visualization.");
		page.write(this.chartType);
		page.write("(document.getElementById('googlegraph");
		page.write(renderCount);
		page.write("'));");
		
		page.write("chart.draw(data,{width:");
		page.write(w);
		page.write(",height:");
		page.write(h);
		if (!Util.isEmpty(this.title))
		{
			page.write(",title:'");
			page.write(jsEncode(this.title));
			page.write("'");
		}
		if (!Util.isEmpty(this.legend))
		{
			page.write(",legend:'");
			page.write(jsEncode(this.legend));
			page.write("'");
		}
		if (this.pointSize>=0)
		{
			page.write(",pointSize:");
			page.write(this.pointSize);
		}
		if (this.lineWidth>=0)
		{
			page.write(",lineWidth:");
			page.write(this.lineWidth);
		}
		if (this.areaOpacity>=0F && this.areaOpacity<1F)
		{
			page.write(",areaOpacity:");
			page.write(String.valueOf(this.areaOpacity));
		}
		
		// Chart area
		page.write(",chartArea:{left:");
		page.write(this.chartArea.left);
		page.write(",top:");
		page.write(this.chartArea.top);
		page.write(",width:");
		page.write(Math.max(0, w - this.chartArea.left - this.chartArea.right));
		page.write(",height:");
		page.write(Math.max(0, h - this.chartArea.top - this.chartArea.bottom));
		page.write("}");
				
		page.write(",series:[");
		int s = 0;
		c = 0;
		for (ColumnDefinition cd : this.columnDefinitions)
		{
			if (isStringColumn[c++])
			{
				continue;
			}
			if (s>0)
			{
				page.write(",");
			}
			page.write("{");
			
			boolean hasVal = false;
			if (cd.visibleInLegend==false)
			{
				page.write("visibleInLegend:false");
				hasVal = true;
			}
			if (!Util.isEmpty(cd.getColor()))
			{
				if (hasVal) page.write(",");
				page.write("color:'");
				page.write(jsEncode(cd.getColor()));
				page.write("'");
				hasVal = true;
			}
			if (cd.pointSize>=0)
			{
				if (hasVal) page.write(",");
				page.write("pointSize:");
				page.write(cd.pointSize);
				hasVal = true;
			}
			if (cd.lineWidth>=0)
			{
				if (hasVal) page.write(",");
				page.write("lineWidth:");
				page.write(cd.lineWidth);
				hasVal = true;
			}
			if (cd.oppositeAxis)
			{
				if (hasVal) page.write(",");
				page.write("targetAxisIndex:1");
				hasVal = true;
			}
			if (this.areaOpacity>=0F && this.areaOpacity<1F)
			{
				if (hasVal) page.write(",");
				page.write("areaOpacity:");
				page.write(String.valueOf(this.areaOpacity));
				hasVal = true;
			}
			if (!Util.isEmpty(cd.getType()))
			{
				if (hasVal) page.write(",");
				page.write("type:'");
				page.write(jsEncode(cd.getType()));
				page.write("'");
				hasVal = true;
			}
			
			page.write("}");
			s++;
		}
		page.write("]");

		if (this.sliceDefinitions!=null && this.sliceDefinitions.size()>0)
		{
			page.write(",slices:[");
			s = 0;
			for (SliceDefinition sd : this.sliceDefinitions)
			{
				if (s>0)
				{
					page.write(",");
				}
				if (sd.getColor()!=null)
				{
					page.write("{color:'");
					page.write(sd.getColor());
					page.write("'}");
				}
				else
				{
					page.write("{}");
				}
				s++;
			}
			page.write("]");
		}
		
		page.write("});");
		page.write("}");
		page.write("</script>");
	}
	
	private static String jsEncode(String s)
	{
		s = Util.strReplace(s, "\"", "\\\"");
		s = Util.strReplace(s, "'", "\\'");
		return s;
	}
}
