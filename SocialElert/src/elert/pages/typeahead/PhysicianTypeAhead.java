package elert.pages.typeahead;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import elert.app.ElertConsts;
import elert.database.UserEx;
import elert.database.UserExStore;

import samoyan.apps.system.TypeAhead;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.servlet.WebPage;

public class PhysicianTypeAhead extends WebPage
{
	public static final String COMMAND = "physician.typeahead";
		
	public PhysicianTypeAhead()
	{
		setChild(new TypeAhead()
		{
			@Override
			protected void doQuery(String query) throws SQLException, Exception
			{
				UserGroup grp = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS);
				
				List<UUID> ids = UserStore.getInstance().searchByNameInGroup(query, grp.getID());
				for(UUID id : ids)
				{
					User physician = UserStore.getInstance().load(id);
					UserEx physicianEx = UserExStore.getInstance().loadByUserID(id);
					
//					StringBuilder html = new StringBuilder();
//					html.append("<table><tr><td>");
//					if (physician.getAvatar()!=null)
//					{
//						html.append("<img src=\"");
//						html.append(getImageURL(physician.getAvatar(), ElertConsts.IMAGESIZE_SQUARE_32, physician.getDisplayName()));
//						html.append("\">");
//					}
//					else
//					{
//						html.append("<img src=\"");
//						html.append(getResourceURL("blank.png"));
//						html.append("\" width=32 height=32>");
//					}
//					html.append("</td><td>");
//					html.append(physician.getDisplayName());
//					html.append("<br><span class=Faded>").append(physicianEx.getNUID()).append("</span>");
//					html.append("</td></tr></table>");
					
					StringBuilder html = new StringBuilder();
					html.append(physician.getDisplayName());
					if (!Util.isEmpty(physicianEx.getNUID()))
					{
						html.append("<small class=Faded> (").append(physicianEx.getNUID()).append(")</small>");
					}
					
					addOption(physician.getID(), physician.getDisplayName(), html.toString());
				}
			}
		});
	}
}
