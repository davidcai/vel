package elert.pages.common;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Region;
import elert.database.ServiceArea;
import elert.database.ServiceAreaStore;
import elert.pages.ElertPage;
import elert.pages.typeahead.FacilityTypeAhead;
import elert.pages.typeahead.PhysicianTypeAhead;
import elert.pages.typeahead.ProcedureTypeAhead;
import elert.pages.typeahead.RegionTypeAhead;
import elert.pages.typeahead.SchedulerTypeAhead;
import elert.pages.typeahead.ServiceAreaTypeAhead;
import samoyan.controls.TwoColFormControl;
import samoyan.core.Pair;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.WebFormException;

public abstract class CommonTimeReportPage extends ElertPage
{
	private UUID getUUID(String paramName)
	{
		Pair<String, String> kvp = getParameterTypeAhead(paramName);
		return kvp==null || !Util.isUUID(kvp.getKey())? null : UUID.fromString(kvp.getKey());
	}

	@Override
	public void validate() throws Exception
	{
		validateParameterDate("datefrom");
		validateParameterDate("dateto");
		
		UUID regionID = getUUID("region");
		UUID serviceAreaID = getUUID("servicearea");
		UUID facilityID = getUUID("facility");
		if (serviceAreaID!=null && regionID!=null)
		{
			ServiceArea serviceArea = ServiceAreaStore.getInstance().load(serviceAreaID);
			if (serviceArea.getRegionID().equals(regionID)==false)
			{
				throw new WebFormException(new String[] {"region", "servicearea"}, getString("elert:TimeReport.ServiceAreaNotInRegion"));
			}
		}
		if (facilityID!=null && serviceAreaID!=null)
		{
			Facility facility = FacilityStore.getInstance().load(facilityID);
			if (facility.getServiceAreaID().equals(serviceAreaID)==false)
			{
				throw new WebFormException(new String[] {"servicearea", "facility"}, getString("elert:TimeReport.FacilityNotInServiceArea"));
			}
		}
		if (facilityID!=null && regionID!=null)
		{
			Facility facility = FacilityStore.getInstance().load(facilityID);
			ServiceArea serviceArea = ServiceAreaStore.getInstance().load(facility.getServiceAreaID());
			if (serviceArea.getRegionID().equals(regionID)==false)
			{
				throw new WebFormException(new String[] {"region", "facility"}, getString("elert:TimeReport.FacilityNotInRegion"));
			}
		}
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		boolean governor = ctx.getCommand(1).equalsIgnoreCase(ElertPage.COMMAND_GOVERN);
		
		String help = this.getHelpString();
		if (!Util.isEmpty(help))
		{
			writeEncode(help);
			write("<br><br>");
		}
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
				
		twoCol.writeRow(getString("elert:TimeReport.DateRange"));
		twoCol.writeDateTimeInput("datefrom", getDefaultFromDate());
		twoCol.writeEncode(" ");
		twoCol.writeEncode(getString("elert:TimeReport.Through"));
		twoCol.writeEncode(" ");
		twoCol.writeDateTimeInput("dateto", getDefaultToDate());
		
		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("elert:TimeReport.Region"));
		twoCol.writeTypeAheadInput("region", null, null, 40, Region.MAXSIZE_NAME, getPageURL(RegionTypeAhead.COMMAND));
		twoCol.writeRow(getString("elert:TimeReport.ServiceArea"));
		twoCol.writeTypeAheadInput("servicearea", null, null, 40, ServiceArea.MAXSIZE_NAME, getPageURL(ServiceAreaTypeAhead.COMMAND));
		twoCol.writeRow(getString("elert:TimeReport.Facility"));
		twoCol.writeTypeAheadInput("facility", null, null, 40, Facility.MAXSIZE_NAME, getPageURL(FacilityTypeAhead.COMMAND));
		
		twoCol.writeSpaceRow();

		twoCol.writeRow(getString("elert:TimeReport.Scheduler"));
		if (governor)
		{
			twoCol.writeTypeAheadInput("scheduler", null, null, 40, User.MAXSIZE_NAME, getPageURL(SchedulerTypeAhead.COMMAND));
		}
		else
		{
			User scheduler = UserStore.getInstance().load(ctx.getUserID());
			twoCol.writeEncode(scheduler.getDisplayName());
		}
		twoCol.writeRow(getString("elert:TimeReport.Physician"));
		twoCol.writeTypeAheadInput("physician", null, null, 40, User.MAXSIZE_NAME, getPageURL(PhysicianTypeAhead.COMMAND));
		twoCol.writeRow(getString("elert:TimeReport.Procedure"));
		twoCol.writeTypeAheadInput("procedure", null, null, 40, User.MAXSIZE_NAME, getPageURL(ProcedureTypeAhead.COMMAND));

		if (this.isTableView()) // Call subclass
		{
			twoCol.writeSpaceRow();
	
			twoCol.writeRow(getString("elert:TimeReport.View"));
			twoCol.writeRadioButton("view", getString("elert:TimeReport.Graph"), "g", "g");
			twoCol.writeEncode(" ");
			twoCol.writeRadioButton("view", getString("elert:TimeReport.Table"), "t", "g");
		}
		
		twoCol.render();
		
		write("<br>");
		writeButton("query", getString("controls:Button.Query"));
		write("<br><br>");

		writeFormClose();

		if (this.isFormException())
		{
			return;
		}
		
		// Generate and render report
		
		Date from = getParameterDate("datefrom");
		if (from==null)
		{
			from = getDefaultFromDate();
		}
		Date to = getParameterDate("dateto");
		if (to==null)
		{
			to = getDefaultToDate();
		}
		
		UUID schedulerID;
		if (governor)
		{
			schedulerID = getUUID("scheduler");
		}
		else
		{
			schedulerID = getContext().getUserID();
		}

		if (isParameter("view")==false || getParameterString("view").equalsIgnoreCase("g"))
		{
			this.renderGraph(from,
					to,
					getUUID("region"),
					getUUID("servicearea"),
					getUUID("facility"),
					schedulerID,
					getUUID("physician"),
					getUUID("procedure")); // Call subclass
		}
		else
		{
			this.renderTable(from,
					to,
					getUUID("region"),
					getUUID("servicearea"),
					getUUID("facility"),
					schedulerID,
					getUUID("physician"),
					getUUID("procedure")); // Call subclass
		}
	}

	protected abstract void renderGraph(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws Exception;
	protected abstract void renderTable(Date from, Date to, UUID regionID, UUID serviceAreaID, UUID facilityID, UUID schedulerID, UUID physicianID, UUID procedureID) throws Exception;
	protected boolean isTableView()
	{
		return true;
	}
	protected String getHelpString()
	{
		return null;
	}
	protected Date getDefaultFromDate()
	{
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.MONTH, -11);
		return cal.getTime();
	}
	protected Date getDefaultToDate()
	{
		Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.MONTH, 1);
		return cal.getTime();
	}
}
