package mind.pages.patient.coaching;

import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import mind.database.Drug;
import mind.database.DrugStore;
import mind.pages.patient.PatientPage;

public class BotChatPage extends PatientPage
{
	public final static String COMMAND = CoachingPage.COMMAND + "/bot-chat";

	@Override
	public String getTitle() throws Exception
	{
		return getString("mind:BotChat.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		String q = ctx.getParameter("q");
		if (q==null)
		{
			writeIncludeCSS("mind/chat.css");
			writeIncludeJS("mind/chat.js");

			renderForm();
		}
		else
		{
			renderAjax();
		}
	}
	
	@Override
	public boolean isEnvelope() throws Exception
	{
		return getContext().getParameter("q")==null;
	}

	public void renderForm() throws Exception
	{
		write("<div id=chatbox>");
		writeAnswer(getString("mind:BotChat.Hello", Setup.getAppTitle(getLocale())));						
		write("</div>");
		
		// Text box
		write("<table id=chatcontrols target=\"");
		write(getPageURL(COMMAND));
		write("\">");
		write("<tr><td>");
		writeTextInput("q", null, 40, 128);
		write("</td><td>");
		writeButton("ask", getString("mind:BotChat.Ask"));
		write("</td></tr></table>");
		
		
		write("<br><br><small>");
		writeEncode(getString("mind:BotChat.EmergencyDisclaimer"));
		write("</small>");
	}
	
	public void renderAjax() throws Exception
	{
		RequestContext ctx = getContext();
		
		String q = ctx.getParameter("q");
		if (Util.isEmpty(q))
		{
			return;
		}
				
		writeQuestion(Util.htmlEncode(q));
		writeAnswer(getAnswer(q));
		
		return;
	}
	
	private String getAnswer(String q) throws Exception
	{
		q = q.toLowerCase(getLocale());
		
		if (q.indexOf("meaning")>=0 && q.indexOf("life")>=0)
		{
			return "42";
		}
		
		if (q.startsWith("who are you"))
		{
			return "I'm an automated bot.";
		}
		
		if (q.startsWith("what are the side effects of "))
		{
			String drugName = q.substring("what are the side effects of ".length());
			if (drugName.endsWith("?"))
			{
				drugName = drugName.substring(0, drugName.length()-1);
			}
			Drug drug = DrugStore.getInstance().loadByName(drugName, null);
			if (drug==null)
			{
				return "I don't recognize the drug " + drugName + ".";
			}
			else
			{
				return drug.getSideEffectsInformation();
			}
		}
		
		if (q.startsWith("tell me about "))
		{
			String drugName = q.substring("tell me about ".length());
			if (drugName.endsWith("."))
			{
				drugName = drugName.substring(0, drugName.length()-1);
			}
			Drug drug = DrugStore.getInstance().loadByName(drugName, null);
			if (drug==null)
			{
				return "I don't recognize the drug " + drugName + ".";
			}
			else
			{
				StringBuffer result = new StringBuffer();
				result.append(drug.getDescription());
				
				RequestContext ctx = getContext();
				if (!Util.isEmpty(drug.getYouTubeVideoID()) && ctx.getUserAgent().isBlackBerry()==false)
				{
					int width = 300;
					if (ctx.getUserAgent().isSmartPhone() && width>ctx.getUserAgent().getScreenWidth()-100)
					{
						width = ctx.getUserAgent().getScreenWidth()-100;
					}

					result.append("<br><br>");
					result.append("<iframe width=\"");
					result.append(width);
					result.append("\" height=\"");
					result.append(width*3/4);
					result.append("\" src=\"http://www.youtube.com/embed/");
					result.append(drug.getYouTubeVideoID());
					result.append("?rel=0\" frameborder=\"0\" allowfullscreen=\"0\"></iframe>");
				}
				
				result.append("<br><br>For detailed information, <a href=\"");
				result.append(getPageURL(DrugInfoPage.COMMAND, new ParameterMap("id", drug.getID().toString())));
				result.append("\">click here</a>.");
				
				return result.toString();
			}
		}

		if (q.startsWith("can i take ") && q.indexOf(" with ")>=0)
		{
			int p = q.indexOf(" with ");
			String drugName1 = q.substring("can i take ".length(), p);
			Drug drug1 = DrugStore.getInstance().loadByName(drugName1, null);
			if (drug1==null)
			{
				drug1 = DrugStore.getInstance().createFromWeb(drugName1);
			}
			if (drug1==null)
			{
				return "I don't recognize the drug " + drugName1 + ".";
			}
			
			String drugName2 = q.substring(p + " with ".length());
			if (drugName2.endsWith("?"))
			{
				drugName2 = drugName2.substring(0, drugName2.length()-1);
			}
			Drug drug2 = DrugStore.getInstance().loadByName(drugName2, null);
			if (drug2==null)
			{
				drug2 = DrugStore.getInstance().createFromWeb(drugName2);
			}
			if (drug2==null)
			{
				return "I don't recognize the drug " + drugName2 + ".";
			}

			if (DrugStore.getInstance().isInteraction(drug1.getID(), drug2.getID()))
			{
				return drug1.getName() + " and " + drug2.getName() + " are known to interact. " +
					"Do not take them together without first consulting with your physician. " +
					"For detailed information, <a href=\"" +
					getPageURL(DrugInteractionPage.COMMAND, new ParameterMap("d0", drug1.getID().toString()).plus("d1", drug2.getID().toString())) +
					"\">click here</a>.";
			}
			else
			{
				return "Yes, it is generally safe to take " + drug1.getName() + " and " + drug2.getName() + " together.";
			}
		}
		
		if (q.equals("help"))
		{
			return "Try asking me: <br>" +
				"Tell me about DRUG.<br>" +
				"What are the side effects of DRUG?<br>" +
				"Can I take DRUG1 with DRUG2?";
		}
		
		return "I don't know how to respond to that.";
	}

	private void writeQuestion(String q) throws Exception
	{
		write("<table width=\"100%\"><tr><td width=32>");
		writeImage("mind/chat-patient.png", null);
		write("</td><td>");
		write("<div class=\"Bubble Left\">");
		write(q);
		write("</div>");
		write("</td></tr></table>");
	}
	
	private void writeAnswer(String a) throws Exception
	{
		write("<table width=\"100%\"><tr><td>");
		write("<div class=\"Bubble Right\">");
		write(a);
		write("</div>");
		write("</td><td width=32>");
		writeImage("mind/chat-dr.png", null);
		write("</td></tr></table>");
	}
}
