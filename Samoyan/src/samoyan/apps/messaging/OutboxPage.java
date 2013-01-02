package samoyan.apps.messaging;

import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.InternalMessage;
import samoyan.database.InternalMessageRecipientStore;
import samoyan.database.InternalMessageStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

public class OutboxPage extends MessagingPage
{
	public final static String COMMAND = MessagingPage.COMMAND + "/outbox";

	@Override
	public String getTitle() throws Exception
	{
		return getString("messaging:Outbox.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		final boolean phone = ctx.getUserAgent().isSmartPhone();
		
		// Toolbar
		new LinkToolbarControl(this)
			.addLink(getString("messaging:Outbox.ComposeNewMessage"), getPageURL(ComposePage.COMMAND), "icons/standard/pencil-16.png")
			.render();
		
		// List messages
		List<UUID> msgIDs = InternalMessageStore.getInstance().queryOutbox(ctx.getUserID());
		
		if (msgIDs.size()==0)
		{
			writeEncode(getString("messaging:Outbox.OutboxEmpty"));
			return;
		}
		
		writeFormOpen();
		
		new DataTableControl<UUID>(this, "msgs", msgIDs)
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
				
				column(getString("messaging:Outbox.To")).width(25);
				column(getString("messaging:Outbox.Subject"));
				column(getString("messaging:Outbox.Date")).width(10).noWrap();
			}

			@Override
			protected void renderRow(UUID msgID) throws Exception
			{
				InternalMessage msg = InternalMessageStore.getInstance().load(msgID);
				
				cell();
				writeCheckbox("chk_" + msg.getID().toString(), null, false);
				
				cell();
				List<UUID> recipientIDs = InternalMessageRecipientStore.getInstance().queryRecipientsOfMessage(msgID);
				User firstRecipient = UserStore.getInstance().load(recipientIDs.get(0));
				writeEncode(firstRecipient.getDisplayName());
				if (recipientIDs.size()>1)
				{
					writeEncode(" +");
					writeEncodeLong(recipientIDs.size()-1);
				}
				
				cell();
				String subject = msg.getSubject();
				if (Util.isEmpty(subject))
				{
					subject = getString("messaging:Outbox.NoSubject");
				}
				writeLink(subject, getPageURL(ReadMessagePage.COMMAND, new ParameterMap(ReadMessagePage.PARAM_ID, msg.getID().toString())));
				
				cell();
				writeEncodeDateOrTime(msg.getCreatedDate());
			}
		}
		.render();
		
		write("<br>");
		writeRemoveButton("remove");
		
		writeFormClose();
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		for (String p : ctx.getParameterNamesThatStartWith("chk_"))
		{
			UUID msgID = UUID.fromString(p.substring(4));
			InternalMessageStore.getInstance().markDeletedForSender(msgID, ctx.getUserID());			
		}
		
		// Redicrect to self
		throw new RedirectException(getContext().getCommand(), null);
	}
}
