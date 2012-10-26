package samoyan.servlet;

import java.util.Locale;

import samoyan.core.StringBundle;

public class Channel
{
	// Channel consts should not be longer than 8 characters and be all letters
	// If adding a new channel:
	// * Add description string in common.properties
	// * Extend user.isChannelActive
	
	public final static String WEB = "web";
	public final static String FACEBOOK = "fb";
	public final static String EMAIL = "email";
	public final static String SMS = "sms";
	public final static String VOICE = "voice";
	public final static String INSTANT_MESSAGE = "im";
	public final static String FACEBOOK_MESSSAGE = "fbmsg";
	public final static String TWITTER = "twitter";
	
	public final static int MAXLEN_SHORT_TEXT = 140;
	
	private final static String[] channels = {	Channel.WEB,
												Channel.FACEBOOK,
												Channel.EMAIL,
												Channel.SMS,
												Channel.VOICE,
												Channel.INSTANT_MESSAGE,
												Channel.FACEBOOK_MESSSAGE,
												Channel.TWITTER};
	private final static String[] pushChannels = {	Channel.EMAIL,
													Channel.SMS,
													Channel.VOICE,
													Channel.INSTANT_MESSAGE,
													Channel.FACEBOOK_MESSSAGE,
													Channel.TWITTER};

	public final static String getDescription(String channel, Locale loc)
	{
		String desc = StringBundle.getString(loc, null, "common:Channel." + channel);
		if (desc==null)
		{
			desc = channel;
		}
		return desc;
	}
	
	public final static boolean isPush(String channel)
	{
		return	channel.equalsIgnoreCase(WEB)==false &&
				channel.equalsIgnoreCase(FACEBOOK)==false;
	}
	
	public final static String[] getAll()
	{
		return channels;
	}

	public final static String[] getPush()
	{
		return pushChannels;
	}

	public static boolean isSupportsSecureSocket(String channel)
	{
		return channel.equalsIgnoreCase(WEB) || channel.equalsIgnoreCase(VOICE);
	}
}
