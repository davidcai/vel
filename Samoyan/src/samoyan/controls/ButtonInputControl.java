package samoyan.controls;

import java.util.UUID;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class ButtonInputControl extends InputControl
{
	private Object value = null;
	private boolean mobileHotAction = false;
	private WebPage out = null;
	
	public ButtonInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		this.out = outputPage;
		
		setAttribute("type", "submit");
	}

	/**
	 * Hot action buttons are shown in the title bar area in the mobile UI.
	 * @param b
	 * @return
	 */
	public ButtonInputControl setMobileHotAction(boolean b)
	{
		this.mobileHotAction = b;
		return this;
	}
	public boolean isMobileHotAction()
	{
		return this.mobileHotAction;
	}

	/**
	 * Will show the button is a subdued color (e.g. grey).
	 * @param b
	 * @return
	 */
	public ButtonInputControl setSubdued(boolean b)
	{
		addCssClass("Subdued");
		return this;
	}
	public boolean isSubdued()
	{
		return isCssClass("Subdued");
	}
	
	/**
	 * Will show the button is a strong color (e.g. red).
	 * @param b
	 * @return
	 */
	public ButtonInputControl setStrong(boolean b)
	{
		addCssClass("Strong");
		return this;
	}
	public boolean isStrong()
	{
		return isCssClass("Strong");
	}
	
	/**
	 * If <code>true</code>, marks the button as the default action of the form.
	 * The first default button is triggered when the user pressed enter.
	 * By default, all buttons are marked as "default action" for form.
	 * @param b
	 * @return
	 */
	public ButtonInputControl setDefaultAction(boolean b)
	{
		setAttribute("type", b?"submit":"button");
		return this;
	}
	public boolean isDefaultAction()
	{
		return getAttribute("type").equalsIgnoreCase("submit");
	}

	public ButtonInputControl setValue(Object value)
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
		// Mobile buttons must have IDs
		boolean mobile = out.getContext().getUserAgent().isSmartPhone();
		if (mobile && mobileHotAction && Util.isEmpty(getAttribute("id")))
		{
			setAttribute("id", "btn" + UUID.randomUUID().toString());
		}
		
		writeTag("input");
		
		if (mobile && mobileHotAction)
		{
			out.write("<script type=\"text/javascript\">$(\"<input type='button' value='");
			out.writeEncode(getValue().toString());
			out.write("'>\").appendTo(\"#hotButtons\").click(function(){$('#");
			out.writeEncode(getAttribute("id"));
			out.write("').click();});</script>");
		}
	}
}
