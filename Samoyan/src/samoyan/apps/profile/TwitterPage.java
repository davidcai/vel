package samoyan.apps.profile;

import samoyan.controls.TwoColFormControl;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.twitter.TwitterServer;

public class TwitterPage extends ProfilePage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/twitter";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("profile:Twitter.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().load(ctx.getUserID());
		Server fed = ServerStore.getInstance().loadFederation();

		if (!fed.isChannelEnabled(Channel.TWITTER) || !fed.isTwitterActive())
		{
			// To prevent guided setup from getting stuck
			progressGuidedSetup();

			throw new PageNotFoundException();
		}
		
		String twitterUser = fed.getTwitterUserName();
		String twitterUrl = "https://twitter.com/" + twitterUser;

		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("profile:Twitter.EnterHelp", twitterUser));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("profile:Twitter.UserName"));
		
		twoCol.write("<table><tr valign=center><td>");
		twoCol.writeTextInput("twitter", user.getTwitter(), 40, User.MAXSIZE_TWITTER);
		twoCol.write("</td><td>");
	
		twoCol.write("<a href=\"");
		twoCol.writeEncode(twitterUrl);
		twoCol.write("\" class=\"twitter-follow-button\" data-show-count=\"false\" data-size=\"large\" data-lang=\"");
		twoCol.writeEncode(getLocale().getLanguage());
		twoCol.write("\" data-dnt=\"true\">");
		twoCol.writeEncode(getString("profile:Twitter.Follow", twitterUser));
		twoCol.write("</a>");
		twoCol.write("</td></tr></table>");
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(user);

		writeFormClose();
		
		write("<script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=\"//platform.twitter.com/widgets.js\";fjs.parentNode.insertBefore(js,fjs);}}(document,\"script\",\"twitter-wjs\");</script>");
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		User user = UserStore.getInstance().open(ctx.getUserID());

		String twitterUserName = getParameterString("twitter");
		if (twitterUserName.startsWith("@"))
		{
			twitterUserName = twitterUserName.substring(1);
		}
		user.setTwitter(twitterUserName);

		UserStore.getInstance().save(user);

		// Support guided setup
		progressGuidedSetup();

		// Go back to the contact info page
		throw new RedirectException(ContactInfoPage.COMMAND, null);
	}
	
	@Override
	public void validate() throws Exception
	{
		String twitterUserName = validateParameterString("twitter", 0, User.MAXSIZE_TWITTER);
		if (twitterUserName.startsWith("@"))
		{
			twitterUserName = twitterUserName.substring(1);
		}
		if (!Util.isEmpty(twitterUserName))
		{
			if (!TwitterServer.validateUser(twitterUserName) || twitterUserName.matches("^\\w{1,15}$")==false)
			{
				throw new WebFormException("twitter", getString("common:Errors.InvalidTwitterUser"));
			}
		}
	}
}
