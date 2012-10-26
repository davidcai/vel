package samoyan.notif;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import samoyan.core.Cache;
import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.LogEntryStore;
import samoyan.database.Notification;
import samoyan.database.NotificationStore;
import samoyan.database.Trackback;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.email.EmailMessage;
import samoyan.email.EmailServer;
import samoyan.servlet.Channel;
import samoyan.servlet.Controller;
import samoyan.servlet.Dispatcher;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.sms.SmsMessage;
import samoyan.sms.SmsServer;
import samoyan.syslog.NotifLogEntry;
import samoyan.twitter.TwitterMessage;
import samoyan.twitter.TwitterServer;
import samoyan.voice.VoiceServer;

final class NotifRunner implements Runnable
{
	private static Map<UUID, UUID> concurrency = new ConcurrentHashMap<UUID, UUID>();
	
	private UUID notifID = null;
	private String actionText = null;
	private boolean isPostAction = false;
	
	public NotifRunner(UUID notifID)
	{
		this.notifID = notifID;
	}
	
	public NotifRunner(UUID notifID, String action)
	{
		// Action can include the trackback info, so strip it
		int digits = (int) Math.log10(Trackback.MAX_ROUNDROBIN);
		action = action.trim();
		if (action.substring(0, 1).equals(Trackback.PREFIX) &&
			action.length()>=1+digits &&
			action.substring(1, 1+digits).matches("[0-9]*")) // #1234 at start of line
		{
			action = action.substring(5).trim();
		}
		
		this.notifID = notifID;
		this.actionText = action;		
		
		this.isPostAction = true;
	}

	@Override
	public void run()
	{
		// Check that no other thread is processing this notification
		if (concurrency.put(this.notifID, this.notifID)!=null)
		{
			return;
		}
		
		// !$! Need to check here that no other server is processing it either
		
		try
		{
			runInternal();
		}
		catch (Exception e)
		{
			// Log exception
			LogEntryStore.log(e);
			Debug.logStackTrace(e);
		}
		finally
		{
			concurrency.remove(this.notifID);
		}
	}
	
	private void runInternal() throws Exception
	{
		boolean sentOK = false;
		Date now = new Date();
				
		// - - -
		// Load the notif

		// Load the latest version from disk
		Notification notif = NotificationStore.getInstance().open(this.notifID);
		if (notif==null)
		{
			// Most likely a cascaded notif that has been deleted following a reply from the user
			return;
		}
		
		if (this.isPostAction==false) // GET
		{
			if (notif.getStatusCode()!=Notification.STATUS_UNSENT)
			{
				// Already sent by another server of the federation
				return;
			}
			if (notif.getDateStatus().after(now))
			{
				// Delay sending
				return;
			}
			
//			// Remove the notif if an action was received for the same event after the notif was created.
//			// The action was most likely a response to a notif on another channel.
//			List<UUID> actionIDs = ActionStore.getInstance().getByEventID(notif.getEventID());
//			if (actionIDs.size()>0)
//			{
//				Action latestAction = ActionStore.getInstance().load(actionIDs.get(actionIDs.size()-1));
//				if (latestAction!=null && latestAction.getDateReceived().after(notif.getDateCreated()))
//				{
//					NotificationStore.getInstance().remove(notif.getID());
//					return;
//				}
//			}
			
			// !$! Set STATUS_PROCESSING so no other thread picks it up
		}
		else // POST action
		{
			if (notif.getStatusCode()!=Notification.STATUS_SENT && notif.getStatusCode()!=Notification.STATUS_DELIVERED)
			{
				// Safety check to make sure notif was indeed sent
				return;
			}
			
			// !$! Disallow posting on a notif that's been successfully posted on before (to prevent duplicates)
			
			// Derive new notif from original
			Notification derivedNotif = new Notification();
			derivedNotif.setChannel(notif.getChannel());
			derivedNotif.setDateCreated(now);
			derivedNotif.setEventID(notif.getEventID());
			derivedNotif.setUserID(notif.getUserID());
			derivedNotif.setStatusCode(Notification.STATUS_UNSENT);
			derivedNotif.setDateStatus(now);
			derivedNotif.setCommand(notif.getCommand());
			derivedNotif.setParameters(notif.getParameters());
			// Do not save the derived notif here; it will be saved below as needed
			
			notif = derivedNotif;
		}
		
		// - - -
		// Render and deliver
		
		WebPage page = null;
		RequestContext prevCtx = null;
		boolean contextSet = false;
		RequestContext ctx = null;
		try
		{
			User user =  UserStore.getInstance().load(notif.getUserID());
			if (user.isSuspended())
			{
				// Abort this notification for suspended users
				throw new PageNotFoundException();
			}

			// Create the request context object
			ctx = new RequestContext();
			ctx.setChannel(notif.getChannel());
			ctx.setUserID(user.getID());
			ctx.setCommand(notif.getCommand());
			ctx.getParameters().putAll(notif.getParameters());
			if (this.actionText!=null)
			{
				ctx.getParameters().put(RequestContext.PARAM_ACTION, this.actionText);
			}
			ctx.setHost(Setup.getHost());
			ctx.setPort(Setup.getPort());
			ctx.setMethod(this.isPostAction? "POST" : "GET");
			ctx.setTimeZone(user.getTimeZone());
			ctx.getLocales().add(user.getLocale());
			
			prevCtx = RequestContext.setCurrent(ctx);
			contextSet = true;
			
			// Create the page
			page = Dispatcher.lookup(ctx);
			if (page==null)
			{
				throw new PageNotFoundException();
			}
			
			// Prevent SecureSocketExceptions. Always use the "correct" SSL mode for notifs.
			ctx.setSecureSocket(page.isSecureSocket());
			
			// Execute the page
			try
			{
				Dispatcher.execute(page, ctx);
			}
			finally
			{
				if (this.isPostAction && page.isCommitted())
				{											
					// Delete yet unsent notifications on the same event (to stop the cascade)
					List<UUID> unsentSiblingNotifIDs = NotificationStore.getInstance().query(now, null, null, null, notif.getEventID(), Notification.STATUS_UNSENT);
					for (UUID siblingID : unsentSiblingNotifIDs)
					{
						if (Util.objectsEqual(siblingID, notif.getID())==false)
						{
							NotificationStore.getInstance().remove(siblingID);
						}
					}
				}
			}

			String title = page.getTitle();
			if (this.isPostAction && ctx.getChannel().equals(Channel.EMAIL))
			{
				// Get gmail to show the reply in the same conversation
				title = "Re: " + title;
			}
			String body = page.getContentAsString();
			String mimeType = page.getMimeType();
			
			sentOK = sendNotif(notif, user, title, body, mimeType);
			
			if (sentOK)
			{
				// Successfully sent
				notif.setStatusCode(Notification.STATUS_SENT);
				notif.setDateStatus(now);
				NotificationStore.getInstance().save(notif);
				
				// Log the event
				LogEntryStore.log(new NotifLogEntry());
			}
			else
			{
//				// Channel is disabled or inactive
//				int failCount = notif.getFailCount();
//				notif.setFailCount(failCount+1);
//	
////				final int minutes[] = {1,2,4,8,15,30,60,240,720,1440,2880};
//				final int minutes[] = {1,2,4,8,15};
//				if (failCount<minutes.length)
//				{
//					// Retry later in the future
//					notif.setStatusCode(Notification.STATUS_UNSENT);
//					notif.setDateStatus(new Date(now.getTime() + minutes[failCount] * 60L*1000L));
//				}
//				else
//				{
//					// Permanently failed
//					notif.setStatusCode(Notification.STATUS_FAILED);
//					notif.setDateStatus(now);
//				}
				NotificationStore.getInstance().reportError(notif, now);
				NotificationStore.getInstance().save(notif);
			}
		}
//		catch (SecureSocketException secureSocket)
//		{
//			Debug.logln("SecureSocketException thrown in NotifRunner for:\r\n" + ctx.toString());
//			throw secureSocket;
//		}
		catch (RedirectException redirect)
		{
			// Send out a new notification on redirect
			Notifier.send(notif.getChannel(), null, notif.getUserID(), notif.getEventID(), redirect.getCommand(), redirect.getParameters());
			
			if (notif.getStatusCode()==Notification.STATUS_UNSENT && notif.isSaved())
			{
				// An unsent notification threw RedirectException, so we can delete its record from the database
				NotificationStore.getInstance().remove(notif.getID());
			}
		}
		catch (PageNotFoundException pnf)
		{
			if (notif.getStatusCode()==Notification.STATUS_UNSENT && notif.isSaved())
			{
				// An unsent notification threw PageNotFoundException, so we can delete its record from the database
				NotificationStore.getInstance().remove(notif.getID());
			}
		}
		catch (Exception e)
		{
			// Error sending
			NotificationStore.getInstance().reportError(notif, now);
			NotificationStore.getInstance().save(notif);
			
			// Log exception
			LogEntryStore.log(e);
			Debug.logStackTrace(e);
		}
		finally
		{
			if (contextSet)
			{
				RequestContext.setCurrent(prevCtx);
			}
		}
	}
	
	/**
	 * Deliver the notification to the user.
	 * @param notif The notification object.
	 * @param user The user to deliver to.
	 * @param page The page, already rendered.
	 * @return Whether or not the delivery was completed.
	 * @throws Exception
	 */
	private static boolean sendNotif(Notification notif, User user, String title, String content, String mimeType) throws Exception
	{
		String channel = notif.getChannel();
		
		// EMAIL
		if (channel.equalsIgnoreCase(Channel.EMAIL))
		{
			if (Util.isValidEmailAddress(user.getEmail())==false)
			{
				return false;
			}			
			
			// !$! Embed any CSS files inline
			
			// Send the message
			EmailMessage email = new EmailMessage();
			email.setSubject(title);
			email.setRecipient(user.getEmail(), user.getName());
			email.setContent(mimeType, content);
			
			String externalID = EmailServer.send(email);
			if (externalID==null)
			{
				return false;
			}
			else
			{
				notif.setExternalID(externalID);
				return true;
			}
		}
		
		// SMS
		else if (channel.equalsIgnoreCase(Channel.SMS))
		{
			if (/*Util.isValidPhoneNumber(user.getMobile())==false ||*/ user.isMobileVerified()==false)
			{
				return false;
			}
			
			SmsMessage sms = new SmsMessage();
			sms.setDestination(user.getMobile());
			sms.setCarrierID(user.getMobileCarrierID());
			sms.write(content);

			String externalID = SmsServer.sendMessage(sms);			
			if (externalID==null)
			{
				return false;
			}
			else
			{
				notif.setExternalID(externalID);
				return true;
			}
		}
		
		// Twitter
		else if (channel.equalsIgnoreCase(Channel.TWITTER))
		{
			TwitterMessage tweet = new TwitterMessage();
			tweet.setDestination(user.getTwitter());
			tweet.write(content);
			String externalID = TwitterServer.sendPrivateMessage(tweet);
			if (externalID==null)
			{
				return false;
			}
			else
			{
				notif.setExternalID(externalID);
				return true;
			}
		}
		
		//Voice
		else if (channel.equalsIgnoreCase(Channel.VOICE))
		{
			if (/*Util.isValidPhoneNumber(user.getPhone())==false ||*/ user.isPhoneVerified()==false)
			{
				return false;
			}
						
//			//save the notification markup for later use (when we get a web service request for the voicexml document)
//			notif.setMarkup(content);
//			NotificationStore.getInstance().save(notif);
//			
//			//create and send initial request
//			VoiceMessage message = new VoiceMessage();
//			message.setDestination(user.getPhone());
//			message.setCallerID(notif.getID().toString());
//			
//			String externalID = VoiceServer.sendMessage(message);			
//			if (externalID==null)
//			{
//				return false;
//			}
//			else
//			{
//				notif.setExternalID(externalID);
//				return true;
//			}
			
			String externalID = VoiceServer.startOutboundCall(user.getID(), user.getPhone(), notif.getCommand(), notif.getParameters());
			if (externalID==null)
			{
				return false;
			}
			else
			{
				notif.setExternalID(externalID);
				return true;
			}			
		}
			
		// Unknown channel
		else
		{
			// Will cause not to retry this notification
			throw new PageNotFoundException();
		}
	}
	
	/**
	 * Embeds the CSS from the indicated files inline into the HTML code.
	 * Supports TAG, #ID and .Class directives only.
	 * @throws IOException 
	 */
	private static String inlineCSS(String html, List<String> cssFiles) throws IOException
	{
		if (cssFiles==null || cssFiles.size()==0)
		{
			return html;
		}
		
		// Load all CSS rules
		Map<String, String> allRules = new HashMap<String, String>();
		for (String cssFile : cssFiles)
		{
			Map<String, String> rules = calcRules(cssFile);
			for (String selector : rules.keySet())
			{
				String rule = rules.get(selector);
				String allRule = allRules.get(selector);
				if (allRule==null)
				{
					allRules.put(selector.toLowerCase(Locale.US), rule);
				}
				else
				{
					allRules.put(selector.toLowerCase(Locale.US), allRule + ";" + rule);
				}
				
			}
		}
		
		// Embed the CSS rules inline
		return inlineCSSRules(html, allRules);
	}
	
	private static Map<String, String> calcRules(String cssFileName) throws IOException
	{
		String cacheKey = "notifier.css:" + cssFileName;
		Map<String, String> cached = (Map<String, String>) Cache.get(cacheKey);
		if (cached!=null)
		{
			return cached;
		}
		
		// Read the CSS file
//		File file = new File(Controller.getWebRoot() + "res" + File.separator + cssFileName.replace('/', File.separatorChar));
//		byte[] buffer = new byte[(int) file.length()];
//		FileInputStream inStm = new FileInputStream(file);
//		inStm.read(buffer);
//		inStm.close();
//		String text = new String(buffer, "UTF-8");
//		inStm = null;
//		buffer = null;
		String text = Util.inputStreamToString(Controller.getResourceAsStream(UrlGenerator.COMMAND_RESOURCE + "/" + cssFileName), "UTF-8");
		
		// Remove any comments
		StringBuffer buf = null;
		int p;
		int q = 0;
		while (true)
		{
			p = text.indexOf("/*", q);
			if (p<0)
			{
				if (buf!=null)
				{
					buf.append(text.substring(q));
				}
				break;
			}
			else
			{
				if (buf==null)
				{
					buf = new StringBuffer(text.length());
				}
				buf.append(text.substring(q, p));
				
				q = text.indexOf("*/", p+2);
				if (q<0)
				{
					break;
				}
				else
				{
					q += 2;
				}
			}
		}
		if (buf!=null)
		{
			text = buf.toString();
			buf = null;
		}
		
		// Parse the rules
		Map<String, String> result = new HashMap<String, String>();
		q = 0;
		while (true)
		{
			p = text.indexOf("{", q);
			if (p<0)
			{
				break;
			}
			
			String selectors = text.substring(q, p);
			
			q = text.indexOf("}", p);
			if (q<0)
			{
				break;
			}
			
			String rules = text.substring(p+1, q);
			
			StringTokenizer tokens = new StringTokenizer(selectors, ",");
			while (tokens.hasMoreTokens())
			{
				String token = tokens.nextToken().trim();
				result.put(token, rules);
			}
			
			q++;
		}

		Cache.insert(cacheKey, result);
		return result;
	}

	private static String inlineCSSRules(String html, Map<String, String> css)
	{
		if (Util.isEmpty(html) || css==null || css.size()==0)
		{
			return html;
		}
		
		StringBuffer result = new StringBuffer(html.length() * 5 / 4); // 20% bigger
		
		int q = 0;
		int p = 0;
		while (true)
		{
			p = html.indexOf("<", q);
			if (p<0)
			{
				result.append(html.substring(q));
				break;
			}
			
			result.append(html.substring(q, p));
			
			q = html.indexOf(">", p);
			if (q<0)
			{
				// Shouldn't happen
				result.append(html.substring(p));
				break;
			}
			
			String fullTag = html.substring(p+1, q); // Remove < and >

			// Tag name
			int s = fullTag.indexOf(" ");
			if (s<0) s = fullTag.length();
			String tagName = fullTag.substring(0, s);

			// Class name
			String className = null;
			int c = fullTag.toLowerCase(Locale.US).indexOf(" class=");
			if (c>=0)
			{				
				c += 7;
				int d = fullTag.indexOf(" ", c);
				if (d<0) d = fullTag.length();
				className = fullTag.substring(c, d);
				if (className.startsWith("\"") || className.startsWith("'"))
				{
					className = className.substring(1);
				}
				if (className.endsWith("\"") || className.endsWith("'"))
				{
					className = className.substring(0, className.length()-1);
				}
			}
			
			// ID
			String id = null;
			int i = fullTag.toLowerCase(Locale.US).indexOf(" id=");
			if (i>=0)
			{				
				i += 4;
				int d = fullTag.indexOf(" ", i);
				if (d<0) d = fullTag.length();
				id = fullTag.substring(i, d);
				if (id.startsWith("\"") || id.startsWith("'"))
				{
					id = id.substring(1);
				}
				if (id.endsWith("\"") || id.endsWith("'"))
				{
					id = id.substring(0, id.length()-1);
				}
			}

			// Get matching CSS rules
			String tagStyle = css.get(tagName.toLowerCase(Locale.US));
			String idStyle = null;
			if (!Util.isEmpty(id))
			{
				id = css.get("#" + id.toLowerCase(Locale.US));
			}
			String classStyle = null;
			if (!Util.isEmpty(className))
			{
				StringTokenizer tokens = new StringTokenizer(className, " ");
				while (tokens.hasMoreTokens())
				{
					String rule = css.get("." + tokens.nextToken().trim().toLowerCase(Locale.US));
					if (rule!=null)
					{
						if (classStyle==null)
						{
							classStyle = rule;
						}
						else
						{
							classStyle += rule;
						}
					}
				}
			}
			
			// Write
			result.append("<");
			result.append(fullTag);
			
			if (tagStyle!=null || idStyle!=null || classStyle!=null)
			{
				result.append(" style=\"");
				if (tagStyle!=null)
				{
					result.append(tagStyle);
				}
				if (classStyle!=null)
				{
					result.append(classStyle);
				}
				if (idStyle!=null)
				{
					result.append(idStyle);
				}
				result.append("\"");
			}			
			result.append(">");
			
			q++;
		}

		return result.toString();
	}
}
