package samoyan.apps.admin.config;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.ViewTableControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class TimelineConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/timeline-config";
	
	private static final int MAX_STOPS = 5;

	@Override
	public void validate() throws Exception
	{
		Map<Integer, Integer> delays = new HashMap<Integer, Integer>();
		for (int s=1; s<MAX_STOPS; s++)
		{
			if (Util.isEmpty(getParameterString("stop." + s)))
			{
				continue;
			}
			
			int v = validateParameterInteger("stop." + s, 1, Server.MAXSIZE_TIMELINE-1);
			if (delays.containsKey(v))
			{
				throw new WebFormException(new String[] {"stop." + s, "stop." + delays.get(v)}, getString("admin:TimelineConfig.SameDelay"));
			}
			delays.put(v, s);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		Server fed = ServerStore.getInstance().openFederation();

		BitSet stopsBitSet = new BitSet();
		for (String channel : Channel.getAll())
		{
			fed.setChannelEnabled(channel, isParameter("on." + channel));
			
			BitSet channelBitSet = new BitSet();
			for (int s=0; s<MAX_STOPS; s++)
			{
				Integer delay;
				if (s==0)
				{
					delay = 0;
				}
				else
				{
					delay = getParameterInteger("stop." + s);
				}
				
				if (delay!=null)
				{
					stopsBitSet.set(delay);
					channelBitSet.set(delay, isParameter("chk." + channel + "." + s));
				}
			}
			
			fed.setTimeline(channel, channelBitSet);
		}
		
		fed.setTimelineStops(stopsBitSet);
		ServerStore.getInstance().save(fed);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public void renderHTML() throws Exception
	{
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

		final boolean smartPhone = getContext().getUserAgent().isSmartPhone();

		ArrayList<String> allChannels = new ArrayList<String>();
		for (String channel : Channel.getPush())
		{
			allChannels.add(channel);
		}

		writeFormOpen();
		
		writeEncode(getString("admin:TimelineConfig.Help", Server.MAXSIZE_TIMELINE-1));
		write("<br><br>");
		
		new ViewTableControl<String>(this, "channels", allChannels)
		{
			@Override
			protected void renderHeaders() throws Exception
			{
				cell();
				
				cellAlign("center");
				writeImageSized("timeline/on-off-switch.png", null, null, smartPhone?32:48, smartPhone?32:48);
				write("<br>");
				writeEncode(getString("admin:TimelineConfig.Enabled"));

				for (int s=0; s<MAX_STOPS; s++)
				{
					cellAlign("center");

					if (s==0)
					{
						writeImageSized("timeline/stopwatch0.png", null, null, smartPhone?32:48, smartPhone?32:48);
						write("<br>");
						writeEncode(getString("admin:TimelineConfig.OnTime"));
					}
					else
					{
						if (s<stops.size() && stops.get(s)<=60 && stops.get(s)%5==0)
						{
							writeImageSized("timeline/stopwatch" + stops.get(s) + ".png", null, null, smartPhone?32:48, smartPhone?32:48);
							write("<br>");
						}
						else
						{
							write("<div style='width:48px;height:48px;'></div>");
						}
						
						writeEncode(getString("admin:TimelineConfig.TimePlus"));
						write("<br>");
						writeTextInput("stop." + s, s<stops.size()? stops.get(s) : null, 2, 3);
						writeEncode(getString("admin:TimelineConfig.Minutes"));
					}
				}
			}

			@Override
			protected void renderRow(String channel) throws Exception
			{
				BitSet channelBitSet = fed.getTimeline(channel);
								
				cell(Channel.getDescription(channel, getLocale()));
				
				cellAlign("center");
				writeCheckbox("on." + channel, null, fed.isChannelEnabled(channel));

				for (int s=0; s<MAX_STOPS; s++)
				{
					cellAlign("center");
					writeCheckbox("chk." + channel + "." + s, null, (s<stops.size() && channelBitSet!=null)? channelBitSet.get(stops.get(s)) : false);
				}
			}
		}.render();
		
		write("<br>");
		writeSaveButton(fed);

		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:TimelineConfig.Title");
	}
}
