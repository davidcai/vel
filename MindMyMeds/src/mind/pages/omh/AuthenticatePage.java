package mind.pages.omh;

import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.User;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;

public class AuthenticatePage extends OmhPage
{
	public static final String COMMAND = "omh/v1.0/authenticate";

	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();

		String username = ctx.getParameter("user");
		String password = ctx.getParameter("password");
		if (Util.isEmpty(username) || Util.isEmpty(password))
		{
			writeError("username and password must be specified");
			statusCode = HttpServletResponse.SC_BAD_REQUEST;
			return;
		}

		User user = authByUsername(username, password);
		if (user == null)
		{
			// Error
			writeError("unauthorized");
			statusCode = HttpServletResponse.SC_UNAUTHORIZED;
			return;
		}

		// Expire in 1 hour
		UUID token = AuthTokenStore.getInstance().createAuthToken(user.getID(), ctx.getUserAgent().getString(), false);

		write("{ ");
		writeJsonStrAttr("auth_token", token.toString(), false);
		write(", ");
		writeJsonStrAttr("expires", df.format(new Date(System.currentTimeMillis() + Setup.getSessionLength())), false);
		write(" }");
	}
}
