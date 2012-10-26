package samoyan.apps.admin.reports;

import java.sql.SQLException;
import java.util.Date;

import samoyan.apps.admin.AdminPage;
import samoyan.database.QueryIterator;
import samoyan.database.User;
import samoyan.database.UserStore;

public class JoinReportPage extends UsersOverTimeReportPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/join-report";

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:JoinReport.Title");
	}

	@Override
	protected QueryIterator<User> query(Date from, Date to) throws SQLException
	{
		return UserStore.getInstance().queryJoinedGhost(from, to);
	}

	@Override
	protected String getDateFieldLabel()
	{
		return getString("admin:JoinReport.Joined");
	}

	@Override
	protected Date getDateField(User user) throws Exception
	{
		return user.getDateJoined();
	}
	
	@Override
	protected String getHelpString()
	{
		return getString("admin:JoinReport.Help");
	}
}
