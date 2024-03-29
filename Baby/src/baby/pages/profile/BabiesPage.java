package baby.pages.profile;

import java.util.List;
import java.util.UUID;

import samoyan.apps.profile.ProfilePage;
import samoyan.controls.ControlArray;
import samoyan.controls.TextInputControl;
import samoyan.servlet.exc.AfterCommitRedirectException;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.pages.BabyPage;

public class BabiesPage extends BabyPage
{
	public static final String COMMAND = ProfilePage.COMMAND + "/babies";
	
	private static final String PARAM_BABIES = "babies";
	private static final String PARAM_NAME_PREFIX = "name_";
	private static final String PARAM_ID_PREFIX = "id_";

	@Override
	public void validate() throws Exception
	{
		int count = getParameterInteger(PARAM_BABIES);
		for (int i = 0; i < count; i++)
		{
			if (isParameter(PARAM_ID_PREFIX + i))
			{
				validateParameterString(PARAM_NAME_PREFIX + i, 0, Baby.MAXSIZE_NAME);
			}
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
			
			if (isParameter(PARAM_ID_PREFIX + i))
			{
				UUID id = getParameterUUID(PARAM_ID_PREFIX + i);
				if (id != null)
				{
					baby = BabyStore.getInstance().open(getParameterUUID(PARAM_ID_PREFIX + i));	
				}
				else
				{
					baby = new Baby();
				}
				
				baby.setUserID(getContext().getUserID());
				baby.setName(getParameterString(PARAM_NAME_PREFIX + i));
// !$! No support for Gender for now
//				baby.setGender(null);
				BabyStore.getInstance().save(baby);
				
				oldIDs.remove(baby.getID());
			}
		}
		
		BabyStore.getInstance().removeMany(oldIDs);
		
		// Redirect to parent
		throw new AfterCommitRedirectException();
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
				if (babyID != null)
				{
					Baby baby = BabyStore.getInstance().load(babyID);
					writeHiddenInput(PARAM_ID_PREFIX + i, baby != null ? baby.getID() : null);
					new TextInputControl(this, PARAM_NAME_PREFIX + i)
						.setSize(30)
						.setMaxLength(Baby.MAXSIZE_NAME)
						.setPlaceholder(getString("babyprofile:Babies.Anonymous") + (i + 1))
						.setInitialValue(baby == null ? null : baby.getName())
						.render();
				}
				else
				{
					writeHiddenInput(PARAM_ID_PREFIX + i, "");
					new TextInputControl(this, PARAM_NAME_PREFIX + i)
						.setSize(30)
						.setMaxLength(Baby.MAXSIZE_NAME)
						.setPlaceholder(getString("babyprofile:Babies.Anonymous"))
						.render();
				}
			}
		}
		.render();
		
		write("<br>");
		writeSaveButton(null);
		
		writeFormClose();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Babies.Title");
	}
}
