package samoyan.apps.admin.reports;

import java.sql.SQLException;
import java.util.Date;

import samoyan.apps.admin.AdminPage;
import samoyan.database.QueryIterator;
import samoyan.database.User;
import samoyan.database.UserStore;

public class LastActivityReportPage extends UsersOverTimeReportPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/last-activity";

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:LastActivityReport.Title");
	}

	@Override
	protected QueryIterator<User> query(Date from, Date to) throws SQLException
	{
		return UserStore.getInstance().queryActiveGhost(from, to);
	}

	@Override
	protected String getDateFieldLabel()
	{
		return getString("admin:LastActivityReport.LastActive");
	}

	@Override
	protected Date getDateField(User user) throws Exception
	{
		return user.getLastActive();
	}

	@Override
	protected String getHelpString()
	{
		return getString("admin:LastActivityReport.Help");
	}
	
	
}
