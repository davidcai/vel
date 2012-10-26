package elert.pages.typeahead;

import samoyan.servlet.Dispatcher;

public class TypeAheadApp
{
	public static void init()
	{
		Dispatcher.bindPage(ServiceAreaTypeAhead.COMMAND,		ServiceAreaTypeAhead.class);
		Dispatcher.bindPage(PhysicianTypeAhead.COMMAND,			PhysicianTypeAhead.class);
		Dispatcher.bindPage(ProcedureTypeAhead.COMMAND,			ProcedureTypeAhead.class);
		Dispatcher.bindPage(RegionTypeAhead.COMMAND,			RegionTypeAhead.class);
		Dispatcher.bindPage(ProcedureTypeTypeAhead.COMMAND,		ProcedureTypeTypeAhead.class);
		Dispatcher.bindPage(ServiceAreaTypeAhead.COMMAND,		ServiceAreaTypeAhead.class);
		Dispatcher.bindPage(ResourceTypeAhead.COMMAND,			ResourceTypeAhead.class);
		Dispatcher.bindPage(FacilityTypeAhead.COMMAND,			FacilityTypeAhead.class);
		Dispatcher.bindPage(HomeFacilityTypeAhead.COMMAND,		HomeFacilityTypeAhead.class);
		Dispatcher.bindPage(HomePhysicianTypeAhead.COMMAND,		HomePhysicianTypeAhead.class);
		Dispatcher.bindPage(HomeProcedureTypeAhead.COMMAND,		HomeProcedureTypeAhead.class);
		Dispatcher.bindPage(SchedulerTypeAhead.COMMAND,			SchedulerTypeAhead.class);
		Dispatcher.bindPage(ProcedureOrTypeTypeAhead.COMMAND,	ProcedureOrTypeTypeAhead.class);
		Dispatcher.bindPage(PatientTypeAhead.COMMAND,			PatientTypeAhead.class);
	}
}
