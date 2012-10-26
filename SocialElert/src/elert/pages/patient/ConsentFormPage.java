package elert.pages.patient;

import samoyan.core.Util;
import samoyan.servlet.Setup;
import elert.pages.ElertPage;
import elert.pages.master.PrivacyPolicyPage;
import elert.pages.master.TermsOfUsePage;

public class ConsentFormPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_PATIENT + "/terms-agreement";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("patient:Consent.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
		
		writeEncode(getString("patient:Consent.Help", Setup.getAppTitle(getLocale()), Setup.getAppOwner(getLocale())));
		write("<br><br>");
		
		writeEncode(getString("patient:Consent.Unencrypted", Setup.getAppTitle(getLocale())));
		write("<br><br>");
		
		writeEncode(getString("patient:Consent.Inboxes", Setup.getAppTitle(getLocale())));
		write("<br><br>");

		writeEncode(getString("elert:General.Disclaimer", Setup.getAppTitle(getLocale())));
		write("<br><br>");

		StringBuilder link1 = new StringBuilder()
			.append("<a href=\"").append(getPageURL(TermsOfUsePage.COMMAND)).append("\">")
			.append(getString("patient:Consent.TermsOfUse")).append("</a>");
		StringBuilder link2 = new StringBuilder()
			.append("<a href=\"").append(getPageURL(PrivacyPolicyPage.COMMAND)).append("\">")
			.append(getString("patient:Consent.PrivacyPolicy")).append("</a>");
		String pattern = Util.htmlEncode(getString("patient:Consent.Terms", Setup.getAppTitle(getLocale()), "$link1$", "$link2$"));
		pattern = Util.strReplace(pattern, "$link1$", link1.toString());
		pattern = Util.strReplace(pattern, "$link2$", link2.toString());
		write(pattern);
		
		write("<br><br>");
		
		writeEncode(getString("patient:Consent.AgreeHelp"));
		write("<br><br>");
		writeButton(getString("patient:Consent.Agree"));
		
		writeFormClose();
	}
		
	@Override
	public void commit() throws Exception
	{
		progressGuidedSetup();
	}
}
