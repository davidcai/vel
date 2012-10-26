package elert.pages.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.CollectionsEx;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;

import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.database.ServiceAreaUserLinkStore;
import elert.database.Subscription;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import elert.pages.common.SubscriptionSorter;
import elert.pages.common.SubscriptionSorter.SortByDateSubscribed;

public class RecentSubscriptionsPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/recent-subscriptions";

	// Init groupers and sorters
	private SubscriptionSorter[] groupers =
	{
		new SubscriptionSorter.AllEqual(),
		new SubscriptionSorter.SortByPhysician(),
		new SubscriptionSorter.SortByProcedure(),
		new SubscriptionSorter.SortByUrgency(),
		new SubscriptionSorter.SortByDuration(60),
		new SubscriptionSorter.SortByVerified(),
		new SubscriptionSorter.SortByDateSubscribed(getLocale(), getTimeZone())
	};
	private SubscriptionSorter[] sorters =
	{
		new SubscriptionSorter.SortByUrgency(),
		new SubscriptionSorter.SortByDuration(1),
		new SubscriptionSorter.SortByVerified(),
		new SubscriptionSorter.SortByDateSubscribed(getLocale(), getTimeZone())
	};
	
//	@Override
//	public void commit() throws Exception
//	{
//		if (isParameter("remove"))
//		{
//			for (String p : getContext().getParameterNamesThatStartWith("chk_"))
//			{
//				UUID subID = UUID.fromString(p.substring(4));
//				if (SubscriptionStore.getInstance().canRemoveBean(subID))
//				{
//					// Remove from the database outright
//					SubscriptionStore.getInstance().remove(subID);
//				}
//				else
//				{
//					// Keep in database, but mark as removed
//					Subscription sub = SubscriptionStore.getInstance().open(subID);
//					sub.setRemoved(true);
//					SubscriptionStore.getInstance().save(sub);
//				}
//			}
//		}
//	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("schedule:RecentSubs.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("schedule:RecentSubs.Help"));
		write("<br><br>");

		// Render filter
		writeFormOpen("GET", getContext().getCommand());
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:RecentSubs.GroupBy"));
		
		SelectInputControl select = new SelectInputControl(twoCol, "group");
		int i = 0;
		for (SubscriptionSorter sorter : this.groupers)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.writeRow(getString("schedule:RecentSubs.SortBy"));
		
		select = new SelectInputControl(twoCol, "sort");
		i = 0;
		for (SubscriptionSorter sorter : this.sorters)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			if (sorter instanceof SortByDateSubscribed)
			{
				select.setInitialValue(i);
				break;
			}
			i++;
		}
		select.setAutoSubmit(true);
		select.render();
		
		twoCol.render();		
		
		writeFormClose();
		write("<br>");

		// Query
		List<UUID> areaIDs = ServiceAreaUserLinkStore.getInstance().getHomeSerivceAreasForUser(getContext().getUserID());
		List<UUID> subscriptionIDs = SubscriptionStore.getInstance().queryOpenSubscriptions(areaIDs, Subscription.MAX_DURATION);
		List<Subscription> subscriptions = new ArrayList<Subscription>(subscriptionIDs.size());
		for (UUID subscriptionID : subscriptionIDs)
		{
			subscriptions.add(SubscriptionStore.getInstance().load(subscriptionID));
		}
		if (subscriptions.size()==0)
		{
			writeEncode(getString("schedule:RecentSubs.NoResults"));
			return;
		}

		// Perform grouping and sorting
		Integer grouperIdx = getParameterInteger("group");
		if (grouperIdx==null) grouperIdx = 0;
		Integer sorterIdx = getParameterInteger("sort");
		if (sorterIdx==null)
		{
			for (int x=0; x<this.sorters.length; x++)
			{
				if (this.sorters[x] instanceof SortByDateSubscribed)
				{
					sorterIdx = x;
					break;
				}
			}
		}
		SubscriptionSorter grouper = this.groupers[grouperIdx];
		SubscriptionSorter sorter = this.sorters[sorterIdx];
		
		Collection<Collection<Subscription>> groups = CollectionsEx.group(subscriptions, grouper, sorter);

		final boolean multiArea = areaIDs.size()>0;
		final boolean phone = getContext().getUserAgent().isSmartPhone();

		// Render the results
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
			
//			writeFormOpen();

			new DataTableControl<Subscription>(this, "subs", group.iterator())
			{
				@Override
				protected void defineColumns() throws Exception
				{
//					Column col = column("").width(1); // Checkbox
//					if (!phone)
//					{
//						col.html(new WebPage()
//						{
//							@Override
//							public void renderHTML() throws Exception
//							{
//								new CheckboxInputControl(this, null)
//									.affectAll("chk_")
//									.render();
//							}
//						});
//					}
					
					column("").width(1); // Verified
					column("").width(1); // Urgent
					column(getString("schedule:RecentSubs.Procedure"));
					column(getString("schedule:RecentSubs.PatientName"));
					column(getString("schedule:RecentSubs.Physician"));
					if (multiArea)
					{
						column(getString("schedule:RecentSubs.ServiceArea"));
					}
					column(getString("schedule:RecentSubs.CreatedDate"));
					column("").width(1); // Finalized
				}

				@Override
				protected void renderRow(Subscription sub) throws Exception
				{
//					cell();
//					writeCheckbox("chk_" + sub.getID().toString(), null, false);

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
					User patient = UserStore.getInstance().load(sub.getUserID());
					writeLink(	patient.getDisplayName(),
								getPageURL(PatientProfilePage.COMMAND, new ParameterMap(PatientProfilePage.PARAM_ID, patient.getID().toString())));
										
					cell();
					List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(sub.getID());
					for (UUID physicianID : physicianIDs)
					{
						User physician = UserStore.getInstance().load(physicianID);
						writeEncode(physician.getDisplayName());
						write("<br>");
					}
					
					if (multiArea)
					{
						cell();
						ServiceArea area = ServiceAreaStore.getInstance().load(sub.getServiceAreaID());
						writeEncode(area.getName());
					}

					cell();
					if (sub.isExpired())
					{
						write("<strike>");
					}
					writeEncodeDateOrTime(sub.getCreatedDate());
					if (sub.isExpired())
					{
						write("</strike>");
					}
					
					cell();
					if (sub.isFinalized())
					{
						writeImage("elert/finalized.png", getString("elert:Legend.Finalized"));
					}
				}				
			}.render();		
		}
		
//		write("<br>");
//		writeRemoveButton("remove");
//		write("<br><br>");
//		writeEncode(getString("schedule:RecentSubs.RemoveWarning"));
//
//		writeFormClose();
		
		writeLegend();
		
	}
}
