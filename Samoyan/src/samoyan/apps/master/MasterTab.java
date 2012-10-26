package samoyan.apps.master;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.RequestContext;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;

public class MasterTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		RequestContext ctx = outputPage.getContext();
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		if (ctx.getUserID()==null)
		{
			navCtrl.addHeader(outputPage.getString("master:Nav.Account"));
			navCtrl.addPage(LoginPage.COMMAND, null);
			navCtrl.addPage(JoinPage.COMMAND, null);
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
		return outputPage.getString("master:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return null;
	}
}
