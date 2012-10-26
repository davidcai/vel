package elert.pages.master;

public class LessStylesheetPage extends samoyan.apps.master.LessStylesheetPage
{

	@Override
	protected String getFontHuge()
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			return "24pt Avenir,Arial,Sans-Serif";
		}
		else
		{
			return super.getFontHuge();
		}
	}

	@Override
	protected String getFontLarge()
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			return "16pt Avenir,Arial,Sans-Serif";
		}
		else
		{
			return super.getFontLarge();
		}
	}

	@Override
	protected String getFontNormal()
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			return "12pt Avenir,Arial,Sans-Serif";
		}
		else
		{
			return super.getFontNormal();
		}
	}

	@Override
	protected String getFontSmall()
	{
		if (getContext().getUserAgent().isSmartPhone()==false)
		{
			return "10pt Avenir,Arial,Sans-Serif";
		}
		else
		{
			return super.getFontSmall();
		}
	}

//	@Override
//	protected void renderEpilogue() throws Exception
//	{
//		write(Util.inputStreamToString(getResourceAsStream("elert/elert-epilogue.less"), "UTF-8"));
//	}

	@Override
	protected void renderPrologue() throws Exception
	{
		writeLESSVar("color-light-green", "#C5DEA7");
		writeLESSVar("color-light-blue", "spin(@color-light-green,140)");
	}

	@Override
	protected String getColorNegativeBackground()
	{
		return "#6C9C38"; // Green from kp.org
	}

	@Override
	protected String getColorAccent()
	{
		return "#3C7BBA"; // Blue from kp.org
	}
}
