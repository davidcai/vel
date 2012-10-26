package samoyan.apps.admin;

import samoyan.apps.admin.config.EmailConfigPage;
import samoyan.apps.admin.config.ExternalServicesConfigPage;
import samoyan.apps.admin.config.GeneralConfig;
import samoyan.apps.admin.config.LogConfigPage;
import samoyan.apps.admin.config.SmsConfigPage;
import samoyan.apps.admin.config.TimelineConfigPage;
import samoyan.apps.admin.config.TwitterConfigPage;
import samoyan.apps.admin.config.VoiceConfigPage;
import samoyan.apps.admin.log.ConsolePage;
import samoyan.apps.admin.log.LogEntryPage;
import samoyan.apps.admin.log.QueryLogPage;
import samoyan.apps.admin.reports.AggregateTimelineReportPage;
import samoyan.apps.admin.reports.CumulativeActivityReportPage;
import samoyan.apps.admin.reports.JoinReportPage;
import samoyan.apps.admin.reports.LastActivityReportPage;
import samoyan.apps.admin.reports.OutgoingNotifsReportPage;
import samoyan.apps.admin.runtime.RecurringTaskListPage;
import samoyan.apps.admin.tools.AdHocMessagePage;
import samoyan.apps.admin.tools.AdHocNotif;
import samoyan.apps.admin.typeahead.PermissionTypeAhead;
import samoyan.apps.admin.typeahead.UserGroupTypeAhead;
import samoyan.apps.admin.typeahead.UserTypeAhead;
import samoyan.apps.admin.usermgmt.ImpersonatePage;
import samoyan.apps.admin.usermgmt.ImportUsersPage;
import samoyan.apps.admin.usermgmt.InviteUsersNotif;
import samoyan.apps.admin.usermgmt.InviteUsersPage;
import samoyan.apps.admin.usermgmt.MemberListPage;
import samoyan.apps.admin.usermgmt.PermissionListPage;
import samoyan.apps.admin.usermgmt.PrincipleListPage;
import samoyan.apps.admin.usermgmt.UserGroupListPage;
import samoyan.apps.admin.usermgmt.UserGroupPage;
import samoyan.apps.admin.usermgmt.UserListPage;
import samoyan.apps.admin.usermgmt.UserPage;
import samoyan.servlet.Dispatcher;

public final class AdminApp
{
	public static void init()
	{
		// Home
		Dispatcher.bindPage(AdminHomePage.COMMAND, 				AdminHomePage.class);
		Dispatcher.bindPage(SystemOverviewPage.COMMAND, 		SystemOverviewPage.class);
		
		// Runtime
		Dispatcher.bindPage(RecurringTaskListPage.COMMAND,		RecurringTaskListPage.class);
		
		// Tools
		Dispatcher.bindPage(AdHocMessagePage.COMMAND,			AdHocMessagePage.class);
		Dispatcher.bindPage(AdHocNotif.COMMAND,					AdHocNotif.class);

		// Configuration
		Dispatcher.bindPage(EmailConfigPage.COMMAND,			EmailConfigPage.class);
		Dispatcher.bindPage(SmsConfigPage.COMMAND,				SmsConfigPage.class);
		Dispatcher.bindPage(TwitterConfigPage.COMMAND,			TwitterConfigPage.class);
		Dispatcher.bindPage(VoiceConfigPage.COMMAND,			VoiceConfigPage.class);
		Dispatcher.bindPage(ExternalServicesConfigPage.COMMAND,	ExternalServicesConfigPage.class);
		Dispatcher.bindPage(TimelineConfigPage.COMMAND,			TimelineConfigPage.class);
		Dispatcher.bindPage(GeneralConfig.COMMAND,				GeneralConfig.class);
		
		// User management
		Dispatcher.bindPage(UserListPage.COMMAND, 				UserListPage.class);
		Dispatcher.bindPage(ImpersonatePage.COMMAND, 			ImpersonatePage.class);
		Dispatcher.bindPage(UserGroupListPage.COMMAND, 			UserGroupListPage.class);
		Dispatcher.bindPage(UserGroupPage.COMMAND, 				UserGroupPage.class);
		Dispatcher.bindPage(ImportUsersPage.COMMAND, 			ImportUsersPage.class);
		Dispatcher.bindPage(UserPage.COMMAND, 					UserPage.class);
		Dispatcher.bindPage(InviteUsersPage.COMMAND,			InviteUsersPage.class);
		Dispatcher.bindPage(InviteUsersNotif.COMMAND,			InviteUsersNotif.class);
		Dispatcher.bindPage(PermissionListPage.COMMAND,			PermissionListPage.class);
		Dispatcher.bindPage(PrincipleListPage.COMMAND,			PrincipleListPage.class);
		Dispatcher.bindPage(MemberListPage.COMMAND,				MemberListPage.class);
		
		// Log
		Dispatcher.bindPage(QueryLogPage.COMMAND,				QueryLogPage.class);
		Dispatcher.bindPage(LogEntryPage.COMMAND,				LogEntryPage.class);
		Dispatcher.bindPage(LogConfigPage.COMMAND,				LogConfigPage.class);
		Dispatcher.bindPage(ConsolePage.COMMAND, 				ConsolePage.class);
		
		// Reports
		Dispatcher.bindPage(OutgoingNotifsReportPage.COMMAND,	OutgoingNotifsReportPage.class);
		Dispatcher.bindPage(JoinReportPage.COMMAND,				JoinReportPage.class);
		Dispatcher.bindPage(CumulativeActivityReportPage.COMMAND,CumulativeActivityReportPage.class);
		Dispatcher.bindPage(LastActivityReportPage.COMMAND,		LastActivityReportPage.class);
		Dispatcher.bindPage(AggregateTimelineReportPage.COMMAND,AggregateTimelineReportPage.class);

		// Type aheads
		Dispatcher.bindPage(UserTypeAhead.COMMAND,				UserTypeAhead.class);
		Dispatcher.bindPage(UserGroupTypeAhead.COMMAND,			UserGroupTypeAhead.class);
		Dispatcher.bindPage(PermissionTypeAhead.COMMAND,		PermissionTypeAhead.class);
	}
}
