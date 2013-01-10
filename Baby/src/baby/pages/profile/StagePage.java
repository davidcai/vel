package baby.pages.profile;

import java.util.Calendar;
import java.util.Date;

import samoyan.apps.profile.ProfilePage;
import samoyan.core.TimeZoneEx;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.AfterCommitRedirectException;
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
		boolean phone = getContext().getUserAgent().isSmartPhone();
		
		Mother mother = MotherStore.getInstance().loadByUserID(getContext().getUserID());
		String initial = "pre";
		if (mother.getBirthDate()!=null)
		{
			initial = "infancy";
		}
		else if (mother.getDueDate()!=null)
		{
			initial = "pregnancy";
		}

		writeEncode(getString("babyprofile:Stage.Help", Setup.getAppTitle(getLocale())));
		write("<br><br>");
		
		writeFormOpen();
				
		write("<table class=StageChooser>");
		
		write("<tr valign=middle>");
		if (!phone)
		{
			write("<td>");
			writeImage("baby/stage-preconception.png", getString("babyprofile:Stage.Preconception"));
			write("</td>");
		}
		write("<td>");
		writeRadioButton("stage", null, "pre", initial);
		write("</td><td>");
		writeEncode(getString("babyprofile:Stage.PreconceptionDetail"));
		write("</td></tr>");
		
		write("<tr><td colspan=");
		write(phone?2:3);
		write(">&nbsp;</td></tr>");
		
		write("<tr valign=middle>");
		if (!phone)
		{
			write("<td>");
			writeImage("baby/stage-pregnancy.png", getString("babyprofile:Stage.Pregnancy"));
			write("</td>");
		}
		write("<td>");
		writeRadioButton("stage", null, "pregnancy", initial);
		if (mother.getDueDate()==null)
		{
			write("<br>&nbsp;"); // To align the checkbox with the label
		}
		write("</td><td>");
		writeEncode(getString("babyprofile:Stage.PregnancyDetail"));
		write(" ");
		writeDateInput("due", mother.getDueDate());
		if (mother.getDueDate()==null)
		{
			write("<br><span class=Faded>");
			writeEncode(getString("babyprofile:Stage.PregnancyHelp"));
			write("</span>");
		}
		write("</td></tr>");

		write("<tr><td colspan=");
		write(phone?2:3);
		write(">&nbsp;</td></tr>");

		write("<tr valign=middle>");
		if (!phone)
		{
			write("<td>");
			writeImage("baby/stage-infancy.png", getString("babyprofile:Stage.Infancy"));
			write("</td>");
		}
		write("<td>");
		writeRadioButton("stage", null, "infancy", initial);
		write("</td><td>");
		writeEncode(getString("babyprofile:Stage.InfancyDetail"));
		write(" ");
		writeDateInput("delivery", mother.getBirthDate());
		write("</td></tr>");

		write("</table>");
		
		write("<br>");
		writeSaveButton(mother);
		
		writeFormClose();		
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
		
		// Redirect to parent
		progressGuidedSetup();
		throw new AfterCommitRedirectException();
	}
}
