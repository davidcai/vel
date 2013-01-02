package samoyan.apps.admin;

import samoyan.apps.admin.config.*;
import samoyan.apps.admin.log.*;
import samoyan.apps.admin.reports.*;
import samoyan.apps.admin.tools.*;
import samoyan.apps.admin.usermgmt.*;
import samoyan.controls.NavTreeControl;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.Setup;
import samoyan.servlet.WebPage;

public class AdminTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);
		
		navCtrl.addHeader(outputPage.getString("admin:Nav.UserManagement"));
		navCtrl.addPage(UserListPage.COMMAND, null);
		navCtrl.addPage(UserGroupListPage.COMMAND, null);
//		navCtrl.addPage(InviteUsersPage.COMMAND, null);
//		navCtrl.addPage(ImportUsersPage.COMMAND, null);
		navCtrl.addPage(PermissionListPage.COMMAND, null);
				
		navCtrl.addHeader(outputPage.getString("admin:Nav.Tools"));
		navCtrl.addPage(AdHocMessagePage.COMMAND, null);

// !$! Hack to hide tasks screens from customer
//		navCtrl.addHeader(outputPage.getString("admin:Nav.Runtime"));
//		navCtrl.addPage(RecurringTaskListPage.COMMAND, null);

		navCtrl.addHeader(outputPage.getString("admin:Nav.Reports"));
		navCtrl.addPage(OutgoingNotifsReportPage.COMMAND, null);
		navCtrl.addPage(JoinReportPage.COMMAND, null);
		navCtrl.addPage(CumulativeActivityReportPage.COMMAND, null);
		navCtrl.addPage(LastActivityReportPage.COMMAND, null);
		navCtrl.addPage(AggregateTimelineReportPage.COMMAND, null);

		navCtrl.addHeader(outputPage.getString("admin:Nav.Log"));
		navCtrl.addPage(QueryLogPage.COMMAND, null);
		if (Setup.isDebug())
		{
			navCtrl.addPage(ConsolePage.COMMAND, null);
		}
		
// !$! Hack to hide config screens from customer
User user = UserStore.getInstance().load(outputPage.getContext().getUserID());
		navCtrl.addHeader(outputPage.getString("admin:Nav.Configuration"));
		navCtrl.addPage(GeneralConfig.COMMAND, null);
if (user.getLoginName().equalsIgnoreCase("admin"))
{
			navCtrl.addPage(EmailConfigPage.COMMAND, null);
			navCtrl.addPage(SmsConfigPage.COMMAND, null);
			navCtrl.addPage(VoiceConfigPage.COMMAND, null);
			navCtrl.addPage(ApplePushConfigPage.COMMAND, null);
			navCtrl.addPage(TwitterConfigPage.COMMAND, null);
			navCtrl.addPage(ExternalServicesConfigPage.COMMAND, null);
}
		navCtrl.addPage(LogConfigPage.COMMAND, null);
		navCtrl.addPage(TimelineConfigPage.COMMAND, null);
		
		return navCtrl;
	}

	@Override
	public String getCommand()
	{
		return AdminPage.COMMAND;
	}

	@Override
	public String getLabel(WebPage outputPage)
	{
		return outputPage.getString("admin:Nav.TabTitle");
	}

	@Override
	public String getIcon(WebPage outputPage)
	{
		return "icons/standard/cogwheel-48.png";
	}
}
