package mind.pages.master;

public class LoginPage extends samoyan.apps.master.LoginPage
{
	@Override
	protected void renderLogo() throws Exception
	{
		writeImage("mind/logo.png", null);
	}
	
	@Override
	protected boolean isAllowRememberMe()
	{
		return false;
	}
}
