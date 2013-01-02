package elert.pages.schedule;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.CollectionsEx;
import samoyan.core.DateFormatEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.notif.Notifier;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.ProcedureOpeningLinkStore;
import elert.database.ResourceProcedureLinkStore;
import elert.database.Subscription;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import elert.pages.common.OpeningSorter;
import elert.pages.patient.UnavailNotif;

public class UnresolvedOpeningsPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/unresolved-openings";
	
	private OpeningSorter[] groupers = {
			new OpeningSorter.AllEqual(),
			new OpeningSorter.SortByFacility(),
			new OpeningSorter.SortByRank(),
			new OpeningSorter.SortByProcedure(),
			new OpeningSorter.SortByPhysician(),
			new OpeningSorter.SortByDuration(60),
			new OpeningSorter.SortByOriginalDuration(60),
			new OpeningSorter.SortByTimeLeft(24),
			new OpeningSorter.SortByBestMatch(20),
			new OpeningSorter.SortByLastElertSentDate(24L*60L*60L*1000L),
			new OpeningSorter.SortByUrgency()
	};
	private OpeningSorter[] sorters = {
			new OpeningSorter.SortByTimeLeft(1),
			new OpeningSorter.SortByBestMatch(1),
			new OpeningSorter.SortByRank(),
			new OpeningSorter.SortByLastElertSentDate(1),
			new OpeningSorter.SortByUrgency()
	};
	
	@Override
	public void commit() throws Exception
	{
		Date now = new Date();
		
		if (isParameter("remove"))
		{
			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
			{
				UUID openingID = UUID.fromString(p.substring(4));
				if (OpeningStore.getInstance().canRemove(openingID))
				{
					// Delete the opening
					OpeningStore.getInstance().remove(openingID);
				}
				else
				{
					// Close the opening
					Opening opening = OpeningStore.getInstance().open(openingID);
					opening.setClosed(true);
					OpeningStore.getInstance().save(opening);
					
					// Inform eLerted patients that the opening is no longer available
					List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(openingID);
					for (UUID elertID : elertIDs)
					{
						Elert elert = ElertStore.getInstance().load(elertID);
						if (elert.getDecision()==Elert.DECISION_NONE)
						{
							// Record decision on eLert
							elert = (Elert) elert.clone(); // Open for writing
							elert.setDecision(Elert.DECISION_NOT_CHOSEN);
							elert.setDateDecision(now);
							ElertStore.getInstance().save(elert);

							if (elert.getReply()!=Elert.REPLY_DECLINED)
							{
								Subscription sub = SubscriptionStore.getInstance().load(elert.getSubscriptionID());
								Notifier.send(sub.getUserID(), elert.getID(), UnavailNotif.COMMAND, new ParameterMap(UnavailNotif.PARAM_ELERT_ID, elert.getID().toString()));
							}
						}
					}
				}
			}

			ParameterMap params = new ParameterMap();
			params.plus("group", getParameterString("group"));
			params.plus("sort", getParameterString("sort"));
			
			throw new RedirectException(getContext().getCommand(), params);
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:Unresolved.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		final Date now = new Date();
		final boolean phone = getContext().getUserAgent().isSmartPhone(); 

		new LinkToolbarControl(this)
			.addLink(getString("schedule:Unresolved.LogNew"), getPageURL(LogNewOpeningPage.COMMAND), "icons/standard/pencil-16.png")
			.render();

		renderSorters();
		
		// Query for the openings
		List<UUID> facilities = FacilityStore.getInstance().queryByUser(getContext().getUserID());
		List<UUID> openingIDs = OpeningStore.getInstance().queryUnresolvedOpenings(facilities);
		List<Opening> openings = new ArrayList<Opening>(openingIDs.size());
		for (UUID openingID : openingIDs)
		{
			openings.add(OpeningStore.getInstance().load(openingID));
		}
		if (openings.size()==0)
		{
			writeEncode(getString("schedule:Unresolved.NoResults"));
			writeLegend();
			return;
		}

		// Perform grouping and sorting
		Integer grouperIdx = getParameterInteger("group");
		if (grouperIdx==null) grouperIdx = 0;
		Integer sorterIdx = getParameterInteger("sort");
		if (sorterIdx==null) sorterIdx = 0;
		OpeningSorter grouper = this.groupers[grouperIdx];
		OpeningSorter sorter = this.sorters[sorterIdx];
		
		Collection<Collection<Opening>> groups = CollectionsEx.group(openings, grouper, sorter);
		
		
		// Print tables
		final DateFormat miniDateTime = DateFormatEx.getMiniDateTimeInstance(getLocale(), getTimeZone());

		writeFormOpen();

		int g = 0;
		for (Collection<Opening> group : groups)
		{
			g++;
			
			String h2Title = grouper.getGroupTitle(group.iterator().next(), getLocale());
			if (!Util.isEmpty(h2Title))
			{
				write("<h2>");
				writeEncode(h2Title);
				write("</h2>");		
			}
	
			new DataTableControl<Opening>(this, "group"+g, group.iterator())
			{			
				@Override
				protected void defineColumns() throws Exception
				{
					Column col = column("").width(1); // Checkbox
					if (!phone)
					{
						col.html(new WebPage()
						{
							@Override
							public void renderHTML() throws Exception
							{
								new CheckboxInputControl(this, null)
									.affectAll("chk_")
									.render();
							}
						});
					}

//					column(getString("schedule:Unresolved.Date"));
					column(getString("schedule:Unresolved.Opening"));
//					column(getString("schedule:Unresolved.Facility"));
					column(getString("schedule:Unresolved.TimeLeft"));
					column(getString("schedule:Unresolved.Duration"));
					column(getString("schedule:Unresolved.Rank"));
					column(getString("schedule:Unresolved.Elert"));
					
					if (!phone)
					{
						column(getString("elert:Legend.Accepted")).align("center").alignHeader("center").image("elert/circle-v.png").width(1);
						column(getString("elert:Legend.Declined")).align("center").alignHeader("center").image("elert/circle-x.png").width(1);
						column(getString("elert:Legend.DidNotReply")).align("center").alignHeader("center").image("elert/circle-q.png").width(1);
					}
					
					if (!phone)
					{
						column("").width(1); // Urgent
					}
					column(getString("schedule:Unresolved.BestMatch"));
				}
	
				@Override
				protected void renderRow(Opening opening) throws Exception
				{
					Facility facility = FacilityStore.getInstance().load(opening.getFacilityID());
					List<UUID> procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(opening.getID());
					
					// Checkbox
					cell();
					writeCheckbox("chk_" + opening.getID().toString(), null, false);
					
					// Name and link
					cell();
					writeLink(	facility.getCode() + " " + miniDateTime.format(opening.getDateTime()),
								getPageURL(OpeningPage.COMMAND, new ParameterMap(OpeningPage.PARAM_ID, opening.getID().toString())));
					
//					cell();
//					writeEncode(facility.getCode());
//					if (!Util.isEmpty(opening.getRoom()))
//					{
//						write(" ");
//						writeEncode(opening.getRoom());
//					}
					
					// Time left
					cell();
					long timeLeft = opening.getDateTime().getTime() - now.getTime();
					timeLeft = timeLeft / (60L*60L*1000L);
					if (timeLeft>72)
					{
						writeEncodeLong(timeLeft/24); // !$! Better rounding or show hours
						write(" ");
						writeEncode(getString("schedule:Unresolved.Days"));
					}
					else
					{
						writeEncodeLong(timeLeft); // !$! Better rounding or show minutes
						write(" ");
						writeEncode(getString("schedule:Unresolved.Hours"));
					}
					
					// Duration
					cell();
					writeEncodeLong(opening.getDuration());
					if (opening.getOriginalDuration()!=opening.getDuration())
					{
						write("<span class=Faded>/");
						writeEncodeLong(opening.getOriginalDuration());
						write("</span>");
					}
					write(" ");
					writeEncode(getString("schedule:Unresolved.Minutes"));
	
					// rank
					cell();
					if (procIDs.size()>0)
					{
						int resourceRank = 0;
						for (UUID procID : procIDs)
						{
							resourceRank += ResourceProcedureLinkStore.getInstance().getTotalRankForProcedure(procID);
						}
						writeEncodeLong(resourceRank);
					}
					else
					{
						write("<span class=Faded>");
						writeEncode(getString("schedule:Unresolved.NotApplicable"));
						write("</span>");
					}
					
					// Minutes since last eLert
					cell();
					List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(opening.getID());
					if (elertIDs.size()>0)
					{
						Elert latestElert = ElertStore.getInstance().load(elertIDs.get(0));
						long timeSinceElert = now.getTime() - latestElert.getDateSent().getTime();
						timeSinceElert /= (60L*1000L); // Calc minutes
						if (timeSinceElert<120)
						{
							writeEncodeLong(timeSinceElert);
							write(" ");
							writeEncode(getString("schedule:Unresolved.Minutes"));
						}
						else if (timeSinceElert/60<=72)
						{
							writeEncodeLong(timeSinceElert / 60L); // !$! Better rounding or show minutes
							write(" ");
							writeEncode(getString("schedule:Unresolved.Hours"));
						}
						else
						{
							writeEncodeLong(timeSinceElert / 60L / 24L); // !$! Better rounding or show hours
							write(" ");
							writeEncode(getString("schedule:Unresolved.Days"));
						}
					}
	
					// eLert statuses
					int countAccepted = 0;
					int countDeclined = 0;
					for (int e=0; e<elertIDs.size(); e++)
					{
						Elert eLert = ElertStore.getInstance().load(elertIDs.get(e));
						if (eLert.getReply()==Elert.REPLY_ACCEPTED)
						{
							countAccepted++;
						}
						else if (eLert.getReply()==Elert.REPLY_DECLINED)
						{
							countDeclined++;
						}
					}
					if (!phone)
					{
						cell();
					}
					else
					{
						write("&nbsp;");
					}
					if (elertIDs.size()>0)
					{
						write("<div class=\"ElertCount Accepted\">");
						writeEncodeLong(countAccepted);
						write("</div>");
					}
					
					if (!phone)
					{
						cell();
					}
					else
					{
						write("&nbsp;");
					}
					if (elertIDs.size()>0)
					{
						write("<div class=\"ElertCount Declined\">");
						writeEncodeLong(countDeclined);
						write("</div>");
					}
	
					if (!phone)
					{
						cell();
					}
					else
					{
						write("&nbsp;");
					}
					if (elertIDs.size()>0)
					{
						write("<div class=\"ElertCount DidNotReply\">");
						writeEncodeLong(elertIDs.size() - countAccepted - countDeclined);
						write("</div>");
					}
	
					// Urgency, best match
					cell();
					Boolean urgentMatch = OpeningStore.getInstance().hasUrgentMatch(opening);
					if (urgentMatch!=null && urgentMatch==true)
					{
						writeImage("elert/urgent.png", getString("elert:Legend.Urgent"));
						if (phone)
						{
							write("&nbsp;");
						}
					}
					if (!phone)
					{
						cell();
					}
					Integer bestMatch = OpeningStore.getInstance().bestMatchPercentage(opening);
					if (bestMatch!=null)
					{
						writeEncodeLong(bestMatch);
						write("%");
					}
				}
			}.render();			
		}
		
		write("<br>");
		writeRemoveButton("remove");

		// Repost group and sort orders
		writeHiddenInput("group", null);
		writeHiddenInput("sort", null);

		writeFormClose();
		write("<br>");
		writeLegend();
	}

	private void renderSorters() throws Exception
	{		
		writeFormOpen("GET", getContext().getCommand());
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:Unresolved.GroupBy"));
		
		SelectInputControl select = new SelectInputControl(twoCol, "group");
		int i = 0;
		for (OpeningSorter sorter : this.groupers)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.writeRow(getString("schedule:Unresolved.SortBy"));
		
		select = new SelectInputControl(twoCol, "sort");
		i = 0;
		for (OpeningSorter sorter : this.sorters)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.render();
//		write("<br>");
//		writeButton(getString("controls:Button.Query"));
				
		writeFormClose();
		write("<br>");
	}
}
