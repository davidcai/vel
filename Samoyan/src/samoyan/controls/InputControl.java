package samoyan.controls;

import samoyan.servlet.WebPage;

public abstract class InputControl extends TagControl
{
	private WebPage outputPage;
	private Object initialValue;
	private boolean autoSubmit;
		
	protected InputControl(WebPage outputPage, String name)
	{
		super(outputPage);
		this.outputPage = outputPage;

		this.initialValue = null;
		this.autoSubmit = false;

		setName(name);		
	}
	
	protected WebPage getOutputPage()
	{
		return this.outputPage;
	}
	
	public InputControl setName(String name)
	{
		setAttribute("name", name);
		return this;
	}
	public String getName()
	{
		return getAttribute("name");
	}

	public InputControl setID(String id)
	{
		setAttribute("id", id);
		return this;
	}
	public String getID()
	{
		return getAttribute("id");
	}

	public InputControl setAutoSubmit(boolean b)
	{
		this.autoSubmit = b;
		if (b)
		{
			setAttribute("onchange", "$(this).parents('FORM').first().submit();");
		}
		else
		{
			setAttribute("onchange", null);
		}
		return this;
	}
	public boolean isAutoSubmit()
	{
		return this.autoSubmit;
	}
	
	public InputControl setInitialValue(Object val)
	{
		this.initialValue = val;
		return this;
	}
	public Object getInitialValue()
	{
		return this.initialValue;
	}
	public String getCurrentValue()
	{
		WebPage out = getOutputPage();
		String value = out.getContext().getParameter(this.getName());
		Object initialValue = this.getInitialValue();
		if (value==null && initialValue!=null)
		{
			value = initialValue.toString();
		}
		return value;
	}
	
	public InputControl setDisabled(boolean b)
	{
		if (b)
		{
			setAttribute("disabled", "1");
		}
		else
		{
			setAttribute("disabled", null);
		}
		return this;
	}
	public boolean isDisabled()
	{
		return getAttribute("disabled")!=null;
	}

// Inline validation design pattern is currently not enabled. It needs more more work before it can be enabled system-wide.	
//	public InputControl setRequired(boolean b)
//	{
//		if (b)
//		{
//			setAttribute("mandatory", "1");
//		}
//		else
//		{
//			setAttribute("mandatory", null);
//		}
//		return this;
//	}
//	public boolean isRequired()
//	{
//		return getAttribute("mandatory")!=null;
//	}
//
//	public InputControl setRegExp(String regExp)
//	{
//		setAttribute("regexp", regExp);
//		return this;
//	}
//	public String getRegExp()
//	{
//		return getAttribute("regexp");
//	}
	
	protected void writeTag(String tagName)
	{
		WebPage out = getOutputPage();
		if (out.isFormException(this.getName()))
		{
			this.addCssClass("Error");
		}

		super.writeTag(tagName);
	}
	
	protected void write(Object s)
	{
		this.getOutputPage().write(s);
	}
	protected void writeEncode(Object s)
	{
		this.getOutputPage().writeEncode(s);
	}	
}
