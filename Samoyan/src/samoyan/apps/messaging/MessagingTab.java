package samoyan.apps.messaging;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class MessagingTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("messaging:Nav.Folders"));
		navCtrl.addPage(InboxPage.COMMAND, null);
		navCtrl.addPage(OutboxPage.COMMAND, null);
				
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return MessagingPage.COMMAND;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("messaging:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "tab-messaging.png";
	}
}
