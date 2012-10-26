package samoyan.controls;

import java.util.Iterator;
import java.util.List;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public abstract class ViewTableControl<T> extends WebPage
{
	private String name;
	private Iterator<T> initialValues;

	private int totalCells = 0;
	private int columnCount = 0;
	private String cellTag;;
	private boolean openCell = false;
	private int width = 0;
	private int pageSize = 50;
	private boolean shadedRow = true;
	
	public ViewTableControl(WebPage outputPage, String name, List<T> initialValues)
	{
		this(outputPage, name, initialValues.iterator());
	}
	
	public ViewTableControl(WebPage outputPage, String name, Iterator<T> initialValues)
	{
		setContainer(outputPage);
		this.name = name;
		this.initialValues = initialValues;
		
		if (outputPage.getContext().getUserAgent().isSmartPhone())
		{
			this.pageSize = 20;
		}
	}

	protected final void cell(String text)
	{
		closeCell();
		write("<");
		write(this.cellTag);
		write(">");
		if (!Util.isEmpty(text))
		{
			writeEncode(text);
		}
		else
		{
			write("&nbsp;");
		}
		write("</");
		write(this.cellTag);
		write(">");
		this.totalCells++;
	}
	protected final void cell()
	{
		cell(0, null, 1, false);
	}
	protected final void cellAlign(String align)
	{
		cell(0, align, 1, false);
	}
	protected final void cellSpan(int colSpan)
	{
		cell(0, null, colSpan, false);
	}
	protected final void cellWidth(int widthPct)
	{
		cell(widthPct, null, 1, false);
	}
	protected final void cell(int widthPct, String align, int colSpan, boolean noWrap)
	{
		closeCell();
		write("<");
		write(this.cellTag);
		if (widthPct>0 && widthPct<=100)
		{
			write(" width=\"");
			writeEncode(widthPct);
			write("%\"");
		}
		if (align!=null)
		{
			write(" align=");
			writeEncode(align);
		}
		if (noWrap)
		{
			write(" nowrap");
		}
		if (colSpan>1)
		{
			write(" colspan=");
			write(colSpan);
		}
		write(">");
		this.openCell = true;
		this.totalCells += colSpan;
	}
	private final void closeCell()
	{
		if (this.openCell)
		{
			write("</");
			write(this.cellTag);
			write(">");
		}
		this.openCell = false;
	}
	
	/**
	 * Closes the current row and opens a new one. Useful in {@link #renderRow(Object)} when wanting to write more than one row.
	 * Otherwise, this method should not be called. One row is automatically created.
	 */
	protected final void row()
	{
		closeCell();
		if (this.totalCells>0)
		{
			write("</tr>");
		}
		write("<tr");
		if (this.shadedRow)
		{
			write(" class=Shaded");
		}
		write(">");
	}
	
	protected abstract void renderHeaders() throws Exception;
	protected abstract void renderRow(T element) throws Exception;
	protected boolean isRenderRow(T element) throws Exception
	{
		return true;
	}
		
	@Override
	public final void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();

		if (this.initialValues.hasNext()==false)
		{
			return;
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
		write("<tr>");
		this.cellTag = "th";
		this.renderHeaders(); // Call subclass
		this.cellTag = "td";
		write("</tr>");
		this.columnCount = this.totalCells;
		
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
				write("<tr");
				this.shadedRow = !this.shadedRow;
				if (this.shadedRow)
				{
					write(" class=Shaded");
				}
				write(">");
				this.renderRow(elem);
				write("</tr>");
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
			write(this.columnCount);
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
	
	public ViewTableControl<T> setWidth(int widthPct)
	{
		this.width = 100;
		return this;
	}
	public ViewTableControl<T> setPageSize(int numRows)
	{
		this.pageSize = numRows;
		return this;
	}
}
