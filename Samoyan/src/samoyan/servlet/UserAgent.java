package samoyan.servlet;

import java.util.HashMap;
import java.util.Map;

import samoyan.core.Cache;

public class UserAgent implements Cloneable
{
	public final static String MOZILLA = "Mozilla";
	
	public final static String FIREFOX = "Firefox";
	public final static String MSIE = "MSIE";
	public final static String SAFARI = "Safari";
	public final static String CHROME = "Chrome";
	public final static String OPERA = "Opera";
	public final static String KONQUEROR = "Konqueror";
	
	public final static String MAC_OS_X = "MacOSX";
	public final static String IOS = "iOS";
	public final static String ANDROID = "Android";
	public final static String WINDOWS_NT = "WindowsNT";

	public final static String GECKO = "Gecko";
	public final static String WEBKIT = "WebKit";
	public final static String TRIDENT = "Trident";
	public final static String PRESTO = "Presto";

	public final static String IPHONE = "iPhone";
	public final static String IPAD = "iPad";
	public final static String IPOD = "iPod";
	public final static String APPLE_TOUCH = "AppleTouch";
	public final static String MACINTOSH = "Macintosh";
	public final static String BLACKBERRY = "BlackBerry";
	public final static String KINDLE = "Kindle";
	public final static String SAMSUNG = "Samsung";
	
	public final static String COMPUTER = "Computer";
	public final static String SMARTPHONE = "SmartPhone";
	public final static String MOBILE = "Mobile";
	public final static String SPIDER = "Spider";
	
	public final static String VOXEO = "Voxeo";

	private String userAgentString = "";
	private HashMap<String, Float> tags = new HashMap<String, Float>();
	private int screenWidth = 0;
	private int screenHeight = 0;
	private int pixelRatio = 1;
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		UserAgent clone = (UserAgent) super.clone();
		clone.tags = (HashMap<String, Float>) this.tags.clone();
		return clone;
	}

	@Override
	public int hashCode()
	{
		return (screenWidth + "x" + screenHeight + "x" + pixelRatio + ":" + userAgentString).hashCode();
	}

	public static UserAgent createInstance(String userAgentString, int screenWidth, int screenHeight, int pixelRatio)
	{
		if (userAgentString==null)
		{
			userAgentString = "";
		}
		else
		{
			userAgentString = userAgentString.trim();
		}
		
		UserAgent cached = (UserAgent) Cache.get("useragent:" + screenWidth + "x" + screenHeight + "@" + pixelRatio + "." + userAgentString);
		if (cached!=null)
		{
			return cached;
		}
		else
		{
			return new UserAgent(userAgentString, screenWidth, screenHeight, pixelRatio);
		}
	}
	
	private UserAgent(String userAgentString, int screenWidth, int screenHeight, int pixelRatio)
	{
		this.userAgentString = userAgentString;
		this.screenHeight = screenHeight;
		this.screenWidth = screenWidth;
		this.pixelRatio = pixelRatio;
		
		// Sniff Mozilla version
		sniff(MOZILLA, "Mozilla/", "Mozilla/");

		// Sniff browsers
		sniff(FIREFOX, "Firefox/", "Firefox/");
		sniff(CHROME, "Chrome/", "Chrome/");
		sniff(SAFARI, "Safari/", "Version/");
		sniff(MSIE, "MSIE ", "MSIE ");
		sniff(OPERA, "Opera/", "Version/");
		sniff(KONQUEROR, "Konqueror/", "Konqueror/");
		
		// Rendering engine
		sniff(WEBKIT, "AppleWebKit/", "AppleWebKit/");
		sniff(GECKO, "Gecko/", "Gecko/");
		sniff(TRIDENT, "Trident/", "Trident/");
		sniff(PRESTO, "Presto/", "Presto/");
		
		// Sniff OS
		sniff(IOS, "like Mac OS X", " OS ");
		sniff(MAC_OS_X, "Mac OS X ", "Mac OS X ");
		sniff(WINDOWS_NT, "Windows NT ", "Windows NT ");
		sniff(ANDROID, "Android ", "Android ");
		sniff(BLACKBERRY, "BlackBerry", "/"); // e.g. Blackberry8530/5.0.0.886
		
		// Sniff device
		sniff(IPHONE, "iPhone;", null);
		sniff(IPOD, "iPod;", null);
		sniff(IPAD, "iPad;", null);
		sniff(MACINTOSH, "Macintosh;", null);
		sniff(KINDLE, "Kindle/", null);
		sniff(KINDLE, "Kindle Fire", null);
		sniff(KINDLE, "Silk/", null);
		sniff(SAMSUNG, "SAMSUNG", null);

		sniff(APPLE_TOUCH, "iPhone;", null);
		sniff(APPLE_TOUCH, "iPod;", null);
		sniff(APPLE_TOUCH, "iPad;", null);

		// Mobile
		sniff(MOBILE, "Mobile/", null);
		sniff(MOBILE, "Mobile Safari", null);
		sniff(MOBILE, "Opera Mini/", null);
		sniff(MOBILE, "Opera Mobi/", null);
		sniff(MOBILE, "Windows Phone", null);
		sniff(MOBILE, "Windows CE", null);
		sniff(MOBILE, "IEMobile", null);
		sniff(MOBILE, "IEMobile ", "IEMobile ");
		sniff(MOBILE, "IEMobile/", "IEMobile/");
		
		// SmartPhone
		sniff(SMARTPHONE, "BlackBerry", null);
		sniff(SMARTPHONE, "iPod;", null);
		sniff(SMARTPHONE, "iPhone;", null);
		
		// Bot
		sniff(SPIDER, "Bot/", null);
		sniff(SPIDER, "bot/", null);
		sniff(SPIDER, "Java/", null);
		sniff(SPIDER, "HttpClient/", null);
		sniff(SPIDER, "HTTPClient/", null);
		sniff(SPIDER, "Spider", null);
		sniff(SPIDER, "Crawler", null);
		
		// Voxeo
		sniff(VOXEO, "Voxeo-VXML/", "Voxeo-VXML/");
		
		// Apple already divides the reported number of pixels by the pixel ratio.
		// Android phones don't, so we do it here.
		if (this.pixelRatio>1 && !isAppleTouch())
		{
			this.screenWidth /= this.pixelRatio;
			this.screenHeight /= this.pixelRatio;
		}
		
		// Best guess for screen dimensions
		if (this.screenHeight==0 || this.screenWidth==0)
		{
			if (isIPhone() || isIPod())
			{
				this.screenWidth = 320;
				this.screenHeight = 480;
				this.pixelRatio = 2;
			}
			else if (isIPad())
			{
				this.screenWidth = 768;
				this.screenHeight = 1024;
				this.pixelRatio = 2;
			}
			else if (isKindle())
			{
				this.screenWidth = 600;
				this.screenHeight = 800;
			}
			else if (isBlackBerry())
			{
				this.screenWidth = 480;
				this.screenHeight = 360;
			}
			else if (isMobile())
			{
				this.screenWidth = 320;
				this.screenHeight = 480;
			}
			else
			{
				this.screenWidth = 1024;
				this.screenHeight = 768;
			}
		}
		
		// Additional detection
		if (this.tags.containsKey(MOBILE) && (this.screenWidth<=640 || this.screenHeight<=640))
		{
			this.tags.put(SMARTPHONE, 0F);
		}
		
		if (this.tags.containsKey(SMARTPHONE)==false)
		{
			this.tags.put(COMPUTER, 0F);
		}
	}
	
	/**
	 * @param tagName One of the constants that identifies this search.
	 * @param idStr The identifier string that must exist in the <code>userAgentString</code>.
	 * @param versionStr The string after which the version number can be found.
	 * <code>null</code> indicates not to look for a version number.
	 * @return -1 if the identifier string was not found.
	 * 0 if the identifier string was found, but no version string was found.
	 * Otherwise, the version number found following the version string.
	 */
	private void sniff(String tagName, String idStr, String versionStr)
	{
		if (versionStr==null && this.tags.containsKey(tagName))
		{
			return;
		}
		
		// Locate the browser search string
		int a = this.userAgentString.indexOf(idStr);
		if (a<0)
		{
			return;
		}
		
		// Locate the version search string
		if (versionStr==null)
		{
			this.tags.put(tagName, 0F);
			return;
		}
		
		int p = this.userAgentString.indexOf(versionStr);
		if (p<0)
		{
			this.tags.put(tagName, 0F);
			return;
		}
		p += versionStr.length();
		
		int q;
		int dot = 0;
		int n = this.userAgentString.length();
		for (q=p; q<n; q++)
		{
			char c = this.userAgentString.charAt(q);
			if (c=='.' || c=='_') // iOS versions use underscores instead of dots
			{
				dot++;
				if (dot==2) break;
			}
			else if (c==' ')
			{
				// Nothing
			}
			else if (c<'0' || c>'9')
			{
				break;
			}
		}
		
		String vs = this.userAgentString.substring(p, q).trim();
		if (vs.length()==0)
		{
			this.tags.put(tagName, 0F);
			return;
		}
		
		vs = vs.replace('_', '.'); // iOS versions use underscores instead of dots
		try
		{
			this.tags.put(tagName, Float.parseFloat(vs));
		}
		catch (NumberFormatException nfe)
		{
			this.tags.put(tagName, 0F);
		}
	}
	
	// - - - - -
	
	public String getString()
	{
		return userAgentString;
	}

	public Map<String, Float> getTags()
	{
		return this.tags;
	}

	// - - - - -
	
	public boolean isSpider()
	{
		return this.tags.containsKey(SPIDER);
	}
	
	/**
	 * Computer devices have a wide screen (640 pixels or more).
	 * @return
	 */
	public boolean isComputer()
	{
		return this.tags.containsKey(COMPUTER);
	}

	/**
	 * SmartPhone devices have a narrow screen (640 pixels or less).
	 * Rendering is considerably affected to fit this constraint.
	 * @return
	 */
	public boolean isSmartPhone()
	{
		return this.tags.containsKey(SMARTPHONE);
	}

	/**
	 * Mobile devices are characterized by the fact that the user cannot resize their viewing window.
	 * All SmartPhones are also Mobile devices, but also an iPad is a mobile device running a mobile browser.
	 * @return
	 */
	public boolean isMobile()
	{
		return this.tags.containsKey(MOBILE);
	}

	public boolean isMSIE()
	{
		return this.tags.containsKey(MSIE);
	}
	public float getVersionMSIE()
	{
		return this.tags.get(MSIE);
	}
	
	public boolean isSafari()
	{
		return this.tags.containsKey(SAFARI);
	}
	public float getVersionSafari()
	{
		return this.tags.get(SAFARI);
	}

	public boolean isChrome()
	{
		return this.tags.containsKey(CHROME);
	}
	public float getVersionChrome()
	{
		return this.tags.get(CHROME);
	}

	public boolean isFirefox()
	{
		return this.tags.containsKey(FIREFOX);
	}
	public float getVersionFirefox()
	{
		return this.tags.get(FIREFOX);
	}

	public boolean isOpera()
	{
		return this.tags.containsKey(OPERA);
	}
	public float getVersionOpera()
	{
		return this.tags.get(OPERA);
	}

	public boolean isKonqueror()
	{
		return this.tags.containsKey(KONQUEROR);
	}
	public float getVersionKonqueror()
	{
		return this.tags.get(KONQUEROR);
	}

	public boolean isAppleTouch()
	{
		return this.tags.containsKey(APPLE_TOUCH);
	}
	public boolean isIPhone()
	{
		return this.tags.containsKey(IPHONE);
	}
	public boolean isIPod()
	{
		return this.tags.containsKey(IPOD);
	}
	public boolean isIPad()
	{
		return this.tags.containsKey(IPAD);
	}

	public boolean isIOS()
	{
		return this.tags.containsKey(IOS);
	}
	public float getVersionIOS()
	{
		return this.tags.get(IOS);
	}

	public boolean isBlackBerry()
	{
		return this.tags.containsKey(BLACKBERRY);
	}
	public float getVersionBlackBerry()
	{
		return this.tags.get(BLACKBERRY);
	}
	
	public boolean isVoxeo()
	{
		return this.tags.containsKey(VOXEO);
	}
	public float getVersionVoxeo()
	{
		return this.tags.get(VOXEO);
	}

	public boolean isKindle()
	{
		return this.tags.containsKey(KINDLE);
	}

	/**
	 * Returns the user's screen width, or 0 if cannot be determined.
	 * @return
	 */
	public int getScreenWidth()
	{
		return this.screenWidth;
	}
	
	/**
	 * Returns the user's screen height, or 0 if cannot be determined.
	 * @return
	 */
	public int getScreenHeight()
	{
		return this.screenHeight;
	}
	
	/**
	 * Retina displays will have pixel ratio of 2, others 1.
	 * @return
	 */
	public int getPixelRatio()
	{
		return this.pixelRatio;
	}
	public boolean isRetina()
	{
		return this.pixelRatio==2;
	}
}
