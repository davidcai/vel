package elert.pages.patient;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.PhysicianOpeningLinkStore;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.Subscription;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import samoyan.controls.ActionListControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.core.XCoShortenUrl;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.WebFormException;

public final class ElertNotif extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/elert.notif";
	public final static String PARAM_ELERT_ID = "id";
	
	private Elert elert;
	private Opening opening;
	private Subscription sub;
	
	private boolean readOnly;
	private String warning;
	
	@Override
	public void init() throws Exception
	{
		this.readOnly = false;
		this.warning = null;
		
		this.elert = ElertStore.getInstance().load(getParameterUUID(PARAM_ELERT_ID));
		if (this.elert.getReply()!=Elert.REPLY_NONE)
		{
			// eLert has been replied to
			this.readOnly = true;
			this.warning = getString("patient:ElertNotif.AlreadyReplied");
//			throw new PageNotFoundException();
		}
		
		this.opening = OpeningStore.getInstance().load(this.elert.getOpeningID());
		if (this.opening.isClosed())
		{
			// Opening has since been closed
			this.readOnly = true;
			this.warning = getString("patient:ElertNotif.ElertClosedWarning");
//			throw new PageNotFoundException();
		}
		
		this.sub = SubscriptionStore.getInstance().load(this.elert.getSubscriptionID());
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:ElertNotif.Title", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime());
	}

	private String getPhysiciansString() throws Exception
	{
		List<UUID> physicianIDs = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(this.opening.getID());
		if (physicianIDs.size()==0)
		{
			// Can happen for openings without physicians
			return null;
		}
		
		StringBuilder result = new StringBuilder();
		for (int i=0; i<physicianIDs.size(); i++)
		{
			User physician = UserStore.getInstance().load(physicianIDs.get(i));
			if (i>0)
			{
				if (i==physicianIDs.size()-1)
				{
					result.append(getString("patient:ElertNotif.And"));
				}
				else
				{
					result.append(getString("patient:ElertNotif.Comma"));
				}
			}
			result.append(physician.getDisplayName());
		}
		
		return result.toString();
	}
	
	private String getProceduresString() throws Exception
	{		
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		if (procIDs.size()==0)
		{
			// Shouldn't happen
			return null;
		}
		
		StringBuilder result = new StringBuilder();
		for (int i=0; i<procIDs.size(); i++)
		{
			Procedure proc = ProcedureStore.getInstance().load(procIDs.get(i));
			if (i>0)
			{
				if (i==procIDs.size()-1)
				{
					result.append(getString("patient:ElertNotif.And"));
				}
				else
				{
					result.append(getString("patient:ElertNotif.Comma"));
				}
			}
			result.append(proc.getDisplayName());
		}
		
		return result.toString();
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (this.isCommitted())
		{
			if (elert.getReply()==Elert.REPLY_ACCEPTED)
			{
				writeEncode(getString("patient:ElertNotif.AcceptConfirmHTML", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
			}
			else if (elert.getReply()==Elert.REPLY_DECLINED)
			{
				writeEncode(getString("patient:ElertNotif.DeclineConfirmHTML", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
			}
			return;
		}
				
		if (this.readOnly)
		{
			write("<div class=WarningMessage>");
			writeEncode(this.warning);
			write("</div>");
		}
		
		String doctors = getPhysiciansString();
		String procedures = getProceduresString();
		if (Util.isEmpty(doctors))
		{
			writeEncode(getString("patient:ElertNotif.BodyWithoutDoctors", procedures, this.opening.getDateTime()));
		}
		else
		{
			writeEncode(getString("patient:ElertNotif.BodyWithDoctors", doctors, procedures, this.opening.getDateTime()));
		}
		
		write(" ");
		Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
		if (Util.isEmpty(facility.getCity()))
		{
			writeEncode(getString("patient:ElertNotif.LocationWithoutCity", facility.getName()));
		}
		else
		{
			String city = facility.getCity();
			if (!Util.isEmpty(facility.getState()))
			{
				city += ", ";
				city += facility.getState();
			}
			writeEncode(getString("patient:ElertNotif.LocationWithCity", facility.getName(), city));
		}
		write("<br><br>");
		
		// Pre-requisites
		boolean prereqPrinted = false;
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		for (UUID procID : procIDs)
		{
			Procedure proc = ProcedureStore.getInstance().load(procID);
			if (!Util.isEmptyHTML(proc.getInstructions()))
			{
				if (prereqPrinted==false)
				{
					writeEncode(getString("patient:ElertNotif.Prerequisites"));
					write("<br><br>");
					prereqPrinted = true;
				}
				
				write("<u>");
				writeEncode(proc.getDisplayName());
				write("</u>: ");
				write(proc.getInstructions());
				write("<br><br>");
			}
		}
		
		new ActionListControl(this)
			.setPrompt(getString("patient:ElertNotif.Prompt"))
			.addAction("elert/circle-v.png", getString("patient:ElertNotif.Accept"), getString("patient:ElertNotif.AcceptHelp"))
			.addAction("elert/circle-x.png", getString("patient:ElertNotif.Decline"), getString("patient:ElertNotif.DeclineHelp"))
			.readOnly(this.elert.getReply()!=Elert.REPLY_NONE)
			.render();
		
		if (getContext().getChannel().equalsIgnoreCase(Channel.WEB)==false)
		{
			write("<br><br>");
			writeEncode(getString("patient:ElertNotif.WebLink"));

			write("<br><br>");
			String url = getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ELERT_ID, this.elert.getID().toString()));
			writeLink(null, url);
		}
		
		write("<br><br>");
		write("<b>");
		writeEncode(getString("elert:General.DisclaimerTitle"));
		write("</b> ");
		writeEncode(getString("elert:General.Disclaimer", Setup.getAppTitle(getLocale())));
	}
	
	@Override
	public void renderShortText() throws Exception
	{
		if (this.isCommitted())
		{
			if (elert.getReply()==Elert.REPLY_ACCEPTED)
			{
				write(getString("patient:ElertNotif.AcceptConfirmShortText", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
			}
			else if (elert.getReply()==Elert.REPLY_DECLINED)
			{
				write(getString("patient:ElertNotif.DeclineConfirmShortText", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
			}
			return;
		}
		
		String doctors = getPhysiciansString();
		String procedures = getProceduresString();
		if (Util.isEmpty(doctors))
		{
			write(getString("patient:ElertNotif.BodyWithoutDoctorsShortText", procedures, this.opening.getDateTime()));
		}
		else
		{
			write(getString("patient:ElertNotif.BodyWithDoctorsShortText", doctors, procedures, this.opening.getDateTime()));
		}
		write(" ");
		
		new ActionListControl(this)
			.setPrompt(getString("patient:ElertNotif.Prompt"))
			.addAction(getString("patient:ElertNotif.Accept"), getString("patient:ElertNotif.AcceptHelp"))
			.addAction(getString("patient:ElertNotif.Decline"), getString("patient:ElertNotif.DeclineHelp"))
			.render();

		// Add x.co short URL to end of message
		Server fed = ServerStore.getInstance().loadFederation();
		if (!Util.isEmpty(fed.getXCoAPIKey()))
		{
			String url = getPageURL(getContext().getCommand(), new ParameterMap(PARAM_ELERT_ID, this.elert.getID().toString()));
			url = XCoShortenUrl.shorten(fed.getXCoAPIKey(), url);
			write(" " + url);
		}
	}
	
	@Override
	public void renderSimpleHTML() throws Exception
	{
		// Delegate to renderHTML
		this.renderHTML();
	}
	
	@Override
	public void renderText() throws Exception
	{
		if (this.isCommitted())
		{
			if (elert.getReply()==Elert.REPLY_ACCEPTED)
			{
				writeEncode(getString("patient:ElertNotif.AcceptConfirmText", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
			}
			else if (elert.getReply()==Elert.REPLY_DECLINED)
			{
				writeEncode(getString("patient:ElertNotif.DeclineConfirmText", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
			}		
			return;
		}
		
		String doctors = getPhysiciansString();
		String procedures = getProceduresString();
		if (Util.isEmpty(doctors))
		{
			write(getString("patient:ElertNotif.BodyWithoutDoctors", procedures, this.opening.getDateTime()));
		}
		else
		{
			write(getString("patient:ElertNotif.BodyWithDoctors", doctors, procedures, this.opening.getDateTime()));
		}

		write(" ");
		Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
		if (Util.isEmpty(facility.getCity()))
		{
			write(getString("patient:ElertNotif.LocationWithoutCity", facility.getName()));
		}
		else
		{
			String city = facility.getCity();
			if (!Util.isEmpty(facility.getState()))
			{
				city += ", ";
				city += facility.getState();
			}
			write(getString("patient:ElertNotif.LocationWithCity", facility.getName(), city));
		}
		write("\r\n\r\n");
		
		// Pre-requisites
		boolean prereqPrinted = false;
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		for (UUID procID : procIDs)
		{
			Procedure proc = ProcedureStore.getInstance().load(procID);
			if (!Util.isEmptyHTML(proc.getInstructions()))
			{
				if (prereqPrinted==false)
				{
					write(getString("patient:ElertNotif.Prerequisites"));
					write("\r\n\r\n");
					prereqPrinted = true;
				}
				
				write(proc.getDisplayName());
				write(": ");
				write(proc.getInstructions());
				write("\r\n\r\n");
			}
		}
		
		new ActionListControl(this)
			.setPrompt(getString("patient:ElertNotif.Prompt"))
			.addAction(getString("patient:ElertNotif.Accept"), getString("patient:ElertNotif.AcceptHelp"))
			.addAction(getString("patient:ElertNotif.Decline"), getString("patient:ElertNotif.DeclineHelp"))
			.render();
		
		write("\r\n\r\n");
		writeEncode(getString("elert:General.DisclaimerTitle"));
		write(" ");
		write(getString("elert:General.Disclaimer", Setup.getAppTitle(getLocale())));
	}
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		if (this.isCommitted())
		{
//			DateFormat df = DateFormatEx.getVXMLDateInstance(getLocale(), getTimeZone());
//			StringBuilder dateStr = new StringBuilder();
//			dateStr.append("<say-as interpret-as=\"vxml:date\">");
//			dateStr.append(Util.htmlEncode(df.format(this.opening.getDateTime())));
//			dateStr.append("</say-as>");
//			String pattern = Util.htmlEncode(getString("patient:ElertNotif.AcceptConfirmVoice", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), "$date$"));

			if (elert.getReply()==Elert.REPLY_ACCEPTED)
			{
				write("<block><prompt>");
				// !$! SAY-AS for $date$
				writeEncode(getString("patient:ElertNotif.AcceptConfirmVoice", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
				write("</prompt></block>");
			}
			else if (elert.getReply()==Elert.REPLY_DECLINED)
			{
				write("<block><prompt>");
				// !$! SAY-AS for $date$
				writeEncode(getString("patient:ElertNotif.DeclineConfirmVoice", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime()));
				write("</prompt></block>");
			}		
			return;
		}

		if (this.readOnly)
		{
			throw new PageNotFoundException();
		}
		
		write("<block><prompt bargein=\"true\" bargeintype=\"hotword\">");
		
		String doctors = getPhysiciansString();
		String procedures = getProceduresString();
		if (Util.isEmpty(doctors))
		{
			writeEncode(getString("patient:ElertNotif.BodyWithoutDoctors", procedures, this.opening.getDateTime()));// !$! SAY-AS for $date$
		}
		else
		{
			writeEncode(getString("patient:ElertNotif.BodyWithDoctors", doctors, procedures, this.opening.getDateTime()));// !$! SAY-AS for $date$
		}
		
		write(" ");
		Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
		if (Util.isEmpty(facility.getCity()))
		{
			writeEncode(getString("patient:ElertNotif.LocationWithoutCity", facility.getName()));
		}
		else
		{
			String city = facility.getCity();
			if (!Util.isEmpty(facility.getState()))
			{
				city += ", ";
				city += facility.getState();
			}
			writeEncode(getString("patient:ElertNotif.LocationWithCity", facility.getName(), city));
		}

		// Pre-requisites
		boolean prereqPrinted = false;
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		for (UUID procID : procIDs)
		{
			Procedure proc = ProcedureStore.getInstance().load(procID);
			if (!Util.isEmptyHTML(proc.getInstructions()))
			{
				if (prereqPrinted==false)
				{
					write("<break time=\"500ms\"/>");
					writeEncode(getString("patient:ElertNotif.Prerequisites"));
					prereqPrinted = true;
				}

				write("<break time=\"500ms\"/>");
				if (procIDs.size()>1)
				{
					writeEncode(proc.getDisplayName());
					write(": ");
				}
				writeEncode(Util.htmlToText(proc.getInstructions()));
			}
		}
		write("<break time=\"500ms\"/>");
		write("</prompt></block>");
		
		new ActionListControl(this)
			.setPrompt(getString("patient:ElertNotif.Prompt"))
			.addAction("elert/circle-v.png", getString("patient:ElertNotif.AcceptVoice").toLowerCase(getLocale()), getString("patient:ElertNotif.AcceptHelp"))
			.addAction("elert/circle-x.png", getString("patient:ElertNotif.DeclineVoice").toLowerCase(getLocale()), getString("patient:ElertNotif.DeclineHelp"))
			.render();
	}

	@Override
	public void commit() throws Exception
	{
		this.elert = ElertStore.getInstance().open(this.elert.getID()); // Open for writing
		
		// Also accept first letter as valid action
		String altAccept = getString("patient:ElertNotif.Accept").substring(0, 1);
		String altDecline = getString("patient:ElertNotif.Decline").substring(0, 1);
		if (altAccept.equalsIgnoreCase(altDecline))
		{
			altAccept = null;
			altDecline = null;
		}

		String action = getParameterString(RequestContext.PARAM_ACTION);
		if (action.equalsIgnoreCase(getString("patient:ElertNotif.Accept")) ||
			action.equalsIgnoreCase(getString("patient:ElertNotif.AcceptVoice")) ||
			(altAccept!=null && action.equalsIgnoreCase(altAccept)))
		{
			elert.setReply(Elert.REPLY_ACCEPTED);
		}
		else if (action.equalsIgnoreCase(getString("patient:ElertNotif.Decline")) ||
				action.equalsIgnoreCase(getString("patient:ElertNotif.DeclineVoice")) ||
				(altDecline!=null && action.equalsIgnoreCase(altDecline)))
		{
			elert.setReply(Elert.REPLY_DECLINED);
		}
		elert.setReplyChannel(getContext().getChannel());
		elert.setDateReply(new Date());
		ElertStore.getInstance().save(elert);
	}

	@Override
	public boolean isActionable() throws Exception
	{
		return true;
	}
	
	@Override
	public boolean isSecureSocket() throws Exception
	{
		String channel = getContext().getChannel();
		return (channel.equalsIgnoreCase(Channel.WEB) || channel.equalsIgnoreCase(Channel.VOICE));
	}

	@Override
	public void validate() throws Exception
	{
		String action = getParameterString(RequestContext.PARAM_ACTION);
		String channel = getContext().getChannel();
		if (channel.equalsIgnoreCase(Channel.VOICE))
		{
			if (action.equalsIgnoreCase(getString("patient:ElertNotif.AcceptVoice"))==false &&
				action.equalsIgnoreCase(getString("patient:ElertNotif.DeclineVoice"))==false)
			{
				throw new WebFormException(getString("patient:ElertNotif.BadReply", getString("patient:ElertNotif.AcceptVoice"), getString("patient:ElertNotif.DeclineVoice")));
			}
		}
		else
		{
			// Also accept first letter as valid action
			String altAccept = getString("patient:ElertNotif.Accept").substring(0, 1);
			String altDecline = getString("patient:ElertNotif.Decline").substring(0, 1);
			if (altAccept.equalsIgnoreCase(altDecline))
			{
				altAccept = null;
				altDecline = null;
			}
			
			if (action.equalsIgnoreCase(getString("patient:ElertNotif.Accept"))==false &&
				action.equalsIgnoreCase(getString("patient:ElertNotif.Decline"))==false &&
				(altAccept==null || action.equalsIgnoreCase(altAccept)==false) &&
				(altDecline==null || action.equalsIgnoreCase(altDecline)==false))
			{
				throw new WebFormException(getString("patient:ElertNotif.BadReply", getString("patient:ElertNotif.Accept"), getString("patient:ElertNotif.Decline")));
			}
		}
		
		if (this.opening.isClosed())
		{
			throw new WebFormException(getString("patient:ElertNotif.ElertClosed"));
		}
		if (this.elert.getReply()!=Elert.REPLY_NONE)
		{
			throw new WebFormException(getString("patient:ElertNotif.AlreadyReplied"));
		}
	}	
}
