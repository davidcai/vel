package elert.pages.physician;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.CollectionsEx;
import samoyan.core.StringBundle;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.database.Subscription;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import elert.pages.common.SubscriptionSorter;

public class UpcomingPatientsPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_PHYSICIAN + "/upcoming-patients";
	
	public static final int DURATION = 60;

	// Init groupers and sorters
	private SubscriptionSorter[] groupers = { 
		new SubscriptionSorter.AllEqual(),
		new SubscriptionSorter.SortByPatient(), 
		new SubscriptionSorter.SortByProcedure(), 
		new SubscriptionSorter.SortByUrgency(),
		new SubscriptionSorter.SortByDuration(DURATION), 
		new SubscriptionSorter.SortByVerified(),
		new SubscriptionSorter.SortByDateScheduled(getLocale(), getTimeZone()) 
	};
	private SubscriptionSorter[] sorters = {
		new SubscriptionSorter.SortByPatient(), 
		new SubscriptionSorter.SortByUrgency(),
		new SubscriptionSorter.SortByDuration(1), 
		new SubscriptionSorter.SortByVerified(),
		new SubscriptionSorter.SortByDateScheduled(getLocale(), getTimeZone()) 
	};

	@Override
	public void renderHTML() throws Exception
	{
		// Render filter
		writeFormOpen("GET", getContext().getCommand());

		TwoColFormControl twoCol = new TwoColFormControl(this);

		twoCol.writeRow(getString("physician:UpcomingPatients.GroupBy"));

		SelectInputControl select = new SelectInputControl(twoCol, "group");
		int i = 0;
		for (SubscriptionSorter sorter : this.groupers)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setAutoSubmit(true);
		select.render();

		twoCol.writeRow(getString("physician:UpcomingPatients.SortBy"));

		select = new SelectInputControl(twoCol, "sort");
		i = 0;
		for (SubscriptionSorter sorter : this.sorters)
		{
			select.addOption(sorter.getTitle(getLocale()), i);
			i++;
		}
		select.setInitialValue(0);
		select.setAutoSubmit(true);
		select.render();

		twoCol.render();

		writeFormClose();
		write("<br>");

		// Query
		List<UUID> subIDs = SubscriptionStore.getInstance().queryPhysicianSubscriptions(getContext().getUserID());
		List<Subscription> subs = new ArrayList<Subscription>(subIDs.size());
		
		// Filter the subscriptions whose scheduled dates are in the past
		Date today = new Date();
		for (UUID subID : subIDs)
		{
			Subscription sub = SubscriptionStore.getInstance().load(subID);
			
			Date scheduled = getScheduledDate(sub);
			if (scheduled == null || scheduled.after(today))
			{
				subs.add(sub);
			}
		}
		if (subs.size() == 0)
		{
			writeEncode(getString("physician:UpcomingPatients.NoResults"));
			return;
		}

		// Perform grouping and sorting
		Integer grouperIdx = getParameterInteger("group");
		if (grouperIdx == null)
		{
			grouperIdx = 0;
		}
		Integer sorterIdx = getParameterInteger("sort");
		if (sorterIdx == null)
		{
			sorterIdx = 0;
		}
		SubscriptionSorter grouper = this.groupers[grouperIdx];
		SubscriptionSorter sorter = this.sorters[sorterIdx];

		Collection<Collection<Subscription>> groups = CollectionsEx.group(subs, grouper, sorter);
		
		final boolean phone = getContext().getUserAgent().isSmartPhone();

		// Render the results
		writeFormOpen();
		
		int g = 0;
		for (Collection<Subscription> group : groups)
		{
			g++;
			
			String h2Title = grouper.getGroupTitle(group.iterator().next(), getLocale());
			if (Util.isEmpty(h2Title) == false)
			{
				write("<h2>");
				writeEncode(h2Title);
				write("</h2>");	
			}
			
			new DataTableControl<Subscription>(this, "subs", group.iterator())
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1); // Verified
					column("").width(1); // Urgent
					column(getString("physician:UpcomingPatients.Procedure"));
					column(getString("physician:UpcomingPatients.PatientName"));
					column(getString("physician:UpcomingPatients.ServiceArea"));
					column(getString("physician:UpcomingPatients.Duration"));
					column(getString("physician:UpcomingPatients.ScheduledDate"));
					column("").width(1); // Finalized
				}

				@Override
				protected void renderRow(Subscription sub) throws Exception
				{
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
					
					// Procedure
					cell();
					List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(sub.getID());
					for (UUID procID : procIDs)
					{
						Procedure proc = ProcedureStore.getInstance().load(procID);
						writeEncode(proc.getName());
						write("<br>");
					}

					// Patient
					cell();
					User patient = UserStore.getInstance().load(sub.getUserID());
					writeEncode(patient.getName());
					
					// Service area
					cell();
					ServiceArea area = ServiceAreaStore.getInstance().load(sub.getServiceAreaID());
					writeEncode(area.getName());
					
					// Duration
					cell();
					long dur = DURATION * (sub.getDuration() / DURATION);
					writeEncode(StringBundle.getString(getLocale(), null, "schedule:Sorters.HoursRange", 
						dur / DURATION, (dur + DURATION) / DURATION));

					// Scheduled date
					cell();
					Date scheduled = getScheduledDate(sub);
					if (scheduled != null)
					{
						writeEncodeDateOrTime(scheduled);
					}
					
					cell();
					if (sub.isFinalized())
					{
						writeImage("elert/finalized.png", getString("elert:Legend.Finalized"));
					}
				}
				
			}.render();
			
		} //-- for (group)

		writeFormClose();
		
		writeLegend();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("physician:UpcomingPatients.Title");
	}
	
	/**
	 * Returns the scheduled date for this subscription. If the subscription is finalized, get the opening date of the
	 * correspondent Elert. Otherwise, use the subscription's original date. Can be NULL.
	 * 
	 * @param sub
	 * @return Scheduled date
	 * @throws Exception
	 */
	private Date getScheduledDate(Subscription sub) throws Exception
	{
		Date dt = null;
		if (sub.isFinalized())
		{
			UUID elertID = ElertStore.getInstance().getFinalForSubscription(sub.getID());
			if (elertID != null)
			{
				Elert elert = ElertStore.getInstance().load(elertID);
				
				// Can be NULL
				dt = elert.getDateOpening();
			}
		}
		else
		{
			// Can be NULL
			dt = sub.getOriginalDate();
		}
		
		return dt;
	}
}
