package elert.pages.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserGroup;
import samoyan.database.UserGroupStore;
import samoyan.database.UserStore;
import samoyan.database.UserUserGroupLinkStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import elert.app.ElertConsts;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.PhysicianFacilityLinkStore;
import elert.database.PhysicianOpeningLinkStore;
import elert.database.Procedure;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureOpeningLinkStore;
import elert.database.ProcedureStore;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;
import elert.pages.typeahead.HomeFacilityTypeAhead;
import elert.pages.typeahead.HomePhysicianTypeAhead;
import elert.pages.typeahead.HomeProcedureTypeAhead;

public class LogNewOpeningPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/new-opening";

	private static final String PARAM_PROCEDURE = "procedure_";
	private static final String PARAM_PHYSICIAN = "physician_";

	@Override
	public final void validate() throws Exception
	{
		Pair<String, String> facilityField = getParameterTypeAhead("facility");
		if(Util.isEmpty(facilityField.getValue()))
			throw new WebFormException("facility", getString("common:Errors.MissingField"));
			
		if(Util.isEmpty(facilityField.getKey()))
			throw new WebFormException("facility", getString("common:Errors.InvalidValue"));
		
		UUID facilityID = UUID.fromString(facilityField.getKey());
		List<UUID> myFacilities = FacilityStore.getInstance().queryByUser(getContext().getUserID()); //facilities belonging to this user service areas
		if(!myFacilities.contains(facilityID))
			throw new WebFormException("facility", getString("common:Errors.InvalidValue"));
		Facility facility = FacilityStore.getInstance().load(facilityID);
		
		validateParameterString("or", 0, Opening.MAXSIZE_ROOM);
		
//		validateParameterInteger("duration", 0, Procedure.MAX_DURATION);

		int minDuration = 0;
		int minLead = 0;
//		boolean foundProcedure = false;
		Integer postedCount = getParameterInteger("procedures");
		for(int i = 0; i < postedCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_PROCEDURE + i);
			if(field == null || Util.isEmpty(field.getValue()))
				continue;

			if(!Util.isUUID(field.getKey()))
				throw new WebFormException(PARAM_PROCEDURE + i, getString("common:Errors.InvalidValue"));

			Procedure procedure = ProcedureStore.getInstance().load(UUID.fromString(field.getKey()));
			if(procedure == null)
				throw new WebFormException(PARAM_PROCEDURE + i, getString("common:Errors.InvalidValue"));

			// Verify procedure is performed in the facility
			if(!ProcedureFacilityLinkStore.getInstance().isProcedureAssignedToFacility(procedure.getID(), facilityID))
				throw new WebFormException(PARAM_PROCEDURE + i, getString("schedule:LogNewOpening.UnassignedProcedure", procedure.getName(), facility.getName()));
			
//			foundProcedure = true;
			minDuration = Math.max(minDuration, procedure.getDuration());
			minLead = Math.max(minLead, procedure.getLead());
		}
//		if (foundProcedure==false)
//		{
//			List<String> fieldNames = new ArrayList<String>();
//			for(int i = 0; i <= postedCount; i++)
//			{
//				fieldNames.add(PARAM_PROCEDURE + i);
//			}
//			throw new WebFormException(fieldNames, getString("common:Errors.MissingField"));
//		}
	
		// Duration of opening must be at least as long as the longest procedure
		validateParameterInteger("duration", minDuration, Opening.MAX_DURATION);

		// Appointment date must be enough days in the future to account for lead time
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.add(Calendar.DATE, minLead);
		Date minDate = cal.getTime();
		cal.add(Calendar.DATE, -minLead);
		cal.add(Calendar.YEAR, 1);
		Date maxDate = cal.getTime();
		validateParameterDate("datetime", minDate, maxDate);
		
		postedCount = getParameterInteger("physicians");
		for(int i = 0; i < postedCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_PHYSICIAN + i);
			if(field == null || Util.isEmpty(field.getValue()))
				continue;

			if(!Util.isUUID(field.getKey()))
				throw new WebFormException(PARAM_PHYSICIAN + i, getString("common:Errors.InvalidValue"));

			User physician = UserStore.getInstance().load(UUID.fromString(field.getKey()));
			if(physician == null)
				throw new WebFormException(PARAM_PHYSICIAN + i, getString("common:Errors.InvalidValue"));
			
			//double check if user is a physician
			UserGroup group = UserGroupStore.getInstance().loadByName(ElertConsts.GROUP_PHYSICIANS);
			if (!UserUserGroupLinkStore.getInstance().isUserInGroup(physician.getID(), group.getID()))
				throw new WebFormException(PARAM_PHYSICIAN + i, getString("common:Errors.InvalidValue"));
			
			// Verify physician is assigned to the facility
			if(!PhysicianFacilityLinkStore.getInstance().isPhysicianAssignedToFacility(physician.getID(), facilityID))
				throw new WebFormException(PARAM_PHYSICIAN + i, getString("schedule:LogNewOpening.UnassignedPhysician", physician.getDisplayName(), facility.getName()));			
		}
		
		// !$! If physicians are defined, each procedure must be covered by a specialized physician
		// TBI...
	}

	@Override
	public final void commit() throws Exception
	{
		RequestContext ctx = getContext();
		
		Opening opening = new Opening();
		
		// Geography
		Pair<String, String> facilityKvp = getParameterTypeAhead("facility");
		UUID facilityID = UUID.fromString(facilityKvp.getKey());
		Facility facility = FacilityStore.getInstance().load(facilityID);
		ServiceArea area = ServiceAreaStore.getInstance().load(facility.getServiceAreaID());
		opening.setFacilityID(facility.getID());
		opening.setServiceAreaID(area.getID());
		opening.setRegionID(area.getRegionID());
		opening.setRoom(getParameterString("or"));
		
		Calendar dateTime = Calendar.getInstance(getTimeZone());
		dateTime.setTime(getParameterDate("datetime"));
		opening.setDateTime(dateTime.getTime());
		opening.setDuration(getParameterInteger("duration"));
		opening.setOriginalDuration(getParameterInteger("duration"));

		opening.setSchedulerID(ctx.getUserID());
		
		OpeningStore.getInstance().save(opening);

		//link procedures
		Integer postedCount = getParameterInteger("procedures");
		for(int i = 0; i < postedCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_PROCEDURE + i);
			if (field==null || !Util.isUUID(field.getKey()))
			{
				continue;
			}

			ProcedureOpeningLinkStore.getInstance().linkProcedure(opening.getID(), UUID.fromString(field.getKey()));
		}

		//link physicians
		postedCount = getParameterInteger("physicians");
		for(int i = 0; i < postedCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_PHYSICIAN + i);
			if (field==null || !Util.isUUID(field.getKey()))
			{
				continue;
			}

			PhysicianOpeningLinkStore.getInstance().linkPhysician(opening.getID(), UUID.fromString(field.getKey()));
		}
		
		// Redirect to candidate selection page
		throw new RedirectException(OpeningPage.COMMAND, new ParameterMap(OpeningPage.PARAM_ID, opening.getID().toString()));
	}

	@Override
	public final String getTitle() throws Exception
	{
		return getString("schedule:LogNewOpening.Title");
	}

	@Override
	public final void renderHTML() throws Exception
	{
		writeFormOpen();

		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeTextRow(getString("schedule:LogNewOpening.HelpTimeAndPlace"));
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("schedule:LogNewOpening.Facility"));
		twoCol.writeTypeAheadInput("facility", null, null, 40, Facility.MAXSIZE_NAME, getPageURL(HomeFacilityTypeAhead.COMMAND));
		
		twoCol.writeRow(getString("schedule:LogNewOpening.OR"));
		twoCol.writeTextInput("or", null, 10, Opening.MAXSIZE_ROOM);

		twoCol.writeSpaceRow();
		
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.add(Calendar.HOUR_OF_DAY, 1);
		twoCol.writeRow(getString("schedule:LogNewOpening.DateTime"));
		twoCol.writeDateTimeInput("datetime", cal.getTime());

		twoCol.writeRow(getString("schedule:LogNewOpening.Duration"));
		twoCol.writeNumberInput("duration", 0, 3, 0, Opening.MAX_DURATION);
		twoCol.write(" ");
		twoCol.write(getString("schedule:LogNewOpening.DurationTimeUnit"));

		twoCol.writeSpaceRow();

		twoCol.writeTextRow(getString("schedule:LogNewOpening.HelpProcedure", Setup.getAppTitle(getLocale())));
		twoCol.writeSpaceRow();

		// Procedures
		twoCol.writeRow(getString("schedule:LogNewOpening.Procedure"));
		List<UUID> emptyUUIDs = new ArrayList<UUID>();
		new ControlArray<UUID>(twoCol, "procedures", emptyUUIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID procedureID) throws Exception
			{
				writeTypeAheadInput(PARAM_PROCEDURE + rowNum, null, null, 40, Procedure.MAXSIZE_NAME, getPageURL(HomeProcedureTypeAhead.COMMAND));				
			}
		}.render();

		twoCol.writeSpaceRow();

		// Physicians
		twoCol.writeRow(getString("schedule:LogNewOpening.Physician"));
		new ControlArray<UUID>(twoCol, "physicians", emptyUUIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID physicianID) throws Exception
			{
				writeTypeAheadInput(PARAM_PHYSICIAN + rowNum, null, null, 40, User.MAXSIZE_NAME, getPageURL(HomePhysicianTypeAhead.COMMAND));
			}
		}.render();

		twoCol.render();

		write("<br>");
		writeButton("save", getString("schedule:LogNewOpening.SaveBtn"));

		writeFormClose();
	}
}
