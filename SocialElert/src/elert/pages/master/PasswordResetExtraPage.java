package elert.pages.master;

import java.util.List;
import java.util.UUID;

import elert.database.UserEx;
import elert.database.UserExStore;

import samoyan.controls.TwoColFormControl;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.WebFormException;

public final class PasswordResetExtraPage extends samoyan.apps.master.PasswordResetPage
{
	@Override
	protected void findExtra(List<UUID> searchResults) throws Exception
	{
		if (isParameterNotEmpty("mrn"))
		{
			UserEx userEx = UserExStore.getInstance().loadByMRN(getParameterString("mrn"));
			if (userEx!=null)
			{
				searchResults.add(userEx.getUserID());
			}
		}
	}

	@Override
	protected void renderExtra(TwoColFormControl twoCol) throws Exception
	{
		twoCol.writeRow(getString("elert:PasswordResetExtra.MRN"), getString("elert:PasswordResetExtra.MRNHelp", Setup.getAppOwner(getLocale())));
		twoCol.writeTextInput("mrn", null, 40, UserEx.MAXSIZE_MRN);
	}

	@Override
	protected void validateExtra() throws Exception
	{
		if (isParameterNotEmpty("mrn"))
		{
			if (getParameterString("mrn").matches("[1-9][0-9]*")==false)
			{
				throw new WebFormException("mrn", getString("common:Errors.InvalidValue"));
			}
		}
	}
}
