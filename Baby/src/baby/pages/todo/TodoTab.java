package baby.pages.todo;

import baby.pages.BabyPage;
import samoyan.controls.NavTreeControl;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public class TodoTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		RequestContext ctx = outputPage.getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		NavTreeControl navCtrl = new NavTreeControl(outputPage);
//		navCtrl.addHeader(outputPage.getString("information:Nav.Articles"));
//		navCtrl.addPage(HealthyBeginningsPage.COMMAND, null);
//		navCtrl.addPage(ResourcesPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_TODO;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("todo:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "baby/tab-todo.png";
	}
}