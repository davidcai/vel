package samoyan.apps.master;

import java.util.UUID;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.AuthToken;
import samoyan.database.AuthTokenStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.UserAgent;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;

public class OverrideUserAgentPage extends WebPage
{
	public final static String COMMAND = "override-user-agent";
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter("save"))
		{
			validateParameterString("ua", 1, -1);
			validateParameterInteger("w", 64, 4096);
			validateParameterInteger("h", 64, 4096);
			validateParameterDecimal("p", 1F, 2F);
		}
	}

	@Override
	public void commit() throws Exception
	{
		override();
	}

	private void override() throws Exception
	{
		RequestContext ctx = getContext();
				
		if (isParameter("clear"))
		{
			setCookie(RequestContext.COOKIE_OVERRIDE_USER_AGENT, null);
			setCookie(RequestContext.COOKIE_OVERRIDE_SCREEN, null);
		}
		else if (isParameter("ua"))
		{
			String ua = getParameterString("ua");
			Integer width = getParameterInteger("w");			
			Integer height = getParameterInteger("h");
			Float pixelRatio = getParameterDecimal("p");
			
			setCookie(RequestContext.COOKIE_OVERRIDE_USER_AGENT, Util.urlEncode(ua));
			if (width!=null && height!=null && width>=64 && height>=64 && pixelRatio>=1)
			{
				setCookie(RequestContext.COOKIE_OVERRIDE_SCREEN, width + "x" + height + "x" + pixelRatio);
			}
		}
		else
		{
			return;
		}
		
		// Create auth token for new user agent and set as cookie
		if (ctx.getUserID()!=null)
		{
			AuthToken oldToken = AuthTokenStore.getInstance().load(UUID.fromString(ctx.getCookie(RequestContext.COOKIE_AUTH)));
			UUID authToken = AuthTokenStore.getInstance().createAuthToken(ctx.getUserID(), null, false, oldToken.getApplePushToken());
			setCookie(RequestContext.COOKIE_AUTH, authToken.toString());
		}
		
		// Refresh rendering according to new cookies
		throw new RedirectException(ctx.getCommand(), null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("master:OverrideUserAgent.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{		
		RequestContext ctx = getContext();
		UserAgent ua = ctx.getUserAgent();
	
		if (ctx.getMethod().equalsIgnoreCase("GET") && isParameter("ua"))
		{
			override();
		}
		
		// Preset user agents
		write("<h2>");
		writeEncode(getString("master:OverrideUserAgent.Presets"));
		write("</h2>");
		
		String[] presets = {
			"Mobile UI", "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A405 Safari/7534.48.3", String.valueOf(ua.getScreenWidth()), String.valueOf(ua.getScreenHeight()), "1",
			"iPhone 3Gs iOS5", "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A405 Safari/7534.48.3", "320", "480", "1",
//			"iPhone 5 iOS6", "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A405 Safari/7534.48.3", "320", "480", "1",
			"Android HTC ADR6400L", "Mozilla/5.0 (Linux; U; Android 2.3.4; en-us; ADR6400L 4G Build/GRJ22) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1", "369", "554", "1",
			"BlackBerry 9700", "BlackBerry9700/5.0.0.714 Profile/MIDP-2.1 Configuration/CLDC-1.1 VendorID/100", "480", "360", "1",
			"New iPad", "Mozilla/5.0 (iPad; CPU OS 5_1_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9B206 Safari/7534.48.3", "768", "1024", "2",
			"Galaxy S II", "Mozilla/5.0 (Linux; Android 4.1.1; SPH-D710 Build/JRO03L) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19", "480", "800", "1.5",
			"Galaxy S III", "Mozilla/5.0 (Linux; U; Android 4.0.4; en-us; SAMSUNG-SGH-I747 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30", "1280", "720", "2"
		};
		
		ParameterMap params = new ParameterMap();
		for (int i=0; i<presets.length; i+=5)
		{			
			params.clear();
			params.plus("ua", presets[i+1]).plus("w", presets[i+2]).plus("h", presets[i+3]).plus("p", presets[i+4]);
			write("<a href=\"");
			write(getPageURL(COMMAND, params));
			write("\">");
			writeEncode(presets[i] + " (" + presets[i+2] + "x" + presets[i+3] + ")");
			write("</a>");
			write("<br>");
		}
			
		float ratio = ua.getPixelRatio();
		if (ua.isAppleTouch() || ua.isMacOSX())
		{
			// Apple already divides the reported number of pixels by the pixel ratio.
			ratio = 1;
		}
		
		// Custom form
		write("<h2>");
		writeEncode(getString("master:OverrideUserAgent.Custom"));
		write("</h2>");

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("master:OverrideUserAgent.UserAgent"));
		twoCol.writeTextInput("ua", ua.getString(), 60, 256);
		twoCol.writeRow(getString("master:OverrideUserAgent.Width"));
		twoCol.writeTextInput("w", Math.round( ua.getScreenWidth() * ratio ), 3, 5);
		twoCol.writeRow(getString("master:OverrideUserAgent.Height"));
		twoCol.writeTextInput("h", Math.round(ua.getScreenHeight() * ratio ), 3, 5);
		twoCol.writeRow(getString("master:OverrideUserAgent.PixelRatio"));
		twoCol.writeTextInput("p", ua.getPixelRatio(), 1, 3);
		
		twoCol.render();
		
		write("<br>");
		writeButton("save", getString("master:OverrideUserAgent.Override"));
		boolean overriding = ctx.getCookie(RequestContext.COOKIE_OVERRIDE_USER_AGENT)!=null || ctx.getCookie(RequestContext.COOKIE_OVERRIDE_SCREEN)!=null;
		if (overriding)
		{
			write("&nbsp;");
			writeButton("clear", getString("master:OverrideUserAgent.Restore"));
		}
		
		writeFormClose();
	}
}
