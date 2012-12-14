package baby.pages.profile;

import java.util.List;
import java.util.UUID;

import samoyan.apps.profile.ProfilePage;
import samoyan.controls.ControlArray;
import samoyan.servlet.exc.GoBackRedirectException;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.pages.BabyPage;

public class BabiesPage extends BabyPage
{
	public static final String COMMAND = ProfilePage.COMMAND + "/babies";
	
	public static final int MAXSIZE_BABY = 8;
	public static final String PARAM_SAVE = "save";
	public static final String PARAM_BABIES = "babies";
	public static final String PARAM_NAME_PREFIX = "name_";
	public static final String PARAM_GENDER_PREFIX = "gender_";
	public static final String PARAM_ID_PREFIX = "id_";

	@Override
	public void validate() throws Exception
	{
		int count = getParameterInteger(PARAM_BABIES);
		for (int i = 0; i < count; i++)
		{
			validateParameterString(PARAM_NAME_PREFIX + i, 1, Baby.MAXSIZE_NAME);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		List<UUID> oldIDs = BabyStore.getInstance().getAtLeastOneBaby(getContext().getUserID());

		int count = getParameterInteger(PARAM_BABIES);
		for (int i = 0; i < count; i++)
		{
			Baby baby = null;
			if (isParameterNotEmpty(PARAM_ID_PREFIX + i))
			{
				baby = BabyStore.getInstance().open(getParameterUUID(PARAM_ID_PREFIX + i));
			}
			else
			{
				baby = new Baby();
			}

			baby.setUserID(getContext().getUserID());
			baby.setName(getParameterString(PARAM_NAME_PREFIX + i));
			baby.setGender(Baby.Gender.fromString(getParameterString(PARAM_GENDER_PREFIX + i)));
			
			BabyStore.getInstance().save(baby);
			
			oldIDs.remove(baby.getID());
		}
		
		BabyStore.getInstance().removeMany(oldIDs);
		
		// Redirect to parent
		throw new GoBackRedirectException();
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		// Help
		writeEncode(getString("babyprofile:Babies.TellUs"));
		write("<br><br>");
		
		// Baby list
		writeFormOpen();
		List<UUID> babyIDs = BabyStore.getInstance().getAtLeastOneBaby(getContext().getUserID());
		new ControlArray<UUID>(this, PARAM_BABIES, babyIDs)
		{
			@Override
			public void renderRow(int i, UUID babyID) throws Exception
			{
				Baby baby = BabyStore.getInstance().load(babyID);
				writeHiddenInput(PARAM_ID_PREFIX + i, baby != null ? baby.getID() : null);
				writeTextInput(PARAM_NAME_PREFIX + i, 
					baby != null ? baby.getName() : getString("babyprofile:Babies.Anonymous"), 30, Baby.MAXSIZE_NAME);
//				write("&nbsp;");
//				new SelectInputControl(this, PARAM_GENDER_PREFIX + i)
//					.addOption(getString("babyprofile:Babies.Undetermined"), Baby.Gender.UNDETERMINED)
//					.addOption(getString("babyprofile:Babies.Male"), Baby.Gender.MALE)
//					.addOption(getString("babyprofile:Babies.Female"), Baby.Gender.FEMALE)
//					.setInitialValue(baby != null ? baby.getGender() : Baby.Gender.UNDETERMINED)
//					.render();
			}
		}
		.render();
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Babies.Title");
	}
}
