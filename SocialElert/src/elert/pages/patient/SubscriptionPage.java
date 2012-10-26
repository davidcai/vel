package elert.pages.patient;

import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.DaysOfMonthChooserControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.database.Subscription;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;

public class SubscriptionPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/subscription";
	public final static String PARAM_ID = "id";
	
	private Subscription sub;
	private boolean readOnly;
	private String warning;
	
	@Override
	public void init() throws Exception
	{
		this.sub = SubscriptionStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.sub==null || this.sub.getUserID().equals(getContext().getUserID())==false)
		{
			throw new PageNotFoundException();
		}
		
		this.warning = null;
		if (this.sub.isFinalized())
		{
			this.readOnly = true;
			this.warning = getString("patient:Subscription.FinalizedSubError");
		}
		else if (this.sub.isExpired())
		{
			this.readOnly = true;
			this.warning = getString("patient:Subscription.ExpiredSubError");
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (this.readOnly)
		{
			throw new WebFormException(this.warning);
		}
		
		int minAdvanceNotice = 0;
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		for (int i=0; i<procIDs.size(); i++)
		{
			Procedure proc = ProcedureStore.getInstance().load(procIDs.get(i));
			minAdvanceNotice = Math.max(minAdvanceNotice, proc.getLead());
		}
		validateParameterInteger("advancenotice", minAdvanceNotice, Subscription.MAX_ADVANCE_NOTICE);

		// Availability
		if (getParameterString("always").equals("1")==false)
		{
			boolean found = false;
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
			{
				int yyyy = cal.get(Calendar.YEAR);
				int mm = cal.get(Calendar.MONTH)+1;
				String bitMap = getParameterString("avail." + String.valueOf(yyyy) + "." + String.valueOf(mm));
				if (bitMap!=null && bitMap.indexOf("1")>=0)
				{
					found = true;
					break;
				}
				cal.add(Calendar.MONTH, 1);
			}
			if (!found)
			{
				String[] fields = new String[Subscription.MAX_AVAILABILITY_MONTHS];
				cal = Calendar.getInstance(getTimeZone(), getLocale());
				for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
				{
					int yyyy = cal.get(Calendar.YEAR);
					int mm = cal.get(Calendar.MONTH)+1;
					fields[i] = "avail." + String.valueOf(yyyy) + "." + String.valueOf(mm);
					cal.add(Calendar.MONTH, 1);
				}
				throw new WebFormException(fields, getString("common:Errors.MissingField"));
			}
		}
		
		// Reason
		validateParameterString("reason", 0, Subscription.MAXSIZE_REASON);
	}

	@Override
	public void commit() throws Exception
	{
		this.sub.setAdvanceNotice(getParameterInteger("advancenotice"));
		this.sub.setAlwaysAvailable(getParameterString("always").equals("1"));
		
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.setTime(this.sub.getCreatedDate());
		for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
		{
			int yyyy = cal.get(Calendar.YEAR);
			int mm = cal.get(Calendar.MONTH)+1;
			String bitMap = getParameterString("avail." + String.valueOf(yyyy) + "." + String.valueOf(mm));
			BitSet bs = new BitSet(bitMap.length());
			for (int c=0; c<bitMap.length(); c++)
			{
				if (bitMap.charAt(c)=='1')
				{
					bs.set(c);
				}
				else
				{
					bs.clear(c);
				}
			}
			this.sub.setAvailable(yyyy, mm, bs);
			cal.add(Calendar.MONTH, 1);
		}

		this.sub.setReason(getParameterString("reason"));
		this.sub.setAcceptOtherPhysician(isParameter("expedite"));

		SubscriptionStore.getInstance().save(this.sub);
		
		throw new RedirectException(SubscriptionsPage.COMMAND, null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:Subscription.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (this.warning!=null)
		{
			write("<div class=WarningMessage>");
			writeEncode(this.warning);
			write("</div>");
		}

		writeFormOpen();
		TwoColFormControl twoCol = new TwoColFormControl(this);

		// - - -
		// Procedure information
		
		twoCol.writeSubtitleRow(getString("patient:Subscription.AppointmentInfo"));
		
		// Procedure
		twoCol.writeRow(getString("patient:Subscription.Procedure"));
		int minAdvanceNotice = 0;
		List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(this.sub.getID());
		for (int i=0; i<procIDs.size(); i++)
		{
			if (i>0)
			{
				twoCol.write(", ");
			}
			Procedure proc = ProcedureStore.getInstance().load(procIDs.get(i));
			twoCol.writeLink(proc.getDisplayName(),
							getPageURL(ProcedureInfoPage.COMMAND, new ParameterMap(ProcedureInfoPage.PARAM_ID, proc.getID().toString())));
			
			minAdvanceNotice = Math.max(minAdvanceNotice, proc.getLead());
		}
		
		// Physician
		List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(this.sub.getID());
		if (physicianIDs.size()>0)
		{
			twoCol.writeRow(getString("patient:Subscription.Physician"));
			for (int i=0; i<physicianIDs.size(); i++)
			{
				if (i>0)
				{
					twoCol.write(", ");
				}
				User physician = UserStore.getInstance().load(physicianIDs.get(i));
				twoCol.writeEncode(physician.getDisplayName());
			}

			// Options
			twoCol.write("<br>");
			if (this.readOnly==false)
			{
				twoCol.write("<small>");
				twoCol.writeCheckbox("expedite", getString("patient:Subscription.AccentOtherPhysician"), this.sub.isAcceptOtherPhysician());
				twoCol.write("</small>");
			}
			else if (this.sub.isAcceptOtherPhysician())
			{
				twoCol.write("<small>");
				twoCol.writeEncode(getString("patient:Subscription.AccentOtherPhysician"));
				twoCol.write("</small>");
			}
		}
		
		// Service area
		ServiceArea area = ServiceAreaStore.getInstance().load(this.sub.getServiceAreaID());
		twoCol.writeRow(getString("patient:Subscription.ServiceArea"));
		twoCol.writeEncode(area.getName());
		
		// Original date
		if (this.sub.getOriginalDate()!=null)
		{
			twoCol.writeRow(getString("patient:Subscription.OriginalDate"));
			twoCol.writeEncodeDate(this.sub.getOriginalDate());
		}
		
		// - - -
		// Scheduling preferences
		
		twoCol.writeSubtitleRow(getString("patient:Subscription.SchedulingPreferences"));

		twoCol.writeRow(getString("patient:Subscription.AdvanceNotice"));
		if (this.readOnly)
		{
			twoCol.writeEncodeLong(this.sub.getAdvanceNotice());
		}
		else
		{
			twoCol.writeNumberInput("advancenotice", this.sub.getAdvanceNotice(), 2, minAdvanceNotice, Subscription.MAX_ADVANCE_NOTICE);
		}
		twoCol.write(" ");
		twoCol.writeEncode(getString("patient:Subscription.AdvanceNoticeDays"));

		twoCol.writeSpaceRow();
		
		// Availability
		twoCol.writeRow(getString("patient:Subscription.Availability"));
		
		twoCol.write("<table><tr><td>");
		twoCol.writeRadioButton("always", getString("patient:Subscribe.AvailableAlways"), 1, this.sub.isAlwaysAvailable()?1:0);
		twoCol.write("</td></tr><tr><td>");
		twoCol.writeRadioButton("always", getString("patient:Subscribe.AvailableMatrix"), 0, this.sub.isAlwaysAvailable()?1:0);
		twoCol.write("</td></tr></table><br>");
		
		int COLS = getContext().getUserAgent().isSmartPhone()? 2 : 3; // Must be a divider or Subscription.MAX_AVAILABILITY_MONTHS
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.setTime(this.sub.getCreatedDate());
		twoCol.write("<table>");
		for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
		{
			if (i%COLS==0)
			{
//				if (i>0)
//				{
//					twoCol.write("<tr><td colspan=");
//					twoCol.write(COLS);
//					twoCol.write(">&nbsp;</td></tr>");
//				}
				twoCol.write("<tr>");
			}
			twoCol.write("<td>");
			
			int yyyy = cal.get(Calendar.YEAR);
			int mm = cal.get(Calendar.MONTH)+1;
			DaysOfMonthChooserControl ctrl = new DaysOfMonthChooserControl(twoCol)
				.setMonth(yyyy, mm)
				.disableBefore(new Date())
				.setName("avail." + String.valueOf(yyyy) + "." + String.valueOf(mm));
			if (this.readOnly)
			{
				ctrl.readOnly();
			}
			
			BitSet bs = this.sub.getAvailable(yyyy, mm);
			for (int b=0; b<bs.length(); b++)
			{
				if (bs.get(b))
				{
					ctrl.select(b+1);
				}
			}
			ctrl.render();
			
			twoCol.write("</td>");
			if (i%COLS==COLS-1)
			{
				twoCol.write("</tr>");
			}
			
			cal.add(Calendar.MONTH, 1);
		}
		twoCol.write("</table>");

		twoCol.writeSpaceRow();

		// Reason
		if (this.readOnly==false)
		{
			twoCol.writeRow(getString("patient:Subscription.Reason"), getString("patient:Subscription.ReasonHelp"));
			twoCol.writeTextAreaInput("reason", this.sub.getReason(), 60, 3, Subscription.MAXSIZE_REASON);
		}
		else if (!Util.isEmpty(this.sub.getReason()))
		{
			twoCol.writeRow(getString("patient:Subscription.Reason"), getString("patient:Subscription.ReasonHelp"));
			twoCol.writeEncode(this.sub.getReason());
		}
		
		twoCol.render();
		
		if (this.readOnly==false)
		{
			write("<br>");
			writeSaveButton(this.sub);
		}
		
		// Postback
		writeHiddenInput(PARAM_ID, null);
		
		writeFormClose();
	}
}
