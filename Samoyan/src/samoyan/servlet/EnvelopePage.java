package samoyan.servlet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.apps.admin.AdminTab;
import samoyan.apps.guidedsetup.GuidedSetupTab;
import samoyan.apps.master.LoginPage;
import samoyan.apps.master.MasterTab;
import samoyan.apps.master.PrivacyPage;
import samoyan.apps.master.TermsPage;
import samoyan.apps.system.UnresponsiveVoiceCallPage;
import samoyan.controls.ImageControl;
import samoyan.controls.MetaTagControl;
import samoyan.controls.NavTreeControl;
import samoyan.controls.TabBarControl;
import samoyan.core.Cache;
import samoyan.core.Util;
import samoyan.database.Permission;
import samoyan.database.PermissionStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.exc.WebFormException;

public class EnvelopePage extends WebPage
{	
	private static MasterTab masterTab = new MasterTab();
	private static AdminTab adminTab = new AdminTab();
	private static GuidedSetupTab guidedSetupTab = new GuidedSetupTab();
	
	/**
	 * To be overridden by subclass to return the list of tabs to show for the given request.
	 * @return
	 * @throws Exception
	 */
	protected List<EnvelopeTab> getTabs() throws Exception
	{
		List<EnvelopeTab> tabs = new ArrayList<EnvelopeTab>();
		tabs.add(masterTab);
		
		if (PermissionStore.getInstance().isUserGrantedPermission(getContext().getUserID(), Permission.SYSTEM_ADMINISTRATION))
		{
			tabs.add(adminTab);
		}
		
		User user = UserStore.getInstance().load(getContext().getUserID());
		if (user!=null && user.isGuidedSetup())
		{
			tabs.add(guidedSetupTab);
		}
		
		return tabs;
	}
	
	/**
	 * To be overridden by subclasses to return the resource file to show in the header of the message.
	 * @return
	 */
	protected String getApplicationLogo()
	{
		return null;
	}
	/**
	 * To be overridden by subclasses to return the resource file to show in the footer of the message.
	 * @return
	 */
	protected String getOwnerLogo()
	{
		return null;
	}

	// - - - - - - - - - -
	// WEB

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		UserAgent ua = ctx.getUserAgent();
		
		write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		write("<html><head>");

			String pageTitle = getTitle(); // subclass
			if (!Util.isEmpty(pageTitle))
			{
				write("<title>");
				writeEncode(pageTitle);
				write("</title>");
			}
			
			MetaTagControl metaTags = new MetaTagControl(this);
			metaTags.contentType();
			metaTags.viewportNoScale();
			metaTags.appleMobileWebAppCapable();
			metaTags.appleMobileWebAppStatusBarStyleBlack();
			metaTags.render();
			
			// Javascript
			writeUserAgentJS();
			if (Setup.isDebug())
			{
				writeIncludeJS("jquery-1.7.1.js");
			}
			else
			{
				writeIncludeJS("jquery-1.7.1.min.js");
			}
			writeIncludeJS("sessionstorage-1.4.js"); // Session storage for older browsers
			writeIncludeJS("samoyan.js");
			
			// CSS
			write("<style>BODY{visibility:hidden;}</style>"); // Hide BODY until CSS loads
			writeIncludeCSS("samoyan.less");
			
			this.renderHTMLMetaTagsAndIncludes(); // subclass

			// Refresh the page if we don't detect the COOKIE_SCREEN or COOKIE_TIMEZONE_OFFSET cookies, but no more than once.
			boolean metaRefresh = false;
			if (ctx.getCookie(RequestContext.COOKIE_SCREEN)==null || ctx.getCookie(RequestContext.COOKIE_TIMEZONE_OFFSET)==null)
			{
				String cacheKey = "cookierefresh:" + ctx.getIPAddress() + "." + ctx.getIdentifyingHeadersSHA256();
				Long lastRefresh = (Long) Cache.get(cacheKey);
				if (lastRefresh==null || lastRefresh + 2000L < ctx.getTime())
				{
					write("<meta http-equiv=\"refresh\" content=\"0\">");
					Cache.insert(cacheKey, ctx.getTime());
					metaRefresh = true;
				}
			}

		write("</head>");
		writeBodyOpen();
		
			if (metaRefresh==false)
			{
				if (ua.isSmartPhone()==false)
				{
					write("<table cellspacing=0 cellpadding=0 id=layout>");
					write("<tr id=header><td>");
						
						write("<noscript><div>");
						writeEncode(getString("common:Errors.EnableJavascript"));
						write("</div></noscript>");
	
						write("<div class=Inner>");
						renderHTMLTopBar(); // subclass
						write("</div>");
						
					write("</td></tr>");
					write("<tr id=middle><td>");
					
						write("<table cellspacing=0 cellpadding=0 class=Inner><tr><td id=navbar>");
							renderHTMLNavBar(); // subclass
						write("</td><td id=page>");
							if (!Util.isEmpty(pageTitle))
							{
								write("<h1>");
								writeEncode(pageTitle);
								write("</h1>");
							}
							renderHTMLPage();
						write("</td></tr></table>");
					
					write("</td></tr>");
					write("<tr id=footer><td>");
					
						write("<div class=Inner>");
						renderHTMLFooter(); // subclass
						write("</div>");
					
					write("</td></tr></table>");
				}
				else
				{
					write("<div id=layout>");
					
					// Fixed title bar
					write("<div id=titlebar>");
					write("<table class=Fixed><tr><td>");
					
					// Render the button initially hidden
					String id = UUID.randomUUID().toString();
					new ImageControl(this)
						.resource("navbar-back.png")
						.setStyleAttribute("display", "none")
						.setAttribute("id", "backBtn"+id)
						.render();
					// Show it, if the page ID is at the top of the stack
					write("<script type=\"text/javascript\">backActivateButton('backBtn");
					write(id);
					write("');</script>");
					
//					writeBackButton(null, null);
					write("</td><td><h1>");
					if (!Util.isEmpty(pageTitle))
					{
						writeEncode(pageTitle);
					}
					write("</h1></td><td align=right>");
					new ImageControl(this).resource("navbar-toggle.png").setAttribute("onclick", "$('#page').toggle();$('#navbar').toggle();").render();
					write("</td></tr>");
					write("</table>");
					write("</div>");
										
					write("<div id=page>");
					renderHTMLPage();
					write("</div>");
	
					write("<div id=navbar style='display:none;'>");
					renderHTMLNavBar(); // subclass
					write("</div>");
					
					write("<div id=footer>");
					renderHTMLFooter(); // subclass
					write("</div>");
	
					write("<div id=tabbar><div class=Fixed>");
					renderHTMLTopBar(); // subclass
					write("</div></div>");
					
					write("</div>");
				}
			}
		
		writeBodyClose();
		write("</html>");
	}
		
	private void renderHTMLPage() throws Exception
	{
		// Error messages
		WebFormException formExc = getFormException();
		if (formExc!=null)
		{
			write("<div class=ErrorMessage>");
			writeEncode(formExc.getMessage());
			write("</div>");
		}
		else if (isParameter(RequestContext.PARAM_SAVED))
		{
			String msg = null;
//			msg = getParameterString(RequestContext.PARAM_SAVED);
//			if (Util.isEmpty(msg))
			{
				msg = getString("common:Envelope.SavedMessage", new Date());
			}
			write("<div id=savemsg class=InfoMessage>");
			writeEncode(msg);
			write("</div>");
//			write("<script>$('#savemsg').delay(5000).fadeOut(1000);</script>");
		}
			
		// Main content
		WebPage child = getChild();
		if (child!=null)
		{
			child.render();
		}
	}

	/**
	 * To be overridden by subclass to render the navigation area of the page.
	 * It's recommended that subclasses can use the {@link NavTreeControl} to render this section of the page.
	 */
	protected void renderHTMLNavBar() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		
		// User name
		boolean smartPhone = ctx.getUserAgent().isSmartPhone();
		if (!smartPhone && user!=null && !Util.isEmpty(user.getName()))
		{
			write("<div id=welcome>");
			write("<small>");
			writeEncode(getString("common:Envelope.Web.Welcome"));
			write("</small><br>");
			writeEncode(user.getName());
			write("</div>");
		}

		// Render the nav bar of the current tab
		getCurrentTab().getNavTree(this).render();
	}
	
	protected final EnvelopeTab getCurrentTab() throws Exception
	{
		RequestContext ctx = getContext();

		List<EnvelopeTab> tabs = getTabs();
		EnvelopeTab defaultTab = null;

		String cmd = ctx.getCommand(1);
		for (EnvelopeTab tab : tabs)
		{
			if (Util.isEmpty(tab.getCommand()))
			{
				defaultTab = tab;
			}
			else if (cmd.equals(tab.getCommand()))
			{
				return tab;
			}
		}
		return defaultTab;
	}
	
	/**
	 * To be overridden by subclass to render the top area of the page.
	 * It's recommended that subclasses can use the {@link TopBarControl} to render this section of the page.
	 */
	protected void renderHTMLTopBar() throws Exception
	{
		TabBarControl tabBar = new TabBarControl(this);
		List<EnvelopeTab> tabs = this.getTabs(); // call subclass
		for (EnvelopeTab tab : tabs)
		{
			tabBar.addTab(tab.getIcon(this), tab.getLabel(this), tab.getCommand());
		}
		tabBar.render();
	}
	
	/**
	 * To be overridden by subclass to render the head area of the page.
	 * Subclasses should use {@link #writeIncludeCSS(String)} and/or {@link #writeIncludeJS(String)} and
	 * the {@link MetaTagControl}.
	 */
	protected void renderHTMLMetaTagsAndIncludes() throws Exception
	{
	}
	
	/**
	 * To be overridden by subclass to render the footer area of the page.
	 * This should typically include a copyright notice and links to the terms of use and privacy policy.
	 */
	protected void renderHTMLFooter() throws Exception
	{
		boolean smartPhone = getContext().getUserAgent().isSmartPhone();
		Calendar cal = Calendar.getInstance(getTimeZone());

		if (!smartPhone)
		{
			write("<table width=\"100%\"><tr><td align=right>");
			writeEncode(getString("common:Envelope.Copyright", String.valueOf(cal.get(Calendar.YEAR)), Setup.getAppOwner(getLocale())));
			write(" | ");
			writeLink(getString("common:Envelope.Terms"), getPageURL(TermsPage.COMMAND));
			write(" | ");
			writeLink(getString("common:Envelope.Privacy"), getPageURL(PrivacyPage.COMMAND));
			write("</td></tr></table>");
		}
		else
		{
			write("<div align=center>");
			writeEncode(getString("common:Envelope.Copyright", String.valueOf(cal.get(Calendar.YEAR)), Setup.getAppOwner(getLocale())));
			write("<br>");
			writeLink(getString("common:Envelope.Terms"), getPageURL(TermsPage.COMMAND));
			write(" | ");
			writeLink(getString("common:Envelope.Privacy"), getPageURL(PrivacyPage.COMMAND));
			write("</div>");
		}
	}
	
	// - - - - - - - - - -
	// Simple HTML
	
	@Override
	public void renderSimpleHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());

		boolean isEmail = getContext().getChannel().equals(Channel.EMAIL);

		// Style
//		write("<div style='font-size:10pt;font-family:verdana,ariel,helvetica,san-serif;'>");
				
		// Header
		write("<div>");
		this.renderSimpleHTMLHeader();
		write("</div>");
		
		// Body (with border)
		write("<div");
		if (isEmail)
		{
			write(" style='border-width:2px 0px; border-style: solid; border-color: #ccc; padding:10px 0px 10px 0px;'");
		}
		write(">");
		
			// Greeting
			if (isEmail)
			{
				if (!Util.isEmpty(user.getName()))
				{
					write(Util.textToHtml(getString("common:Envelope.Email.Greeting", user.getName())));
				}
				else
				{
					write(Util.textToHtml(getString("common:Envelope.Email.GreetingWithoutName")));
				}
				write("<br><br>");
			}
		
			WebFormException wfExc = getFormException();
			if (wfExc!=null)
			{
				write(Util.textToHtml(getString("common:Envelope.SimpleHTML.ErrorPreamble", Setup.getAppTitle(getLocale()))));
				write("<br><br>");
				writeEncode(wfExc.getMessage());
				write("<br><br>");
				writeEncode(getString("common:Envelope.SimpleHTML.ErrorPostamble"));
			}
			else
			{
				WebPage child = getChild();
				if (child!=null)
				{
					child.render();
				}
			}
			
			if (isEmail)
			{
				write("<br><br>");
				write(Util.textToHtml(getString("common:Envelope.Email.Signature", Setup.getAppOwner(getLocale()))));
			}
			
		write("</div>");
		
		// Footer
		write("<div>");	
		this.renderSimpleHTMLFooter();
		write("</div>");
		
		// Style
//		write("</div>");
	}
	
	/**
	 * To be overridden by the subclass to render the header of the page. By default, renders the application logo.
	 * @throws Exception
	 */
	protected void renderSimpleHTMLHeader() throws Exception
	{
		if (getContext().getChannel().equals(Channel.EMAIL))
		{
			// Banner
			String appLogo = getApplicationLogo();
			if (appLogo!=null)
			{
				writeImage(appLogo, Setup.getAppTitle(getLocale()));
			}
		}
	}
	
	/**
	 * To be overridden by the subclass to render the footer of the page. By default, renders a copyright notice and
	 * the owner's logo.
	 * @throws Exception
	 */
	protected void renderSimpleHTMLFooter() throws Exception
	{		
		if (getContext().getChannel().equals(Channel.EMAIL))
		{
			StringBuffer link = new StringBuffer();
			link.append("<a href=\"");
			link.append(getPageURL(LoginPage.COMMAND));
			link.append("\">");
			link.append(Util.htmlEncode(getString("common:Envelope.Email.Login", Setup.getAppTitle(getLocale()))));
			link.append("</a>");
			
			String pattern = Util.htmlEncode(getString("common:Envelope.Email.Footer", "$link$"));
			pattern = Util.strReplace(pattern, "$link$", link.toString());
			write("<small>");
			write(pattern);
			write("</small>");		
			
			// Logo
			String ownerLogo = getOwnerLogo();
			if (ownerLogo!=null)
			{
				write("<div align=right>");
				writeImage(ownerLogo, Setup.getAppOwner(getLocale()));
				write("</div>");
			}
		}
	}

	// - - - - - - - - - -
	// Short text
	
	@Override
	public void renderShortText() throws Exception
	{
		WebFormException wfExc = getFormException();
		if (wfExc!=null)
		{
			// Send back error messages
			write(wfExc.getMessage());
		}
		else
		{
			WebPage child = getChild();
			if (child!=null)
			{
				child.render();
			}
		}
		
//		// Add x.co short URL to end of message
//		Server fed = ServerStore.getInstance().loadFederation();
//		if (!Util.isEmpty(fed.getXCoAPIKey()))
//		{
//			RequestContext ctx = getContext();
//			Map<String, String> params = new HashMap<String, String>(ctx.getParameters());
//			params.remove(RequestContext.PARAM_ACTION);
//			String url = getPageURL(ctx.getCommand(), params);
//			url = XCoShortenUrl.shorten(fed.getXCoAPIKey(), url);
//			write(" " + url);
//		}
	}

	// - - - - - - - - - -
	// Voice XML
	
	@Override
	public void renderVoiceXML() throws Exception
	{		
		UUID sessionID = getContext().getSessionID();

		write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		write("<vxml version=\"2.1\" xml:lang=\"");
		writeEncode(getLocale().getLanguage());
		if (!Util.isEmpty(getLocale().getCountry()))
		{
			write("-");
			writeEncode(getLocale().getCountry());
		}
		write("\">");

//		boolean pressToContinuePrompt = true;
		if (getContext().getMethod().equalsIgnoreCase("GET"))
		{
			write("<form id=\"intro\">");
			if (sessionID!=null)
			{
				write("<var name=\"");
				writeEncode(RequestContext.PARAM_SESSION);
				write("\" expr=\"'");
				writeEncode(sessionID.toString());
				write("'\"/>");
			}
			
//			write("<block>");
//			this.renderVoiceXMLGreeting();
//			write("</block>");
			
//			if (pressToContinuePrompt==false)
//			{
//				write("<field name=\"dummy\">");
//				write("<prompt timeout=\"500ms\">.</prompt>");
//				write("<grammar root=\"ruleroot\" mode=\"dtmf\">");
//				write("<rule id=\"ruleroot\" scope=\"public\">");
//				write("<one-of><item>1</item></one-of>");
//				write("</rule>");
//				write("</grammar>");
//				write("</field>");
//	
//				write("<filled namelist=\"dummy\"><goto next=\"#home\"/></filled>");
//				write("<noinput><goto next=\"#home\"/></noinput>");
//				write("<nomatch><goto next=\"#home\"/></nomatch>");
//			}
//			else
			{
				final String DIGIT = "1";
				
				write("<field name=\"cont\">");
				
				write("<prompt bargein=\"true\" bargeintype=\"hotword\"><break time=\"1s\"/></prompt>"); // To reduce chances of message truncation
				this.renderVoiceXMLGreeting();
				
				write("<prompt timeout=\"500ms\" bargein=\"true\" bargeintype=\"hotword\">");
				write("<break time=\"500ms\"/>");
				writeEncode(getString("common:Envelope.Voice.PressToContinue", DIGIT, getString("common:Envelope.Voice.Continue")));
				write("<audio src=\"");
				write(getResourceURL("sound/lazysnake3.mp3"));
//				write("\" maxage=\"");
//				write(Setup.getClientCacheExpires()/1000L);
//				write("\" maxstale=\"");
//				write(Setup.getClientCacheExpires()/1000L);
				write("\"/>");
				write("</prompt>");
				
				write("<grammar root=\"druleroot\" mode=\"dtmf\">");
				write("<rule id=\"druleroot\" scope=\"public\">");
				write("<one-of>");
				for (int i=0; i<=9; i++)
				{
					write("<item>");
					write(i);
					write("</item>");
				}
				write("<item>*</item>");
				write("<item>#</item>");
				write("</one-of>");
				write("</rule>");
				write("</grammar>");

				write("<grammar root=\"vrootrule\" mode=\"voice\">");
				write("<rule id=\"vrootrule\" scope=\"public\">");
				write("<one-of>");
				write("<item>");
				writeEncode(getString("common:Envelope.Voice.Continue"));
				write("<tag>out.cont=\"");
				write(DIGIT);
				write("\"</tag>");
				write("</item>");
				write("</one-of>");
				write("</rule>");
				write("</grammar>");

				write("<filled namelist=\"cont\">");
				write("<if cond=\"cont=='");
				write(DIGIT);
				write("'\">");
				write("<break time=\"500ms\"/><goto next=\"#home\"/>");
				write("<else/>");
				write("<clear name=\"cont\"/><throw event=\"nomatch\"/>"); // Manually throw a nomatch
				write("</if>");
				write("</filled>");
								
				write("<nomatch><reprompt/></nomatch>");
//				write("<catch event=\"nomatch\"><goto next=\"#intro\"/></catch>");
				
				write("<noinput count=\"1\"><reprompt/></noinput>");
//				write("<noinput count=\"2\"><reprompt/></noinput>");
//				write("<noinput count=\"3\"><reprompt/></noinput>");
//				write("<noinput count=\"4\"><reprompt/></noinput>");
				
				write("<noinput count=\"2\">");
				this.renderVoiceXMLUnresponsive();
				write("<submit method=\"POST\" next=\"");
				write(getPageURL(UnresponsiveVoiceCallPage.COMMAND));
				write("\" namelist=\"session.sessionid ");
				writeEncode(RequestContext.PARAM_SESSION);
				write("\"/>");
				write("</noinput>");
				
				write("</field>");
			}
			
			write("</form>");
		}

		write("<form id=\"home\">"); // ID "home" is assumed in ActionListControl and above
		if (sessionID!=null)
		{
			write("<var name=\"");
			writeEncode(RequestContext.PARAM_SESSION);
			write("\" expr=\"'");
			writeEncode(sessionID.toString());
			write("'\"/>");
		}

		WebFormException wfExc = getFormException();
		if(wfExc!=null)
		{
			write("<block><prompt>");
			writeEncode(wfExc.getMessage());
			write("</prompt></block>");
		}
		
		WebPage child = getChild();
		if (child!=null)
		{
			child.render();
		}
		
		// Handle errors
		// !$! detect 404, say goodbye. Other errors should be noted to the user that the action was not performed.
		write("<catch event=\"error.badfetch\">");
		renderVoiceXMLGoodbye();
		write("<disconnect/></catch>");

		write("<block>");
		renderVoiceXMLGoodbye();
		write("<disconnect/></block>");
		
		write("</form>");
		write("</vxml>");
	}

	/**
	 * To be overwritten by subclasses to render the content of the greeting block that is to be played at the start of each call.
	 * The method is called within the scope of a block VXML tag. Implementations should generally write a prompt tag with some text,
	 * or an audio tag with a link to .wav file.
	 * @throws Exception
	 */
	protected void renderVoiceXMLGreeting() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		write("<prompt bargein=\"true\" bargeintype=\"hotword\">");
		writeEncode(getString("common:Envelope.Voice.Greeting", Setup.getAppTitle(getLocale()), user.getName()));
		write("</prompt>");
	}
	
	/**
	 * To be overwritten by subclasses to render the content of the goodbye block that is to be played at the end of each call.
	 * The method is called within the scope of a block VXML tag. Implementations should generally write a prompt tag with some text,
	 * or an audio tag with a link to .wav file.
	 * @throws Exception
	 */
	protected void renderVoiceXMLGoodbye() throws Exception
	{
		write("<prompt bargein=\"false\">");
		write("<break time=\"500ms\"/>");
		writeEncode(getString("common:Envelope.Voice.Goodbye"));
		write("</prompt>");
	}

	/**
	 * To be overwritten by subclasses to render the content of the message block that is to be played when the user does not seem to be responsive.
	 * The method is called within the scope of a block VXML tag. Implementations should generally write a prompt tag with some text,
	 * or an audio tag with a link to .wav file.
	 * @throws Exception
	 */
	protected void renderVoiceXMLUnresponsive() throws Exception
	{
		User user = UserStore.getInstance().load(getContext().getUserID());
		write("<prompt bargein=\"false\">");
		writeEncode(getString("common:Envelope.Voice.Unresponsive", Setup.getAppTitle(getLocale()), user.getName()));
		write("</prompt>");
	}
}
