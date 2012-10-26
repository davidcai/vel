package samoyan.twitter;

import java.util.HashSet;
import java.util.Set;

import samoyan.core.Debug;
import samoyan.database.LogEntryStore;
import samoyan.database.Trackback;
import samoyan.database.TrackbackStore;
import samoyan.servlet.Channel;
import twitter4j.DirectMessage;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import twitter4j.UserStreamAdapter;

final class AccountStreamListener extends UserStreamAdapter
{
	private long ownerID = 0;
	TwitterListener listener = null;
	
	public AccountStreamListener(long ownerID, TwitterListener listener)
	{
		this.ownerID = ownerID;
		this.listener = listener;
	}
	
	@Override
	public void onFollow(User source, User followedUser)
	{
		Debug.logln("onFollow source:@" + source.getScreenName() + " target:@" + followedUser.getScreenName());
	}

	@Override
	public void onDirectMessage(DirectMessage directMessage)
	{
		Debug.logln("TwitterServer.onDirectMessage sender: " + directMessage.getSenderScreenName() + " recipient: "
				+ directMessage.getRecipientScreenName() + " text: " + directMessage.getText());

		if(directMessage.getSenderId() == this.ownerID)
		{
			return; //outgoing message
		}

		//incoming message
		String text = directMessage.getText();
		String sender = directMessage.getSenderScreenName();
		String recipient = directMessage.getRecipientScreenName();
		
		// Detect trackback
		Trackback trackback = null;
		try
		{
			trackback = TrackbackStore.getInstance().loadByIncomingText(Channel.TWITTER, sender, text);
		}
		catch (Exception e)
		{
			LogEntryStore.log(e);
		}
		if (trackback==null)
		{
			// !$! Send back error message?
			return;
		}
		
		// Dispatch event
		TwitterMessage msg = new TwitterMessage();
		msg.setDestination(recipient);
		msg.setSender(sender);
		msg.write(TrackbackStore.getInstance().cleanIncomingText(text));

		this.listener.onTwitterReceived(msg, trackback.getExternalID());
	}

	@Override
	public void onStatus(Status status)
	{
		Set<String> mentions = new HashSet<String>(status.getUserMentionEntities().length);
		for(UserMentionEntity mention : status.getUserMentionEntities())
			mentions.add(mention.getScreenName());

		Set<String> hashtags = new HashSet<String>(status.getHashtagEntities().length);
		for(HashtagEntity hashtag : status.getHashtagEntities())
			hashtags.add(hashtag.getText());

		Debug.logln("onStatus id: " + status.getId() + " sender: " + status.getUser().getScreenName() + " mentions: "
				+ mentions.toString() + " hashtags: " + hashtags.toString() + " in reply: " + status.getInReplyToStatusId()
				+ " text: " + status.getText());
	}
}
