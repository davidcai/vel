package baby.pages.profile;

import java.util.Calendar;
import java.util.Date;

import samoyan.apps.profile.CloseAccountPage;
import samoyan.apps.profile.ProfilePage;
import samoyan.core.ParameterMap;
import samoyan.core.TimeZoneEx;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public final class StagePage extends BabyPage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/stage";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Stage.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		String initial = "pre";
		if (mother.getBirthDate()!=null)
		{
			initial = "infancy";
		}
		else if (mother.getDueDate()!=null)
		{
			initial = "pregnany";
		}

		writeEncode(getString("babyprofile:Stage.Help", Setup.getAppTitle(getLocale())));
		write("<br><br>");
		
		writeFormOpen();
		
		write("<table>");
		
		write("<tr valign=middle><td>");
		writeImage("baby/stage-preconception.jpg", getString("babyprofile:Stage.Preconception"));
		write("</td><td>");
		write("<big>");
		writeRadioButton("stage", getString("babyprofile:Stage.PreconceptionDetail"), "pre", initial);
		write("</big>");
		write("</td></tr>");
		
		write("<tr valign=middle><td>");
		writeImage("baby/stage-pregnancy.jpg", getString("babyprofile:Stage.Pregnancy"));
		write("</td><td>");
		write("<big>");
		writeRadioButton("stage", getString("babyprofile:Stage.PregnancyDetail"), "pregnancy", initial);
		write(" ");
		writeDateInput("due", mother.getDueDate());
		write("</big>");
		if (mother.getDueDate()==null)
		{
			write("<br><span class=Faded>");
			writeEncode(getString("babyprofile:Stage.PregnancyHelp"));
			write("</span>");
		}
		write("</td></tr>");

		write("<tr valign=middle><td>");
		writeImage("baby/stage-infancy.jpg", getString("babyprofile:Stage.Infancy"));
		write("</td><td>");
		write("<big>");
		writeRadioButton("stage", getString("babyprofile:Stage.InfancyDetail"), "infancy", initial);
		write(" ");
		writeDateInput("delivery", mother.getBirthDate());
		write("</big>");
		write("</td></tr>");

		write("</table>");
		
		write("<br>");
		writeSaveButton(mother);
		
		writeFormClose();
		
		write("<br><br>");
		writeLink(getString("babyprofile:Stage.Unsubscribe", Setup.getAppTitle(getLocale())), getPageURL(CloseAccountPage.COMMAND));
	}
	
	@Override
	public void validate() throws Exception
	{
		String stage = getParameterString("stage");
		if (stage.equals("pre"))
		{
			// Nothing
		}
		else if (stage.equals("pregnancy"))
		{
			Calendar cal = Calendar.getInstance(TimeZoneEx.GMT, getLocale());
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.add(Calendar.DATE, 1);
			Date from = cal.getTime();
			
			cal.add(Calendar.MONTH, 9);
			Date to = cal.getTime();
			
			validateParameterDate("due", from, to);
		}
		else if (stage.equals("infancy"))
		{
			Calendar cal = Calendar.getInstance(TimeZoneEx.GMT, getLocale());
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			Date to = cal.getTime();
			
			cal.add(Calendar.MONTH, -12);
			Date from = cal.getTime();
			
			validateParameterDate("delivery", from, to);
		}
		else
		{
			throw new WebFormException("stage", getString("common:Errors.InvalidValue"));
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		Mother mother = MotherStore.getInstance().openByUserID(getContext().getUserID());

		String stage = getParameterString("stage");
		if (stage.equals("pre"))
		{
			mother.setDueDate(null);
			mother.setBirthDate(null);
		}
		else if (stage.equals("pregnancy"))
		{
			mother.setDueDate(getParameterDate("due"));
			mother.setBirthDate(null);
		}
		else if (stage.equals("infancy"))
		{
			mother.setDueDate(null);
			mother.setBirthDate(getParameterDate("delivery"));
		}
		
		MotherStore.getInstance().save(mother);
		
		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}
}
