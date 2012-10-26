package elert.pages.patient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ActionListControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.app.ElertConsts;
import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.PhysicianOpeningLinkStore;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.pages.ElertPage;

public final class WallPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/wall";
	private static final Integer PAGE_SIZE = 10;

	@Override
	public void validate() throws Exception
	{
		Elert elert = ElertStore.getInstance().load(getParameterUUID("elert"));
		if (elert==null)
		{
			throw new WebFormException(getString("common:Errors.InvalidValue"));
		}
	}

	@Override
	public void commit() throws Exception
	{
		Elert elert = ElertStore.getInstance().open(getParameterUUID("elert"));
		String action = getParameterString(RequestContext.PARAM_ACTION);
		if (action.equalsIgnoreCase(getString("patient:ElertNotif.Accept")))
		{
			elert.setReply(Elert.REPLY_ACCEPTED);
		}
		else if (action.equalsIgnoreCase(getString("patient:ElertNotif.Decline")))
		{
			elert.setReply(Elert.REPLY_DECLINED);
		}
		elert.setReplyChannel(getContext().getChannel());
		elert.setDateReply(new Date());
		ElertStore.getInstance().save(elert);
		
		// Redirect to self
		Integer at = getParameterInteger("at");
		if (at==null || at==0)
		{
			throw new RedirectException(COMMAND, null);
		}
		else
		{
			throw new RedirectException(COMMAND, new ParameterMap("at", String.valueOf(at)));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:Wall.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		boolean phone = getContext().getUserAgent().isSmartPhone();
		
		// Hide/show requests
		UUID toHide = getParameterUUID("hide");
		if (toHide!=null)
		{
			Elert e = ElertStore.getInstance().open(toHide);
			if (e.getPatientID().equals(ctx.getUserID()))
			{
				e.setHidden(true);
				ElertStore.getInstance().save(e);
			}
		}
		UUID toShow = getParameterUUID("show");
		if (toShow!=null)
		{
			Elert e = ElertStore.getInstance().open(toShow);
			if (e.getPatientID().equals(ctx.getUserID()))
			{
				e.setHidden(false);
				ElertStore.getInstance().save(e);
			}
		}
		
		
		// Get all eLerts for this patient, ordered by OpeningDate
		List<UUID> elertIDs = ElertStore.getInstance().queryByPatientID(ctx.getUserID());
		if (elertIDs.size()==0)
		{
			writeEncode(getString("patient:Wall.NoResults"));
			return;
		}
		
		Integer at = getParameterInteger("at");
		if (at==null) at = 0;
		
		boolean lastHidden = false;
		write("<table width=\"100%\">");
		for (int i=at; i<at+PAGE_SIZE && i<elertIDs.size(); i++)
		{
			Elert elert = ElertStore.getInstance().load(elertIDs.get(i));
			Opening opening = OpeningStore.getInstance().load(elert.getOpeningID());
			
			if (i>at && (lastHidden==false || elert.isHidden()==false))
			{
				write("<tr><td colspan=");
				write(phone?1:2);
				write("><a name=\"");
				writeEncode(elert.getID().toString());
				write("\">&nbsp;</a>");
				if (!phone)
				{
					write("<br><hr>");
				}
				write("</td></tr>");
			}
			lastHidden = elert.isHidden();
			
			// Calendar
			write("<tr><td");
			if (!phone && elert.isHidden())
			{
				write(" colspan=2");
			}
			else if (!elert.isHidden())
			{
				write(" align=center");
			}
			write(">");
			writeMiniCalendar(elert);
			write("</td>");
			
			if (phone || elert.isHidden())
			{
				write("</tr>");
			}
			if (elert.isHidden())
			{
				continue;
			}
			if (phone)
			{
				write("<tr>");
			}
			
			write("<td>");
			
			// Original eLert - - -
			
			writeBubbleOpen(elert.getSchedulerID(), elert.getDateSent(), false);

			String doctors = getPhysiciansString(elert.getOpeningID());
			String procedures = getProceduresString(elert.getSubscriptionID());
			if (Util.isEmpty(doctors))
			{
				writeEncode(getString("patient:ElertNotif.BodyWithoutDoctors", procedures, opening.getDateTime()));
			}
			else
			{
				writeEncode(getString("patient:ElertNotif.BodyWithDoctors", doctors, procedures, opening.getDateTime()));
			}

			write(" ");
			Facility facility = FacilityStore.getInstance().load(opening.getFacilityID());
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
			List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(elert.getSubscriptionID());
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
				.readOnly(elert.getReply()!=Elert.REPLY_NONE || elert.getDecision()!=Elert.DECISION_NONE)
				.setPostAction(COMMAND, new ParameterMap("elert", elert.getID().toString()).plus("at", String.valueOf(at)))
				.render();

			write("<br>");
			write("<b>");
			writeEncode(getString("elert:General.DisclaimerTitle"));
			write("</b> ");
			writeEncode(getString("elert:General.Disclaimer", Setup.getAppTitle(getLocale())));

			writeBubbleClose();
			
			// Reply - - -
			
			if (elert.getReply()!=Elert.REPLY_NONE)
			{
				writeBubbleOpen(elert.getPatientID(), elert.getDateReply(), true);

				if (elert.getReply()==Elert.REPLY_ACCEPTED)
				{
					writeEncode(getString("patient:ElertNotif.Accept"));
				}
				else if (elert.getReply()==Elert.REPLY_DECLINED)
				{
					writeEncode(getString("patient:ElertNotif.Decline"));
				}
				
				writeBubbleClose();

				if (elert.getReply()==Elert.REPLY_ACCEPTED)
				{
					writeBubbleOpen(elert.getSchedulerID(), elert.getDateReply(), false);
					writeEncode(getString("patient:ElertNotif.AcceptConfirmHTML", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), elert.getDateOpening()));
					writeBubbleClose();
				}
				else if (elert.getReply()==Elert.REPLY_DECLINED)
				{
					writeBubbleOpen(elert.getSchedulerID(), elert.getDateReply(), false);
					writeEncode(getString("patient:ElertNotif.DeclineConfirmHTML", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), elert.getDateOpening()));
					writeBubbleClose();
				}
			}
			
			// Decision - - -
			
			if (elert.getDecision()==Elert.DECISION_CHOSEN)
			{
				writeBubbleOpen(elert.getSchedulerID(), elert.getDateDecision(), false);
				writeEncode(getString("patient:ChosenNotif.LongText", opening.getDateTime(), getProceduresString(elert.getSubscriptionID()), facility.getName()));
				writeBubbleClose();
			}
			else if (elert.getDecision()==Elert.DECISION_NOT_CHOSEN && elert.getReply()!=Elert.REPLY_DECLINED)
			{
				writeBubbleOpen(elert.getSchedulerID(), elert.getDateDecision(), false);
				writeEncode(getString("patient:UnavailNotif.LongText", opening.getDateTime(), getProceduresString(elert.getSubscriptionID()), facility.getName()));
				writeBubbleClose();
			}
			
//			if (elert.getDecision()!=Elert.DECISION_NONE)
//			{
//				writeBubbleOpen(elert.getSchedulerID(), elert.getDateDecision(), false);
//
//				if (elert.getDecision()==Elert.DECISION_CHOSEN)
//				{
//					writeEncode(getString("patient:ChosenNotif.LongText", opening.getDateTime(), getProceduresString(elert.getSubscriptionID()), facility.getName()));
//				}
//				else if (elert.getDecision()==Elert.DECISION_NOT_CHOSEN)
//				{
//					writeEncode(getString("patient:UnavailNotif.LongText", opening.getDateTime(), getProceduresString(elert.getSubscriptionID()), facility.getName()));
//				}
//				
//				writeBubbleClose();
//			}
			write("</td></tr>");			
		}
		write("</table>");
		
		if (at+PAGE_SIZE < elertIDs.size())
		{
			writeLink(	getString("patient:Wall.OlderElerts"),
						getPageURL(COMMAND, new ParameterMap("at", String.valueOf(at+PAGE_SIZE))));
		}
	}

	private void writeMiniCalendar(Elert elert)
	{
		Date date = elert.getDateOpening();
		boolean phone = getContext().getUserAgent().isSmartPhone();

		if (elert.isHidden())
		{
			write("<a class=Faded href=\"");
			writeEncode(getPageURL(getContext().getCommand(), new ParameterMap("show", elert.getID().toString())));
			write("#");
			writeEncode(elert.getID().toString());
			write("\">");
			writeEncode(getString("patient:Wall.Show"));
			write(" ");
//			writeEncodeDateTime(date);
			writeEncodeDate(date);
			write("</a>");
		}
		else if (!phone)
		{
			DateFormat mmm = new SimpleDateFormat("MMM", getLocale());
			mmm.setTimeZone(getTimeZone());
			
			DateFormat d = new SimpleDateFormat("d", getLocale());
			d.setTimeZone(getTimeZone());
			
//			DateFormat hmm = DateFormatEx.getTimeInstance(getLocale(), getTimeZone());

			write("<div class=MiniCalendar>");
			write("<div>");
			writeEncode(mmm.format(date));
			write("</div>");
			writeEncode(d.format(date));
			write("</div>");
			
//			write("<small>");
//			writeEncode(hmm.format(date));
//			write("</small>");
//			write("<br>");
			
			write("<a class=Faded href=\"");
			writeEncode(getPageURL(getContext().getCommand(), new ParameterMap("hide", elert.getID().toString())));
			write("#");
			writeEncode(elert.getID().toString());
			write("\">");
			writeEncode(getString("patient:Wall.Hide"));
			write("</a>");
		}
		else
		{
			write("<table class=MiniCalendar><tr><td>");
//			writeEncodeDateTime(date);
			writeEncodeDate(date);
			write("</td><td align=right>");
			write("<a href=\"");
			writeEncode(getPageURL(getContext().getCommand(), new ParameterMap("hide", elert.getID().toString())));
			write("#");
			writeEncode(elert.getID().toString());
			write("\">");
			writeEncode(getString("patient:Wall.Hide"));
			write("</a>");
			write("</td></tr></table>");
		}
	}

	private void writeBubbleOpen(UUID userID, Date date, boolean altStyle) throws Exception
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		
		if (!phone)
		{
			write("<table><tr><td>");
			User user = UserStore.getInstance().load(userID);
			Image avatar = user.getAvatar();
			if (avatar!=null)
			{
				writeImage(avatar, ElertConsts.IMAGESIZE_SQUARE_50, user.getDisplayName(), null);
			}
			else if (user.getGender()==null || user.getGender().equalsIgnoreCase(User.GENDER_MALE))
			{
				writeImage("elert/male-avatar.png", user.getDisplayName());
			}
			else
			{
				writeImage("elert/female-avatar.png", user.getDisplayName());
			}
			write("</td><td>");
		}
		
		write("<div class=\"Bubble");
		if (altStyle)
		{
			write(" Alt");
		}
		write("\">");
		write("<small class=Faded>");
		writeEncodeDateTime(date);
		write("</small><br>");
	}
	private void writeBubbleClose()
	{
		boolean phone = getContext().getUserAgent().isSmartPhone();
		
		write("</div>");
		if (!phone)
		{
			write("</td></tr></table>");
		}
	}
	
	private String getPhysiciansString(UUID openingID) throws Exception
	{
		List<UUID> physicianIDs = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(openingID);
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
	
	private String getProceduresString(UUID subID) throws Exception
	{		
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(subID);
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
}
