package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.servlet.WebPage;

public class SelectInputControl extends TextInputControl
{
	private class Option
	{
		String label;
		String value;
	}
	private List<Option> options;

	public SelectInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		this.options = new ArrayList<Option>();
	}

	public SelectInputControl addOption(String label, Object value)
	{
		Option opt = new Option();
		opt.label = label;
		opt.value = value.toString();
		this.options.add(opt);
		
		return this;
	}
	
	public void render()
	{
		writeTag("select");

		Object v = getCurrentValue();

		for (Option opt : this.options)
		{
			write("<option value=\"");
			writeEncode(opt.value);
			write("\"");
			if (v!=null && v.equals(opt.value))
			{
				write(" selected");
			}
			write(">");
			writeEncode(opt.label);
			write("</option>");
		}
		
		write("</select>");
	}
}
