package samoyan.apps.profile;

import java.util.Random;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.PhoneInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.voice.VoiceServer;

public class PhonePage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/phone";

	private final static int CODE_LEN = 6;

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Server fed = ServerStore.getInstance().loadFederation();

		if (!fed.isChannelEnabled(Channel.VOICE))
		{
			// To prevent guided setup from getting stuck
			progressGuidedSetup();

			throw new PageNotFoundException();
		}

		if (isParameter("send"))
		{
			if (!this.isFormException())
			{
				renderEnterCode();
			}
			else
			{
				renderSendCode();
			}
		}
		else if (isParameter("enter"))
		{
			if (!this.isFormException())
			{
				renderSendCode();
			}
			else
			{
				renderEnterPhone();
			}
		}
		else if (isParameter("verify"))
		{
			if (!this.isFormException())
			{
				// Should not reach here
				throw new PageNotFoundException();
			}
			else
			{
				renderEnterCode();
			}
		}
		else
		{
			renderEnterPhone();
		}
	}

	private void renderEnterPhone() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Server fed = ServerStore.getInstance().loadFederation();

		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:Phone.EnterHelp"));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:Phone.Number"));
		new PhoneInputControl(twoCol, "number")
			.limitCountries(fed.getVoiceCountries())
			.setInitialValue(user.getPhone())
			.render();
		
		twoCol.render();
		
		write("<br>");
		writeButton("enter", getString("controls:Button.Next"));
		write(" ");
		if (ctx.getCommand(1).equals(UrlGenerator.COMMAND_SETUP))
		{
			new ButtonInputControl(this, "clear")
				.setSubdued(true)
				.setValue(getString("profile:Phone.Skip"))
				.render();
		}
		else if (!Util.isEmpty(user.getPhone()))
		{
			new ButtonInputControl(this, "clear")
				.setStrong(true)
				.setValue(getString("profile:Phone.Clear"))
				.render();
		}

		writeFormClose();
	}

	private void renderSendCode() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		writeFormOpen();
		
		writeEncode(getString("profile:Phone.VerifyHelp", CODE_LEN));
		write("<br><br>");
		writeButton("send", getString("profile:Phone.SendCode", Util.stripCountryCodeFromPhoneNumber(getParameterPhone("number"))));

		// Post back the number
		writeHiddenInput("fullnumber", getParameterPhone("number"));
		
		writeFormClose();
	}
	
	private void renderEnterCode() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:Phone.CodeHelp", CODE_LEN));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:Phone.Number"));
		twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(getParameterString("fullnumber")));

		twoCol.writeRow(getString("profile:Phone.Code", CODE_LEN));
		twoCol.writeTextInput("code", null, CODE_LEN, CODE_LEN);
		
		twoCol.render();
		
		write("<br>");
		writeButton("verify", getString("profile:Phone.Verify"));

		// Post back the number
		writeHiddenInput("fullnumber", null);

		writeFormClose();
	}

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		if (isParameter("enter"))
		{
			String phone = validateParameterPhone("number");
//			if (phone.equals(user.getPhone()))
//			{
//				throw new WebFormException("number", getString("profile:Phone.NoChange"));
//			}
		}
		
		if (isParameter("verify"))
		{
			String code = validateParameterString("code", CODE_LEN, CODE_LEN) + ":" + getParameterString("fullnumber");
			if (code.equals(user.getPhoneVerificationCode())==false)
			{
				// Wrong code
				throw new WebFormException("code", getString("profile:Phone.IncorrectCode"));
			}
		}		
	}
	
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		final User user = UserStore.getInstance().open(ctx.getUserID());

		if (isParameter("send"))
		{
			String code = user.getPhoneVerificationCode();
			if (Util.isEmpty(code))
			{
				Random r = new Random();
				code = "";
				while (code.length()<CODE_LEN)
				{
					code += String.valueOf(r.nextInt(10));
				}
				user.setPhoneVerificationCode(code + ":" + getParameterString("fullnumber"));
				UserStore.getInstance().save(user);
			}

			// Initiate call to phone number with verification code
			new Thread()
			{
				@Override
				public void run()
				{
					try
					{
//						if (Setup.isDebug())
						{
							Debug.logln("Verification code: " + user.getPhoneVerificationCode());
						}
//						else
						{
							VoiceServer.startOutboundCall(user.getID(), getParameterString("fullnumber"), COMMAND, null);
						}
					}
					catch (Exception e)
					{
					}
				}
			}.start();
		}
		
		else if (isParameter("verify"))
		{
			// Verified!
			user.setPhone(getParameterString("fullnumber"));
			user.setPhoneVerificationCode(null);
			UserStore.getInstance().save(user);
						
			// Support guided setup
			progressGuidedSetup();
			
			// Go back to the contact info page
			throw new RedirectException(ContactInfoPage.COMMAND, null);
		}
		
		else if (isParameter("clear"))
		{
			user.setPhone(null);
			user.setPhoneVerificationCode(null);
			UserStore.getInstance().save(user);
			
			// Support guided setup
			progressGuidedSetup();

			// Go back to the contact info page
			throw new RedirectException(ContactInfoPage.COMMAND, null);
		}
	}
	
	@Override
	public void renderVoiceXML() throws Exception
	{
		User user = UserStore.getInstance().open(getContext().getUserID());

		String code = user.getPhoneVerificationCode();
		int p = code.indexOf(":");
		if (p>=0)
		{
			code = code.substring(0, p);
		}
		
		String msg = Util.htmlEncode(getString("profile:Phone.VerifyMessage", Setup.getAppTitle(getLocale()), "$digits$"));
		StringBuilder digits = new StringBuilder();
		for (int i=0; i<code.length(); i++)
		{
			digits.append("<break time=\"200ms\"/>");
			digits.append(code.charAt(i));
		}
		msg = Util.strReplace(msg, "$digits$", digits.toString());
		
		write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		write("<vxml version=\"2.1\" xml:lang=\"");
		writeEncode(getLocale().getLanguage());
		if (!Util.isEmpty(getLocale().getCountry()))
		{
			write("-");
			writeEncode(getLocale().getCountry());
		}
		write("\">");
		write("<form>");

		write("<block>");
		for (int i=0; i<10; i++)
		{
			write("<prompt bargein=\"false\">");
			write(msg);
			write("</prompt>");
			write("<break time=\"2s\"/>");
		}
		write("</block>");
		
		write("</form>");
		write("</vxml>");
	}
	
	@Override
	public boolean isEnvelope() throws Exception
	{
		if (getContext().getChannel().equalsIgnoreCase(Channel.VOICE))
		{
			// For the verification message
			return false;
		}
		else
		{
			return super.isEnvelope();
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:Phone.Title");
	}
	
	@Override
	public boolean isAuthorized() throws Exception
	{
		return this.getContext().getUserID()!=null;
	}
}
