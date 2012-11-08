package samoyan.apps.profile;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class ChangeLoginNamePage extends ProfilePage
{	
	public final static String COMMAND = ProfilePage.COMMAND + "/login-name";

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		String oldLoginName = user.getLoginName();
		
		// User name
		String loginName = validateParameterString("loginname", User.MINSIZE_LOGINNAME, User.MAXSIZE_LOGINNAME);
		if (loginName.equalsIgnoreCase(oldLoginName)==false)
		{
			if (UserStore.getInstance().loadByLoginName(loginName)!=null)
			{
				throw new WebFormException("loginname", getString("profile:ChangeLoginName.LoginNameTaken"));
			}
			if (loginName.matches("[a-zA-Z0-9_\\x2d]*")==false)
			{
				throw new WebFormException("loginname", getString("profile:ChangeLoginName.LoginNameNonAlpha"));
			}
			if (Util.isUUID(loginName) || user.isPassword(loginName))
			{
				throw new WebFormException("loginname", getString("common:Errors.InvalidValue"));
			}
		}
		
		// Password
		String password = validateParameterString("password", User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		if (user.isPassword(password)==false)
		{
			throw new WebFormException("password", getString("profile:ChangeLoginName.WrongPassword"));
		}
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().open(ctx.getUserID());
		String oldLoginName = user.getLoginName();
		if (oldLoginName.equalsIgnoreCase(getParameterString("loginname"))==false)
		{
			// Rename the user
			user.setLoginName(UserStore.getInstance().generateUniqueLoginName(getParameterString("loginname")));
			UserStore.getInstance().save(user);
			
			// Send email notif re: user name change
			Notifier.send(Channel.EMAIL, null, user.getID(), null, LoginNameChangedNotif.COMMAND, new ParameterMap(LoginNameChangedNotif.PARAM_OLD_LOGINNAME, oldLoginName));
		}
		
		// Redirect to self in order to clear form submission
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, "")); // getString("profile:ChangeLoginName.Confirmation")));
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:ChangeLoginName.Title");
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		return getContext().getUserID()!=null;
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void renderHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);

		twoCol.writeTextRow(getString("profile:ChangeLoginName.Help", User.MINSIZE_LOGINNAME));
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("profile:ChangeLoginName.LoginName"));
		twoCol.writeTextInput("loginname", user.getLoginName(), 20, User.MAXSIZE_LOGINNAME);

		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("profile:ChangeLoginName.PasswordHelp"));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:ChangeLoginName.Password"));
		twoCol.writePasswordInput("password", null, 20, User.MAXSIZE_PASSWORD);
		
		twoCol.render();

		write("<br>");
		writeSaveButton(user);
//		write(" ");
//		writeBackButton(null);
		
		writeFormClose();
	}
	
}
