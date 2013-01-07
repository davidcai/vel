package baby.pages.master;

import samoyan.apps.master.JoinPage;
import samoyan.controls.NavTreeControl;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.RequestContext;
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

			navCtrl.addHeader(outputPage.getString("baby:Nav.Account"));
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
		return "";
//		return Setup.getAppTitle(outputPage.getLocale());
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		if (outputPage.getContext().getUserAgent().isSmartPhone())
		{
			if (outputPage.getContext().getUserID()==null)
			{
				// Wide logo
				return "baby/corner-logo-60.png";
			}
			else
			{
				// Square logo
				return "baby/tab-home.png";
			}
		}
		else
		{
			return "baby/corner-logo-60.png";
		}
	}
}
