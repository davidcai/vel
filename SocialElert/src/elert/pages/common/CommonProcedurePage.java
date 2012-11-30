package elert.pages.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import samoyan.controls.ControlArray;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.UnauthorizedException;
import samoyan.servlet.exc.WebFormException;
import elert.database.FacilityStore;
import elert.database.Procedure;
import elert.database.ProcedureFacilityLinkStore;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import elert.database.Resource;
import elert.database.ResourceProcedureLinkStore;
import elert.database.ResourceStore;
import elert.pages.typeahead.ProcedureTypeTypeAhead;
import elert.pages.typeahead.ResourceTypeAhead;

public abstract class CommonProcedurePage extends WebPage
{
	private static final String PARAM_RESOURCE = "resource_";
	private static final String PARAM_RANK = "rank_";
	
	public static final String PARAM_ID = "id";	

	private Procedure procedure = null;
	private boolean customProcedure = false;
	
	/**
	 * To be overridden by implementation class to indicate if this is a scheduler that is editing a custom procedure.
	 * @return
	 */
	protected abstract boolean isCustomProcedure();
	
	/**
	 * To be overridden by implementation class to indicate where to redirect the client on success.
	 * @return
	 */
	protected abstract String getRedirectCommand();
	
	@Override
	public void init() throws Exception
	{
		// Flag to indicate if this is a scheduler that is editing a custom procedure
		this.customProcedure = this.isCustomProcedure(); // Call subclass

		this.procedure = ProcedureStore.getInstance().open(getParameterUUID(PARAM_ID));		
		if(this.procedure == null)
		{
			this.procedure = new Procedure();
			this.procedure.setCustom(this.customProcedure);
		}
		
		if (this.procedure.isCustom()!=this.customProcedure)
		{
			throw new UnauthorizedException();
		}
	}

	@Override
	public void validate() throws Exception
	{
		// Name and type
		String name = validateParameterString("Name", 1, Procedure.MAXSIZE_NAME);
		String procedureTypeName = validateParameterString("Type", 1, ProcedureType.MAXSIZE_NAME);
		ProcedureType procedureType = ProcedureTypeStore.getInstance().loadByName(procedureTypeName);
		
		if (procedureType!=null)
		{
			// Procedure names must be unique within the scope of a procedure type
			Procedure existingProcedure = ProcedureStore.getInstance().loadByName(name);
			if (existingProcedure!=null &&
				existingProcedure.getID().equals(this.procedure.getID())==false &&
				existingProcedure.getTypeID().equals(procedureType.getID())==true)
			{
				throw new WebFormException("Name", getString("elert:Procedure.DuplicateProcedureName"));
			}
		}
		
		if (this.customProcedure==true && procedureType==null)
		{
			// Schedulers must choose from an existing procedure type.
			// Only governors can create procedure types on the fly
			throw new WebFormException("Type", getString("common:Errors.InvalidValue"));
		}
		
		validateParameterInteger("Duration", 0, Procedure.MAX_DURATION);
		validateParameterInteger("Lead", 0, Procedure.MAX_LEAD);

		Integer postedCount = getParameterInteger("resources");
		for (int i=0; i<postedCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_RESOURCE + i);
			if (field==null || Util.isEmpty(field.getValue()) || Util.isEmpty(field.getKey())) continue;
			
			if (!Util.isUUID(field.getKey()))
			{
				throw new WebFormException(PARAM_RESOURCE + i, getString("common:Errors.InvalidValue"));
			}
			Resource res = ResourceStore.getInstance().load(UUID.fromString(field.getKey()));
			if (res==null)
			{
				throw new WebFormException(PARAM_RESOURCE + i, getString("common:Errors.InvalidValue"));
			}
		}
		
		validateParameterString("CommonName", 0, Procedure.MAXSIZE_COMMON_NAME);
		validateParameterString("ShortDescription", 0, Procedure.MAXSIZE_SHORT_DESCRIPTION);

		// !$! TODO: validate video link
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		
		boolean insertingNew = !procedure.isSaved();
		
		String procedureTypeName = getParameterString("Type");
		ProcedureType procedureType = ProcedureTypeStore.getInstance().loadByName(procedureTypeName);
		if(procedureType == null)
		{
			//procedure type doesn't exist in db, so save it
			procedureType = new ProcedureType();
			procedureType.setName(procedureTypeName);
			ProcedureTypeStore.getInstance().save(procedureType);
		}

		procedure.setName(getParameterString("Name"));
		procedure.setTypeID(procedureType.getID());
		procedure.setDuration(getParameterInteger("Duration"));
		procedure.setLead(getParameterInteger("Lead"));
		procedure.setNotes(getParameterRichEdit("Notes"));
		
		procedure.setCommonName(getParameterString("CommonName"));
		procedure.setShortDescription(getParameterString("ShortDescription"));
		procedure.setInstructions(getParameterRichEdit("Instructions"));
		procedure.setDefinition(getParameterRichEdit("Definition"));
		procedure.setVideo(getParameterString("Video"));
		
		procedure.setCustom(this.customProcedure);

		ProcedureStore.getInstance().save(procedure);
		
		// Get/create the posted resources
		List<UUID> postedResources = new ArrayList<UUID>();
		List<Integer> postedRanking = new ArrayList<Integer>();
		Integer postedCount = getParameterInteger("resources");
		for (int i=0; i<postedCount; i++)
		{
			Pair<String, String> field = getParameterTypeAhead(PARAM_RESOURCE + i);
			if (field==null || Util.isEmpty(field.getValue())) continue;
			
			String resourceName = field.getValue().trim();
			Resource resource = null;
			if (Util.isUUID(field.getKey()))
			{
				resource = ResourceStore.getInstance().load(UUID.fromString(field.getKey()));
			}
			else
			{
				resource = ResourceStore.getInstance().loadByName(resourceName);
			}
			if (resource==null)
			{
				// Create a new resource on the fly
				resource = new Resource();
				resource.setName(resourceName);
				ResourceStore.getInstance().save(resource);
			}

			postedResources.add(resource.getID());

			Integer rank = getParameterInteger(PARAM_RANK + i);
			postedRanking.add(rank);
		}
		
		// Unlink all resources
		ResourceProcedureLinkStore.getInstance().unlinkAllResources(this.procedure.getID());
		
		// Relink resources
		for (int i=0; i<postedResources.size(); i++)
		{
			ResourceProcedureLinkStore.getInstance().linkResource(this.procedure.getID(), postedResources.get(i), postedRanking.get(i));
		}
						
		// New custom procedures are linked to all of the scheduler's facilities by default
		if(this.customProcedure && insertingNew)
		{									
			List<UUID> myFacilities = FacilityStore.getInstance().queryByUser(ctx.getUserID());
			for(UUID facilityID : myFacilities)
			{
				ProcedureFacilityLinkStore.getInstance().unassignProcedureFromFacility(procedure.getID(), facilityID);
			}
		}
		
		// self redirect to clean the form after save
		throw new RedirectException(this.getRedirectCommand(), null); // Call subclass
	}

	@Override
	public void renderHTML() throws Exception
	{
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);

		// ---
		// DEFINITION

		twoCol.writeSubtitleRow(getString("elert:Procedure.Definition") );
				
		twoCol.writeRow(getString("elert:Procedure.Name"));
		twoCol.writeTextInput("Name", procedure.getName(), 80, Procedure.MAXSIZE_NAME);
		
		ProcedureType procedureType = ProcedureTypeStore.getInstance().load(procedure.getTypeID());
		String procedureTypeName = procedureType != null ? procedureType.getName() : null;
		twoCol.writeRow(getString("elert:Procedure.Type"));
		twoCol.writeTypeAheadInput("Type", procedureTypeName, procedureTypeName, 80, Procedure.MAXSIZE_TYPE,
				getPageURL(ProcedureTypeTypeAhead.COMMAND));

		twoCol.writeRow(getString("elert:Procedure.Duration"));
		twoCol.writeNumberInput("Duration", procedure.getDuration() , 4, 0, Procedure.MAX_DURATION);
		twoCol.write(" ");
		twoCol.write(getString("elert:Procedure.DurationTimeUnit"));

		twoCol.writeRow(getString("elert:Procedure.Lead"));
		twoCol.writeNumberInput("Lead", procedure.getLead(), 2, 0, Procedure.MAX_LEAD);
		twoCol.write(" ");
		twoCol.write(getString("elert:Procedure.LeadTimeUnit"));		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("elert:Procedure.Resources"));
		
		List<UUID> resourceIDs = ResourceProcedureLinkStore.getInstance().getResourcesForProcedure(procedure.getID());
		new ControlArray<UUID>(twoCol, "resources", resourceIDs)
		{
			@Override
			public void renderRow(int rowNum, UUID resourceID) throws Exception
			{
				UUID resID = null;
				String resName = null;
				int rank = 1;
				if (resourceID!=null)
				{
					Resource resource = ResourceStore.getInstance().load(resourceID);
					resID = resource.getID();
					resName = resource.getName();
					rank = ResourceProcedureLinkStore.getInstance().getResourceRank(procedure.getID(), resourceID);
				}
				
				writeTypeAheadInput(PARAM_RESOURCE + rowNum, resID, resName, 40, Resource.MAXSIZE_NAME,
						getPageURL(ResourceTypeAhead.COMMAND));
				write("&nbsp");
				
				SelectInputControl select = new SelectInputControl(this, PARAM_RANK + rowNum);
				
				int max = Math.max(10, rank);
				for (int i=1; i<=max; i++)
				{
					String descStr = getString("elert:Resource.Rank_" + i, i);
					if (descStr!=null)
					{
						select.addOption(descStr, i);
					}
					else if (rank==i)
					{
						select.addOption(String.valueOf(i), i);
					}
				}
				select.setInitialValue(rank);
				select.render();
			}
		}.render();
		
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("elert:Procedure.Notes"), getString("elert:Procedure.NotesHelp"));
		twoCol.writeRichEditField("Notes", procedure.getNotes(), 80, 5);
		
		twoCol.writeSpaceRow();

		// ---
		// PUBLIC INFO
		
		twoCol.writeSubtitleRow(getString("elert:Procedure.PublicInfo") );
		
		twoCol.writeRow(getString("elert:Procedure.CommonName"));
		twoCol.writeTextInput("CommonName", procedure.getCommonName(), 80, Procedure.MAXSIZE_COMMON_NAME);
		
		twoCol.writeRow(getString("elert:Procedure.ShortDescription"));
		twoCol.writeTextAreaInput("ShortDescription", procedure.getShortDescription(), 80, 3, Procedure.MAXSIZE_SHORT_DESCRIPTION);		
		
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("elert:Procedure.Definition"));
		twoCol.writeRichEditField("Definition", procedure.getDefinition(), 80, 5);
		
		twoCol.writeRow(getString("elert:Procedure.Video"), getString("elert:Procedure.VideoHelp"));
		twoCol.writeTextInput("Video", procedure.getVideo(), 80, 256);

		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("elert:Procedure.Instructions"));
		twoCol.writeRichEditField("Instructions", procedure.getInstructions(), 80, 5);
		

		twoCol.render();

		write("<br>");
		writeSaveButton(procedure);

		// Postback
		writeHiddenInput(PARAM_ID, null);

		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return procedure.isSaved() ? procedure.getName() : getString("elert:Procedure.Title");
	}
}
