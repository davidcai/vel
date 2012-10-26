package samoyan.apps.admin.config;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

public class TwitterConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/twitter-config";
	@Override
	public void validate() throws Exception
	{
		int min = isParameter("active")? 1:0;	
		
		validateParameterString("user", min, -1);
		validateParameterString("oauth.consumerKey", min, -1);
		validateParameterString("oauth.consumerSecret", min, -1);
		validateParameterString("oauth.accessToken", min, -1);
		validateParameterString("oauth.accessTokenSecret", min, -1);
	}

	@Override
	public void commit() throws Exception
	{
		Server fed = ServerStore.getInstance().openFederation();

		fed.setTwitterUserName(getParameterString("user"));
		
		//OAuth properties
		fed.setTwitterOAuthConsumerKey(getParameterString("oauth.consumerKey"));
		fed.setTwitterOAuthConsumerSecret(getParameterString("oauth.consumerSecret"));
		fed.setTwitterOAuthAccessToken(getParameterString("oauth.accessToken"));
		fed.setTwitterOAuthAccessTokenSecret(getParameterString("oauth.accessTokenSecret"));
		
		// Status
		fed.setTwitterDebug(isParameter("debug"));
		fed.setTwitterActive(isParameter("active"));

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
		
		twoCol.writeRow(getString("admin:TwitterConfig.Status"));
		twoCol.writeCheckbox("active", getString("admin:TwitterConfig.Active"), fed.isTwitterActive());
		twoCol.writeCheckbox("debug", getString("admin:TwitterConfig.Debug"), fed.isTwitterDebug());
		
		twoCol.writeRow(getString("admin:TwitterConfig.User"));
		twoCol.writeTextInput("user", fed.getTwitterUserName(), 60, Server.MAXSIZE_USER);

		twoCol.writeRow(getString("admin:TwitterConfig.ConsumerKey"));
		twoCol.writeTextInput("oauth.consumerKey", fed.getTwitterOAuthConsumerKey(), 60, -1);
		twoCol.writeRow(getString("admin:TwitterConfig.ConsumerSecret"));
		twoCol.writeTextInput("oauth.consumerSecret", fed.getTwitterOAuthConsumerSecret(), 60, -1);
		twoCol.writeRow(getString("admin:TwitterConfig.AccessToken"));
		twoCol.writeTextInput("oauth.accessToken", fed.getTwitterOAuthAccessToken(), 60, -1);
		twoCol.writeRow(getString("admin:TwitterConfig.AccessTokenSecret"));
		twoCol.writeTextInput("oauth.accessTokenSecret",  fed.getTwitterOAuthAccessTokenSecret(), 60, -1);
		twoCol.writeSpaceRow();

		twoCol.render();
		writeSaveButton(fed);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:TwitterConfig.Title");
	}
}
