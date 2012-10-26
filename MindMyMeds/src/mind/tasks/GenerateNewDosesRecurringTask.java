package mind.tasks;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import mind.database.*;
import mind.pages.patient.reminders.DoseReminderNotif;
import samoyan.core.ParameterMap;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.tasks.RecurringTask;

public class GenerateNewDosesRecurringTask implements RecurringTask
{
	public void work() throws Exception
	{
		Date now = new Date();
		
		// Get prescriptions that have doses remaining, and whose next dose date is in the past
		List<UUID> rxIDs = PrescriptionStore.getInstance().getByDoseDue(now);
		for (UUID rxID : rxIDs)
		{
			Prescription rx = PrescriptionStore.getInstance().open(rxID);
			Patient patient = PatientStore.getInstance().load(rx.getPatientID());
			User user = UserStore.getInstance().load(patient.getLoginID());
			
			while (!rx.getNextDoseDate().after(now) && rx.getDosesRemaining()>0)
			{
				// Create the dose
				Dose dose = new Dose();
				dose.setPatientID(patient.getID());
				dose.setPrescriptionID(rxID);
				dose.setResolution(Dose.UNRESOLVED);
				dose.setTakeDate(rx.getNextDoseDate());
				DoseStore.getInstance().save(dose);
				
				rx.setDosesRemaining(rx.getDosesRemaining()-1);
				rx.progressNextDoseDate(user.getTimeZone());
				PrescriptionStore.getInstance().save(rx);
				
				// Send out the reminders
				Notifier.send(	patient.getLoginID(),
								dose.getID(),
								DoseReminderNotif.COMMAND,
								new ParameterMap(DoseReminderNotif.PARAM_DOSE_ID, dose.getID().toString()));
			}
		}
	}
	
	@Override
	public long getInterval()
	{
		return 60000L; // 1 min
	}
}
