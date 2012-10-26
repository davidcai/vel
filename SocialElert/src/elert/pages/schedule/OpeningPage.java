package elert.pages.schedule;

import java.util.*;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TabControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.CollectionsEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.database.*;
import elert.pages.ElertPage;
import elert.pages.common.SubscriptionSorter;
import elert.pages.common.SubscriptionSorter.SortByElertResponse;
import elert.pages.patient.ElertNotif;

public class OpeningPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/opening";
	public final static String PARAM_ID = "id";

	private Opening opening;
	private String tab;
	private List<Subscription> subscriptions = null;
	private Map<UUID, Elert> subToElertMap = null;
	private boolean readOnly = false;
	
	private List<SubscriptionSorter> groupers;
	private List<SubscriptionSorter> sorters;

	@Override
	public void commit() throws Exception
	{
		Date now = new Date();
		
		if (isParameter("send"))
		{
			// Send eLert for each subscription
			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID subID = UUID.fromString(p.substring(4));
				Subscription sub = SubscriptionStore.getInstance().load(subID);
				
				Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
				ServiceArea serviceArea = ServiceAreaStore.getInstance().load(facility.getServiceAreaID());
				
				// Create an eLert for the subscription
				Elert elert = new Elert();
				elert.setDateSent(now);
				elert.setFacilityID(facility.getID());
				elert.setOpeningID(this.opening.getID());
				elert.setPatientID(sub.getUserID());
				elert.setRegionID(serviceArea.getRegionID());
				elert.setSchedulerID(getContext().getUserID());
				elert.setServiceAreaID(serviceArea.getID());
				elert.setSubscriptionID(sub.getID());
				elert.setDateOpening(this.opening.getDateTime());
				ElertStore.getInstance().save(elert);
				
				// Send notification
				Notifier.send(elert.getPatientID(), elert.getID(), ElertNotif.COMMAND, new ParameterMap(ElertNotif.PARAM_ELERT_ID, elert.getID().toString()));
			}
			
			ParameterMap params = new ParameterMap();
			params.plus(PARAM_ID, getParameterString(PARAM_ID));
			params.plus("na", getParameterString("na"));
			params.plus("dp", getParameterString("dp"));
			params.plus("da", getParameterString("da"));
			params.plus("group", getParameterString("group"));
			params.plus("sort", getParameterString("sort"));
			params.plus("tab", "c");
			
			throw new RedirectException(getContext().getCommand(), params);
		}
		
		if (isParameter("finalists"))
		{
			StringBuilder elertIDs = new StringBuilder();
			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID subID = UUID.fromString(p.substring(4));
				if (elertIDs.length()>0)
				{
					elertIDs.append(",");
				}
				
				Elert elert = this.subToElertMap.get(subID);
				elertIDs.append(elert.getID().toString());
			}
			
			throw new RedirectException(FinalizePage.COMMAND,
										new ParameterMap(FinalizePage.PARAM_ELERT_IDS, elertIDs.toString()));
		}
	}

	@Override
	public void validate() throws Exception
	{
		if (this.readOnly)
		{
			throw new PageNotFoundException();
		}
		
		if (isParameter("send") || isParameter("finalists"))
		{
			boolean found = false;
			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID subscriptionID = UUID.fromString(p.substring(4));
				Subscription sub = SubscriptionStore.getInstance().load(subscriptionID);
				if (sub==null)
				{
					throw new WebFormException(p, getString("common:Errors.InvalidValue"));
				}
				found = true;
			}
			if (found==false)
			{
				List<String> fields = new ArrayList<String>();
				for (Subscription sub : this.subscriptions)
				{
					fields.add("chk_" + sub.getID().toString());
				}
				throw new WebFormException(fields, getString("common:Errors.MissingField"));
			}
		}
	}

	@Override
	public void init() throws Exception
	{
		this.opening = OpeningStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.opening==null)
		{
			throw new PageNotFoundException();
		}
		this.readOnly = this.opening.isClosed();
		
		this.tab = getParameterString("tab");
		if (this.tab==null)
		{
			List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
			if (elertIDs.size()>0)
			{
				this.tab = "e";
			}
			else
			{
				this.tab = "c";
			}
		}
		if (this.readOnly)
		{
			this.tab = "e";
		}
			
		if (this.tab.equals("c"))
		{
			// Fetch candidates
			this.subscriptions = findCandidates(this.opening, getContext().getUserID(), isParameter("na"), isParameter("da"), isParameter("dp"));
			this.subToElertMap = null;
		}
		else
		{
			// Fetch
			this.subToElertMap = new HashMap<UUID, Elert>();
			this.subscriptions = new ArrayList<Subscription>();
			List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
			for (UUID elertID : elertIDs)
			{
				Elert elert = ElertStore.getInstance().load(elertID);
				this.subscriptions.add(SubscriptionStore.getInstance().load(elert.getSubscriptionID()));
				this.subToElertMap.put(elert.getSubscriptionID(), elert);
			}
		}

		// Init groupers and sorters
		this.groupers = new ArrayList<SubscriptionSorter>();
		this.groupers.add(new SubscriptionSorter.AllEqual());
		this.groupers.add(new SubscriptionSorter.SortByMatch(this.opening, 20));
		this.groupers.add(new SubscriptionSorter.SortByPhysician());
		this.groupers.add(new SubscriptionSorter.SortByProcedure());
		this.groupers.add(new SubscriptionSorter.SortByUrgency());
		if (this.tab.equalsIgnoreCase("e"))
		{
			this.groupers.add(new SubscriptionSorter.SortByElertResponse(this.opening));
		}
		this.groupers.add(new SubscriptionSorter.SortByDuration(60));
		this.groupers.add(new SubscriptionSorter.SortByVerified());
		this.groupers.add(new SubscriptionSorter.SortByDateSubscribed(getLocale(), getTimeZone()));

		this.sorters = new ArrayList<SubscriptionSorter>();
		this.sorters.add(new SubscriptionSorter.SortByMatch(this.opening, 20));
		this.sorters.add(new SubscriptionSorter.SortByUrgency());
		if (this.tab.equalsIgnoreCase("e"))
		{
			this.sorters.add(new SubscriptionSorter.SortByElertResponse(this.opening));
		}
		this.sorters.add(new SubscriptionSorter.SortByDuration(1));
		this.sorters.add(new SubscriptionSorter.SortByVerified());
		this.sorters.add(new SubscriptionSorter.SortByDateSubscribed(getLocale(), getTimeZone()));		
	}

	@Override
	public String getTitle() throws Exception
	{
		Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
		return getString("schedule:Opening.Title", this.opening.getDateTime(), facility.getCode());
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		if (this.readOnly)
		{
			write("<div class=WarningMessage>");
			writeEncode(getString("schedule:Opening.OpeningIsClosed"));
			write("</div>");
		}

		renderDetails();
		write("<br>");
				
		ParameterMap params = new ParameterMap(PARAM_ID, this.opening.getID().toString());
		
		TabControl tabCtrl = new TabControl(this);
		tabCtrl.addTab(	"e",
						getString("schedule:Opening.TabElerts"),
						getPageURL(ctx.getCommand(), params.plus("tab", "e")));
		tabCtrl.addTab(	"c",
						getString("schedule:Opening.Candidates"),
						this.readOnly? null : getPageURL(ctx.getCommand(), params.plus("tab", "c")));
		tabCtrl.setCurrentTab(this.tab);
		tabCtrl.render();
		
		if (this.tab.equals("c"))
		{
			renderCandidates();
		}
		else
		{
			renderElerts();
		}
	}
	
	private void renderDetails() throws Exception
	{
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:Opening.DateTime"));
		twoCol.writeEncodeDateTime(this.opening.getDateTime());

		twoCol.write(" (");
		twoCol.writeEncodeLong(this.opening.getDuration());
		if (this.opening.getOriginalDuration()!=this.opening.getDuration())
		{
			twoCol.write("<span class=Faded>/");
			twoCol.writeEncodeLong(opening.getOriginalDuration());
			twoCol.write("</span>");
		}
		twoCol.write(" ");
		twoCol.writeEncode(getString("schedule:Opening.Minutes"));
		twoCol.write(")");
		
		Facility facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
		twoCol.writeRow(getString("schedule:Opening.Facility"));
		twoCol.writeEncode(facility.getName());
		
		if (!Util.isEmpty(this.opening.getRoom()))
		{
			twoCol.write(" (");
			twoCol.writeEncode(this.opening.getRoom());
			twoCol.write(")");
		}
		
		List<UUID> procedures = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(this.opening.getID());
		for (int i=0; i<procedures.size(); i++)
		{
			Procedure proc = ProcedureStore.getInstance().load(procedures.get(i));
			if (i==0)
			{
				twoCol.writeRow(getString("schedule:Opening.Procedure"));
			}
			else
			{
				twoCol.write(", ");
			}
			twoCol.writeEncode(proc.getName());
		}

		
		List<UUID> physicians = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(this.opening.getID());
		for (int i=0; i<physicians.size(); i++)
		{
			User physician = UserStore.getInstance().load(physicians.get(i));
			if (i==0)
			{
				twoCol.writeRow(getString("schedule:Opening.Physician"));
			}
			else
			{
				twoCol.write(", ");
			}
			twoCol.writeEncode(physician.getDisplayName());
		}
		
		twoCol.render();
	}
	
	private void renderElerts() throws Exception
	{
		// Render filter
		writeFormOpen("GET", getContext().getCommand());
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:Opening.GroupBy"));
		
		SelectInputControl select = new SelectInputControl(twoCol, "group");
		int i = 0;
		for (SubscriptionSorter sorter : this.groupers)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			if (sorter instanceof SortByElertResponse)
			{
				select.setInitialValue(i);
				break;
			}
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.writeRow(getString("schedule:Opening.SortBy"));
		
		select = new SelectInputControl(twoCol, "sort");
		i = 0;
		for (SubscriptionSorter sorter : this.sorters)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.render();		
//		write("<br>");
//		writeButton(getString("schedule:Opening.Refresh"));
		
		writeHiddenInput(PARAM_ID, null);
		writeHiddenInput("tab", "e");
		writeFormClose();
		write("<br>");

		
		// Render list of eLerted patients
		if (this.subscriptions.size()==0)
		{
			writeEncode(getString("schedule:Opening.NoElerts"));
			write("<br>");
		}
		else
		{
			writeFormOpen();
			writeHiddenInput("group", null);
			writeHiddenInput("sort", null);
			writeHiddenInput(PARAM_ID, null);
			writeHiddenInput("tab", "e");
			
			renderSubscriptions();
		
			if (!this.readOnly)
			{
				write("<br>");
				writeButton("finalists", getString("schedule:Opening.SelectFinalists"));
			}
			
			writeLegend();
			
			writeFormClose();
		}
	}
	
	private void renderCandidates() throws Exception
	{
		// Render filter
		writeFormOpen("GET", getContext().getCommand());
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:Opening.GroupBy"));
		
		SelectInputControl select = new SelectInputControl(twoCol, "group");
		int i = 0;
		for (SubscriptionSorter sorter : this.groupers)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.writeRow(getString("schedule:Opening.SortBy"));
		
		select = new SelectInputControl(twoCol, "sort");
		i = 0;
		for (SubscriptionSorter sorter : this.sorters)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.writeRow(getString("schedule:Opening.Filters"));
		new CheckboxInputControl(twoCol, "na")
			.setLabel(getString("schedule:Opening.IncludeNeighboring"))
			.setInitialValue(isParameter("na"))
			.setAutoSubmit(true)
			.render();
//		twoCol.writeCheckbox("na", getString("schedule:Opening.IncludeNeighboring"), isParameter("na"));
		twoCol.write("<br>");
		new CheckboxInputControl(twoCol, "dp")
			.setLabel(getString("schedule:Opening.DisregardPhysician"))
			.setInitialValue(isParameter("dp"))
			.setAutoSubmit(true)
			.render();
//		twoCol.writeCheckbox("dp", getString("schedule:Opening.DisregardPhysician"), isParameter("dp"));
		twoCol.write("<br>");
		new CheckboxInputControl(twoCol, "da")
			.setLabel(getString("schedule:Opening.DisregardAvailability"))
			.setInitialValue(isParameter("da"))
			.setAutoSubmit(true)
			.render();
//		twoCol.writeCheckbox("da", getString("schedule:Opening.DisregardAvailability"), isParameter("da"));
		twoCol.write("<br>");

		twoCol.render();		
//		write("<br>");
//		writeButton(getString("schedule:Opening.FindCandidates"));
		
		writeHiddenInput(PARAM_ID, null);
		writeHiddenInput("tab", "c");
		writeFormClose();
		write("<br>");
		

		// Render list of candidates
		if (this.subscriptions.size()==0)
		{
			writeEncode(getString("schedule:Opening.NoCandidates"));
			write("<br>");
		}
		else
		{
			writeFormOpen();
			
			writeHiddenInput(PARAM_ID, null);
			writeHiddenInput("na", null);
			writeHiddenInput("dp", null);
			writeHiddenInput("da", null);
			writeHiddenInput("tab", "c");
			writeHiddenInput("group", null);
			writeHiddenInput("sort", null);

			renderSubscriptions();
			
			write("<br>");
			writeButton("send", getString("schedule:Opening.SendElerts"));
			
			writeLegend();
			
			writeFormClose();
		}
	}
	
	private void renderSubscriptions() throws Exception
	{
		// Perform grouping and sorting
		Integer grouperIdx = getParameterInteger("group");
		if (grouperIdx==null)
		{
			if (this.tab.equalsIgnoreCase("e"))
			{
				for (int x=0; x<this.groupers.size(); x++)
				{
					if (this.groupers.get(x) instanceof SortByElertResponse)
					{
						grouperIdx = x;
						break;
					}
				}
			}
			else
			{
				grouperIdx = 0;
			}
		}
		Integer sorterIdx = getParameterInteger("sort");
		if (sorterIdx==null) sorterIdx = 0;
		SubscriptionSorter grouper = this.groupers.get(grouperIdx);
		SubscriptionSorter sorter = this.sorters.get(sorterIdx);
		
		Collection<Collection<Subscription>> groups = CollectionsEx.group(this.subscriptions, grouper, sorter);

		// Render each group
		int g = 0;
		for (Collection<Subscription> group : groups)
		{
			g++;
			
			String h2Title = grouper.getGroupTitle(group.iterator().next(), getLocale());
			if (!Util.isEmpty(h2Title))
			{
				write("<h2>");
				writeEncode(h2Title);
				write("</h2>");		
			}
			
			new DataTableControl<Subscription>(this, "subs"+g, group.iterator())
			{
				@Override
				protected void defineColumns() throws Exception
				{
					if (!readOnly)
					{
						column("").width(1); // Checkbox
					}
					if (subToElertMap!=null)
					{
						column(getString("schedule:Opening.Elert")).width(1);
					}
					column("").width(1); // Verified
					column("").width(1); // Urgent
					column(getString("schedule:Opening.Procedure"));
					column(getString("schedule:Opening.Duration"));
					column(getString("schedule:Opening.Physician"));
					column(getString("schedule:Opening.Patient"));
					column(getString("schedule:Opening.Match"));
				}
	
				@Override
				protected void renderRow(Subscription sub) throws Exception
				{
					Elert elert = null;
					if (subToElertMap!=null)
					{
						elert = subToElertMap.get(sub.getID());
					}
					
					if (!readOnly)
					{
						cell();
						new CheckboxInputControl(this, "chk_" + sub.getID())
							.setDisabled(	sub.isFinalized() ||
											sub.getDuration() > opening.getDuration() ||
											(elert!=null && elert.getDecision()!=Elert.DECISION_NONE))
							.render();
	//					if (sub.isFinalized()==false &&
	//						sub.getDuration() <= opening.getDuration() &&
	//						(elert==null || elert.getDecision()==Elert.DECISION_NONE))
	//					{
	//						writeCheckbox("chk_" + sub.getID(), false);
	//					}
	//					else
	//					{
	//						write("<input type=checkbox disabled>");
	//					}
					}
					
					if (subToElertMap!=null)
					{
						cell();
						if (elert.getDecision()==Elert.DECISION_CHOSEN)
						{
							writeImage("elert/finalized.png", getString("elert:Legend.Finalized"));
						}
						else
						{
							Integer status = elert.getReply();
							if (status==Elert.REPLY_ACCEPTED)
							{
								writeImage("elert/circle-v.png", getString("elert:Legend.Accepted"));
							}
							else if (status==Elert.REPLY_DECLINED)
							{
								writeImage("elert/circle-x.png", getString("elert:Legend.Declined"));
							}
							else
							{
								writeImage("elert/circle-q.png", getString("elert:Legend.DidNotReply"));
							}
						}
					}
	
					cell();
					if (sub.getVerifiedByUserID()!=null)
					{
						writeImage("elert/verified.png", getString("elert:Legend.Verified"));
					}
					
					cell();
					if (sub.isUrgent())
					{
						writeImage("elert/urgent.png", getString("elert:Legend.Urgent"));
					}
					
					cell();
					write("<a href=\"");
					write(getPageURL(VerifySubscriptionPage.COMMAND, new ParameterMap(VerifySubscriptionPage.PARAM_ID, sub.getID().toString())));
					write("\">");
					List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(sub.getID());
					for (UUID procID : procIDs)
					{
						Procedure proc = ProcedureStore.getInstance().load(procID);
						writeEncode(proc.getName());
						write("<br>");
					}
					write("</a>");
	
					cell();
					writeEncodeLong(sub.getDuration());
					write(" ");
					writeEncode(getString("schedule:Opening.Mins"));
					
					cell();
					List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(sub.getID());
					for (UUID physicianID : physicianIDs)
					{
						User physician = UserStore.getInstance().load(physicianID);
						writeEncode(physician.getDisplayName());
						write("<br>");
					}
					
					cell();
					User patient = UserStore.getInstance().load(sub.getUserID());
					writeLink(	patient.getDisplayName(),
								getPageURL(PatientProfilePage.COMMAND, new ParameterMap(PatientProfilePage.PARAM_ID, patient.getID().toString())));
	
	
					cell();
					Integer match = OpeningStore.getInstance().matchPercentage(opening, sub);
					if (match!=null)
					{
						writeEncodeLong(match);
						writeEncode("%");
					}
					else
					{
						write("<span class=Faded>");
						writeEncode(getString("schedule:Opening.NotApplicable"));
						write("</span>");
					}
				}	
			}.render();
		}
	}
	
	private List<Subscription> findCandidates(Opening opening, UUID schedulerID, boolean includeNeighboringAreas, boolean ignoreAvailabilityPrefs, boolean disregardPhysicians) throws Exception
	{
		// First, fetch all subscriptions for the scheduler's service areas
		List<UUID> homeServiceAreaIDs = ServiceAreaUserLinkStore.getInstance().getHomeSerivceAreasForUser(schedulerID);
		List<UUID> allServiceAreaIDs = new ArrayList<UUID>(homeServiceAreaIDs);
		if (includeNeighboringAreas)
		{
			allServiceAreaIDs.addAll(ServiceAreaUserLinkStore.getInstance().getNeighboringSerivceAreasForUser(schedulerID));
		}
		List<UUID> subIDs = new ArrayList<UUID>(SubscriptionStore.getInstance().queryOpenSubscriptions(allServiceAreaIDs, opening.getDuration()));
		
		// Subtract those subscriptions that have already been eLerted for this opening
		List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
		for (UUID elertID : elertIDs)
		{
			Elert elert = ElertStore.getInstance().load(elertID);
			subIDs.remove(elert.getSubscriptionID());
		}
		
		// Load the subscriptions
		List<Subscription> subs = new ArrayList<Subscription>(subIDs.size());
		for (UUID subID : subIDs)
		{
			subs.add(SubscriptionStore.getInstance().load(subID));
		}
		
		// Safety checks (should have been filtered by query)
		Iterator<Subscription> iter = subs.iterator();
		while (iter.hasNext())
		{
			Subscription sub = iter.next();
			if (sub.getDuration() > opening.getDuration())
			{
				// Filter by duration
				iter.remove();
			}
			else if (sub.isFinalized() || sub.isRemoved() || sub.isExpired())
			{
				// Filter finalized, removed and expired subs
				iter.remove();
			}
		}
		
		// Filter by availability date
		if (!ignoreAvailabilityPrefs)
		{
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			cal.setTime(opening.getDateTime());
			int yyyy = cal.get(Calendar.YEAR);
			int mm = cal.get(Calendar.MONTH) + 1;
			int dd = cal.get(Calendar.DAY_OF_MONTH);
			
			iter = subs.iterator();
			while (iter.hasNext())
			{
				Subscription sub = iter.next();
				if (!sub.isAlwaysAvailable())
				{
					BitSet bs = sub.getAvailable(yyyy, mm);
					if (bs.get(dd-1)==false) // The bitset is zero-based
					{
						iter.remove();
					}
				}
			}
		}
		
		// Filter by facilities and physicians, but only for subscription from home area
		List<UUID> openingPhysicians = new ArrayList<UUID>();
		if (!disregardPhysicians)
		{
			openingPhysicians = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(opening.getID());
		}
		iter = subs.iterator();
		while (iter.hasNext())
		{
			Subscription sub = iter.next();
			if (homeServiceAreaIDs.contains(sub.getServiceAreaID())==false)
			{
				continue;
			}
			
			List<UUID> facilityIDs = SubscriptionFacilityLinkStore.getInstance().getFacilitiesForSubscription(sub.getID());
			if (facilityIDs.contains(opening.getFacilityID())==false)
			{
				iter.remove();
				continue;
			}
			
			if (openingPhysicians.size()>0 && !sub.isAcceptOtherPhysician())
			{
				List<UUID> subPhysicians = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(sub.getID());
				
				boolean found = subPhysicians.size()==0;
				for (UUID id : subPhysicians)
				{
					if (openingPhysicians.contains(id))
					{
						found = true;
						break;
					}
				}
				if (!found)
				{
					iter.remove();
				}
			}
		}
				
		return subs;
	}	
}
