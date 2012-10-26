package samoyan.apps.profile;

import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class EmailPage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/email";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:Email.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Server fed = ServerStore.getInstance().loadFederation();

		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:Email.EnterHelp"));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:Email.Email"));
		twoCol.writeTextInput("email", user.getEmail(), 60, User.MAXSIZE_EMAIL);
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(user);

		writeFormClose();
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().open(ctx.getUserID());
		user.setEmail(getParameterString("email"));
		UserStore.getInstance().save(user);

		// Support guided setup
		progressGuidedSetup();

		// Go back to the contact info page
		throw new RedirectException(ContactInfoPage.COMMAND, null);
	}
	
	@Override
	public void validate() throws Exception
	{
		String email = validateParameterString("email", 1, User.MAXSIZE_EMAIL);
		if (!Util.isValidEmailAddress(email))
		{
			throw new WebFormException("email", getString("common:Errors.InvalidValue"));
		}
	}
}
