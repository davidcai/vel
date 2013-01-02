package samoyan.apps.master;

import java.util.Locale;
import java.util.UUID;

import samoyan.apps.master.PrivacyPage;
import samoyan.apps.master.TermsPage;
import samoyan.apps.master.WelcomePage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.LocaleEx;
import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.LogEntryStore;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.NewUserLogEntry;

public class JoinPage extends WebPage
{
	public final static String COMMAND = "join";
	
	@Override
	public void validate() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isOpenRegistration()==false)
		{
			throw new PageNotFoundException();
		}

		// Name
		validateParameterString("name", User.MINSIZE_NAME, User.MAXSIZE_NAME);
		
		// Email
		String email = validateParameterString("email", 1, User.MAXSIZE_EMAIL);
		if (Util.isValidEmailAddress(email)==false)
		{
			throw new WebFormException("email", getString("common:Errors.InvalidValue"));
		}
		
		// User name
		String loginName = validateParameterString("loginname", User.MINSIZE_LOGINNAME, User.MAXSIZE_LOGINNAME);		
		if (UserStore.getInstance().loadByLoginName(loginName)!=null)
		{
			throw new WebFormException("loginname", getString("master:Join.LoginNameTaken"));
		}
		if (loginName.matches("[a-zA-Z0-9_\\x2d]*")==false)
		{
			throw new WebFormException("loginname", getString("master:Join.LoginNameNonAlpha"));
		}
		if (Util.isUUID(loginName))
		{
			throw new WebFormException("loginname", getString("common:Errors.InvalidValue"));
		}
		
		// Password
		String password1 = validateParameterString("password1", User.MINSIZE_PASSWORD,  User.MAXSIZE_PASSWORD);
		String password2 = validateParameterString("password2", User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		
		final String[] PASSWORD_FIELDS = {"password1", "password2"};
				
		if (password1.equals(password2)==false)
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.PasswordMismatch"));
		}
		
		if (password1.equalsIgnoreCase(loginName))
		{
			throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.InvalidValue"));
		}
		
		// Captcha
		validateParameterCaptcha("captcha");
		
		// Agree to terms
		if (isParameter("agree")==false)
		{
			throw new WebFormException("agree", getString("master:Join.MustAgree"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		Server fed = ServerStore.getInstance().loadFederation();
		
		// Create the user
		User user = new User();
		user.setName(getParameterString("name"));
		user.setEmail(getParameterString("email").toLowerCase(Locale.US));
		user.setLoginName(UserStore.getInstance().generateUniqueLoginName(getParameterString("loginname")));
		user.setPassword(getParameterString("password1"));
		user.setTimeZone(ctx.getTimeZone());
		user.setLocale(LocaleEx.bestMatch(fed.getLocales(), ctx.getLocales()));
		UserStore.getInstance().save(user);

		// Grant administrative permissions to the initial admin user
		boolean admin = user.getLoginName().equalsIgnoreCase("admin");
		if (admin)
		{
			PermissionStore.getInstance().authorize(user.getID(), Permission.SYSTEM_ADMINISTRATION);
		}

		// Log the event
		LogEntryStore.log(new NewUserLogEntry(user.getID()));

		// Send welcome email
		Notifier.send(Channel.EMAIL, null, user.getID(), null, JoinNotif.COMMAND, null);
		
		// Create auth token and set as cookie
		UUID authToken = AuthTokenStore.getInstance().createAuthToken(user.getID(), getContext().getUserAgent().getString(), false, getParameterString(RequestContext.PARAM_APPLE_PUSH_TOKEN));
		setCookie(RequestContext.COOKIE_AUTH, authToken.toString());
		
		// Redirect admins to overview page, all others to welcome page.
		throw new RedirectException(WelcomePage.COMMAND, null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:Join.Title");
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isOpenRegistration()==false)
		{
			writeEncode(getString("master:Join.ByInvitationOnly"));
			return;
		}
				
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Help
		twoCol.writeTextRow(getString("master:Join.Instructions1"));
		twoCol.writeSpaceRow();

		// Name
		twoCol.writeRow(getString("master:Join.Name"));
		twoCol.writeTextInput("name", null, 40, User.MAXSIZE_NAME);

		twoCol.writeSpaceRow();

		// Email
		twoCol.writeRow(getString("master:Join.Email"), getString("master:Join.EmailNote"));
		twoCol.writeTextInput("email", null, 40, User.MAXSIZE_EMAIL);

		// Help
		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("master:Join.Instructions2"));
		twoCol.writeSpaceRow();

		// User name
		twoCol.writeRow(getString("master:Join.LoginName"), getString("master:Join.LoginNameNote", User.MINSIZE_LOGINNAME));
		twoCol.writeTextInput("loginname", null, 20, User.MAXSIZE_LOGINNAME);

		twoCol.writeSpaceRow();

		// Password
		twoCol.writeRow(getString("master:Join.Password"), getString("master:Join.PasswordNote", User.MINSIZE_PASSWORD));
		twoCol.writePasswordInput("password1", null, 20, User.MAXSIZE_PASSWORD);

		twoCol.writeRow(getString("master:Join.RepeatPassword"));
		twoCol.writePasswordInput("password2", null, 20, User.MAXSIZE_PASSWORD);
		
		// Help
		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("master:Join.Instructions3"));
		twoCol.writeSpaceRow();
		
		// Captcha
		twoCol.writeRow(getString("master:Join.Captcha"), getString("master:Join.CaptchaNote"));
		twoCol.writeCaptcha("captcha");
		
		twoCol.render();
				
		// Agree to terms
		write("<br>");	
		writeCheckbox("agree", null, false);
		write(" ");
		
		StringBuffer termsLink = new StringBuffer();
		termsLink.append("<a href=\"");
		termsLink.append(getPageURL(TermsPage.COMMAND));
		termsLink.append("\">");
		termsLink.append(Util.htmlEncode(getString("master:Join.TermsOfUse")));
		termsLink.append("</a>");
		
		StringBuffer privacyLink = new StringBuffer();
		privacyLink.append("<a href=\"");
		privacyLink.append(getPageURL(PrivacyPage.COMMAND));
		privacyLink.append("\">");
		privacyLink.append(Util.htmlEncode(getString("master:Join.PrivacyPolicy")));
		privacyLink.append("</a>");

		String pattern = Util.htmlEncode(getString("master:Join.Agree", "$terms$", "$privacy$", getString("master.base.Title")));
		pattern = Util.strReplace(pattern, "$terms$", termsLink.toString());
		pattern = Util.strReplace(pattern, "$privacy$", privacyLink.toString());
		write(pattern);
						
		// Submit button
		write("<br><br>");	
		super.writeButton(getString("master:Join.Join"));
		
		// Will be filled by PhoneGap wrapper with the Apple Push Notification token
		writeHiddenInput(RequestContext.PARAM_APPLE_PUSH_TOKEN, "");

		writeFormClose();
		
//		// Neat caps script
//		write("<script type=\"text/javascript\">");
//		write("$('INPUT[name=name]').blur(neatCapsInput);");
//		write("</script>");
	}

	@Override
	public int getXRobotFlags() throws Exception
	{
		return NO_INDEX;
	}
}
