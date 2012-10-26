package samoyan.apps.system;

import java.util.Date;

import samoyan.core.Util;
import samoyan.email.EmailServer;
import samoyan.servlet.WebPage;

public class EmailBeaconPngPage extends WebPage
{
	public final static String COMMAND = "email.png";

	public final static String PARAM_EXTERNAL_ID = "xid";

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}

	@Override
	public void renderHTML() throws Exception
	{
		// Dispatch event to listeners
		EmailServer.beaconDetected(getParameterString(PARAM_EXTERNAL_ID), new Date());
		
		writeBinary(Util.inputStreamToBytes(getResourceAsStream("blank.png")));
	}

	@Override
	public String getMimeType() throws Exception
	{
		return "image/png";
	}

	@Override
	public boolean isAuthorized() throws Exception
	{
		return true;
	}

	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
	}
}
