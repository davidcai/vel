package samoyan.apps.profile;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.MobileCarrier;
import samoyan.database.MobileCarrierStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;

public class ContactInfoPage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/contact-info";

	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:ContactInfo.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Server fed = ServerStore.getInstance().loadFederation();
		ParameterMap goBackParams = new ParameterMap(RequestContext.PARAM_GO_BACK_ON_SAVE, "");

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);

		// Help
		twoCol.writeTextRow(getString("profile:ContactInfo.Help"));

		twoCol.writeSpaceRow();

		// Email
		twoCol.writeRow(getString("profile:ContactInfo.Email"));
		twoCol.writeEncode(user.getEmail());
		twoCol.write(" <small>");
		twoCol.writeLink(getString("profile:ContactInfo.Edit"), getPageURL(EmailPage.COMMAND, goBackParams));
		twoCol.write("</small>");

		// Mobile
		if (fed.isChannelEnabled(Channel.SMS))
		{
			String mobile = user.getMobile();
			twoCol.writeRow(getString("profile:ContactInfo.Mobile"), getString("profile:ContactInfo.MobileHelp"));
			if (!Util.isEmpty(mobile))
			{
				twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(mobile));

				MobileCarrier carrier = MobileCarrierStore.getInstance().load(user.getMobileCarrierID());
				if (carrier!=null)
				{
					twoCol.writeEncode(" ");
					twoCol.writeEncode(carrier.getName());
				}
			}
			else
			{
				twoCol.write("<span class=Faded>");
				twoCol.writeEncode(getString("profile:ContactInfo.None"));
				twoCol.write("</span>");
			}
			twoCol.write(" <small>");
			twoCol.writeLink(getString("profile:ContactInfo.Edit"), getPageURL(MobilePage.COMMAND, goBackParams));
			twoCol.write("</small>");
		}
		
		// Phone
		if (fed.isChannelEnabled(Channel.VOICE))
		{
			String phone = user.getPhone();
			twoCol.writeRow(getString("profile:ContactInfo.Phone"), getString("profile:ContactInfo.PhoneHelp"));
			if(!Util.isEmpty(phone))
			{
				twoCol.writeEncode(Util.stripCountryCodeFromPhoneNumber(phone));
			}
			else
			{
				twoCol.write("<span class=Faded>");
				twoCol.writeEncode(getString("profile:ContactInfo.None"));
				twoCol.write("</span>");
			}			
			twoCol.write(" <small>");
			twoCol.writeLink(getString("profile:ContactInfo.Edit"), getPageURL(PhonePage.COMMAND, goBackParams));
			twoCol.write("</small>");
		}
		
//		boolean spacer = (fed.isChannelEnabled(Channel.SMS) || fed.isChannelEnabled(Channel.VOICE));
//
//		// Instant message
//		if (fed.isChannelEnabled(Channel.INSTANT_MESSAGE))
//		{
//			if (spacer)
//			{
//				twoCol.writeSpaceRow();
//				spacer = false;
//			}
//			
//			twoCol.writeRow(getString("profile:ContactInfo.IM"), getString("profile:ContactInfo.IMHelp"));
//			twoCol.writeTextInput("xmpp", user.getXMPP(), 40, User.MAXSIZE_EMAIL);
//		}
//		
//		// Facebook
//		if (fed.isChannelEnabled(Channel.FACEBOOK))
//		{
//			if (spacer)
//			{
//				twoCol.writeSpaceRow();
//				spacer = false;
//			}
//			
//			twoCol.writeRow(getString("profile:ContactInfo.Facebook"));
//			twoCol.writeTextInput("fb", user.getFacebook(), 20, User.MAXSIZE_FACEBOOK);		
//		}
		
		// Twitter
		if (fed.isChannelEnabled(Channel.TWITTER) && fed.isTwitterActive())
		{
//			if (spacer)
//			{
//				twoCol.writeSpaceRow();
//				spacer = false;
//			}
			
			twoCol.writeRow(getString("profile:ContactInfo.Twitter"));
			if(!Util.isEmpty(user.getTwitter()))
			{
				twoCol.writeEncode("@" + user.getTwitter());
			}
			else
			{
				twoCol.write("<span class=Faded>");
				twoCol.writeEncode(getString("profile:ContactInfo.None"));
				twoCol.write("</span>");
			}			
			twoCol.write(" <small>");
			twoCol.writeLink(getString("profile:ContactInfo.Edit"), getPageURL(TwitterPage.COMMAND, goBackParams));
			twoCol.write("</small>");
		}

		twoCol.render();
	}
}
