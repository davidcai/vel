package samoyan.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import samoyan.core.Util;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public class ActionListControl extends WebPage
{
	private class ActionSpec
	{
		public String image = null;
		public String action = null;
		public String description = null;
	}
	
	private boolean hasImages = false;
	private boolean hasDescriptions = false;
	private boolean hasFreeText = false;
	private boolean hasButtons = false;
	private List<ActionSpec> actions = new ArrayList<ActionSpec>();
	private String prompt = null;
	private boolean readOnly = false;
	private String command = null;
	private Map<String, String> params = null;
	
	public ActionListControl(WebPage outputPage)
	{
		setContainer(outputPage);
		
		this.command = getContext().getCommand();
		this.params = getContext().getParameters();
	}
	
	public ActionListControl addAction(String action, String description)
	{
		return addAction(null, action, description);
	}
	
	public ActionListControl addAction(String image, String action, String description)
	{
		ActionSpec spec = new ActionSpec();
		spec.image = image;
		spec.action = action;
		spec.description = description;
		actions.add(spec);
		
		this.hasImages = this.hasImages || (image!=null);
		this.hasDescriptions = this.hasDescriptions || (description!=null);
		this.hasFreeText = this.hasFreeText || (action==null);
		this.hasButtons = this.hasButtons || (action!=null);
		return this;
	}
	
	public ActionListControl setPrompt(String prompt)
	{
		this.prompt = prompt;
		return this;
	}

	public ActionListControl readOnly(boolean b)
	{
		this.readOnly = b;
		return this;
	}

	public ActionListControl setPostAction(String command, Map<String, String> params)
	{
		this.command = command;
		this.params = params;
		return this;
	}

	@Override
	public void renderHTML() throws Exception
	{
		// Get unique number for this action list in the page
		String indexStr = getEphemeral("actionlist.count");
		if (indexStr==null) indexStr = "0";
		setEphemeral("actionlist.count", String.valueOf(Integer.parseInt(indexStr)+1));
		
		if (!Util.isEmpty(this.prompt))
		{
			write("<b>");
			writeEncode(this.prompt);
			write("</b><br><br>");
		}
		
		writeFormOpen("POST", this.command);
		
		write("<table>");
		for (ActionSpec spec : this.actions)
		{
			write("<tr>");
//			if (this.hasImages)
//			{
//				write("<td>");
//				if (spec.image!=null)
//				{
//					writeImage(spec.image, spec.text);
//				}
//				write("</td>");
//			}
			
			if (!this.readOnly)
			{
				write("<td>");
				if (this.hasFreeText==false)
				{
					writeButton(RequestContext.PARAM_ACTION, spec.action);
				}
				else if (spec.action!=null)
				{
					writeRadioButton(RequestContext.PARAM_ACTION, null, spec.action, null);
				}
				else
				{
					write("<input id=otherradio");
					write(indexStr);
					write(" type=radio name=\"");
					writeEncode(RequestContext.PARAM_ACTION);
					write("\">");
				}
				write("</td>");
			}
			else if (this.hasDescriptions)
			{
				write("<td><b>");
				if (!Util.isEmpty(spec.action))
				{
					writeEncode(spec.action);
				}
				else
				{
					// !$! write "Other"?
				}
				write("</b></td>");
			}
			
			write("<td>");
			if (spec.action==null && !this.readOnly)
			{
				write("<input type=text size=30 maxlength=128 id=other");
				write(indexStr);
				write(">");
			}
			if (this.hasDescriptions)
			{
				if (!Util.isEmpty(spec.description))
				{
					if (spec.action==null && !this.readOnly)
					{
						write("<br>");
					}
					writeEncode(spec.description);
				}
			}
			else if (!Util.isEmpty(spec.action))
			{
				writeEncode(spec.action);
			}
			write("</td>");
			write("</tr>");
		}
		write("</table>");
		
		if (this.hasFreeText)
		{
			writeButton(getString("controls:Button.Enter"));
		}
		
		// Post all parameters
		for (String param : this.params.keySet())
		{
			write("<input type=hidden name=\"");
			writeEncode(param);
			write("\" value=\"");
			writeEncode(this.params.get(param));
			write("\">");
		}
		writeFormClose();
		
		// Auto-check "other" radio button when entering "other" edit box.
		// Auto-copy value from "other" edit box to "other" radio button.
		write("<script>$('#other");
		write(indexStr);
		write("').focus(function(ev){$('#otherradio");
		write(indexStr);
		write("').attr('checked',true);}).change(function(ev){$('#otherradio");
		write(indexStr);
		write("').val($('#other");
		write(indexStr);
		write("').val());});</script>");
	}

	@Override
	public void renderSimpleHTML() throws Exception
	{
		if (!Util.isEmpty(this.prompt))
		{
			write("<b>");
			writeEncode(this.prompt);
			write("</b><br><br>");
		}

		if (this.hasButtons)
		{
			write("<table>");
			for (ActionSpec spec : this.actions)
			{
				if (spec.action==null) continue;
				
				write("<tr>");
				if (this.hasImages)
				{
					write("<td>");
					if (spec.image!=null)
					{
						writeImage(spec.image, spec.action);
					}
					write("</td>");
				}
				
				write("<td>");
				write("<b>");
				writeEncode(spec.action);
				write("</b>");
				write("</td>");
				
				if (this.hasDescriptions)
				{
					write("<td>");
					if (!Util.isEmpty(spec.description))
					{
						writeEncode(spec.description);
					}
					write("</td>");
				}
				write("</tr>");
			}
			write("</table>");
			write("<br>");
		}
		
		if (getContext().getChannel().equals(Channel.EMAIL))
		{
			if (this.hasFreeText && this.hasButtons)
			{
				writeEncode(getString("controls:ActionList.EmailHelpWithFreeText"));
			}
			else if (this.hasButtons)
			{
				writeEncode(getString("controls:ActionList.EmailHelp"));
			}
			else if (this.hasFreeText)
			{
				writeEncode(getString("controls:ActionList.ActionList.EmailHelpOnlyFreeText"));
			}
		}
	}

	@Override
	public void renderShortText() throws Exception
	{
		boolean first = true;
		write("[");
//		writeEncode(getString("controls:ActionList.Reply"));
//		write(" ");
		for (ActionSpec spec : this.actions)
		{
			if (!first)
			{
				write("/");
			}
			first = false;
			if (!Util.isEmpty(spec.action))
			{
				write(spec.action);
			}
			else
			{
				writeEncode(getString("controls:ActionList.FreeText"));
			}
		}
		write("]");
	}

	@Override
	public void renderText() throws Exception
	{
		if (!Util.isEmpty(this.prompt))
		{
			writeEncode(this.prompt);
			write("\r\n");
		}

		for (ActionSpec spec : this.actions)
		{
			write("* ");
			write(spec.action);
			write(" - ");
			write(spec.description);
			write("\r\n");
		}
	}

	@Override
	public void renderVoiceXML() throws Exception
	{
		write("<field name=\"");
		writeEncode(RequestContext.PARAM_ACTION);
		write("\">");
		
		// Prompt
		write("<prompt timeout=\"10s\" bargein=\"true\" bargeintype=\"hotword\">");
		if (!Util.isEmpty(this.prompt))
		{
			writeEncode(this.prompt);
			write(" ");
			write("<break time=\"500ms\"/>");
		}
		int i = 1;
		boolean dtmf = this.actions.size()<=10;
		for (ActionSpec spec : this.actions)
		{
			if (!Util.isEmpty(spec.action))
			{
				if (dtmf)
				{
					writeEncode(getString("controls:ActionList.VoicePromptWithDTMF", i, spec.action, spec.description));
				}
				else
				{
					writeEncode(getString("controls:ActionList.VoicePromptWithoutDTMF", spec.action, spec.description));
				}
				i++;
			}
			write("; ");
		}
		// !$! Free text not implemented yet. Should be inserted here.
		writeEncode(getString("controls:ActionList.VoicePromptRepeat", getString("controls:ActionList.VoiceRepeat")));
		write("</prompt>");
	    
		// DTMF grammar
		if (dtmf)
		{
			write("<grammar root=\"ddrootrule\" mode=\"dtmf\">");
			write("<rule id=\"ddrootrule\" scope=\"public\">");
			write("<one-of>");
			
			i = 1;
			for (ActionSpec spec : this.actions)
			{
				if (!Util.isEmpty(spec.action))
				{
					write("<item>");
					write(i%10);
					write("<tag>out.");
					writeEncode(RequestContext.PARAM_ACTION);
					write("=\"");
					writeEncode(spec.action);
					write("\"</tag></item>");
					i++;
				}
			}
			while (i<=10)
			{
				write("<item>");
				write(i%10);
				write("<tag>out.");
				writeEncode(RequestContext.PARAM_ACTION);
				write("=\"");
				write("_NOMATCH_");
				write("\"</tag></item>");
				i++;
			}
			write("<item>");
			write("#");
			write("<tag>out.");
			writeEncode(RequestContext.PARAM_ACTION);
			write("=\"");
			write("_NOMATCH_");
			write("\"</tag></item>");
			
			write("<item>");
			write("*");
			write("<tag>out.");
			writeEncode(RequestContext.PARAM_ACTION);
			write("=\"");
			writeEncode(getString("controls:ActionList.VoiceRepeat"));
			write("\"</tag></item>");
			
  			write("</one-of>");
  			write("</rule>");
  			write("</grammar>");
		}

		// Voice grammar
		write("<grammar root=\"vvrootrule\" mode=\"voice\">");
		write("<rule id=\"vvrootrule\" scope=\"public\">");
		write("<one-of>");
		for (ActionSpec spec : this.actions)
		{
			if (!Util.isEmpty(spec.action))
			{
				write("<item>");
				writeEncode(spec.action);
				write("</item>");
			}
		}
		write("<item>");
		writeEncode(getString("controls:ActionList.VoiceRepeat"));
		write("</item>");
		write("</one-of>");
		write("</rule>");
		write("</grammar>");
		
		// No input / no match
		for (i=1; i<=3; i++)
		{
			write("<noinput count=\"");
			write(i);
			write("\"><prompt>");
			writeEncode(getString("controls:ActionList.VoiceNoInput"+i));
	  		write("</prompt>");
	  		if (i==3)
	  		{
	  			write("<disconnect/>");
	  		}
	  		else
	  		{
	  			write("<reprompt/>");
	  		}
  			write("</noinput>");
		}
		
		write("<nomatch>");
		writeEncode(getString("controls:ActionList.VoiceNoMatch"));
		write("<reprompt/></nomatch>");

		// Handle response
		write("<filled namelist=\"");
		writeEncode(RequestContext.PARAM_ACTION);
		write("\">");
		write("<if cond=\"");
		writeEncode(RequestContext.PARAM_ACTION);
		write("=='");
		writeEncode(getString("controls:ActionList.VoiceRepeat"));
		write("'\"><prompt>");
		writeEncode(getString("controls:ActionList.VoiceRepeating"));
		write("</prompt><goto next=\"#home\"/>"); // We assume the form has ID "home"
		
		write("<elseif cond=\"");
		writeEncode(RequestContext.PARAM_ACTION);
		write("=='");
		write("_NOMATCH_");
		write("'\"/>");
		write("<clear name=\"");
		writeEncode(RequestContext.PARAM_ACTION);
		write("\"/><throw event=\"nomatch\"/>"); // Manually throw a nomatch

		write("<else/>");
		
		Pattern paramNameRegEx = null;
		if (this.params!=null)
		{
			paramNameRegEx = Pattern.compile("^[_\\w]*$"); // underline,a-z,0-9
			for (String p : this.params.keySet())
			{
				if (paramNameRegEx.matcher(p).matches() && this.params.get(p)!=null)
				{
					write("<var name=\"");
					writeEncode(p);
					write("\" expr=\"'");
					writeEncode(this.params.get(p));
					write("'\"/>");
				}
			}
		}
		
		write("<submit method=\"POST\" next=\"");
		write(getPageURL(this.command));
		write("\" namelist=\"");
		if (this.params!=null)
		{
			for (String p : this.params.keySet())
			{
				if (paramNameRegEx.matcher(p).matches() && this.params.get(p)!=null)
				{
					writeEncode(p);
					write(" ");
				}
			}
		}
		writeEncode(RequestContext.PARAM_ACTION);
		write(" ");
		writeEncode(RequestContext.PARAM_SESSION); // Posted with every VXML form. See EnvelopPage
		write("\"/>");
		write("</if>");
		write("</filled>");

		write("</field>");
	}
}
