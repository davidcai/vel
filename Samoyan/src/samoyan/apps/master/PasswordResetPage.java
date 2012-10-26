package samoyan.apps.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import samoyan.controls.DataTableControl;
import samoyan.controls.PhoneInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class PasswordResetPage extends WebPage
{
	public final static String COMMAND = "password-reset";
	private final static int CODE_LEN = 6;

	private List<UUID> searchResults = new ArrayList<UUID>();
	private boolean codeSent = false;
		
	@Override
	public void validate() throws Exception
	{
		if (isParameter("find"))
		{
//			validateParameterString("loginname", 0, User.MAXSIZE_LOGINNAME);
			validateParameterString("name", 0, User.MAXSIZE_NAME);
						
			if (isParameterNotEmpty("email"))
			{
				String email = validateParameterString("email", 0, User.MAXSIZE_EMAIL);
				if (!Util.isValidEmailAddress(email))
				{
					throw new WebFormException("email", "common:Errors.InvalidValue");
				}
			}
			
			if (isParameterNotEmpty("phone"))
			{
				validateParameterPhone("phone");
			}
			
			validateExtra();
			
			validateParameterCaptcha("captcha");
			
			// Perform the search
			search();
			Collections.sort(searchResults);
			
			if (searchResults.size()==0)
			{
				throw new WebFormException(getString("master:PasswordReset.NoResults"));
			}
			if (searchResults.size()>10)
			{
				throw new WebFormException(getString("master:PasswordReset.TooManyResults"));
			}
		}
		
		if (isParameter("sendemail") || isParameter("sendsms") || isParameter("sendvoice"))
		{
			// Recreate search results
			for (String p : getContext().getParameterNamesThatStartWith("id_"))
			{
				searchResults.add(UUID.fromString(p.substring(3)));
			}
			Collections.sort(searchResults);

			User user = UserStore.getInstance().load(validateParameterUUID("radio"));
			if (isParameter("sendsms") && Util.isEmpty(user.getMobile()))
			{
				throw new WebFormException(getString("master:PasswordReset.NoMobile"));
			}
			if (isParameter("sendvoice") && Util.isEmpty(user.getPhone()))
			{
				throw new WebFormException(getString("master:PasswordReset.NoPhone"));
			}
		}
		
		if (isParameter("set"))
		{
			User user = UserStore.getInstance().open(validateParameterUUID("radio"));

			String code = validateParameterString("code", CODE_LEN,  CODE_LEN);
			if (user.getPasswordResetCode()==null || user.getPasswordResetCode().equals(code)==false ||
				user.getPasswordResetDate()==null || user.getPasswordResetDate().getTime() + Setup.getSessionLength() < System.currentTimeMillis())
			{
				user.setPasswordResetCode(null);
				user.setPasswordResetDate(null);
				UserStore.getInstance().save(user);

				throw new WebFormException("code", getString("master:PasswordReset.WrongCode"));
			}
			
			String password1 = validateParameterString("password1", User.MINSIZE_PASSWORD,  User.MAXSIZE_PASSWORD);
			String password2 = validateParameterString("password2", User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
			
			final String[] PASSWORD_FIELDS = {"password1", "password2"};
					
			if (password1.equals(password2)==false)
			{
				throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.PasswordMismatch"));
			}
			
			if (password1.equalsIgnoreCase(user.getLoginName()))
			{
				throw new WebFormException(PASSWORD_FIELDS, getString("common:Errors.InvalidValue"));
			}
		}
	}

	private void addSearchResults(Collection<UUID> userIDs) throws Exception
	{
		// Remove entries of terminated users
		userIDs = new ArrayList<UUID>(userIDs);
		Iterator<UUID> iter = userIDs.iterator();
		while (iter.hasNext())
		{
			User user = UserStore.getInstance().load(iter.next());
			if (user==null || user.isTerminated() || user.isSuspended()) // || PermissionStore.getInstance().isUserGrantedPermission(user.getID(), Permission.SYSTEM_ADMINISTRATION))
			{
				iter.remove();
			}
		}
		if (userIDs.size()==0)
		{
			return;
		}

		if (searchResults.size()==0)
		{
			searchResults.addAll(userIDs);
		}
		else if (userIDs.size()>0)
		{
			searchResults.retainAll(userIDs);
		}
	}
	
	private void addSearchResults(UUID userID) throws Exception
	{
		if (userID!=null)
		{
			List<UUID> wrapper = new ArrayList<UUID>();
			wrapper.add(userID);
			addSearchResults(wrapper);
		}
	}
	
	private void search() throws Exception
	{
//		User loginNameMatch = UserStore.getInstance().loadByLoginName(getParameterString("loginname"));
//		if (loginNameMatch!=null)
//		{
//			addSearchResults(loginNameMatch.getID());
//		}

		if (isParameterNotEmpty("name"))
		{
			List<UUID> byFullName = UserStore.getInstance().searchByName(getParameterString("name"));
			if (byFullName.size()==1)
			{
				addSearchResults(byFullName.get(0));
			}
			
			Set<UUID> set = new HashSet<UUID>();
			for (String namePart : Util.tokenize(getParameterString("name"), " "))
			{
				set.addAll(UserStore.getInstance().searchByName(namePart));
			}
			addSearchResults(set);
		}
				
		if (isParameterNotEmpty("email"))
		{
			addSearchResults(UserStore.getInstance().getByEmail(getParameterString("email")));
		}
		
		if (isParameterNotEmpty("phone"))
		{
			addSearchResults(UserStore.getInstance().getByPhone(getParameterPhone("phone")));
			addSearchResults(UserStore.getInstance().getByMobile(getParameterPhone("phone")));
		}

		List<UUID> extra = new ArrayList<UUID>();
		findExtra(extra);
		addSearchResults(extra);
		
		// Remove duplicates by using a set
		Set<UUID> searchSet = new HashSet<UUID>(searchResults);
		searchResults = new ArrayList<UUID>(searchSet);		
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("sendemail") || isParameter("sendsms") || isParameter("sendvoice"))
		{
			User user = UserStore.getInstance().open(getParameterUUID("radio"));
			
			// Store code and date on user
			Random r = new Random();
			String code = "";
			while (code.length()<CODE_LEN)
			{
				code += String.valueOf(r.nextInt(10));
			}
			user.setPasswordResetCode(code);
			user.setPasswordResetDate(new Date());
			UserStore.getInstance().save(user);

			// Send notif
			String channel = Channel.EMAIL;
			if (isParameter("sendsms"))
			{
				channel = Channel.SMS;
			}
			else if (isParameter("sendvoice"))
			{
				channel = Channel.VOICE;
			}
			Notifier.send(channel, null, user.getID(), null, PasswordResetNotif.COMMAND, null);
			
			codeSent = true;
		}
		
		if (isParameter("set"))
		{
			User user = UserStore.getInstance().open(validateParameterUUID("radio"));
			String password = getParameterString("password1");
			user.setPassword(password);
			user.setPasswordResetCode(null);
			user.setPasswordResetDate(null);
			UserStore.getInstance().save(user);
			
			// Create auth token and set as cookie
			setCookie(RequestContext.COOKIE_AUTH, AuthTokenStore.getInstance().createAuthToken(user.getID(), getContext().getUserAgent().getString(), false).toString());

			throw new RedirectException(WelcomePage.COMMAND, null);
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		if (codeSent || isParameter("codesent"))
		{
			renderEnterCode();
		}
		else if (searchResults.size()>0)
		{
			renderFindResults();
		}
		else
		{
			renderFindForm();
		}
	}
	
	private void renderEnterCode() throws Exception
	{
		if (isFormException("code"))
		{
			writeLink(getString("master:PasswordReset.StartOver"), getPageURL(COMMAND));
			return;
		}
		
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("master:PasswordReset.HelpCode", CODE_LEN));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("master:PasswordReset.VerificationCode"));
		twoCol.writeTextInput("code", null, CODE_LEN, CODE_LEN);

		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("master:PasswordReset.HelpPassword", CODE_LEN));
		twoCol.writeSpaceRow();

		// Password
		twoCol.writeRow(getString("master:PasswordReset.NewPassword"), getString("master:PasswordReset.NewPasswordHelp", User.MINSIZE_PASSWORD));
		twoCol.writePasswordInput("password1", null, 20, User.MAXSIZE_PASSWORD);

		twoCol.writeRow(getString("master:PasswordReset.RepeatPassword"));
		twoCol.writePasswordInput("password2", null, 20, User.MAXSIZE_PASSWORD);
		
		twoCol.render();
		
		write("<br>");
		writeButton("set", getString("master:PasswordReset.SetPassword"));
		
		writeHiddenInput("codesent", "1");
		writeHiddenInput("radio", null);
		writeFormClose();
	}
	
	private void renderFindResults() throws Exception
	{
		writeFormOpen();
		
		writeEncode(getString("master:PasswordReset.HelpResults"));
		write("<br><br>");
		
		new DataTableControl<UUID>(this, "users", searchResults.iterator())
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column("").width(1);
				column(getString("master:PasswordReset.LoginName"));
				column(getString("master:PasswordReset.Email"));
				column(getString("master:PasswordReset.Mobile"));
				column(getString("master:PasswordReset.Phone"));
			}

			@Override
			protected void renderRow(UUID userID) throws Exception
			{
				User user = UserStore.getInstance().load(userID);
				
				cell();
				writeRadioButton("radio", null, userID.toString(), searchResults.size()==1? userID.toString() : null);
				writeHiddenInput("id_" + user.getID().toString(), "1");

				cell();
				writeEncode(lastLetters(user.getLoginName(), 3));

				cell();
				String email = user.getEmail();
				int at = email.indexOf("@");
				writeEncode(lastLetters(email.substring(0, at), 3));
				write("@");
				List<String> domainParts = Util.tokenize(email.substring(at+1), ".");
				for (int d=0; d<domainParts.size(); d++)
				{
					if (d>0)
					{
						write(".");
					}
					if (d<domainParts.size()-1)
					{
						writeEncode(firstLetters(domainParts.get(d), 1));
					}
					else
					{
						writeEncode(domainParts.get(d));
					}
				}

				cell();
				writeEncode(lastLetters(Util.stripCountryCodeFromPhoneNumber(user.getMobile()), 2));
				
				cell();
				writeEncode(lastLetters(Util.stripCountryCodeFromPhoneNumber(user.getPhone()), 2));
			}

			private String firstLetters(String s, int i)
			{
				if (s==null)
				{
					return "";
				}
				else if (s.length()<=i)
				{
					return s;
				}
				else
				{
					String r = s.substring(0, i);
					while (r.length()<s.length())
					{
						r += "*";
					}
					return r;
				}
			}
			
			private String lastLetters(String s, int i)
			{
				if (s==null)
				{
					return "";
				}
				else if (s.length()<=i)
				{
					return s;
				}
				else
				{
					String r = s.substring(s.length()-i);
					while (r.length()<s.length())
					{
						r = "*" + r;
					}
					return r;
				}
			}
		}.render();
		
		write("<br>");
		writeButton("sendemail", getString("master:PasswordReset.SendEmail"));
		write(" ");
		writeButton("sendsms", getString("master:PasswordReset.SendSMS"));
		write(" ");
		writeButton("sendvoice", getString("master:PasswordReset.SendVoice"));
		
		writeFormClose();
	}
	
	private void renderFindForm() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("master:PasswordReset.Help"));
		twoCol.writeSpaceRow();
		
//		twoCol.writeRow(getString("master:PasswordReset.LoginName"));
//		twoCol.writeTextInput("loginname", null, 40, User.MAXSIZE_LOGINNAME);

		twoCol.writeRow(getString("master:PasswordReset.Name"));
		twoCol.writeTextInput("name", null, 40, User.MAXSIZE_NAME);
				
		twoCol.writeRow(getString("master:PasswordReset.Email"), getString("master:PasswordReset.EmailHelp"));
		twoCol.writeTextInput("email", null, 40, User.MAXSIZE_EMAIL);
		
		twoCol.writeRow(getString("master:PasswordReset.PhoneOrMobile"), getString("master:PasswordReset.PhoneOrMobileHelp"));
		new PhoneInputControl(twoCol, "phone")
			.limitCountries(fed.getSMSCountries())
			.limitCountries(fed.getVoiceCountries())
			.render();

		renderExtra(twoCol);
		
		twoCol.writeSpaceRow();
		twoCol.writeTextRow(getString("master:PasswordReset.CaptchaHelp"));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("master:PasswordReset.Captcha"));
		twoCol.writeCaptcha("captcha");
		
		twoCol.render();
		
		write("<br>");
		writeButton("find", getString("master:PasswordReset.FindAccount"));
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:PasswordReset.Title");
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	// - - - - - - - - - -
	
	/**
	 * To be overridden by subclasses to render any extra fields.
	 * @param twoCol Subclass must write into this <code>TwoColFormControl</code>.
	 * @throws Exception
	 */
	protected void renderExtra(TwoColFormControl twoCol) throws Exception
	{
	}
	
	/**
	 * To be overridden by subclasses to validate any extra fields.
	 * @throws Exception
	 */
	protected void validateExtra() throws Exception
	{
	}

	/**
	 * To be overridden by subclasses to search using the extra fields.
	 * @param searchResults Search results should be added to this <code>List</code> of user IDs.
	 * @throws Exception
	 */
	protected void findExtra(List<UUID> searchResults) throws Exception
	{
	}
}
