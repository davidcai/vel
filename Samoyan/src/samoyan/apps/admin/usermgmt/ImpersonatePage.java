package samoyan.apps.admin.usermgmt;

import java.util.UUID;

import samoyan.apps.admin.AdminPage;
import samoyan.apps.master.WelcomePage;
import samoyan.database.AuthToken;
import samoyan.database.AuthTokenStore;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;

public class ImpersonatePage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/impersonate";
	public static final String PARAM_ID = "id";
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User userToImpersonate = UserStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (userToImpersonate==null ||
			PermissionStore.getInstance().isUserGrantedPermission(userToImpersonate.getID(), Permission.SYSTEM_ADMINISTRATION))
		{
			// Don't allow impersonating other admins
			throw new PageNotFoundException();
		}
		
		// Deauth the current admin cookie
		AuthToken oldToken = AuthTokenStore.getInstance().load(UUID.fromString(ctx.getCookie(RequestContext.COOKIE_AUTH)));
		AuthTokenStore.getInstance().remove(oldToken.getID());
		
		// Create auth token and set as cookie
		UUID authToken = AuthTokenStore.getInstance().createAuthToken(userToImpersonate.getID(), getContext().getUserAgent().getString(), false, oldToken.getApplePushToken());
		setCookie(RequestContext.COOKIE_AUTH, authToken.toString());
		
		throw new RedirectException(WelcomePage.COMMAND, null);
	}
}
