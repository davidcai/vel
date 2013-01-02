package elert.pages.patient;

import java.util.List;
import java.util.UUID;

import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.database.Subscription;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;

import samoyan.controls.LinkToolbarControl;
import samoyan.controls.DataTableControl;
import samoyan.core.ParameterMap;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;

public class SubscriptionsPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/subscriptions";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:Subscriptions.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{		
		writeEncode(getString("patient:Subscriptions.Help", Setup.getAppTitle(getLocale())));
		write("<br><br>");

		new LinkToolbarControl(this)
			.addLink(getString("patient:Subscriptions.SubscribeNew"), getPageURL(NewSubscriptionWizardPage.COMMAND), "icons/standard/pencil-16.png")
			.render();
		
		// List current subscriptions here
		final List<UUID> subIDs = SubscriptionStore.getInstance().getByUserID(getContext().getUserID());
		
		if (subIDs.size()==0)
		{
			writeEncode(getString("patient:Subscriptions.NoResults"));
			write("<br>");
		}
		
		writeFormOpen();
		
		new DataTableControl<UUID>(this, "subs", subIDs)
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);

				column(getString("patient:Subscriptions.Procedure"));
				column(getString("patient:Subscriptions.ServiceArea"));
				column(getString("patient:Subscriptions.Physician"));
//				column(getString("patient:Subscriptions.OriginalDate"));
				column(getString("patient:Subscriptions.Status"));
			}

			@Override
			protected void renderRow(UUID subID) throws Exception
			{
				Subscription sub = SubscriptionStore.getInstance().load(subID);
				
				cell();
				writeCheckbox("chk_" + subID.toString(), null, false);

//				new CheckboxInputControl(this, "chk_" + subID.toString())
//					.setDisabled(SubscriptionStore.getInstance().canRemoveBean(subID)==false)
//					.render();
				
//				if (SubscriptionStore.getInstance().canRemoveBean(subID))
//				{
//					writeCheckbox("chk_" + subID.toString(), null, false);
//				}
//				else
//				{
//					write("<input type=checkbox disabled>");
//				}
				
				cell();
				write("<a href=\"");
				write(getPageURL(SubscriptionPage.COMMAND, new ParameterMap(SubscriptionPage.PARAM_ID, sub.getID().toString())));
				write("\">");
				List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(subID);
				for (UUID procID : procIDs)
				{
					Procedure proc = ProcedureStore.getInstance().load(procID);
					writeEncode(proc.getDisplayName());
					write("<br>");
				}
				write("</a>");
				
				cell();
				ServiceArea area = ServiceAreaStore.getInstance().load(sub.getServiceAreaID());
				writeEncode(area.getName());
				
				cell();
				List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(subID);
				for (UUID physicianID : physicianIDs)
				{
					User physician = UserStore.getInstance().load(physicianID);
					writeEncode(physician.getDisplayName());
					write("<br>");
				}

//				cell();
//				if (sub.getOriginalDate()!=null)
//				{
//					writeEncodeDate(sub.getOriginalDate());
//				}

				cell();
				String status;
				if (sub.isFinalized())
				{
					status = "Closed";
				}
				else if (sub.isExpired())
				{
					status = "Expired";
				}
				else if (sub.getVerifiedDate()!=null)
				{
					status = "Verified";
				}
				else
				{
					status = "Received";
				}
				writeTooltipRightAligned(
						getString("patient:Subscriptions.Status"+status),
						getString("patient:Subscriptions.Status"+status+"Help", Setup.getAppOwner(getLocale()), Setup.getAppTitle(getLocale()), Subscription.MAX_AVAILABILITY_MONTHS));
			}
		}.render();
		
		if (subIDs.size()>0)
		{
			write("<br>");
			writeButtonRed("remove", getString("patient:Subscriptions.Unsubscribe"));
			write("<br>");
		}
		
		writeFormClose();
				
		write("<br><br><small>");
		write("<b>");
		writeEncode(getString("elert:General.DisclaimerTitle"));
		write("</b> ");
		writeEncode(getString("elert:General.Disclaimer", Setup.getAppTitle(getLocale())));
		write("</small>");
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		
		if (isParameter("remove"))
		{
			for (String paramName : ctx.getParameterNamesThatStartWith("chk_"))
			{
				Subscription sub = SubscriptionStore.getInstance().open(UUID.fromString(paramName.substring(4)));
				if (sub!=null && sub.getUserID().equals(ctx.getUserID()))
				{
					if (SubscriptionStore.getInstance().canRemove(sub.getID()))
					{
						// Remove from the database outright
						SubscriptionStore.getInstance().remove(sub.getID());
					}
					else
					{
						// Keep in database, but mark as removed
						sub.setRemoved(true);
						SubscriptionStore.getInstance().save(sub);
					}
				}
			}
		}
	}
}
