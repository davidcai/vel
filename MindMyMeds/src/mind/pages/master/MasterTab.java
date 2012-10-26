package mind.pages.master;

import samoyan.apps.master.JoinPage;
import samoyan.controls.NavTreeControl;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Setup;
import samoyan.servlet.RequestContext;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class MasterTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		RequestContext ctx = outputPage.getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		if (user==null)
		{
			Server fed = ServerStore.getInstance().loadFederation();

			navCtrl.addHeader(outputPage.getString("mind:Nav.Account"));
			navCtrl.addPage(LoginPage.COMMAND, null);
			if (fed.isOpenRegistration())
			{
				navCtrl.addPage(JoinPage.COMMAND, null);
			}
		}
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return "";
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return Setup.getAppTitle(outputPage.getLocale());
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return outputPage.getContext().getUserAgent().isSmartPhone()? "mind/corner-banner-phone.png":"mind/corner-banner.png";
	}
}
