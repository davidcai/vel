package samoyan.apps.profile;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.ViewTableControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.AuthTokenStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;

public class AlertTimelinePage extends ProfilePage
{	
	public final static String COMMAND = ProfilePage.COMMAND + "/alert-timeline";

	// !$! Somewhat of a hack to restrict one stop per channel
	private final static boolean FORCE_UNIQUE = true;
	
	@Override
	public void commit() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		BitSet stopsBitSet = fed.getTimelineStops();

		User user = UserStore.getInstance().open(getContext().getUserID());
		for (String channel : Channel.getPush())
		{
			if (fed.isChannelEnabled(channel)==false)
			{
				continue;
			}

			BitSet channelBitSet = new BitSet();
			int s = 0;
			for (int m=0; m<stopsBitSet.size(); m++)
			{
				if (stopsBitSet.get(m)==false)
				{
					continue;
				}
				
				boolean checked = isParameter("chk." + channel + "." + s);
				channelBitSet.set(m, checked);
				
				// FORCE_UNIQUE: Do not allow checking more than one stop per channel
				if (checked && FORCE_UNIQUE)
				{
					break;
				}
				
				s++;
			}
			
			user.setTimeline(channel, channelBitSet);
		}
		
		UserStore.getInstance().save(user);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, "")); // getString("profile:AlertTimeline.Confirmation")));
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		final User user = UserStore.getInstance().load(ctx.getUserID());
		final boolean smartPhone = ctx.getUserAgent().isSmartPhone();

		// Load federal object and figure out the stops
		final Server fed = ServerStore.getInstance().loadFederation();
		BitSet stopsBitSet = fed.getTimelineStops();
		final List<Integer> stops = new ArrayList<Integer>();
		for (int m=0; m<stopsBitSet.size(); m++)
		{
			if (stopsBitSet.get(m)==true)
			{
				stops.add(m);
			}
		}

		ArrayList<String> allChannels = new ArrayList<String>();
		for (String channel : Channel.getPush())
		{
			allChannels.add(channel);
		}

		// Check user settings per channel
		final Map<String, String> userChannelSettings = new HashMap<String, String>();
		userChannelSettings.put(Channel.EMAIL, user.getEmail());
		userChannelSettings.put(Channel.SMS, user.getMobile());
		userChannelSettings.put(Channel.VOICE, user.getPhone());
		userChannelSettings.put(Channel.FACEBOOK_MESSSAGE, user.getFacebook());
		userChannelSettings.put(Channel.TWITTER, user.getTwitter());
		userChannelSettings.put(Channel.INSTANT_MESSAGE, user.getXMPP());
		userChannelSettings.put(Channel.APPLE_PUSH, AuthTokenStore.getInstance().getApplePushTokensByUser(user.getID()).size()>0?"APN":null);
		
		final Map<String, String> channelEditScreens = new HashMap<String, String>();
		channelEditScreens.put(Channel.EMAIL, EmailPage.COMMAND);
		channelEditScreens.put(Channel.SMS, MobilePage.COMMAND);
		channelEditScreens.put(Channel.VOICE, PhonePage.COMMAND);
		channelEditScreens.put(Channel.FACEBOOK_MESSSAGE, null); // !$! FacebookPage not yet impl.
		channelEditScreens.put(Channel.TWITTER, TwitterPage.COMMAND);
		channelEditScreens.put(Channel.INSTANT_MESSAGE, null); // !$! InstantMessagePage not yet impl.
		channelEditScreens.put(Channel.APPLE_PUSH, fed.getApplePushDownloadURL());
		
		writeFormOpen();
		
		writeEncode(getString("profile:AlertTimeline.Help", stops.get(stops.size()-1)));
		write("<br><br>");
		
		new ViewTableControl<String>(this, "channels", allChannels)
		{
			@Override
			protected void renderHeaders() throws Exception
			{
				cell();
				for (int s=0; s<stops.size(); s++)
				{
					cellAlign("center");

					if (s==0)
					{
						writeImageSized("timeline/stopwatch0.png", getString("profile:AlertTimeline.OnTime"), null, smartPhone?32:48, smartPhone?32:48);
						write("<br>");
						writeEncode(getString("profile:AlertTimeline.OnTime"));
					}
					else
					{
						if (stops.get(s)<=60 && stops.get(s)%5==0)
						{
							writeImageSized("timeline/stopwatch" + stops.get(s) + ".png", getString("profile:AlertTimeline.TimePlusMinutes", stops.get(s)), null, smartPhone?32:48, smartPhone?32:48);
							write("<br>");
						}
						else
						{
							write("<div style=\"width:");
							write(smartPhone?32:48);
							write("px;height:");
							write(smartPhone?32:48);
							write("px;\"></div>");
						}
						writeEncode(getString("profile:AlertTimeline.TimePlusMinutes", stops.get(s)));
					}
				}
			}

			@Override
			protected boolean isRenderRow(String channel) throws Exception
			{
				return fed.isChannelEnabled(channel);
			}

			@Override
			protected void renderRow(String channel) throws Exception
			{
				BitSet channelBitSet = user.getTimeline(channel);
				if (channelBitSet==null)
				{
					channelBitSet = fed.getTimeline(channel);
				}
				if (channelBitSet==null)
				{
					channelBitSet = new BitSet();
				}
				
				cell();
				boolean disabled = Util.isEmpty(userChannelSettings.get(channel));
				if (disabled)
				{
					write("<span class=Faded>");
				}
				if (disabled && smartPhone && channelEditScreens.get(channel)!=null)
				{
					writeLink(Channel.getDescription(channel, getLocale()), getPageURL(channelEditScreens.get(channel)));
				}
				else
				{
					writeEncode(Channel.getDescription(channel, getLocale()));
				}
				if (channel.equalsIgnoreCase(Channel.SMS) || channel.equalsIgnoreCase(Channel.VOICE))
				{
					write(" <sup>$</sup>");
				}
				if (disabled)
				{
					if (!smartPhone && channelEditScreens.get(channel)!=null)
					{
						write(" <small>");
						writeLink(getString("profile:AlertTimeline.Enable"), getPageURL(channelEditScreens.get(channel)));
						write("</small>");
					}
					write("</span>");
				}
				
				for (int s=0; s<stops.size(); s++)
				{
					cellAlign("center");
					new CheckboxInputControl(this, "chk." + channel + "." + s)
						.setInitialValue(channelBitSet.get(stops.get(s)))
						.setAttribute("onclick", FORCE_UNIQUE? "forceUnique(this,'" + channel + "')" : null)
						.render();
					// writeCheckbox("chk." + channel + "." + s, null, channelBitSet.get(stops.get(s)));
				}
			}
		}.render();
				
		write("<br>");
		writeSaveButton(user);

		if (fed.isChannelEnabled(Channel.SMS) || fed.isChannelEnabled(Channel.VOICE))
		{
			write("<br><br><sup>$</sup> ");
			writeEncode(getString("profile:AlertTimeline.MessageCosts"));
		}

		writeFormClose();
		
		// FORCE_UNIQUE: Do not allow checking more than one stop per channel
		if (FORCE_UNIQUE)
		{
			write("<script>function forceUnique(e,ch){");
			write("var $e=$(e);");
			write("if ($e.attr('checked')){");
			write("$('INPUT[name^=\"chk.'+ch+'.\"]').removeAttr('checked');");
			write("$e.attr('checked','checked');");
			write("}");
			write("}</script>");
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:AlertTimeline.Title");
	}
}
