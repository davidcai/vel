package samoyan.apps.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.PhoneInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.Country;
import samoyan.database.CountryStore;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;
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
import samoyan.sms.SmsMessage;
import samoyan.sms.SmsServer;

public class MobilePage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/mobile";

	
	private final static int CODE_LEN = 6;
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Server fed = ServerStore.getInstance().loadFederation();

		if (!fed.isChannelEnabled(Channel.SMS))
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
		
		twoCol.writeTextRow(getString("profile:Mobile.EnterHelp"));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:Mobile.Number"));
		new PhoneInputControl(twoCol, "number")
			.limitCountries(fed.getSMSCountries())
			.setInitialValue(user.getMobile())
			.render();
		
		twoCol.render();
		
		write("<br>");
		writeButton("enter", getString("controls:Button.Next"));
		write(" ");
		if (ctx.getCommand(1).equals(UrlGenerator.COMMAND_SETUP))
		{
			new ButtonInputControl(this, "clear")
				.setSubdued(true)
				.setValue(getString("profile:Mobile.Skip"))
				.render();
		}
		else if (!Util.isEmpty(user.getMobile()))
		{
			new ButtonInputControl(this, "clear")
				.setStrong(true)
				.setValue(getString("profile:Mobile.Clear"))
				.render();
		}

		writeFormClose();
	}

	private void renderSendCode() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		writeFormOpen();
		
		writeEncode(getString("profile:Mobile.VerifyHelp", CODE_LEN));
		write("<br><br>");

		// Load country of number
		String number = getParameterPhone("number");
		int slash = number.indexOf("/");
		String countryCode = number.substring(0, slash);
		Country country = CountryStore.getInstance().loadByCodeISO2(countryCode);		
		
		// Get mobile carriers for this country
		List<MobileCarrier> carriers = new ArrayList<MobileCarrier>();
		for (UUID id : MobileCarrierStore.getInstance().queryByCountryCode(country.getCodeISO2()))
		{
			carriers.add(MobileCarrierStore.getInstance().load(id));
		}
		Collections.sort(carriers, new MobileCarrier.SortByMinor(getLocale()));

		if (carriers.size()>0)
		{
			writeCarrierList(carriers);
		}
		
		write("<br>");			
		writeButton("send", getString("profile:Mobile.SendCode", Util.stripCountryCodeFromPhoneNumber(number)));

		// Post back the number
		writeHiddenInput("fullnumber", number);

		writeFormClose();
	}
	
	private void renderEnterCode() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:Mobile.CodeHelp", CODE_LEN));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:Mobile.Number"));
		twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(getParameterString("fullnumber")));

		twoCol.writeRow(getString("profile:Mobile.Code", CODE_LEN));
		twoCol.writeTextInput("code", null, CODE_LEN, CODE_LEN);
		
		twoCol.render();
		
		write("<br>");
		writeButton("verify", getString("profile:Mobile.Verify"));

		// Post back the number
		writeHiddenInput("fullnumber", null);

		writeFormClose();
	}

	private void writeCarrierList(List<MobileCarrier> carriers) throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		final int COLS = getContext().getUserAgent().isSmartPhone()? 1 : 4;
		write("<table>");
		for (int c=0; c<carriers.size(); c++)
		{
			MobileCarrier mc = carriers.get(c);
			
			if (c%COLS==0)
			{
				write("<tr>");
			}
			
			write("<td>");
			if (mc.isMinor())
			{
				write("<small>");
			}
			writeRadioButton("carrier", mc.getName(), mc.getID(), user.getMobileCarrierID());
			if (mc.isMinor())
			{
				write("</small>");
			}
			write("</td>");
			
			if (c%COLS==COLS-1)
			{
				write("</tr>");
			}
		}
		if (carriers.size()%COLS!=0)
		{
			write("<td colspan=");
			write(COLS - carriers.size()%COLS);
			write("></td></tr>");
		}
		
		write("<tr><td colspan=");
		write(COLS);
		write(">");
		writeRadioButton("carrier", getString("profile:Mobile.UnknownCarrier"), "", null);
		write("</td></tr>");
		
		write("</table>");
	}
	
	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		if (isParameter("enter"))
		{
			String phone = validateParameterPhone("number");
//			if (phone.equals(user.getMobile()))
//			{
//				throw new WebFormException("number", getString("profile:Mobile.NoChange"));
//			}
		}

		if (isParameter("send"))
		{
			if (isParameter("carrier")==false)
			{
				throw new WebFormException("carrier", getString("common:Errors.MissingField"));
			}
			if (isParameterNotEmpty("carrier"))
			{
				UUID carrierID = validateParameterUUID("carrier");
				if (MobileCarrierStore.getInstance().load(carrierID)==null)
				{
					throw new WebFormException("carrier", getString("common:Errors.InvalidValue"));
				}
			}
		}
		
		if (isParameter("verify"))
		{
			String code = validateParameterString("code", CODE_LEN, CODE_LEN) + ":" + getParameterString("fullnumber");;
			if (code.equals(user.getMobileVerificationCode())==false)
			{
				// Wrong code
				throw new WebFormException("code", getString("profile:Mobile.IncorrectCode"));
			}
		}		
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().open(ctx.getUserID());

		if (isParameter("send"))
		{
			if (getParameterUUID("carrier")!=null)
			{
				MobileCarrier mc = MobileCarrierStore.getInstance().load(getParameterUUID("carrier"));
				if (mc.getID().equals(user.getMobileCarrierID())==false)
				{
					// If user changes their mobile carrier, reset the verification code
					user.setMobileVerificationCode(null);
					user.setMobileCarrierID(mc.getID());
				}
			}
			else
			{
				if (user.getMobileCarrierID()!=null)
				{
					// If user changes their mobile carrier, reset the verification code
					user.setMobileVerificationCode(null);
					user.setMobileCarrierID(null);
				}
			}
			
			String code = user.getMobileVerificationCode();
			if (Util.isEmpty(code))
			{
				Random r = new Random();
				code = "";
				while (code.length()<CODE_LEN)
				{
					code += String.valueOf(r.nextInt(10));
				}
				user.setMobileVerificationCode(code + ":" + getParameterString("fullnumber"));
				UserStore.getInstance().save(user);
			}
			else
			{
				int p = code.indexOf(":");
				if (p>=0)
				{
					code = code.substring(0, p);
				}
			}
			
//			if (Setup.isDebug())
			{
				Debug.logln("Verification code: " + user.getMobileVerificationCode());
			}
//			else
			{
				// Sending the SMS via the low-level API because otherwise the non-verified number will be pre-empted
				SmsMessage sms = new SmsMessage();
				sms.setDestination(getParameterString("fullnumber"));
				sms.setCarrierID(user.getMobileCarrierID());
				sms.write(getString("profile:Mobile.VerifyMessage", Setup.getAppTitle(getLocale()), code));
				SmsServer.sendMessage(sms);
			}
		}
		
		else if (isParameter("verify"))
		{
			// Verified!
			user.setMobile(getParameterString("fullnumber"));
			user.setMobileVerified(true);
			user.setMobileVerificationCode(null);
			UserStore.getInstance().save(user);
						
			// Support guided setup
			progressGuidedSetup();

			// Go back to the contact info page
			throw new RedirectException(ContactInfoPage.COMMAND, null);
		}
		
		else if (isParameter("clear"))
		{
			user.setMobile(null);
			user.setMobileVerified(false);
			user.setMobileVerificationCode(null);
			UserStore.getInstance().save(user);
			
			// Support guided setup
			progressGuidedSetup();

			// Go back to the contact info page
			throw new RedirectException(ContactInfoPage.COMMAND, null);
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:Mobile.Title");
	}
	
	@Override
	public boolean isAuthorized() throws Exception
	{
		return this.getContext().getUserID()!=null;
	}
}
