package samoyan.apps.admin.config;

import java.util.List;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.database.LogType;
import samoyan.database.LogTypeStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class LogConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/log-config";

	@Override
	public void validate() throws Exception
	{
		List<String> typeNames = LogTypeStore.getInstance().getNames();
		for (String t : typeNames)
		{
			Integer life = getParameterInteger(t);
			if (life!=null && life<0)
			{
				throw new WebFormException(t, getString("common:Errors.InvalidValue"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		
		List<String> typeNames = LogTypeStore.getInstance().getNames();
		for (String t : typeNames)
		{
			LogType type = LogTypeStore.getInstance().openByName(t);
			Integer life = getParameterInteger(t);
			if (life==null)
			{
				life = -1;
			}
			type.setLife(life);
			LogTypeStore.getInstance().save(type);
		}
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("admin:LogConfig.Help"));
		write("<br><br>");

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		List<String> typeNames = LogTypeStore.getInstance().getNames();
		for (String t : typeNames)
		{
			LogType type = LogTypeStore.getInstance().loadByName(t);
			twoCol.writeRow(type.getName());
			twoCol.writeTextInput(t, type.getLife()>=0? type.getLife() : null, 4, 6);
			twoCol.write(" ");
			twoCol.writeEncode(getString("admin:LogConfig.Days"));
		}
		
		twoCol.render();
		
		write("<br>");
		writeSaveButton(null);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:LogConfig.Title");
	}

}
