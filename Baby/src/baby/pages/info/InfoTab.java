package baby.pages.info;

import samoyan.controls.NavTreeControl;
import samoyan.servlet.EnvelopeTab;
import samoyan.servlet.WebPage;
import baby.database.MotherStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class InfoTab extends EnvelopeTab
{
	@Override
	public NavTreeControl getNavTree(WebPage outputPage) throws Exception
	{
		NavTreeControl navCtrl = new NavTreeControl(outputPage);

		navCtrl.addHeader(outputPage.getString("information:Nav.ToDo"));
		navCtrl.addPage(ChecklistPage.COMMAND, null);
		navCtrl.addPage(AppointmentsListPage.COMMAND, null);
		navCtrl.addPage(CalendarPage.COMMAND, null);
		
		navCtrl.addHeader(outputPage.getString("information:Nav.Articles"));
		navCtrl.addPage(ViewArticleListPage.COMMAND, null);
		navCtrl.addPage(ViewResourceListPage.COMMAND, null);
		// !$! Temp code, should not be here. Appointments should be linked with Calendar
		navCtrl.addPage(SearchPage.COMMAND, null);
		
		return navCtrl;
	}
	
	@Override
	public String getCommand()
	{
		return BabyPage.COMMAND_INFORMATION;
	}

	@Override
	public String getLabel(WebPage outputPage) throws Exception
	{
		Stage stage = MotherStore.getInstance().loadByUserID(outputPage.getContext().getUserID()).getPregnancyStage();
		if (stage.isInfancy())
		{
			return outputPage.getString("information:Nav.TabTitleInfancy");
		}
		else if (stage.isPreconception())
		{
			return outputPage.getString("information:Nav.TabTitlePreconception");
		}
		else
		{
			return outputPage.getString("information:Nav.TabTitlePregnancy");
		}
	}

	@Override
	public String getIcon(WebPage outputPage) throws Exception
	{
		Stage stage = MotherStore.getInstance().loadByUserID(outputPage.getContext().getUserID()).getPregnancyStage();
		if (stage.isInfancy())
		{
			return "baby/tab-infancy.png";
		}
		else if (stage.isPreconception())
		{
			return "baby/tab-preconception.png";
		}
		else
		{
			return "baby/tab-pregnancy.png";
		}
	}
}
