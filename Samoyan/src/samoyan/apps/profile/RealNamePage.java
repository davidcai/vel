package samoyan.apps.profile;

import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;

public class RealNamePage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/name"; 

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:RealName.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
				
		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		// Help
		twoCol.writeTextRow(getString("profile:RealName.Help", Setup.getAppTitle(getLocale()), User.MINSIZE_NAME));
		
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:RealName.RealName"));
		new TextInputControl(twoCol, "name")
			.setMaxLength(User.MAXSIZE_NAME)
			.setSize(40)
			.setInitialValue(user.getName())
			.render();
		
		twoCol.render();

		write("<br>");	
		writeSaveButton(user);
				
		writeFormClose();
	}
	
	@Override
	public void validate() throws Exception
	{
		validateParameterString("name", User.MINSIZE_NAME, User.MAXSIZE_NAME);
	}
	
	@Override
	public void commit() throws Exception
	{
		User user = UserStore.getInstance().open(getContext().getUserID());
		user.setName(getParameterString("name"));
		UserStore.getInstance().save(user);
		
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}
}
