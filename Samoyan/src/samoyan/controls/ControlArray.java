package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.servlet.WebPage;

/**
 * An expandable container for a list of controls. Usage:
 * <pre>
 * new ControlArray(this, "arr", 3)
 * {
 * 	public void writeRow(int rowNum)
 * 	{
 * 		writeTextInput("text_" + rowNum, null, 30, 0);
 * 	}
 * }.render();
 * </pre>
 * @author brian
 *
 */
public abstract class ControlArray<T> extends WebPage
{
	private String name;
	private List<T> initialValues;
	
	/**
	 * 
	 * @param outputPage The page to render this control to.
	 * @param name The name of the control. Call <code>outputPage.getParameterInteger(name)</code> to get
	 * the number of rows submitted. 
	 * @param initialValues The <code>List</code> of row elements to render, each of which is passed to {@link #renderRow(int, Object)}.
	 */
	public ControlArray(WebPage outputPage, String name, List<T> initialValues)
	{
		setContainer(outputPage);
		this.name = name;
		this.initialValues = initialValues;
		if (this.initialValues==null)
		{
			this.initialValues = new ArrayList<T>(0);
		}
	}
	
	/**
	 * Subclasses should override this method to render the content of the row.
	 * Any input boxes MUST have <code>rowNum</code> as part of their name, e.g. "name_1", "name_2", etc.
	 * @param rowNum The row number (zero-based) of the row to render.
	 * This number can be negative to indicate the rendering of the insert row.
	 * It can also be larger than the initial number of rows following form submittal.
	 * @param rowElement The element from the <code>initialValues</code> passed to the constructor matching
	 * this row. It may be <code>null</code> for the insert row or for rows added after form submittal,
	 * in which case this method should render any input controls with default values.
	 * @throws Exception 
	 */
	public abstract void renderRow(int rowNum, T rowElement) throws Exception;
	
	@Override
	public final void renderHTML() throws Exception
	{
		boolean smartPhone = getContext().getUserAgent().isSmartPhone();
		
		int initialSize = this.initialValues.size();
		int size = initialSize;
		Integer postedSize = getParameterInteger(this.name);
		if (postedSize!=null)
		{
			size = postedSize;
		}
		

		// Count visible rows
		int countVisibleRows = 0;
		for (int i=0; i<size; i++)
		{
			if (postedSize==null || isParameter(this.name + "_row_" + i))
			{
				countVisibleRows++;
			}
		}

		// Hidden INPUT must immediately precede the TABLE (Javascript relies on it)
		write("<input type=hidden name=\"");
		writeEncode(this.name);
		write("\" value=\"");
		write(size + (countVisibleRows==0?1:0));
		write("\">");
//		writeHiddenInput(this.name, size + ((postedSize==null && initialSize==0)?1:0));

		write("<table class=CtrlArr id=\"");
		writeEncode(this.name);
		write("\">");
		
		for (int i=0; i<size; i++)
		{
			if (postedSize==null || isParameter(this.name + "_row_" + i))
			{
				write("<tr><td>");
				renderRow(i, i<initialSize? this.initialValues.get(i) : null);
				write("</td><td>");
				write("<span class=RemoveBtn onclick=\"ctrlArrRemoveRow(this);\">");
				if (!smartPhone)
				{
					writeEncode(getString("controls:CtrlArr.Remove"));
				}
				else
				{
					write("&nbsp;");
				}
				write("</span>");
				writeHiddenInput(this.name + "_row_" + i, i);
				write("</td></tr>");
			}
		}
		
		// Visible insert row at the end, if no rows displayed
		if (countVisibleRows==0)
		{
			write("<tr><td>");
			renderRow(size, null);
			write("</td><td>");
			write("<span class=RemoveBtn onclick=\"ctrlArrRemoveRow(this);\">");
			if (!smartPhone)
			{
				writeEncode(getString("controls:CtrlArr.Remove"));
			}
			else
			{
				write("&nbsp;");
			}
			write("</span>");
			writeHiddenInput(this.name + "_row_" + size, size);
			write("</td></tr>");
		}
		
		// Invisible row to use as base for copying
		final int INSERT = -999;
		write("<tr new=");
		write(INSERT);
		write(" style='display:none;'><td>");
		renderRow(INSERT, null);
		write("</td><td>");
		write("<span class=RemoveBtn onclick=\"ctrlArrRemoveRow(this);\">");
		if (!smartPhone)
		{
			writeEncode(getString("controls:CtrlArr.Remove"));
		}
		else
		{
			write("&nbsp;");
		}
		write("</span>");
		writeHiddenInput(this.name + "_row_" + INSERT, INSERT);
		write("</td></tr>");

		write("<tr><td><span class=AddBtn onclick=\"ctrlArrAddRow(this);\">");
		writeEncode(getString("controls:CtrlArr.Add"));
		write("</span></td></tr>");

		write("</table>");
	}
}
