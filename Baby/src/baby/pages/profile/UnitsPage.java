package baby.pages.profile;

import samoyan.apps.profile.ProfilePage;
import samoyan.servlet.exc.AfterCommitRedirectException;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public final class UnitsPage extends BabyPage
{
	public final static String COMMAND = ProfilePage.COMMAND + "/units";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Units.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		Mother mother = MotherStore.getInstance().loadByUserID( getContext().getUserID());
		
		writeFormOpen();
		
//		new SelectInputControl(this, "metric")
//			.addOption(getString("babyprofile:Units.Imperial"), "0")
//			.addOption(getString("babyprofile:Units.Metric"), "1")
//			.setInitialValue(mother.isMetric()?"1":"0")
//			.render();
//		write("<br><br>");
		
		write("<table><tr><td>");
		writeRadioButton("metric", getString("babyprofile:Units.Imperial"), "0", mother.isMetric()?"1":"0");
		write("</td></tr><tr><td>");
		writeRadioButton("metric", getString("babyprofile:Units.Metric"), "1", mother.isMetric()?"1":"0");
		write("</td></tr></table><br>");
		
		writeSaveButton(mother);
		
		writeFormClose();
	}
	
	@Override
	public void commit() throws Exception
	{
		Mother mother = MotherStore.getInstance().openByUserID(getContext().getUserID());
		mother.setMetric(getParameterString("metric").equalsIgnoreCase("1"));
		MotherStore.getInstance().save(mother);
		
		throw new AfterCommitRedirectException();
	}
}
