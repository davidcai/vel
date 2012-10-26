package samoyan.sms;

import java.util.Date;

public interface SmsListener
{
	public void onSmsSent(SmsMessage smsSent);
	public void onSmsReceived(SmsMessage smsReceived, String trackback);
	public void onSmsDeliveryFailed(String externalID, Date date, String diagnostic);
	public void onSmsDeliveryConfirmed(String externalID, Date date);
}
