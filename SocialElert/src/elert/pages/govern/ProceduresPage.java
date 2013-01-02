package elert.pages.govern;

import java.util.List;
import java.util.UUID;

import samoyan.controls.CheckboxInputControl;
import samoyan.controls.DataTableControl;
import samoyan.controls.LinkToolbarControl;
import samoyan.core.ParameterMap;
import samoyan.servlet.RequestContext;
import samoyan.servlet.Setup;
import samoyan.servlet.exc.RedirectException;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.ProcedureType;
import elert.database.ProcedureTypeStore;
import elert.database.Resource;
import elert.database.ResourceProcedureLinkStore;
import elert.database.ResourceStore;
import elert.pages.ElertPage;
import elert.pages.common.CommonProcedurePage;

public class ProceduresPage extends ElertPage
{
	public static final String COMMAND = ElertPage.COMMAND_GOVERN + "/procedures";

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		if (isParameter("remove"))
		{
			for (String prmName : getContext().getParameterNamesThatStartWith("chk_"))
			{
				ProcedureStore.getInstance().remove(UUID.fromString(prmName.substring(4)));				
			}
		}
		
		// self redirect to clean the form after save		
		throw new RedirectException(ctx.getCommand(), null);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		writeEncode(getString("govern:Procedures.Help", Setup.getAppTitle(getLocale())));
		write("<br><br>");

		//create new procedure link
		new LinkToolbarControl(this)
			.addLink(getString("govern:Procedures.CreateNew"), getPageURL(ProcedurePage.COMMAND), "icons/standard/pencil-16.png")
			.render();
		
		boolean hasProcs = false;
		
		// Get procedure types
		List<UUID> procTypeIDs = ProcedureTypeStore.getInstance().getAllIDs();
		for (UUID procTypeID : procTypeIDs)
		{			
			List<UUID> all = ProcedureStore.getInstance().queryStandardByType(procTypeID);
			if (all.size()==0)
			{
				continue;
			}
			hasProcs = true;
			
			ProcedureType procType = ProcedureTypeStore.getInstance().load(procTypeID);
			write("<h2>");
			writeEncode(procType.getName());
			write("</h2>");
			
			writeFormOpen();
	
			new DataTableControl<UUID>(this, "procs", all)
			{
				@Override
				protected void defineColumns() throws Exception
				{
					column("").width(1);
					column(getString("govern:Procedures.Name"));
//					column(getString("govern:Procedures.Type"));
					column(getString("govern:Procedures.Duration"));
					column(getString("govern:Procedures.Rank"));	
					column(getString("govern:Procedures.Resources"));	
				}
	
				@Override
				protected void renderRow(UUID procedureID) throws Exception
				{
					Procedure procedure = ProcedureStore.getInstance().load(procedureID);			
					
					cell();
					new CheckboxInputControl(this, "chk_" + procedure.getID().toString())
						.setDisabled(ProcedureStore.getInstance().canRemove(procedureID)==false)
						.render();
//					if (ProcedureStore.getInstance().canRemoveBean(procedureID))
//					{
//						writeCheckbox("chk_" + procedure.getID().toString() , false);
//					}
//					else
//					{
//						write("<input type=checkbox disabled>");
//					}
					
					cell();
					writeLink(procedure.getName(), getPageURL(ProcedurePage.COMMAND, new ParameterMap(CommonProcedurePage.PARAM_ID, procedure.getID().toString())));
					
//					cell();
//					ProcedureType type = ProcedureTypeStore.getInstance().load(procedure.getTypeID());
//					writeEncode(type.getName());
					
					cell();
					writeEncodeLong(procedure.getDuration());
					write(" ");
					writeEncode(getString("govern:Procedures.Minutes"));
					
					cell();
					writeEncodeLong(ResourceProcedureLinkStore.getInstance().getTotalRankForProcedure(procedure.getID()));
	
					cell();
					List<UUID> resourceIDs = ResourceProcedureLinkStore.getInstance().getResourcesForProcedure(procedure.getID());
					for (int r=0; r<resourceIDs.size(); r++)
					{
						if (r>0)
						{
							write(", ");
						}
						Resource res = ResourceStore.getInstance().load(resourceIDs.get(r));
						writeEncode(res.getName());
					}				
				}
				
			}.render();
			
			write("<br>");
			writeRemoveButton("remove");
			writeFormClose();
			write("<br>");
		}
		
		if (hasProcs==false)
		{
			writeEncode(getString("govern:Procedures.NoResults"));
		}
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("govern:Procedures.Title");
	}
}
