package samoyan.controls;

import samoyan.servlet.WebPage;

public class RadioButtonInputControl extends InputControl
{
	private String label = null;
	private Object value = null;
	
	public RadioButtonInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "radio");
	}

	public RadioButtonInputControl setLabel(String label)
	{
		this.label = label;
		return this;
	}
	public String getLabel()
	{
		return this.label;
	}
	
	public RadioButtonInputControl setValue(Object value)
	{
		this.value = value;
		if (value!=null)
		{
			setAttribute("value", value.toString());
		}
		else
		{
			setAttribute("value", null);
		}
		return this;
	}
	public Object getValue()
	{
		return this.value;
	}

	@Override
	public void render()
	{
		WebPage out = this.getOutputPage();
		String name = this.getName();

		// Create unique ID for radio control, if it has a label attached
		String id = getID();
		if (id==null && this.label!=null)
		{
			String index = out.getEphemeral("radiobutton");
			if (index==null)
			{
				id = "_radio1";
				out.setEphemeral("radiobutton", "1");
			}
			else
			{
				int i = Integer.parseInt(index) + 1;
				id = "_radio" + i;
				out.setEphemeral("radiobutton", String.valueOf(i));
			}
			setID(id);
		}
		
		// Checked?
		String v = getCurrentValue();
		if (v!=null && this.value!=null && v.equals(this.value.toString()))
		{
			setAttribute("checked", "");
		}
				
		if (out.isFormException(name))
		{
			write("<span class=Error>");
		}
		writeTag("input");
		if (out.isFormException(name))
		{
			write("</span>");
		}

		if (this.label!=null)
		{
			write("<label for=\"");
			writeEncode(id);
			write("\">");
			write("&nbsp;");
			writeEncode(this.label);
			write("</label>");
		}
	}
}
