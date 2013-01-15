package samoyan.controls;

import samoyan.core.Util;
import samoyan.servlet.WebPage;

public class ExpandableTextControl
{
	private WebPage out;
	private String text;
	private int initialDisplaySize;
	private String moreAction;
	private String lessAction;
	
	public ExpandableTextControl(WebPage outputPage)
	{
		this.out = outputPage;
		this.text = "";
		this.initialDisplaySize = 0;
		this.moreAction = null;
		this.lessAction = null;
	}
	
	public ExpandableTextControl setText(String text, int initialDisplaySize)
	{
		this.text = text;
		this.initialDisplaySize = initialDisplaySize;
		return this;
	}
	
	public ExpandableTextControl setLabels(String moreAction, String lessAction)
	{
		this.moreAction = moreAction;
		this.lessAction = lessAction;
		return this;
	}
	
	public void render()
	{
		if (Util.isEmpty(this.text))
		{
			return;
		}
		if (this.initialDisplaySize<=0)
		{
			out.writeEncode(this.text);
			return;
		}
		
		out.write("<span class=ExpandableText>");
		
		out.writeEncode(this.text.substring(0, this.initialDisplaySize));
		
		out.write("<span class=Expander onclick='$(this).hide().next().show().next().show();'> ");
		out.writeEncode(Util.isEmpty(this.moreAction)? out.getString("controls:ExpandableText.More") : this.moreAction);
		out.write("</span>");
		
		out.write("<span style='display:none'>");
		out.writeEncode(this.text.substring(this.initialDisplaySize));
		out.write("</span>");
		
		out.write("<span class=Expander style='display:none' onclick='$(this).hide().prev().hide().prev().show();'> ");
		out.writeEncode(Util.isEmpty(this.lessAction)? out.getString("controls:ExpandableText.Less") : this.lessAction);
		out.write("</span>");
		
		out.write("</span>");
	}
}
