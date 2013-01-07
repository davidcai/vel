package samoyan.apps.profile;

import samoyan.controls.TwoColFormControl;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.AfterCommitRedirectException;
import samoyan.servlet.exc.WebFormException;

public class ChangePasswordPage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/password";

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		String current = ctx.getParameter("current");
		if (user.isPassword(current)==false)
		{
			throw new WebFormException("current", getString("common:Errors.InvalidValue"));
		}
				
		String password1 = ctx.getParameter("password1");
		if (password1.length()<User.MINSIZE_PASSWORD)
		{
			throw new WebFormException("password1", getString("common:Errors.FieldTooShort", User.MINSIZE_PASSWORD));
		}
		if (password1.length()>User.MAXSIZE_PASSWORD)
		{
			throw new WebFormException("password1", getString("common:Errors.Errors.FieldTooLong", User.MAXSIZE_PASSWORD));
		}
				
		final String[] PASSWORD_FIELDS = {"password1", "password2"};

		String password2 = ctx.getParameter("password2");
		if (password1.equals(password2)==false)
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.PasswordMismatch"));
		}
		
		if (password1.equalsIgnoreCase(user.getLoginName()))
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.InvalidValue"));
		}
		
		if (current.equals(password1))
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("profile:ChangePassword.CantChangeToCurrentPassword"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().open(ctx.getUserID());

		// Update password
		user.setPassword(ctx.getParameter("password1"));
		UserStore.getInstance().save(user);
		
		// Send email notif re: password change
		Notifier.send(Channel.EMAIL, null, user.getID(), null, PasswordChangedNotif.COMMAND, null);
		
		// Redirect to self in order to clear form submission
		throw new AfterCommitRedirectException();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:ChangePassword.Title");
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
		User user = UserStore.getInstance().open(getContext().getUserID());

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:ChangePassword.Help", User.MINSIZE_PASSWORD));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:ChangePassword.Current"));
		twoCol.writePasswordInput("current", null, 20, User.MAXSIZE_PASSWORD);

		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:ChangePassword.New"));
		twoCol.writePasswordInput("password1", null, 20, User.MAXSIZE_PASSWORD);

		twoCol.writeRow(getString("profile:ChangePassword.Repeat"));
		twoCol.writePasswordInput("password2", null, 20, User.MAXSIZE_PASSWORD);
		
		twoCol.render();

		write("<br>");
		writeSaveButton(user);
//		write(" ");
//		writeBackButton(null, null);
		
		writeFormClose();
	}
}
