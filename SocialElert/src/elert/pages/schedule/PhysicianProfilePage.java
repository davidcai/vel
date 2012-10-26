package elert.pages.schedule;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import samoyan.controls.PhoneInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.database.LogEntryStore;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.database.User;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import samoyan.syslog.NewUserLogEntry;
import elert.app.ElertConsts;
import elert.database.FacilityStore;
import elert.database.PhysicianFacilityLinkStore;
import elert.database.PhysicianProcedureTypeLinkStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import elert.database.UserEx;
import elert.database.UserExStore;
import elert.pages.ElertPage;

public class PhysicianProfilePage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/physician-profile";
	public static final String PARAM_ID = "id";
	public static final String PARAM_NAME = "name";

	private User physician;
	private UserEx physicianEx;

	@Override
	public void init() throws Exception
	{
		physician = UserStore.getInstance().open(getParameterUUID(PARAM_ID));
		if(physician == null)
			physician = new User();

		physicianEx = UserExStore.getInstance().openByUserID(getParameterUUID(PARAM_ID));
		if(physicianEx == null)
			physicianEx = new UserEx();
	}

	@Override
	public final void validate() throws Exception
	{
		//name
		validateParameterString(PARAM_NAME, User.MINSIZE_NAME, User.MAXSIZE_NAME);

		String nuid = validateParameterString("nuid", UserEx.SIZE_NUID, UserEx.SIZE_NUID);
		if(nuid.toUpperCase(Locale.US).matches("[A-Z][0-9]{6}") == false)
			throw new WebFormException("nuid", getString("common:Errors.InvalidValue"));

		UserEx userExByNuid = UserExStore.getInstance().loadByNUID(nuid);
		if (userExByNuid!=null && userExByNuid.getID().equals(this.physicianEx.getID())==false)
		{
			throw new WebFormException("nuid", getString("schedule:PhysicianProfile.DuplicateNUID", nuid));
		}
		
		//email
		String email = validateParameterString("email", 1, User.MAXSIZE_EMAIL);
		if(Util.isValidEmailAddress(email) == false)
			throw new WebFormException("email", getString("common:Errors.InvalidValue"));

		//mobile
		if (!Util.isEmpty(getParameterString("mobile")))
		{
			validateParameterPhone("mobile");
		}

		//phone
		if (!Util.isEmpty(getParameterString("phone")))
		{
			validateParameterPhone("phone");
		}
	}

	@Override
	public final void commit() throws Exception
	{
		RequestContext ctx = getContext();

		boolean isEdit = physician.isSaved();

		String name = getParameterString(PARAM_NAME);
		physician.setName(name);
		physician.setEmail(getParameterString("email"));
		physician.setMobile(getParameterPhone("mobile"));
		physician.setMobileVerified(true);
		physician.setPhone(getParameterPhone("phone"));
		physician.setPhoneVerified(true);
		Image avatar = getParameterImage("avatar");
		physician.setAvatar(avatar);

		if (isEdit==false)
		{
			physician.setLoginName(UserStore.getInstance().generateUniqueLoginName(name));
			physician.setPassword(Util.randomPassword(User.MINSIZE_PASSWORD));
		}

		UserStore.getInstance().save(physician);

		//add new user to the 'Physicians' group
		UserUserGroupLinkStore.getInstance().join(physician.getID(), UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS).getID());

		physicianEx.setUserID(physician.getID());
		physicianEx.setNUID(getParameterString("nuid").toUpperCase(Locale.US));
		UserExStore.getInstance().save(physicianEx);
		
		// Assign specialties
		if (isEdit)
		{
			PhysicianProcedureTypeLinkStore.getInstance().clearPhysicianSpecialties(physician.getID());
		}
		for (String p : ctx.getParameterNamesThatStartWith("proctype_"))
		{
			ProcedureType procType = ProcedureTypeStore.getInstance().load(UUID.fromString(p.substring("proctype_".length())));
			if (procType!=null)
			{
				PhysicianProcedureTypeLinkStore.getInstance().addPhysicianSpecialty(physician.getID(), procType.getID());
			}
		}
		
		//assign the physician to facilities (only on creation)
		if(!isEdit)
		{
			List<UUID> facilityIDs = FacilityStore.getInstance().queryByUser(ctx.getUserID()); //fetch all facilities associated with the current user
			for(UUID facilityID : facilityIDs)
				PhysicianFacilityLinkStore.getInstance().assignPhysicianToFacility(physician.getID(), facilityID);

			// Log the event
			LogEntryStore.log(new NewUserLogEntry(physician.getID()));
		}

		if (isEdit)
		{
			throw new RedirectException(ctx.getCommand(), new ParameterMap(PARAM_ID, physician.getID().toString()));
		}
		else
		{
			throw new RedirectException(PhysiciansPage.COMMAND, null);
		}
	}

	@Override
	public final String getTitle() throws Exception
	{
		return physician.isSaved() ? physician.getName() : getString("schedule:PhysicianProfile.Title");
	}

	@Override
	public final void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation();
		
		writeFormOpen();
		writeHiddenInput(PARAM_ID, null);
		
		if(isParameter(PARAM_NAME) && !physician.isSaved())
		{
			writeEncode(getString("schedule:PhysicianProfile.NewPhysicianHelp", getParameterString(PARAM_NAME), Setup.getAppTitle(getLocale())));
			write("<br>");
		}

		TwoColFormControl twoCol = new TwoColFormControl(this);

		//personal info
		twoCol.writeSubtitleRow(getString("schedule:PhysicianProfile.PersonalInfo"));

		//name
		twoCol.writeRow(getString("schedule:PhysicianProfile.Name"));
		twoCol.writeTextInput(PARAM_NAME, physician.getName(), 40, User.MAXSIZE_NAME);

		//NUID
		twoCol.writeRow(getString("schedule:PhysicianProfile.NUID"), getString("schedule:PhysicianProfile.NUIDHelp", Setup.getAppOwner(getLocale())));
		twoCol.writeTextInput("nuid", physicianEx.getNUID(), 8, UserEx.SIZE_NUID);

		twoCol.writeSpaceRow();

		//avatar photo
		twoCol.writeRow(getString("schedule:PhysicianProfile.Avatar"));
		twoCol.writeImageInput("avatar", physician.getAvatar());


		List<UUID> procTypeIDs = ProcedureTypeStore.getInstance().getAllIDs();
		if (procTypeIDs.size()>0)
		{
			twoCol.writeSpaceRow();
			for (int i=0; i<procTypeIDs.size(); i++)
			{
				ProcedureType procType = ProcedureTypeStore.getInstance().load(procTypeIDs.get(i));
				twoCol.writeRow(i==0? getString("schedule:PhysicianProfile.Specialties") : "");
				twoCol.writeCheckbox("proctype_" + procType.getID(), procType.getName(), PhysicianProcedureTypeLinkStore.getInstance().isPhysicianSpecialized(physician.getID(), procType.getID()));
			}	
		}

		//contact info
		twoCol.writeSubtitleRow(getString("schedule:PhysicianProfile.ContactInfo"));

		//email
		twoCol.writeRow(getString("schedule:PhysicianProfile.Email"));
		twoCol.writeTextInput("email", physician.getEmail(), 40, User.MAXSIZE_EMAIL);

		//mobile
		twoCol.writeRow(getString("schedule:PhysicianProfile.Mobile"));
		new PhoneInputControl(twoCol, "mobile")
			.limitCountries(fed.getSMSCountries())
			.setInitialValue(physician.getMobile())
			.render();
//		twoCol.writePhoneInput("mobile", physician.getMobile());

		//phone
		twoCol.writeRow(getString("schedule:PhysicianProfile.Phone"));
		new PhoneInputControl(twoCol, "phone")
				.limitCountries(fed.getVoiceCountries())
				.setInitialValue(physician.getPhone())
				.render();
//		twoCol.writePhoneInput("phone", physician.getPhone());

		twoCol.render();

		write("<br>");
		writeSaveButton(physician);

		writeFormClose();
	}
}
