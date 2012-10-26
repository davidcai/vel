package samoyan.controls;

import samoyan.servlet.WebPage;

public class TextInputControl extends InputControl
{
	private int width = 0;
	private int maxWidth;
	private boolean autoFocus;
	
	public TextInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "text");
		
		this.maxWidth = outputPage.getContext().getUserAgent().getScreenWidth() - 16; 
		this.autoFocus = true;
	}

	public TextInputControl setSize(int size)
	{
		this.width = Math.min(30+6*size, this.maxWidth);
		setStyleAttribute("width", String.valueOf(this.width) + "px");
		return this;
	}
	public TextInputControl setWidth(int width)
	{
		this.width = Math.min(width, this.maxWidth);
		setStyleAttribute("width", String.valueOf(this.width) + "px");
		return this;
	}
	public int getWidth()
	{
		return this.width;
	}
	
	public TextInputControl setMaxLength(int maxLen)
	{
		setAttribute("maxlength", String.valueOf(maxLen));
		return this;
	}
	public int getMaxLength()
	{
		String str = getAttribute("maxlength");
		return str==null? 0 : Integer.parseInt(str);
	}

	public TextInputControl setPlaceholder(String placeholder)
	{
		setAttribute("placeholder", placeholder);
		return this;
	}
	public String getPlaceholder()
	{
		return getAttribute("placeholder");
	}

	public TextInputControl setAlign(String align)
	{
		setStyleAttribute("text-align", align);
		return this;
	}
	public String getAlign()
	{
		return getStyleAttribute("text-align");
	}

	public InputControl setAutoFocus(boolean b)
	{
		this.autoFocus = b;
		return this;
	}
	public boolean isAutoFocus()
	{
		return this.autoFocus;
	}
		
	@Override
	public void render()
	{
		Object v = getCurrentValue();
		if (v!=null)
		{
			setAttribute("value", v.toString());
		}
		else
		{
			// To prevent certain IE9 problem when no initial value is set
			// (field is cleared when pressing Submit)
			setAttribute("value", "");
		}
		
		// Autofocus
		if (this.isAutoFocus())
		{
			WebPage out = this.getOutputPage();
			if (out.getEphemeral("autofocus")==null)
			{
				setAttribute("autofocus", "");
				out.setEphemeral("autofocus", "1");
			}
		}
		
		writeTag("input");
	}
}
