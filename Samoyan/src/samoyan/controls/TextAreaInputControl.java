package samoyan.controls;

import samoyan.servlet.WebPage;

public class TextAreaInputControl extends TextInputControl
{
	private int rows = 0;
	
	public TextAreaInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
	}

	public TextAreaInputControl setCols(int cols)
	{
		super.setSize(cols);
		return this;
	}
	
	public TextAreaInputControl setRows(int rows)
	{
		this.rows = rows;
		
		WebPage out = this.getOutputPage();
		if (rows>1 && out.getContext().getUserAgent().isFirefox())
		{
			rows--;
		}
		setAttribute("rows", String.valueOf(rows));
		return this;
	}
	public int getRows()
	{
		return this.rows;
	}
	
	@Override
	public void render()
	{		
		writeTag("textarea");
		Object v = getCurrentValue();
		if (v!=null)
		{
			writeEncode(v);
		}
		write("</textarea>");
	}
}
