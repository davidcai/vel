package samoyan.apps.admin.runtime;

import java.util.Date;
import samoyan.apps.admin.AdminPage;
import samoyan.controls.DataTableControl;
import samoyan.tasks.RecurringTaskStats;
import samoyan.tasks.TaskManager;

public class RecurringTaskListPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/task-list";
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:RecurringTasks.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		new DataTableControl<RecurringTaskStats>(this, "tasks", TaskManager.getStats())
		{
			@Override
			protected void defineColumns() throws Exception
			{
				column(getString("admin:RecurringTasks.Name"));
				column(getString("admin:RecurringTasks.RunningThreads")).align("right");
				column(getString("admin:RecurringTasks.MaxThreads")).align("right");
				column(getString("admin:RecurringTasks.LastExec")).align("right");
				column(getString("admin:RecurringTasks.LastDuration")).align("right");
			}

			@Override
			protected void renderRow(RecurringTaskStats stat) throws Exception
			{
				cell();
				writeEncode(stat.getName());
				
				cell();
				writeEncodeLong(stat.getCountRunning());
				
				cell();
				writeEncodeLong(stat.getMaxRunning());
				
				cell();
				if (stat.getLastRun()!=0)
				{
					writeEncodeDateTime(new Date(stat.getLastRun()));
				}
				
				cell();
				if (stat.getLastDuration()!=0)
				{
					writeDuration(stat.getLastDuration());
				}
			}
		}.render();		
	}
}
