package samoyan.controls;

import samoyan.servlet.WebPage;

public class PasswordInputControl extends TextInputControl
{
	public PasswordInputControl(WebPage outputPage, String name)
	{
		super(outputPage, name);
		
		setAttribute("type", "password");
	}
}
