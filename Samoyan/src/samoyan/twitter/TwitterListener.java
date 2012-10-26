package samoyan.twitter;

import java.util.Date;

public interface TwitterListener
{
	public void onTwitterSent(TwitterMessage tweetSent);
	public void onTwitterReceived(TwitterMessage tweetReceived, String trackback);
	public void onTwitterDeliveryFailed(String externalID, Date date, String diagnostic);
}
