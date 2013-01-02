package samoyan.apps.master;

import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.BCrypt;
import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.LogEntryStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.LoginOKLogEntry;

public class InitPasswordPage extends WebPage
{
	public final static String COMMAND = "init-password";
	
	public final static String PARAM_LOGINNAME = "login";
	public final static String PARAM_BCRYPTED_USER_ID = "key";
	
	private User user = null;
	
	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void validate() throws Exception
	{
//		// Name
//		if (Util.isEmpty(this.user.getName()))
//		{
//			validateParameterString("name", User.MINSIZE_NAME, User.MAXSIZE_NAME);
//		}
		
		// Password
		String password1 = validateParameterString("password1", User.MINSIZE_PASSWORD,  User.MAXSIZE_PASSWORD);
		String password2 = validateParameterString("password2", User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		
		final String[] PASSWORD_FIELDS = {"password1", "password2"};
				
		if (password1.equals(password2)==false)
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.PasswordMismatch"));
		}
		
		if (password1.equalsIgnoreCase(this.user.getLoginName()))
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.InvalidValue"));
		}
		
		if (this.user.isPassword(password1))
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("master:InitPassword.CantChangeToCurrentPassword"));
		}

		// Captcha
		validateParameterCaptcha("captcha");
	}
	
	@Override
	public void commit() throws Exception
	{
		// Change the user's password
//		if (Util.isEmpty(this.user.getName()))
//		{
//			this.user.setName(getParameterString("name"));
//		}
		this.user.setPassword(getParameterString("password1"));
		UserStore.getInstance().save(this.user);
		
		// Create auth token and set as cookie
		UUID authToken = AuthTokenStore.getInstance().createAuthToken(this.user.getID(), getContext().getUserAgent().getString(), false, getParameterString(RequestContext.PARAM_APPLE_PUSH_TOKEN));
		setCookie(RequestContext.COOKIE_AUTH, authToken.toString());

		// Log the event
		LoginOKLogEntry log = new LoginOKLogEntry();
		log.setUserID(this.user.getID());
		LogEntryStore.log(log);
		
		// Redirect
		throw new RedirectException(WelcomePage.COMMAND, null);
	}

	@Override
	public void init() throws Exception
	{
		RequestContext ctx = getContext();
		String loginName = ctx.getParameter(PARAM_LOGINNAME);
		if (Util.isEmpty(loginName))
		{
			throw new PageNotFoundException();
		}
		
		this.user = UserStore.getInstance().openByLoginName(loginName);
		if (this.user==null || this.user.isSuspended() || this.user.isTerminated() || this.user.getLastActive()!=null)
		{
			throw new PageNotFoundException();
		}
		
		String bcryptedUserID = ctx.getParameter(PARAM_BCRYPTED_USER_ID);
		if (Util.isEmpty(bcryptedUserID))
		{
			throw new PageNotFoundException();
		}
		
		if (BCrypt.checkpw(this.user.getID().toString(), bcryptedUserID)==false)
		{
			throw new PageNotFoundException();
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:InitPassword.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
//		if (Util.isEmpty(this.user.getName()))
//		{
//			// Name
//			twoCol.writeTextRow(getString("master:InitPassword.HelpName"));
//			twoCol.writeSpaceRow();
//			
//			twoCol.writeRow(getString("master:InitPassword.Name"));
//			twoCol.writeTextInput("name", this.user.getName(), 20, User.MAXSIZE_NAME);
//			
//			twoCol.writeSpaceRow();
//		}
		
		// Password
		twoCol.writeTextRow(getString("master:InitPassword.HelpPassword"));
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("master:InitPassword.Password"));
		twoCol.writePasswordInput("password1", null, 20, User.MAXSIZE_PASSWORD);

		twoCol.writeRow(getString("master:InitPassword.RepeatPassword"));
		twoCol.writePasswordInput("password2", null, 20, User.MAXSIZE_PASSWORD);
		
		// Captcha
		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("master:InitPassword.HelpCaptcha"));
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("master:InitPassword.Captcha"));
		twoCol.writeCaptcha("captcha");

		twoCol.render();
		write("<br>");
		writeSaveButton(this.user);
		
		// Post back
		writeHiddenInput(PARAM_LOGINNAME, null);
		writeHiddenInput(PARAM_BCRYPTED_USER_ID, null);
		
		// Will be filled by PhoneGap wrapper with the Apple Push Notification token
		writeHiddenInput(RequestContext.PARAM_APPLE_PUSH_TOKEN, "");

		writeFormClose();

//		// Neat caps script
//		if (Util.isEmpty(this.user.getName()))
//		{
//			write("<script type=\"text/javascript\">");
//			write("$('INPUT[name=name]').blur(neatCapsInput);");
//			write("</script>");
//		}
	}
}
