package samoyan.apps.system;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import samoyan.core.Captcha;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;


public class CaptchaImagePage extends WebPage
{
	public final static String COMMAND = "captcha.jpg";
	
	public final static String PARAM_ID = "id";
	
	@Override
	public void renderHTML() throws Exception
	{
		UUID uuid = getParameterUUID(PARAM_ID);
		if (uuid==null)
		{
			throw new PageNotFoundException();
		}

		Captcha cap = Captcha.getByKey(uuid);
		if (cap==null)
		{
			throw new PageNotFoundException();
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		cap.createImage(os);
		writeBinary(os.toByteArray());
	}

	@Override
	public String getMimeType() throws Exception
	{
		return "image/jpeg";
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return getContext().isSecureSocket();
	}

	@Override
	public boolean isEnvelope() throws Exception
	{
		return false;
	}
}
