package samoyan.email;

import java.util.Date;

public interface EmailListener
{
	public void onEmailSent(EmailMessage msgSent);
	
	/**
	 * 
	 * @param msgReceived The message received.
	 * @param trackback If the message received is a reply to an outgoing message, this will be the external ID of the original message.
	 */
	public void onEmailReceived(EmailMessage msgReceived, String trackback);
	
	/**
	 * 
	 * @param failureNotice
	 * @param trackback The external ID of the original message that failed to deliver.
	 * @param failedAddress
	 * @param diagnostic
	 */
	public void onEmailDeliveryFailure(EmailMessage failureNotice, String trackback, String failedAddress, String diagnostic);
	
	public void onEmailOpened(String externalID, Date date);
}
