package baby.pages.profile;

import java.util.List;
import java.util.UUID;

import samoyan.apps.profile.ProfilePage;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TextInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.servlet.exc.RedirectException;
import baby.database.Baby;
import baby.database.BabyStore;
import baby.pages.BabyPage;

public class BabiesPage extends BabyPage
{
	public static final String COMMAND = ProfilePage.COMMAND + "/babies";
	
	public static final String PARAM_SAVE = "save";
	public static final String PARAM_NUMBER = "number";
	public static final String PARAM_NAME_PREFIX = "name_";
	public static final String PARAM_GENDER_PREFIX = "gender_";
	public static final String PARAM_ID_PREFIX = "id_";

	@Override
	public void validate() throws Exception
	{
		int count = getParameterInteger(PARAM_NUMBER);
		for (int i = 0; i < count; i++)
		{
			validateParameterString(PARAM_NAME_PREFIX + i, 1, Baby.MAXSIZE_NAME);
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		List<UUID> oldIDs = BabyStore.getInstance().getByUser(getContext().getUserID());

		int count = getParameterInteger(PARAM_NUMBER);
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
			baby.setMale(Boolean.parseBoolean(getParameterString(PARAM_GENDER_PREFIX + i)));
			
			BabyStore.getInstance().save(baby);
			
			oldIDs.remove(baby.getID());
		}
		
		BabyStore.getInstance().removeMany(oldIDs);
		
		throw new RedirectException(COMMAND, null);
	}
	
	@Override
	public void render() throws Exception
	{
		List<UUID> babyIDs = BabyStore.getInstance().getByUser(getContext().getUserID());
		int count;
		try
		{
			count = getParameterInteger(PARAM_NUMBER);
		}
		catch (Exception e)
		{
			count = babyIDs.size();
		}
		if (count == 0)
		{
			count = 1;
		}
		
		writeFormOpen();
		
		write("<div id=\"BabyNumber\">");
		writeEncode(getString("babyprofile:Babies.TellUs"));
		TwoColFormControl twoCol = new TwoColFormControl(this);
		twoCol.writeSpaceRow();
		twoCol.writeRow(getString("babyprofile:Babies.Number"));
		SelectInputControl sel = new SelectInputControl(twoCol, PARAM_NUMBER);
		for (int i = 1; i <= 8; i++)
		{
			sel.addOption(getString("babyprofile:Babies.Number." + i), i);
		}
		sel.setInitialValue(count);
		sel.render();
		twoCol.writeSpaceRow();
		twoCol.render();
		write("</div>");
		
		write("<ul id=\"BabyList\" class=\"PlainList\">");
		for (int i = 0; i < count; i++)
		{
			Baby baby = null;
			if (i < babyIDs.size())
			{
				baby = BabyStore.getInstance().load(babyIDs.get(i));
			}
			else
			{
				baby = new Baby();
			}
			
			write("<li>");
			new TextInputControl(this, PARAM_NAME_PREFIX + i)
//				.setPlaceholder(getString("babyprofile:Babies.DefaultName") + (i + 1))
				.setPlaceholder(getString("babyprofile:Babies.DefaultName"))
				.setSize(32)
				.setMaxLength(Baby.MAXSIZE_NAME)
				.setInitialValue(baby.getName() == null ?  null : baby.getName())
				.render();
//			writeTextInput(PARAM_NAME_PREFIX + i, 
//				baby.getName() == null ?  getString("babyprofile:Babies.DefaultName") + (i + 1) : baby.getName(), 
//				32, Baby.MAXSIZE_NAME);
			write("&nbsp;");
			new SelectInputControl(this, PARAM_GENDER_PREFIX + i)
				.addOption(getString("babyprofile:Babies.Male"), true)
				.addOption(getString("babyprofile:Babies.Female"), false)
				.setInitialValue(baby.isMale())
				.render();
			writeHiddenInput(PARAM_ID_PREFIX + i, baby.isSaved() ? baby.getID().toString() : "");
			write("</li>");
		}
		write("</ul>");
		
		write("<br>");
		writeSaveButton(PARAM_SAVE, null);
		
		writeFormClose();
		
		writeIncludeJS("baby/baby.js");
		write("<script type=\"text/javascript\">");
		write("initBabyNumberChangeListener(\"" + getString("babyprofile:Babies.DefaultName") + "\");");
		write("</script>");
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("babyprofile:Babies.Title");
	}
}
