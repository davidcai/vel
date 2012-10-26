package samoyan.sms;

import java.util.Date;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.CountryStore;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.Trackback;
import samoyan.database.TrackbackStore;
import samoyan.email.EmailMessage;
import samoyan.email.EmailServer;
import samoyan.servlet.Channel;

public final class SmsServer implements SmsListener
{
	private SmsListenerPool listeners = new SmsListenerPool(false);
	private long countReceived = 0;
	private long countSent = 0;
	private long countFailed = 0;
	
	private static SmsServer instance = new SmsServer();
	private SmsServer()
	{
	}

	public static void init() throws Exception
	{
		// Add self as listener
		instance.listeners.addListener(instance);
	}
	
	public static void terminate()
	{
		// Clear listeners
		instance.listeners.removeAllListeners();
	}

	/**
	 * Adds a listener to intercept SMS events.
	 * @param listener This listener is invoked in the same thread as the <code>SmsServer</code> so implementations should return quickly or otherwise
	 * create their own sub-thread. 
	 */
	public static void addListener(SmsListener listener)
	{
		instance.listeners.addListener(listener);
	}
	
	public static void removeListener(SmsListener listener)
	{
		instance.listeners.removeListener(listener);
	}

	public static void incoming(String from, String to, String text) throws Exception
	{
		// Detect trackback
		Trackback trackback = TrackbackStore.getInstance().loadByIncomingText(Channel.SMS, from, text);
		if (trackback==null)
		{
			// !$! Send error message back to sender?
			return;
		}
		
		// Dispatch the message to the SmsListeners
		Debug.logln("Incoming SMS for trackback " + trackback.getExternalID());
		
		SmsMessage sms = new SmsMessage();
		sms.write(TrackbackStore.getInstance().cleanIncomingText(text));
		sms.setSender(from);
		sms.setDestination(to);
		
		SmsServer.incomingMessage(sms, trackback.getExternalID());
	}
	
	/**
	 * This method is called when an SMS message is received by the gateway.
	 * It should not be called by others.
	 * @param sms
	 */
	public static void incomingMessage(SmsMessage sms, String trackback)
	{
		instance.listeners.onSmsReceived(sms, trackback);
	}
	
	/**
	 * This method is called when an SMS message failure notice is received by the gateway.
	 * It should not be called by others.
	 * @param sms
	 */
	public static void deliveryFailed(String externalID, Date date, String diagnostic)
	{
		instance.listeners.onSmsDeliveryFailed(externalID, date, diagnostic);
	}

	/**
	 * This method is called when an SMS message confirmation is received by the gateway.
	 * It should not be called by others.
	 * @param sms
	 */
	public static void deliveryConfirmed(String externalID, Date date)
	{
		instance.listeners.onSmsDeliveryConfirmed(externalID, date);
	}

	/**
	 * Sends an SMS message.
	 * @param sms
	 * @return <code>true</code> if the message was sent; <code>false</code> otherwise.
	 * @throws Exception
	 */
	public static String sendMessage(SmsMessage sms) throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();

		// --If we know the carrier, we can send the message by email gateway
		
		if (fed.isUseEmailGatewaysForSMS() && fed.isSMTPActive() && fed.isIMAPActive())
		{
			MobileCarrier carrier = MobileCarrierStore.getInstance().load(sms.getCarrierID());
			if (carrier!=null && Util.isValidEmailAddress(carrier.getSMSEmail()))
			{
				String destination = sms.getDestination();

				// Isolate the country prefix of this phone number
				String countryCode = null;
				String prefix = null;
				String localNumber = null;
				int slash = destination.indexOf("/");
				if (slash>=0)
				{
					countryCode = destination.substring(0, slash);
					prefix = CountryStore.getInstance().loadByCodeISO2(countryCode).getPhonePrefix();
					localNumber = destination.substring(slash + 1 + prefix.length());
				}
				else if (destination.startsWith("1"))
				{
					// Backward compatibility for US numbers
					countryCode = "US";
					prefix = "1";
					localNumber = destination.substring(1);
				}
				
				// Find the country of this phone number
				if (localNumber!=null)
				{
					String gateway = carrier.getSMSEmail();
					int at = gateway.indexOf("@");
					String smsEmail = Util.strReplace(gateway.substring(0, at), "n", localNumber) + gateway.substring(at);

					if (Util.isValidEmailAddress(smsEmail))
					{
						// Trackback
						Trackback trackback = new Trackback();
						trackback.setChannel(Channel.EMAIL);
						trackback.setAddressee(smsEmail.substring(0, smsEmail.indexOf("@"))); // The pre-@ part of the email address
						TrackbackStore.getInstance().save(trackback); // Must save here in order to get correct roundrobin number
						String trackbackStr = " " + Trackback.PREFIX + trackback.getRoundRobinAsString();

						EmailMessage email = new EmailMessage();
						email.setSubject(null);
						email.setRecipientAddress(smsEmail);
						email.setContent("text/plain", sms.getText() + trackbackStr);
						
						try
						{
							String id = EmailServer.send(email);
							if (id!=null)
							{
								trackback.setExternalID(id);
								TrackbackStore.getInstance().save(trackback);
	
								instance.listeners.onSmsSent(sms);
								return id;
							}
							else
							{
								TrackbackStore.getInstance().remove(trackback.getID());
							}
						}
						catch (Exception e)
						{
							TrackbackStore.getInstance().remove(trackback.getID());
							throw e;
						}
					}
				}
			}
		}
		
		// --Otherwise, we send through a service provider
		
		if (fed.isOpenMarketActive()==false && fed.isClickatellActive()==false && fed.isBulkSMSActive()==false)
		{
			throw new IllegalStateException("No active SMS providers");
		}

		// Trackback
		Trackback trackback = new Trackback();
		trackback.setChannel(Channel.SMS);
		trackback.setAddressee(Util.stripCountryCodeFromPhoneNumber(sms.getDestination()));
		TrackbackStore.getInstance().save(trackback); // Must save here in order to get correct roundrobin number
		String trackbackStr = " " + Trackback.PREFIX + trackback.getRoundRobinAsString();

		if (fed.isOpenMarketActive() && !Util.isEmpty(fed.getOpenMarketDemoPrefix()))
		{
			trackbackStr = " " + fed.getOpenMarketDemoPrefix() + trackbackStr;
		}
		
		// Send SMS
		try
		{
			String id = null;
			if (fed.isOpenMarketActive())
			{
				id = OpenMarket.send(sms.getSender(), sms.getDestination(), sms.getText() + trackbackStr);
			}
			else if (fed.isClickatellActive())
			{
				id = Clickatell.send(sms.getSender(), sms.getDestination(), sms.getText() + trackbackStr);
			}
			else if (fed.isBulkSMSActive())
			{
				id = BulkSMS.send(sms.getSender(), sms.getDestination(), sms.getText() + trackbackStr);
			}
			
			trackback.setExternalID(id);
			TrackbackStore.getInstance().save(trackback);

			instance.listeners.onSmsSent(sms);

			return id;
		}
		catch (Exception e)
		{
			TrackbackStore.getInstance().remove(trackback.getID());
			throw e;
		}
	}

	@Override
	public void onSmsSent(SmsMessage msg)
	{
		this.countSent++;
	}

	@Override
	public void onSmsDeliveryFailed(String externalID, Date date, String diagnostic)
	{
		this.countFailed++;
	}

	@Override
	public void onSmsReceived(SmsMessage msg, String trackback)
	{
		this.countReceived++;
	}
	
	@Override
	public void onSmsDeliveryConfirmed(String externalID, Date date)
	{
		// Do nothing
	}

	public static long getCountMessagesReceived()
	{
		return instance.countReceived;
	}

	public static long getCountDeliveryFailures()
	{
		return instance.countFailed;
	}

	public static long getCountMessagesSent()
	{
		return instance.countSent;
	}
}
