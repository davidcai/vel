package samoyan.controls;

import samoyan.servlet.WebPage;

public class CheckboxInputControl extends InputControl
{
	private String label = null;
	
	public CheckboxInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "checkbox");
	}

	public CheckboxInputControl setLabel(String label)
	{
		this.label = label;
		return this;
	}
	public String getLabel()
	{
		return this.label;
	}
	
	/**
	 * When this checkbox is clicked, the checkboxes whose names start with the given prefix will be checked/unchecked as well. 
	 * @param prefix
	 * @return
	 */
	public CheckboxInputControl affectAll(String prefix)
	{
		setAttribute("onclick", "$(this).attr('checked')?$('INPUT[name^=\"" + prefix + "\"]').attr('checked','checked'):$('INPUT[name^=\"" + prefix + "\"]').removeAttr('checked');");
		return this;
	}
	
	@Override
	public void render()
	{
		WebPage out = this.getOutputPage();
		String name = this.getName(); // Can be null for dummy checkboxes

		// Create unique ID for checkbox control, if it has a label attached
		String id = getID();
		if (id==null && this.label!=null)
		{
			String index = out.getEphemeral("checkbox");
			if (index==null)
			{
				id = "_check1";
				out.setEphemeral("checkbox", "1");
			}
			else
			{
				int i = Integer.parseInt(index) + 1;
				id = "_check" + i;
				out.setEphemeral("checkbox", String.valueOf(i));
			}
			setID(id);
		}
		
		// Checked?
		boolean checked = false;
		if (name!=null && out.isParameter("_cb_" + name)) // Check shadow var
		{
			checked = out.isParameter(name);
		}
		else
		{
			Object initVal = this.getInitialValue();
			checked = initVal!=null && !initVal.equals(false);
		}
		if (checked)
		{
			setAttribute("checked", "");
		}
		
		if (name!=null && out.isFormException(name))
		{
			write("<span class=Error>");
		}
		writeTag("input");
		if (name!=null && out.isFormException(name))
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
		
		// Shadow hidden var
		if (name!=null)
		{
			write("<input type=hidden name=\"_cb_");
			writeEncode(name);
			write("\" value=1>");
		}
	}
}
