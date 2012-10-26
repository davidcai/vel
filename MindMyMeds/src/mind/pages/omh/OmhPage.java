package mind.pages.omh;

import java.text.DateFormat;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import samoyan.core.DateFormatEx;
import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;

public class OmhPage extends WebPage
{
	protected int statusCode = HttpServletResponse.SC_OK;
	protected DateFormat df = DateFormatEx.getISO8601Instance();

	@Override
	public String getMimeType() throws Exception
	{
		return "application/json";
	}

	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
	}

	@Override
	public boolean isProtectXSS() throws Exception
	{
		return false;
	}

	@Override
	public int getStatusCode() throws Exception
	{
		return statusCode;
	}

	protected User authByUsername(String username, String password) throws Exception
	{
		User user = UserStore.getInstance().loadByLoginName(username);
		if (user != null && user.isPassword(password) == false)
		{
			// Reset user to null if password doesn't match
			user = null;
		}

		return user;
	}

	protected User authByToken(String token, String userAgentString) throws Exception
	{
		UUID userId = AuthTokenStore.getInstance().validateAuthToken(UUID.fromString(token), userAgentString);
		return UserStore.getInstance().load(userId);
	}

	protected void writeError(String error)
	{
		write("{ \"errors\": [ \"");
		write(Util.jsonEncode(error));
		write("\" ] }");
	}

	protected void writeJsonStrAttr(String attrName, String attrVal, boolean encode)
	{
		String val = (attrVal != null) ? attrVal : "";

		write("\"");
		write(attrName);
		write("\": \"");
		if (encode)
		{
			write(Util.jsonEncode(val));
		}
		else
		{
			write(val);
		}
		write("\"");
	}

	protected void writeJsonIntAttr(String attrName, int attrVal)
	{
		write("\"");
		write(attrName);
		write("\": ");
		write(attrVal);
	}
}
