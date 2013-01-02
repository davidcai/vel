package samoyan.applepush;

import java.util.List;
import samoyan.core.Debug;
import samoyan.database.AuthTokenStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.Setup;

import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import javapns.notification.PushNotificationPayload;

/**
 * User the JavaPNS library to send Apple Push Notifications.
 * @author brianwillis
 * @see http://code.google.com/p/javapns/
 * @see http://www.bouncycastle.org/java.html
 *
 */
public class ApplePushServer
{
	public final static String SOUND_DEFAULT = "default";
	
	private static long lastExpunge = 0;
	
	/**
	 * Calls Apple to get a list of APN tokens that are no longer valid, and remove them from the database so that
	 * messages are no longer sent to those devices.
	 * This method is called periodically before sending an alert, so it typically does not need to be called externally.
	 * @throws KeystoreException 
	 * @throws CommunicationException 
	 */
	public static void expungeInvalidTokens() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isApplePushActive()==false)
		{
			return;
		}
		
		List<Device> devices = Push.feedback(
			fed.getApplePushKeystore(),
			fed.getApplePushKeystorePassword(),
			fed.isApplePushProduction());
		for (Device d : devices)
		{
			AuthTokenStore.getInstance().invalidateApplePushToken(d.getToken());
			Debug.logln("Apple push token invalidated: " + d.getToken());
		}
	}
	
	private static void expungeInvalidTokensIfDue() throws Exception
	{
		if (lastExpunge + Setup.getSessionLength() < System.currentTimeMillis())
		{
			lastExpunge = System.currentTimeMillis();
			expungeInvalidTokens();
		}
	}
	
	/**
	 * 
	 * @param deviceIDs <code>String</code> or <code>List&lt;String&gt;</code> of Apple push tokens.
	 * @param text
	 * @param url
	 * @param sound
	 * @param badge
	 * @throws Exception
	 * @return Number of successful messages sent.
	 */
	public static int sendPayload(Object deviceIDs, String text, String url, String sound, int badge) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isApplePushActive()==false)
		{
			return 0;
		}
		expungeInvalidTokensIfDue();
				
		PushNotificationPayload complexPayLoad = PushNotificationPayload.complex();
		complexPayLoad.addAlert(text);
		if (badge>=0)
		{
			complexPayLoad.addBadge(badge);
		}
		if (sound!=null)
		{
			complexPayLoad.addSound(sound);
		}
		if (url!=null)
		{
			complexPayLoad.addCustomDictionary("url", url);
		}
		
		return Push.payload(
			complexPayLoad,
			fed.getApplePushKeystore(),
			fed.getApplePushKeystorePassword(),
			fed.isApplePushProduction(),
			deviceIDs)
		.getSuccessfulNotifications().size();
	}
	
	/**
	 * Updates the badge number without popping a notification.
	 * @param deviceIDs
	 * @param badge
	 * @return
	 * @throws Exception
	 * @return Number of successful messages sent.
	 */
	public static int updateBadge(Object deviceIDs, int badge) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		if (fed.isApplePushActive()==false)
		{
			return 0;
		}
		expungeInvalidTokensIfDue();
		
		return Push.badge(
			badge,
			fed.getApplePushKeystore(),
			fed.getApplePushKeystorePassword(),
			fed.isApplePushProduction(),
			deviceIDs)
		.getSuccessfulNotifications().size();
	}
}
