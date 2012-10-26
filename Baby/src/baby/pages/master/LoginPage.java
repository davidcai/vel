package baby.pages.master;

public class LoginPage extends samoyan.apps.master.LoginPage
{
	@Override
	protected void renderLogo() throws Exception
	{
		writeImage("baby/logo.png", null);
	}	
}
