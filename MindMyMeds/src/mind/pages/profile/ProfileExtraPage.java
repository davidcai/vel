package mind.pages.profile;

import mind.database.Patient;
import mind.database.PatientStore;
import samoyan.apps.profile.PersonalInfoPage;
import samoyan.controls.TwoColFormControl;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.WebFormException;

public class ProfileExtraPage extends PersonalInfoPage
{
	@Override
	protected void validateExtra() throws Exception
	{
		if (isParameter("mrn"))
		{
			String mrn = validateParameterString("mrn", 1, Patient.SIZE_MRN);
			while (mrn.startsWith("0"))
			{
				mrn = mrn.substring(1);
			}
			if (mrn.matches("[0-9]*")==false)
			{
				throw new WebFormException("mrn", getString("common:Errors.InvalidValue"));
			}
		}
	}

	@Override
	protected void commitExtra() throws Exception
	{
		Patient patient = PatientStore.getInstance().openByUserID(getContext().getUserID());
		
		if (isParameter("mrn"))
		{
			patient.setMRN(getParameterLong("mrn"));
		}
		PatientStore.getInstance().save(patient);
	}

	@Override
	protected void renderExtra(TwoColFormControl twoCol) throws Exception
	{
		Patient patient = PatientStore.getInstance().loadByUserID(getContext().getUserID());
		
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("mind:ProfileExtra.MRN"), getString("mind:ProfileExtra.MRNHelp", Setup.getAppOwner(getLocale())));
		twoCol.writeTextInput("mrn", patient.getMRN(), 20, Patient.SIZE_MRN);
	}
	
	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}
}
