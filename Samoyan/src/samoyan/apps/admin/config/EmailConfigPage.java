package samoyan.apps.admin.config;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

/**
 * Email configuration settings for the system.
 * @author brian
 *
 */
public class EmailConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/email-config";

	
	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void validate() throws Exception
	{
		// Validate SMTP
		String smtpHost = validateParameterString("smtp.host", isParameter("smtp.status")?1:0, Server.MAXSIZE_HOST);
		if (smtpHost.matches("[\\w\\x2e\\x2d]*")==false) // Word letters, dot, hyphen
		{
			throw new WebFormException("smtp.host", getString("common:Errors.InvalidValue"));
		}
		validateParameterInteger("smtp.port", 0, 65535);
		validateParameterString("smtp.user", isParameter("smtp.status")?1:0, Server.MAXSIZE_USER);
				
		// Validate IMAP
		String imapHost = validateParameterString("imap.host", isParameter("imap.status")?1:0, Server.MAXSIZE_HOST);
		if (imapHost.matches("[\\w\\x2e\\x2d]*")==false) // Word letters, dot, hyphen
		{
			throw new WebFormException("imap.host", getString("common:Errors.InvalidValue"));
		}
		validateParameterInteger("imap.port", 0, 65535);
		validateParameterString("imap.user", isParameter("imap.status")?1:0, Server.MAXSIZE_USER);
		validateParameterInteger("imap.poll", 1, 3600);
	}
	
	@Override
	public void commit() throws Exception
	{
		// Commit
		Server fed = ServerStore.getInstance().openFederation(); 

		// SMTP
		fed.setSMTPActive(isParameter("smtp.status"));
		fed.setSMTPHost(getParameterString("smtp.host"));
		fed.setSMTPPort(getParameterInteger("smtp.port"));
		fed.setSMTPUser(getParameterString("smtp.user"));
		fed.setSMTPPassword(getParameterString("smtp.pw"));
		
		fed.setUseEmailBeacon(isParameter("beacon"));
		
		// IMAP
		fed.setIMAPActive(isParameter("imap.status"));
		fed.setIMAPHost(getParameterString("imap.host"));
		fed.setIMAPPort(getParameterInteger("imap.port"));
		fed.setIMAPUser(getParameterString("imap.user"));
		fed.setIMAPPassword(getParameterString("imap.pw"));
		fed.setIMAPPollingInterval(getParameterInteger("imap.poll") *1000L);

		ServerStore.getInstance().save(fed);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation(); 
		
		writeFormOpen();
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeSubtitleRow(getString("admin:EmailConfig.SMTP"));
		
		twoCol.writeRow(getString("admin:EmailConfig.Status"));
		twoCol.writeCheckbox("smtp.status", getString("admin:EmailConfig.Active"), fed.isSMTPActive());
		twoCol.writeRow(getString("admin:EmailConfig.Host"));
		twoCol.writeTextInput("smtp.host", fed.getSMTPHost(), 40, Server.MAXSIZE_HOST);
		twoCol.writeRow(getString("admin:EmailConfig.Port"));
		twoCol.writeNumberInput("smtp.port", fed.getSMTPPort(), 5, 0, 65535);
		twoCol.writeRow(getString("admin:EmailConfig.User"));
		twoCol.writeTextInput("smtp.user", fed.getSMTPUser(), 40, Server.MAXSIZE_USER);
		twoCol.writeRow(getString("admin:EmailConfig.Password"));
		twoCol.writePasswordInput("smtp.pw", fed.getSMTPPassword(), 20, Server.MAXSIZE_PASSWORD);
		
		twoCol.writeRow(getString("admin:EmailConfig.WebBeacon"), getString("admin:EmailConfig.WebBeaconHelp"));
		twoCol.writeCheckbox("beacon", getString("admin:EmailConfig.Enabled"), fed.isUseEmailBeacon());
				
		twoCol.writeSubtitleRow(getString("admin:EmailConfig.IMAP"));
		
		twoCol.writeRow(getString("admin:EmailConfig.Status"));
		twoCol.writeCheckbox("imap.status", getString("admin:EmailConfig.Active"), fed.isIMAPActive());
		twoCol.writeRow(getString("admin:EmailConfig.Host"));
		twoCol.writeTextInput("imap.host", fed.getIMAPHost(), 40, Server.MAXSIZE_HOST);
		twoCol.writeRow(getString("admin:EmailConfig.Port"));
		twoCol.writeNumberInput("imap.port", fed.getIMAPPort(), 5, 0, 65535);
		twoCol.writeRow(getString("admin:EmailConfig.User"));
		twoCol.writeTextInput("imap.user", fed.getIMAPUser(), 40, Server.MAXSIZE_USER);
		twoCol.writeRow(getString("admin:EmailConfig.Password"));
		twoCol.writePasswordInput("imap.pw", fed.getIMAPPassword(), 20, Server.MAXSIZE_PASSWORD);
		twoCol.writeRow(getString("admin:EmailConfig.Polling"));
		twoCol.writeNumberInput("imap.poll", (int) (fed.getIMAPPollingInterval()/1000L), 4, 1, 3600);
		twoCol.write(" ");
		twoCol.writeEncode(getString("admin:EmailConfig.Seconds"));

		twoCol.render();
		write("<br>");
		writeSaveButton(fed);
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:EmailConfig.Title");
	}
}
