package samoyan.controls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public abstract class DataTableControl<T> extends WebPage
{
	public class Column
	{
		private String label = null;
		private String image = null;
		private WebPage webPage = null;
		private String align = null;
		private String alignHeader = null;
		private boolean noWrap = false;
		private boolean noWrapHeader = false;
		private int width = 0;
		
		public Column(String label)
		{
			this.label = label;
		}
		public Column align(String align)
		{
			this.align = align;
			return this;
		}
		public Column alignHeader(String align)
		{
			this.alignHeader = align;
			return this;
		}
		public Column noWrap()
		{
			this.noWrap = true;
			return this;
		}
		public Column noWrapHeader()
		{
			this.noWrapHeader = true;
			return this;
		}
		public Column width(int pct)
		{
			this.width = pct;
			return this;
		}
		public Column html(WebPage webPage)
		{
			this.webPage = webPage;
			return this;
		}
		public Column image(String image)
		{
			this.image = image;
			return this;
		}
		
		private void writeColumnHeader() throws Exception
		{
			write("<th");
			if (this.alignHeader!=null && horizontal==true)
			{
				write(" align=");
				writeEncode(this.alignHeader);
			}
			if (this.width>0 && this.width<=100 && horizontal==true)
			{
				write(" width=\"");
				write(this.width);
				write("%\"");
			}
			if (this.noWrapHeader)
			{
				write(" nowrap");
			}
			write(">");
			if (this.webPage!=null)
			{
				this.webPage.setContainer(getContainer()); // The container of the parent web page
				this.webPage.render();
			}
			else if (this.image!=null)
			{
				writeImage(this.image, this.label);
			}
			else if (Util.isEmpty(this.label))
			{
				write("&nbsp;");
			}
			else
			{
				writeEncode(this.label);
			}
			write("</th>");
		}
		private void writeCellOpen()
		{
			write("<td");
			if (this.align!=null)
			{
				write(" align=");
				writeEncode(this.align);
			}
			if (this.noWrap)
			{
				write(" nowrap");
			}
			write(">");
		}
	}
	private List<Column> columns = new ArrayList<Column>();
	
	private String name;
	private Iterator<T> initialValues;
	private int pageSize = 0;
	private int width = 0;
	private Boolean horizontal = null;
	private boolean shadedRow = true;
	private int columnIndex = 0;
	private boolean openTR = false;
	private boolean openTD = false;
	
	public DataTableControl(WebPage outputPage, String name, List<T> initialValues)
	{
		this(outputPage, name, initialValues.iterator());
	}
	
	public DataTableControl(WebPage outputPage, String name, Iterator<T> initialValues)
	{
		setContainer(outputPage);
		this.name = name;
		this.initialValues = initialValues;		
	}

	public DataTableControl<T> setWidth(int widthPct)
	{
		this.width = 100;
		return this;
	}
	public DataTableControl<T> setPageSize(int numRows)
	{
		this.pageSize = numRows;
		return this;
	}

	protected Column column(String label)
	{
		Column col = new Column(label);
		this.columns.add(col);
		return col;
	}
	
	protected void cell() throws Exception
	{
		if (this.openTD)
		{
			write("</td>");
		}
		
		Column col = this.columns.get(this.columnIndex);
		
		if (this.horizontal && this.columnIndex==0)
		{
			if (this.openTR)
			{
				write("</tr>");
			}
			write("<tr");
			if (this.shadedRow)
			{
				write(" class=Shaded");
			}
			write(">");
			this.openTR = true;
		}
		
		if (this.horizontal==false)
		{
			if (this.openTR)
			{
				write("</tr>");
			}
			write("<tr");
			if (this.shadedRow)
			{
				write(" class=Shaded");
			}
			write(">");
			this.openTR = true;

			col.writeColumnHeader();
		}
		
		col.writeCellOpen();
		this.openTD = true;
		
		this.columnIndex++;
	}
	
	// - - -
	
	protected abstract void defineColumns() throws Exception;
	protected abstract void renderRow(T element) throws Exception;
	protected boolean isRenderRow(T element) throws Exception
	{
		return true;
	}
	
	// - - -
	
	@Override
	public final void renderHTML() throws Exception
	{
		if (this.initialValues.hasNext()==false)
		{
			return;
		}
		
		// Call subclass to define the columns of this table
		this.defineColumns();
		
		// Set default rendering values
		if (this.getContainer().getContext().getUserAgent().isSmartPhone())
		{
			// Smartphone
			if (this.pageSize<=0)
			{
				this.pageSize = 20;
			}
			if (this.horizontal==null)
			{
				this.horizontal = this.columns.size()<=3;
			}
		}
		else
		{
			// Others
			if (this.pageSize<=0)
			{
				this.pageSize = 50;
			}
			if (this.horizontal==null)
			{
				this.horizontal = true;
			}
		}
		
		write("<table class=ViewTable");
		if (this.width>0 && this.width<=100)
		{
			write(" width=\"");
			write(this.width);
			write("%\"");
		}
		write(">");
		
		// Write header row
		if (this.horizontal==true)
		{
			write("<tr>");
			for (Column col : this.columns)
			{
				col.writeColumnHeader();
			}
			write("</tr>");
		}
		else
		{
			write("<tr><td colspan=2></td></tr>");
		}
		
		// Cursor
		int cursor = 0;
		Integer cursorParam = getParameterInteger(this.name);
		if (cursorParam!=null)
		{
			cursor = cursorParam;
		}
		
		// Write rows
		int prevPage = -1;
		int at = 0;
		int printed = 0;
		int PAGE_SIZE = this.pageSize;
		while (this.initialValues.hasNext() && printed<PAGE_SIZE)
		{
			T elem = this.initialValues.next();
			if (!isRenderRow(elem)) continue;

			if (at<cursor)
			{
				if (at%PAGE_SIZE==0)
				{
					prevPage = at;
				}
			}
			else // (at>=cursor)
			{
				this.shadedRow = !this.shadedRow;
				this.columnIndex = 0;
				this.renderRow(elem);
				
				if (this.openTD)
				{
					write("</td>");
					this.openTD = false;
				}
				if (this.openTR)
				{
					write("</tr>");
					this.openTR = false;
				}
				
				printed ++;
			}
			at++;				
		}

		// Write paging row
//		if (this.paging && this.initialValues.hasNext() || prevPage>=0)
		{
			// Next page
			write("<tr>");
			write("<td colspan=");
			write(this.horizontal? this.columns.size() : 2);
			write(">");
			if (this.initialValues.hasNext() || prevPage>=0)
			{
				write("<table width=\"100%\"><tr><td>");
				if (prevPage>=0)
				{
//					write("<small>");
					writePaging(getString("controls:ViewTable.PreviousPage"), prevPage);
//					write("</small>");
				}
				else
				{
					write("<span class=Faded>");
					writeEncode(getString("controls:ViewTable.PreviousPage"));
					write("</span>");
				}
				write("</td><td align=right>");
				if (this.initialValues.hasNext())
				{
//					write("<small>");
					writePaging(getString("controls:ViewTable.NextPage"), at);
//					write("</small>");
				}
				else
				{
					write("<span class=Faded>");
					writeEncode(getString("controls:ViewTable.NextPage"));
					write("</span>");
				}
				write("</td></tr></table>");
			}
			write("</td>");
			write("</tr>");
		}
		
		write("</table>");		
	}
	
	private boolean hiddenPrinted = false;
	private void writePaging(String label, int cursor)
	{
		RequestContext ctx = getContext();
		if (ctx.getMethod().equalsIgnoreCase("GET"))
		{
			writeLink(label, getPageURL(ctx.getCommand(), new ParameterMap(ctx.getParameters()).plus(this.name, String.valueOf(cursor))));
		}
		else
		{
			if (hiddenPrinted==false)
			{
				writeHiddenInput(this.name, "");
				hiddenPrinted = true;
			}
			
			writeLink(label, "javascript:vuTblScroll('"+this.name+"',"+cursor+");");
		}
	}
}
