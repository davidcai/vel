package elert.pages.common;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import samoyan.core.StringBundle;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Facility;
import elert.database.FacilityStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.PhysicianOpeningLinkStore;
import elert.database.Procedure;
import elert.database.ProcedureOpeningLinkStore;
import elert.database.ProcedureStore;
import elert.database.ResourceProcedureLinkStore;

public abstract class OpeningSorter implements Comparator<Opening>
{
	public abstract String getGroupTitle(Opening o, Locale loc);
	public abstract String getTitle(Locale loc);

	// - - - - - - - - - -
	
	public static class SortByFacility extends OpeningSorter
	{
		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				Facility f1 = FacilityStore.getInstance().load(o1.getFacilityID());
				Facility f2 = FacilityStore.getInstance().load(o2.getFacilityID());
				return f1.getName().compareTo(f2.getName());
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				Facility f = FacilityStore.getInstance().load(o.getFacilityID());
				return f.getName();
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByFacility");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByRank extends OpeningSorter
	{
		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				List<UUID> procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(o1.getID());
				int r1 = 0;
				for (UUID procID : procIDs)
				{
					r1 += ResourceProcedureLinkStore.getInstance().getTotalRankForProcedure(procID);
				}
				
				procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(o2.getID());
				int r2 = 0;
				for (UUID procID : procIDs)
				{
					r2 += ResourceProcedureLinkStore.getInstance().getTotalRankForProcedure(procID);
				}

				return r2-r1;
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				List<UUID> procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(o.getID());
				int r = 0;
				for (UUID procID : procIDs)
				{
					r += ResourceProcedureLinkStore.getInstance().getTotalRankForProcedure(procID);
				}
				return StringBundle.getString(loc, null, "elert:Sorters.Rank", r);
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByRank");
		}
	}
	
	// - - - - - - - - - -
	
	public static class SortByProcedure extends OpeningSorter
	{
		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				String n1 = null;
				List<UUID> procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(o1.getID());
				if (procIDs.size()==0)
				{
					n1 = "~none~";
				}
				else if (procIDs.size()>1)
				{
					n1 = "~multi~";
				}
				else
				{
					Procedure proc = ProcedureStore.getInstance().load(procIDs.get(0));
					n1 = proc.getName();
				}
				
				String n2 = null;
				procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(o2.getID());
				if (procIDs.size()==0)
				{
					n2 = "~none~";
				}
				else if (procIDs.size()>1)
				{
					n2 = "~multi~";
				}
				else
				{
					Procedure proc = ProcedureStore.getInstance().load(procIDs.get(0));
					n2 = proc.getName();
				}

				return n1.compareTo(n2);
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				List<UUID> procIDs = ProcedureOpeningLinkStore.getInstance().getProceduresByOpening(o.getID());
				if (procIDs.size()==0)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.None");
				}
				else if (procIDs.size()>1)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.Multiple");
				}
				else
				{
					Procedure proc = ProcedureStore.getInstance().load(procIDs.get(0));
					return proc.getName();
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByProcedure");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByPhysician extends OpeningSorter
	{
		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				String n1 = null;
				List<UUID> physicianIDs = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(o1.getID());
				if (physicianIDs.size()==0)
				{
					n1 = "~none~";
				}
				else if (physicianIDs.size()>1)
				{
					n1 = "~multi~";
				}
				else
				{
					User physician = UserStore.getInstance().load(physicianIDs.get(0));
					n1 = physician.getName();
				}
				
				String n2 = null;
				physicianIDs = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(o2.getID());
				if (physicianIDs.size()==0)
				{
					n1 = "~none~";
				}
				else if (physicianIDs.size()>1)
				{
					n1 = "~multi~";
				}
				else
				{
					User physician = UserStore.getInstance().load(physicianIDs.get(0));
					n2 = physician.getDisplayName();
				}

				return n1.compareTo(n2);
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				List<UUID> physicianIDs = PhysicianOpeningLinkStore.getInstance().getPhysiciansByOpening(o.getID());
				if (physicianIDs.size()==0)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.None");
				}
				else if (physicianIDs.size()>1)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.Multiple");
				}
				else
				{
					User physician = UserStore.getInstance().load(physicianIDs.get(0));
					return physician.getDisplayName();
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByPhysician");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByDuration extends OpeningSorter
	{
		private int block = 1;
		public SortByDuration(int block)
		{
			this.block = block;
		}
		
		@Override
		public int compare(Opening o1, Opening o2)
		{
			return (o2.getDuration()/this.block) - (o1.getDuration()/this.block);
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			if (this.block==1)
			{
				return StringBundle.getString(loc, null, "elert:Sorters.Minutes", o.getDuration());
			}
			else
			{
				long x = this.block*(o.getDuration()/this.block);
				if (this.block%60==0)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.HoursRange", x/60, (x+this.block)/60);
				}
				else
				{
					return StringBundle.getString(loc, null, "elert:Sorters.MinutesRange", x, x+this.block);
				}
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByDuration");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByOriginalDuration extends OpeningSorter
	{
		private int block = 1;
		public SortByOriginalDuration(int block)
		{
			this.block = block;
		}
		
		@Override
		public int compare(Opening o1, Opening o2)
		{
			return (o2.getOriginalDuration()/this.block) - (o1.getOriginalDuration()/this.block);
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			if (this.block==1)
			{
				return StringBundle.getString(loc, null, "elert:Sorters.Minutes", o.getOriginalDuration());
			}
			else
			{
				long x = this.block*(o.getOriginalDuration()/this.block);
				if (this.block%60==0)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.HoursRange", x/60, (x+this.block)/60);
				}
				else
				{
					return StringBundle.getString(loc, null, "elert:Sorters.MinutesRange", x, x+this.block);
				}
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByOriginalDuration");
		}
	}

	// - - - - - - - - - -
	
	public static class AllEqual extends OpeningSorter
	{
		@Override
		public int compare(Opening o1, Opening o2)
		{
			return 0;
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			return "";
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.Ungrouped");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByTimeLeft extends OpeningSorter
	{
		private int block = 1;
		private Date now = new Date();
		
		public SortByTimeLeft(int block)
		{
			this.block = block;
		}
		
		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				long t1 = o1.getDateTime().getTime() - now.getTime();
				t1 = t1 / (60L*60L*1000L);
				long t2 = o2.getDateTime().getTime() - now.getTime();
				t2 = t2 / (60L*60L*1000L);
				return (int) (t1-t2);
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				long t = o.getDateTime().getTime() - now.getTime();
				t = t / (60L*60L*1000L);
				if (this.block==1)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.Hours", t);
				}
				else
				{
					long x = this.block*(t/this.block);
					if (this.block%24==0)
					{
						return StringBundle.getString(loc, null, "elert:Sorters.DaysRange", x/24, (x+this.block)/24);
					}
					else
					{
						return StringBundle.getString(loc, null, "elert:Sorters.HoursRange", x, x+this.block);
					}
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByTimeLeft");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByBestMatch extends OpeningSorter
	{
		private int block = 1;
		
		public SortByBestMatch(int block)
		{
			this.block = block;
		}

		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				Integer m1 = OpeningStore.getInstance().bestMatchPercentage(o1);
				Integer m2 = OpeningStore.getInstance().bestMatchPercentage(o2);
				if (Util.objectsEqual(m1, m2))
				{
					return 0;
				}
				else if (m1==null)
				{
					return 1;
				}
				else if (m2==null)
				{
					return -1;
				}
				else
				{
					return m2-m1;
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				Integer m = OpeningStore.getInstance().bestMatchPercentage(o);
				if (m==null)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.NotApplicable");
				}
				else if (this.block==1)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.Match", m);
				}
				else
				{
					long x = this.block*(m/this.block);
					return StringBundle.getString(loc, null, "elert:Sorters.MatchRange", x, x+this.block);
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByBestMatch");
		}
	}
	
	// - - - - - - - - - -
	
	public static class SortByLastElertSentDate extends OpeningSorter
	{
		private long block = 1;
		private Date now = new Date();
		
		public SortByLastElertSentDate(long block)
		{
			this.block = block;
		}

		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				Date d1 = null;
				List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(o1.getID());
				if (elertIDs.size()>0)
				{
					Elert latestElert = ElertStore.getInstance().load(elertIDs.get(0));
					d1 = latestElert.getDateSent();
				}
				
				Date d2 = null;
				elertIDs = ElertStore.getInstance().queryByOpeningID(o2.getID());
				if (elertIDs.size()>0)
				{
					Elert latestElert = ElertStore.getInstance().load(elertIDs.get(0));
					d2 = latestElert.getDateSent();
				}

				if (Util.objectsEqual(d1, d2))
				{
					return 0;
				}
				else if (d1==null)
				{
					return 1;
				}
				else if (d2==null)
				{
					return -1;
				}
				else
				{
					long t1 = (now.getTime() - d1.getTime()) / this.block;
					long t2 = (now.getTime() - d2.getTime()) / this.block;
					
					return (int) (t1-t2);
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				List<UUID> elertIDs = ElertStore.getInstance().queryByOpeningID(o.getID());
				if (elertIDs.size()>0)
				{
					Elert latestElert = ElertStore.getInstance().load(elertIDs.get(0));
					long delta = now.getTime() - latestElert.getDateSent().getTime();
					if (this.block%(24L*60L*60L*1000L)==0)
					{
						return StringBundle.getString(loc, null, "elert:Sorters.DaysRange", delta/(24L*60L*60L*1000L), (delta+this.block)/(24L*60L*60L*1000L));
					}
					else if (this.block%(60L*60L*1000L)==0)
					{
						return StringBundle.getString(loc, null, "elert:Sorters.HoursRange", delta/(60L*60L*1000L), (delta+this.block)/(60L*60L*1000L));
					}
					else
					{
						return StringBundle.getString(loc, null, "elert:Sorters.Minutes", delta/(60L*1000L));
					}
				}
				else
				{
					return StringBundle.getString(loc, null, "elert:Sorters.NotApplicable");
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByElertSent");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByUrgency extends OpeningSorter
	{
		@Override
		public int compare(Opening o1, Opening o2)
		{
			try
			{
				Boolean b1 = OpeningStore.getInstance().hasUrgentMatch(o1);
				Boolean b2 = OpeningStore.getInstance().hasUrgentMatch(o2);
				
				if (Util.objectsEqual(b1, b2))
				{
					return 0;
				}
				else if (b1==null)
				{
					return 1;
				}
				else if (b2==null)
				{
					return -1;
				}
				else if (b1==false)
				{
					return 1;
				}
				else if (b2==false)
				{
					return -1;
				}
				else
				{
					// Shouldn't reach here
					return 0;
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Opening o, Locale loc)
		{
			try
			{
				Boolean b = OpeningStore.getInstance().hasUrgentMatch(o);
				if (b==null)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.NotApplicable");
				}
				else if (b==false)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.PriorityNormal");
				}
				else
				{
					return StringBundle.getString(loc, null, "elert:Sorters.PriorityUrgent");
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return null;
			}
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByUrgency");
		}
	}
}
