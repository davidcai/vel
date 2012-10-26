package samoyan.apps.admin.config;

import java.util.Locale;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class ExternalServicesConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/external-services-config";
	
	@Override
	public void validate() throws Exception
	{
		// Verify
		String xco = validateParameterString("xco", Server.SIZE_XCO_API_KEY, Server.SIZE_XCO_API_KEY);
		if (xco.matches("[a-fA-F0-9]*")==false)
		{
			throw new WebFormException("xco", getString("common:Errors.InvalidValue"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		// Commit
		Server fed = ServerStore.getInstance().openFederation(); 
		fed.setXCoAPIKey(getParameterString("xco").toLowerCase(Locale.US));
		ServerStore.getInstance().save(fed);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
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
		
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("admin:ExternalServicesConfig.XCoAPIKey"));
		twoCol.writeTextInput("xco", fed.getXCoAPIKey(), 60, Server.SIZE_XCO_API_KEY);
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(fed);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:ExternalServicesConfig.Title");
	}
}
