package samoyan.apps.profile;

import samoyan.apps.master.GoodbyePage;
import samoyan.controls.TwoColFormControl;
import samoyan.database.LogEntryStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.UserTerminatedLogEntry;

public class CloseAccountPage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/close";

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		// Password
		String pw = validateParameterString("pw", User.MINSIZE_PASSWORD, User.MAXSIZE_PASSWORD);
		if (user.isPassword(pw)==false)
		{
			throw new WebFormException("pw", getString("common:Errors.InvalidValue"));
		}
		
//		// Captcha
//		validateParameterCaptcha("captcha");
		
//		// Confirmation
//		String msg = getString("profile:CloseAccount.CloseMyAccount");
//		String confirm = getParameterString("confirm");
//		if (confirm.equalsIgnoreCase(msg)==false)
//		{
//			throw new WebFormException("confirm", getString("profile:CloseAccount.ConfirmError", msg));
//		}
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		// TERMINATE USER!!!
		
// !$!
//		// Notify user
//		Notifier.sendNotification(Notifier.TERMINATION_NOTIF, user.getID());

		// Log the event
		UserTerminatedLogEntry log = new UserTerminatedLogEntry(ctx.getUserID());
		log.setUserID(ctx.getUserID());
		LogEntryStore.log(log);

		// Terminate user account!!! (must be last action)
		UserStore.getInstance().remove(ctx.getUserID());
		
		// Redirect to goodbye page
		throw new RedirectException(GoodbyePage.COMMAND, null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:CloseAccount.Title");
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		return this.getContext().getUserID()!=null;
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User login = UserStore.getInstance().load(ctx.getUserID());
				
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:CloseAccount.Help"));
		twoCol.writeSpaceRow();

		// User name
		twoCol.writeRow(getString("profile:CloseAccount.LoginName"));
		twoCol.writeEncode(login.getLoginName());

		// Password
		twoCol.writeRow(getString("profile:CloseAccount.Password"));
		twoCol.writePasswordInput("pw", null, 20, User.MAXSIZE_PASSWORD);

		twoCol.writeSpaceRow();

//		// Captcha
//		twoCol.writeRow(getString("profile:CloseAccount.Captcha"));
//		twoCol.writeCaptcha("captcha");
//
//		twoCol.writeSpaceRow();

//		// Signature
//		String msg = getString("profile:CloseAccount.CloseMyAccount");
//		twoCol.writeRow(getString("profile:CloseAccount.Signature"), getString("profile:CloseAccount.ConfirmationMessage", msg));
//		TextInputControl text = new TextInputControl(twoCol, "confirm");
//		text.setSize(20);
//		text.setMaxLength(msg.length());
//		text.setPlaceholder(msg);
//		text.render();
				
		twoCol.render();
		
		write("<br>");
		writeButtonRed("close", getString("profile:CloseAccount.CloseAccount"));
		write("<br><br><b>");
		writeEncode(getString("profile:CloseAccount.Warning"));
		write("</b>");
		
		writeFormClose();
		
//		// Script to disable copy&paste on signature field
//		write("<script type=\"text/javascript\">");
//		write(	"function disableCtrl(ev){");
//		write(		"if(ev.ctrlKey){");
//		write(			"ev.preventDefault()");
//		write(		"}");
//		write(	"}");
//		write(	"$('INPUT[name=confirm]').keydown(disableCtrl);");
//		write("</script>");
	}
	
	
}
