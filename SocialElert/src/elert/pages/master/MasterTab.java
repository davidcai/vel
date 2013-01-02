package elert.pages.master;

import samoyan.apps.master.JoinPage;
import samoyan.controls.NavTreeControl;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
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

			navCtrl.addHeader(outputPage.getString("elert:Nav.Account"));
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
//		return Setup.getAppTitle(outputPage.getLocale());
		return null;
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		String phone = "";
		String reversed = "";
		if (outputPage.getContext().getUserAgent().isSmartPhone())
		{
			phone = "-phone";
		}
		if (outputPage.getContext().getCommand().equalsIgnoreCase(RootPage.COMMAND))
		{
			// Display the black-texted logo on the home page
			reversed = "-rev";
		}
		return "elert/corner-banner" + phone + reversed + ".png";
	}
}
