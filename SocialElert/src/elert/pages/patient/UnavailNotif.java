package elert.pages.patient;

import java.util.List;
import java.util.UUID;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.Subscription;
import elert.database.SubscriptionProcedureLinkStore;
import elert.database.SubscriptionStore;
import elert.pages.ElertPage;
import samoyan.servlet.Setup;

public class UnavailNotif extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/unavail.notif";
	public final static String PARAM_ELERT_ID = "id";

	private Opening opening;
	private Elert elert;
	private Subscription sub;
	private Facility facility;
	
	@Override
	public void init() throws Exception
	{
		this.elert = ElertStore.getInstance().load(getParameterUUID(PARAM_ELERT_ID));
		this.opening = OpeningStore.getInstance().load(this.elert.getOpeningID());
		this.sub = SubscriptionStore.getInstance().load(this.elert.getSubscriptionID());
		this.facility = FacilityStore.getInstance().load(this.opening.getFacilityID());
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:UnavailNotif.Title", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale()), this.opening.getDateTime());
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("patient:UnavailNotif.LongText", this.opening.getDateTime(), getProceduresString(), this.facility.getName()));
	}

	@Override
	public void renderShortText() throws Exception
	{
		write(getString("patient:UnavailNotif.ShortText", this.opening.getDateTime(), getProceduresString(), this.facility.getName()));
	}

	@Override
	public void renderSimpleHTML() throws Exception
	{
		writeEncode(getString("patient:UnavailNotif.LongText", this.opening.getDateTime(), getProceduresString(), this.facility.getName()));
	}

	@Override
	public void renderText() throws Exception
	{
		write(getString("patient:UnavailNotif.LongText", this.opening.getDateTime(), getProceduresString(), this.facility.getName()));
	}

	@Override
	public void renderVoiceXML() throws Exception
	{
		write("<block><prompt bargeiin=\"false\">");
		writeEncode(getString("patient:UnavailNotif.LongText", this.opening.getDateTime(), getProceduresString(), this.facility.getName()));
		write("</prompt></block>");
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
					result.append(getString("patient:UnavailNotif.And"));
				}
				else
				{
					result.append(getString("patient:UnavailNotif.Comma"));
				}
			}
			result.append(proc.getDisplayName());
		}
		
		return result.toString();
	}
}
