package elert.pages.patient;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import elert.app.ElertConsts;
import elert.database.FacilityStore;
import elert.database.PhysicianFacilityLinkStore;
import elert.database.PhysicianProcedureTypeLinkStore;
import elert.database.Procedure;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import elert.database.Region;
import elert.database.RegionStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.database.Subscription;
import elert.database.SubscriptionFacilityLinkStore;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import samoyan.controls.ButtonInputControl;
import samoyan.controls.DaysOfMonthChooserControl;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class NewSubscriptionWizardPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/subscribe";

	private Integer step;
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("next"))
		{
			this.step ++;
		}

		if (isParameter("finish"))
		{
			// Create subscription
			Subscription sub = new Subscription();
			sub.setUserID(getContext().getUserID());
			sub.setServiceAreaID(getParameterUUID("area"));
			sub.setAcceptOtherPhysician(isParameter("expedite"));
			sub.setReason(getParameterString("reason"));
			if (getParameterString("originalchk").equals("1"))
			{
				sub.setOriginalDate(getParameterDate("originaldate"));
			}
			sub.setAdvanceNotice(getParameterInteger("advancenotice"));
			
			sub.setAlwaysAvailable(getParameterString("always").equals("1"));
			
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
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
				sub.setAvailable(yyyy, mm, bs);
				cal.add(Calendar.MONTH, 1);
			}

			Procedure proc = ProcedureStore.getInstance().load(getParameterUUID("proc"));
			sub.setDuration(proc.getDuration());
			
			SubscriptionStore.getInstance().save(sub);
			
			// Link to procedure
			SubscriptionProcedureLinkStore.getInstance().addProcedureToSubscription(getParameterUUID("proc"), sub.getID());

			// Link to physician
			if (getParameterUUID("physician")!=null)
			{
				SubscriptionPhysicianLinkStore.getInstance().addPhysicianToSubscription(getParameterUUID("physician"), sub.getID());
			}
			
			// Link to facilities
			for (UUID facilityID : getFacilitiesForProcedureInServiceArea(getParameterUUID("area"), getParameterUUID("proc")))
			{
				SubscriptionFacilityLinkStore.getInstance().addFacilityToSubscription(facilityID, sub.getID());
			}
			
			throw new RedirectException(SubscriptionsPage.COMMAND, null);
		}
	}

	private List<UUID> getFacilitiesForProcedureInServiceArea(UUID areaID, UUID procedureID) throws Exception
	{
		// Link to facilities
		List<UUID> facilitiesInServiceArea = FacilityStore.getInstance().queryByServiceArea(getParameterUUID("area"));
		List<UUID> facilitiesForProcedure = ProcedureFacilityLinkStore.getInstance().getFacilitiesWithAssignedProcedure(getParameterUUID("proc"));
		List<UUID> intersect = new ArrayList<UUID>(facilitiesInServiceArea);
		intersect.retainAll(facilitiesForProcedure);
		return intersect;
	}
	
	private List<UUID> getPhysiciansForProcedureInServiceArea(UUID areaID, UUID procedureID) throws Exception
	{
		Set<UUID> physicianSet = new HashSet<UUID>();
		List<UUID> facilityIDs = getFacilitiesForProcedureInServiceArea(getParameterUUID("area"), getParameterUUID("proc"));
		for (UUID facilityID : facilityIDs)
		{
			physicianSet.addAll(PhysicianFacilityLinkStore.getInstance().getPhysiciansAssignedToFacility(facilityID));
		}
		
		// Physician must be specialized in the procedure type
		UserGroup group = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS);
		Procedure proc = ProcedureStore.getInstance().load(procedureID);
		Iterator<UUID> iter = physicianSet.iterator();
		while (iter.hasNext())
		{
			UUID physicianID = iter.next();
			if (PhysicianProcedureTypeLinkStore.getInstance().isPhysicianSpecialized(physicianID, proc.getTypeID())==false)
			{
				iter.remove();
				continue;
			}

			if (UserUserGroupLinkStore.getInstance().isUserInGroup(physicianID, group.getID())==false)
			{
				iter.remove();
				continue;
			}

			User physician = UserStore.getInstance().load(physicianID);
			if (physician.isTerminated())
			{
				iter.remove();
				continue;
			}
		}
		
		return new ArrayList<UUID>(physicianSet);
	}
	
	@Override
	public void init() throws Exception
	{
		this.step = getParameterInteger("step");
		if (this.step==null)
		{
			this.step = 1;
		}
	}

	@Override
	public void validate() throws Exception
	{
		if (isParameter("prev"))
		{
			this.step --;
		}

		if (this.step>=1)
		{
			UUID areaID = validateParameterUUID("area");
			if (ServiceAreaStore.getInstance().load(areaID)==null)
			{
				throw new WebFormException("area", getString("common:Errors.InvalidValue"));
			}
		}
		if (this.step>=2)
		{
			UUID typeID = validateParameterUUID("proctype");
			if (ProcedureTypeStore.getInstance().load(typeID)==null)
			{
				throw new WebFormException("proctype", getString("common:Errors.InvalidValue"));
			}
		}
		if (this.step>=3)
		{
			UUID procID = validateParameterUUID("proc");
			if (ProcedureStore.getInstance().load(procID)==null)
			{
				throw new WebFormException("proc", getString("common:Errors.InvalidValue"));
			}
		}
		if (this.step>=4)
		{
			if (!isParameter("physician"))
			{
				throw new WebFormException("physician", getString("common:Errors.MissingField"));
			}
			if (!Util.isEmpty(getParameterString("physician")))
			{
				UUID physicianID = validateParameterUUID("physician");
//				User physician = UserStore.getInstance().load(physicianID);
//				if (physician==null || physician.isTerminated())
//				{
//					throw new WebFormException("physician", getString("common:Errors.InvalidValue"));
//				}
//				
//				// Make sure the user is a physician
//				UserGroup group = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS);
//				if (UserUserGroupLinkStore.getInstance().isUserInGroup(physicianID, group.getID())==false)
//				{
//					throw new WebFormException("physician", getString("common:Errors.InvalidValue"));
//				}
//				
//				// Double check physician is assigned to this service area
//				List<UUID> physicianIDs = PhysicianFacilityLinkStore.getInstance().queryByServiceArea(getParameterUUID("area"));
//				if (physicianIDs.contains(physicianID)==false)
//				{
//					throw new WebFormException("physician", getString("common:Errors.InvalidValue"));
//				}
//				
//				// Double check physician has the specialty required
//				if (!PhysicianProcedureTypeLinkStore.getInstance().isPhysicianSpecialized(physicianID, getParameterUUID("proctype")))
//				{
//					throw new WebFormException("physician", getString("common:Errors.InvalidValue"));
//				}
				
				// Double check the physician is assigned to a facility that can perform the procedure
				List<UUID> physicianIDs = getPhysiciansForProcedureInServiceArea(getParameterUUID("area"), getParameterUUID("proc"));
				if (physicianIDs.contains(physicianID)==false)
				{
					throw new WebFormException("physician", getString("common:Errors.InvalidValue"));
				}
			}
		}
		if (this.step>=5)
		{
			if (getParameterString("originalchk").equals("1"))
			{
				Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
				Date today = cal.getTime();
				cal.add(Calendar.YEAR, 1);
				Date nextYear = cal.getTime();
				validateParameterDate("originaldate", today, nextYear);
			}
			
			Procedure proc = ProcedureStore.getInstance().load(getParameterUUID("proc"));
			validateParameterInteger("advancenotice", proc.getLead(), Subscription.MAX_ADVANCE_NOTICE);

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
			
			validateParameterString("reason", 0, Subscription.MAXSIZE_REASON);
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:Subscribe.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		
		writeFormOpen();
		renderProgress(this.step);
		
		if (this.step>=1)
		{
			renderStep1();
		}
		if (this.step>=2)
		{
			renderStep2();
		}
		if (this.step>=3)
		{
			renderStep3();
		}
		if (this.step>=4)
		{
			renderStep4();
		}
		if (this.step>=5)
		{
			renderStep5();
		}
		
		write("<br>");
		if (this.step<5)
		{
			writeButton("next", getString("patient:Subscribe.NextBtn"));
		}
		else
		{
			writeButton("finish", getString("patient:Subscribe.FinishBtn"));
		}
		if (this.step>1)
		{
			write(" ");
			new ButtonInputControl(this, "prev").setValue(getString("patient:Subscribe.BackBtn")).setSubdued(true).render();
//			writeButton("prev", getString("patient:Subscribe.BackBtn"));
		}
		
		write("<input type=hidden name=step value=");
		write(this.step);
		write(">");
		
		writeFormClose();
	}

	private void renderProgress(int step)
	{
		write("<h2>");
		writeEncode(getString("patient:Subscribe.TitleStep" + step));
		write("</h2>");
	}
	
	/**
	 * Choose service area.
	 * @throws Exception
	 */
	private void renderStep1() throws Exception
	{
		if (this.step==1)
		{
			writeEncode(getString("patient:Subscribe.ServiceAreaHelp", Setup.getAppOwner(getLocale())));
			write("<br><br>");
			
			List<UUID> regionIDs = RegionStore.getInstance().getAllIDs();
			for (UUID regiodID : regionIDs)
			{
				List<UUID> areaIDs = ServiceAreaStore.getInstance().getByRegion(regiodID);
				if (areaIDs.size()==0) continue;
				
				Region region = RegionStore.getInstance().load(regiodID);
				write("<h3>");
				writeEncode(region.getName());
				write("</h3>");
				
				write("<table>");
				for (UUID areaID : areaIDs)
				{
					ServiceArea area = ServiceAreaStore.getInstance().load(areaID);
	
					write("<tr><td>");
					writeRadioButton("area", area.getName(), areaID, null);
					write("</tr></td>");
				}
				write("</table>");
			}
		}
		else
		{
			writeHiddenInput("area", null);
		}
	}
	
	/**
	 * Choose procedure types that are linked to any facility in the chosen area.
	 * @throws Exception
	 */
	private void renderStep2() throws Exception
	{
		if (this.step==2)
		{
			writeEncode(getString("patient:Subscribe.ProcedureTypeHelp"));
			write("<br><br>");
	
			UUID areaID = getParameterUUID("area");
			List<UUID> procedureTypeIDs = ProcedureTypeStore.getInstance().queryByServiceArea(areaID);
			
			write("<table>");
			for (UUID typeID : procedureTypeIDs)
			{
				ProcedureType type = ProcedureTypeStore.getInstance().load(typeID);
	
				write("<tr><td>");
				writeRadioButton("proctype", type.getName(), typeID, null);
				write("</tr></td>");
			}
			write("</table>");
		}
		else
		{
			writeHiddenInput("proctype", null);
		}
	}
	
	/**
	 * Choose procedure that is linked to any facility in the chosen area.
	 * @throws Exception
	 */
	private void renderStep3() throws Exception
	{
		if (this.step==3)
		{
			writeEncode(getString("patient:Subscribe.ProcedureHelp"));
			write("<br><br>");
	
			UUID areaID = getParameterUUID("area");
			UUID typeID = getParameterUUID("proctype");
			List<UUID> procedureIDs = ProcedureStore.getInstance().queryByServiceAreaAndType(areaID, typeID);
			
			write("<table>");
			for (UUID procID : procedureIDs)
			{
				Procedure proc = ProcedureStore.getInstance().load(procID);
	
				write("<tr><td>");
				writeRadioButton("proc", proc.getDisplayName(), procID, null);
				if (!Util.isEmpty(proc.getShortDescription()))
				{
					write("<small style='display:none;margin-left:25px;'>");
					writeEncode(proc.getShortDescription());
					write("</small>");
//					writeTooltip(proc.getDisplayName(), proc.getShortDescription());
				}
//				else
//				{
//					writeEncode(proc.getDisplayName());
//				}
				write("</tr></td>");
			}
			write("</table>");
			
			write("<script>$('INPUT[name=proc]').on('click',function(){");
			write("var $x=$(this);");
//			write("$x.parents('TABLE').first().find('SMALL').css('display','none');");
			write("$x.siblings('SMALL').css('display','block');");
			write("});</script>");
		}
		else
		{
			writeHiddenInput("proc", null);
		}
	}

	/**
	 * Choose a physician that is linked to any facility in the chosen area.
	 * @throws Exception
	 */
	private void renderStep4() throws Exception
	{
		if (this.step==4)
		{
			writeEncode(getString("patient:Subscribe.PhysicianHelp"));
			write("<br><br>");
	
			UUID areaID = getParameterUUID("area");
//			List<UUID> physicianIDs = PhysicianFacilityLinkStore.getInstance().queryByServiceArea(areaID);
			UUID procID = getParameterUUID("proc");
			List<UUID> physicianIDs = getPhysiciansForProcedureInServiceArea(areaID, procID);
			
			UUID procTypeID = getParameterUUID("proctype");

			write("<table>");
			for (UUID physicianID : physicianIDs)
			{
				User physician = UserStore.getInstance().load(physicianID);
					
				write("<tr><td>");
				writeRadioButton("physician", physician.getDisplayName(), physicianID, null);
				write("</tr></td>");
			}
			
			write("<tr><td>");
			writeRadioButton("physician", getString("patient:Subscribe.PhysicianUnassigned"), "", null);
			write("</tr></td>");

			write("<tr><td>");
			writeRadioButton("physician", getString("patient:Subscribe.PhysicianNone"), "", null);
			write("</tr></td>");

			write("<tr><td>");
			writeRadioButton("physician", getString("patient:Subscribe.PhysicianOther"), "", null);
			write("</tr></td>");

			write("</table><br>");
			
			writeCheckbox("expedite", getString("patient:Subscribe.AccentOtherPhysician"), false);
			write("<br>");
		}
		else
		{
			writeHiddenInput("physician", null);
			writeHiddenInput("expedite", null);
		}
	}
	
	/**
	 * Scheduling.
	 * @throws Exception
	 */
	private void renderStep5() throws Exception
	{
		if (this.step==5)
		{
			writeEncode(getString("patient:Subscribe.OriginalDateHelp"));
			write("<br><br>");
	
			write("<table>");
			write("<tr valign=middle><td>");
			writeRadioButton("originalchk", null, 0, 0);
			write("</td><td>");
			writeEncode(getString("patient:Subscribe.NotYetScheduled"));
			write("</td></tr>");
			write("<tr valign=middle><td>");
			writeRadioButton("originalchk", null, 1, 0);
			write("</td><td>");
			writeEncode(getString("patient:Subscribe.ScheduledFor"));
			write(" ");
			writeDateInput("originaldate", null);
			write("</td></tr>");
			write("</table>");
			
			write("<br>");
			writeEncode(getString("patient:Subscribe.AdvanceNoticeHelp"));
			write("<br><br>");

			Procedure proc = ProcedureStore.getInstance().load(getParameterUUID("proc"));			
			writeNumberInput("advancenotice", proc.getLead(), 2, proc.getLead(), Subscription.MAX_ADVANCE_NOTICE);
			write(" ");
			writeEncode(getString("patient:Subscribe.AdvanceNoticeDays"));
			write("<br><br>");

			writeEncode(getString("patient:Subscribe.CalendarHelp"));
			write("<br><br>");
			
			write("<table><tr><td>");
			writeRadioButton("always", getString("patient:Subscribe.AvailableAlways"), 1, 1);
			write("</td></tr><tr><td>");
			writeRadioButton("always", getString("patient:Subscribe.AvailableMatrix"), 0, 1);
			write("</td></tr></table><br>");
			
			int COLS = getContext().getUserAgent().isSmartPhone()? 2 : 3; // Must be divider of Subscription.MAX_AVAILABILITY_MONTHS
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			write("<table>");
			for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
			{
				if (i%COLS==0)
				{
//					if (i>0)
//					{
//						write("<tr><td colspan=");
//						write(COLS);
//						write(">&nbsp;</td></tr>");
//					}
					write("<tr>");
				}
				write("<td>");
				
				int yyyy = cal.get(Calendar.YEAR);
				int mm = cal.get(Calendar.MONTH)+1;
				new DaysOfMonthChooserControl(this)
					.setMonth(yyyy, mm)
					.disableBefore(new Date())
					.setName("avail." + String.valueOf(yyyy) + "." + String.valueOf(mm))
					.render();
				
				write("</td>");
				if (i%COLS==COLS-1)
				{
					write("</tr>");
				}
				
				cal.add(Calendar.MONTH, 1);
			}
			write("</table>");
			
			write("<br>");
			writeEncode(getString("patient:Subscribe.ReasonHelp"));
			write("<br><br>");
			writeTextAreaInput("reason", null, 80, 3, Subscription.MAXSIZE_REASON);
			write("<br>");
		}
		else
		{
			writeHiddenInput("originalchk", null);
			writeHiddenInput("originaldate", null);
			writeHiddenInput("advancenotice", null);
			writeHiddenInput("reason", null);

			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			for (int i=0; i<Subscription.MAX_AVAILABILITY_MONTHS; i++)
			{
				int yyyy = cal.get(Calendar.YEAR);
				int mm = cal.get(Calendar.MONTH)+1;
				writeHiddenInput("avail." + String.valueOf(yyyy) + "." + String.valueOf(mm), null);
				cal.add(Calendar.MONTH, 1);
			}
		}
	}
}
