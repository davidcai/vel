package elert.pages.profile;

import elert.database.UserEx;
import elert.database.UserExStore;
import samoyan.apps.profile.PersonalInfoPage;
import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.WebFormException;

public class PersonalExtraPage extends PersonalInfoPage
{
	@Override
	protected void validateExtra() throws Exception
	{
		String mrn = validateParameterString("mrn", 1, UserEx.MAXSIZE_MRN);
		if (mrn.matches("[1-9][0-9]*")==false)
		{
			throw new WebFormException("mrn", getString("common:Errors.InvalidValue"));
		}
		
		// Prevent duplicate MRNs
		UserEx userEx = UserExStore.getInstance().loadByMRN(mrn);
		if (userEx!=null && userEx.getUserID().equals(getContext().getUserID())==false)
		{
			throw new WebFormException("mrn", getString("elert:ProfileExtra.DuplicateMRN"));
		}
	}

	@Override
	protected void commitExtra() throws Exception
	{
		UserEx userEx = UserExStore.getInstance().openByUserID(getContext().getUserID());		
		userEx.setMRN(getParameterString("mrn"));
		UserExStore.getInstance().save(userEx);
	}

	@Override
	protected void renderExtra(TwoColFormControl twoCol) throws Exception
	{
		UserEx userEx = UserExStore.getInstance().loadByUserID(getContext().getUserID());
		
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("elert:ProfileExtra.MRN") + getString("elert:ProfileExtra.Required"), getString("elert:ProfileExtra.MRNHelp", Setup.getAppOwner(getLocale())));
		
//		twoCol.writeTextInput("mrn", userEx.getMRN(), 20, UserEx.SIZE_MRN);
		
		new TextInputControl(twoCol, "mrn")
			.setSize(20)
			.setMaxLength(UserEx.MAXSIZE_MRN)
//			.setRegExp("[0-9]{" + UserEx.SIZE_MRN + "}")
//			.setRequired(true)
			.setInitialValue(userEx.getMRN())
			.render();
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	protected int getFieldRequired(String name)
	{
		// Make birthday and gender mandatory
		if (name.equals(PARAM_BIRTHDAY) || name.equals(PARAM_GENDER))
		{
			return 1;
		}
		else
		{
			return super.getFieldRequired(name);
		}
	}
	
	@Override
	protected String getFieldLabel(String name)
	{
		String label = super.getFieldLabel(name);
		if (getFieldRequired(name)==1)
		{
			label += getString("elert:ProfileExtra.Required");
		}
		return label;
	}
	
	@Override
	protected void renderExtraFooter() throws Exception
	{
		write("<br><br>");
		writeEncode(getString("elert:ProfileExtra.RequiredHelp"));
	}
}
