package elert.pages.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import samoyan.core.DateFormatEx;
import samoyan.core.StringBundle;
import samoyan.database.User;
import samoyan.database.UserStore;

import elert.database.Elert;
import elert.database.ElertStore;
import elert.database.Opening;
import elert.database.OpeningStore;
import elert.database.Procedure;
import elert.database.ProcedureStore;
import elert.database.Subscription;
import elert.database.SubscriptionPhysicianLinkStore;
import elert.database.SubscriptionProcedureLinkStore;

public abstract class SubscriptionSorter implements Comparator<Subscription>
{
	public abstract String getGroupTitle(Subscription s, Locale loc);
	public abstract String getTitle(Locale loc);

	// - - - - - - - - - -
	
	public static class SortByPatient extends SubscriptionSorter
	{
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			try
			{
				User patient1 = UserStore.getInstance().load(s1.getUserID());
				User patient2 = UserStore.getInstance().load(s2.getUserID());
				return patient1.getName().compareTo(patient2.getName());
			}
			catch (Exception e)
			{
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			try
			{
				User patient = UserStore.getInstance().load(s.getUserID());
				return patient.getName();
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
			return StringBundle.getString(loc, null, "elert:Sorters.ByPatient");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByProcedure extends SubscriptionSorter
	{
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			try
			{
				String n1 = null;
				List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(s1.getID());
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
				procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(s2.getID());
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
		public String getGroupTitle(Subscription s, Locale loc)
		{
			try
			{
				List<UUID> procIDs = SubscriptionProcedureLinkStore.getInstance().getProceduresForSubscription(s.getID());
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

	public static class SortByPhysician extends SubscriptionSorter
	{
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			try
			{
				String n1 = null;
				List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(s1.getID());
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
				physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(s2.getID());
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
		public String getGroupTitle(Subscription s, Locale loc)
		{
			try
			{
				List<UUID> physicianIDs = SubscriptionPhysicianLinkStore.getInstance().getPhysiciansForSubscription(s.getID());
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

	public static class SortByVerified extends SubscriptionSorter
	{
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			boolean v1 = s1.getVerifiedByUserID()!=null;
			boolean v2 = s2.getVerifiedByUserID()!=null;

			if (v1==v2)
			{
				return 0;
			}
			else if (v1==false)
			{
				return 1;
			}
			else
			{
				return -1;
			}
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			boolean v = s.getVerifiedByUserID()!=null;
			return StringBundle.getString(loc, null, v? "elert:Sorters.Verified" : "elert:Sorters.NotVerified");
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByVerified");
		}
	}

	// - - - - - - - - - -
	
	public static class SortByUrgency extends SubscriptionSorter
	{
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			boolean u1 = s1.isUrgent();
			boolean u2 = s2.isUrgent();

			if (u1==u2)
			{
				return 0;
			}
			else if (u1==false)
			{
				return 1;
			}
			else
			{
				return -1;
			}
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			boolean u = s.isUrgent();
			return StringBundle.getString(loc, null, u? "elert:Sorters.PriorityUrgent" : "elert:Sorters.PriorityNormal");
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByUrgency");
		}
	}

	// - - - - - - - - - -

	public static class SortByDuration extends SubscriptionSorter
	{
		private int block = 1;
		public SortByDuration(int block)
		{
			this.block = block;
		}
		
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			return (s2.getDuration()/this.block) - (s1.getDuration()/this.block);
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			if (this.block==1)
			{
				return StringBundle.getString(loc, null, "elert:Sorters.Minutes", s.getDuration());
			}
			else
			{
				long x = this.block*(s.getDuration()/this.block);
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

	public static class SortByMatch extends SubscriptionSorter
	{
		private Opening opening;
		private int block = 1;
		
		public SortByMatch(Opening o, int block)
		{
			this.opening = o;
			this.block = block;
		}
		
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			try
			{
				Integer m1 = OpeningStore.getInstance().matchPercentage(this.opening, s1);
				Integer m2 = OpeningStore.getInstance().matchPercentage(this.opening, s2);

				if (m1==null && m2==null)
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
					return m2/this.block - m1/this.block;
				}
			}
			catch (Exception e)
			{
				// Shouldn't happen
				return 0;
			}
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			try
			{
				Integer m = OpeningStore.getInstance().matchPercentage(this.opening, s);
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
			return StringBundle.getString(loc, null, "elert:Sorters.ByMatch");
		}
	}

	// - - - - - - - - - -

	public static class SortByElertResponse extends SubscriptionSorter
	{
		private Opening opening;

		public SortByElertResponse(Opening o)
		{
			this.opening = o;
		}
		
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			try
			{
				Elert e1 = ElertStore.getInstance().loadByOpeningAndSubscription(this.opening.getID(), s1.getID());
				Elert e2 = ElertStore.getInstance().loadByOpeningAndSubscription(this.opening.getID(), s2.getID());
				
				if (e1==null && e2==null)
				{
					return 0;
				}
				else if (e1==null)
				{
					return 1;
				}
				else if (e2==null)
				{
					return -1;
				}
				else if (e1.getDecision()==Elert.DECISION_CHOSEN && e2.getDecision()!=Elert.DECISION_CHOSEN)
				{
					return -1;
				}
				else if (e1.getDecision()!=Elert.DECISION_CHOSEN && e2.getDecision()==Elert.DECISION_CHOSEN)
				{
					return 1;
				}
				else if (e1.getReply()==e2.getReply())
				{
					return 0;
				}
				else if (e1.getReply()==Elert.REPLY_NONE)
				{
					return 1;
				}
				else if (e2.getReply()==Elert.REPLY_NONE)
				{
					return -1;
				}
				else if (e1.getReply()==Elert.REPLY_DECLINED)
				{
					return 1;
				}
				else if (e2.getReply()==Elert.REPLY_DECLINED)
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
		public String getGroupTitle(Subscription s, Locale loc)
		{
			try
			{
				Elert e = ElertStore.getInstance().loadByOpeningAndSubscription(this.opening.getID(), s.getID());
				if (e==null)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.NotApplicable");
				}
				else if (e.getDecision()==Elert.DECISION_CHOSEN)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.ElertFinalized");
				}
				else if (e.getReply()==Elert.REPLY_DECLINED)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.ElertDeclined");
				}
				else if (e.getReply()==Elert.REPLY_ACCEPTED)
				{
					return StringBundle.getString(loc, null, "elert:Sorters.ElertAccepted");
				}
				else
				{
					return StringBundle.getString(loc, null, "elert:Sorters.ElertDidNotReply");
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
			return StringBundle.getString(loc, null, "elert:Sorters.ByElertResponse");
		}
	}

	// - - - - - - - - - -

	public static class SortByDateSubscribed extends SubscriptionSorter
	{
		private TimeZone tz;
		private Locale loc;
		
		public SortByDateSubscribed(Locale loc, TimeZone tz)
		{
			this.tz = tz;
			this.loc = loc;
		}
		
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			DateFormat df = new SimpleDateFormat("YYYYMMdd", this.loc);
			df.setTimeZone(this.tz);
			
			String d1 = df.format(s1.getCreatedDate());
			String d2 = df.format(s2.getCreatedDate());
			
			return d2.compareTo(d1);
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			DateFormat df = DateFormatEx.getDateInstance(this.loc, this.tz);
			return df.format(s.getCreatedDate());
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.BySubscriptionDate");
		}
	}

	// - - - - - - - - - -

	public static class SortByDateScheduled extends SubscriptionSorter
	{
		private TimeZone tz;
		private Locale loc;
		
		public SortByDateScheduled(Locale loc, TimeZone tz)
		{
			this.tz = tz;
			this.loc = loc;
		}
		
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			Date d1 = getScheduledDate(s1);
			Date d2 = getScheduledDate(s2);
			
			if (d1 == null && d2 == null)
			{
				return 0;
			}
			if (d1 == null)
			{
				return 1;
			}
			if (d2 == null)
			{
				return -1;
			}
			
			DateFormat df = new SimpleDateFormat("YYYYMMdd", this.loc);
			df.setTimeZone(this.tz);
			
			String strD1 = df.format(d1);
			String strD2 = df.format(d2);
			
			return strD1.compareTo(strD2);
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			Date dt = getScheduledDate(s);
			if (dt != null)
			{
				DateFormat df = DateFormatEx.getDateInstance(this.loc, this.tz);
				return df.format(dt);
			}
			
			return StringBundle.getString(loc, null, "elert:Sorters.NotScheduled");
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.ByScheduledDate");
		}
		
		/**
		 * Returns the scheduled date for this subscription. If the subscription is finalized, get the opening date of the
		 * correspondent Elert. Otherwise, use the subscription's original date. Can be NULL.
		 * 
		 * @param sub
		 * @return Scheduled date
		 */
		private Date getScheduledDate(Subscription sub) 
		{
			Date dt = null;
			
			try
			{
				if (sub.isFinalized())
				{
					UUID elertID = ElertStore.getInstance().getFinalForSubscription(sub.getID());
					if (elertID != null)
					{
						Elert elert = ElertStore.getInstance().load(elertID);
						
						// Can be NULL
						dt = elert.getDateOpening();
					}
				}
				else
				{
					// Can be NULL
					dt = sub.getOriginalDate();
				}
			}
			catch (Exception e)
			{
				dt = null;
			}
			
			return dt;
		}
	}

	// - - - - - - - - - -

	public static class AllEqual extends SubscriptionSorter
	{
		@Override
		public int compare(Subscription s1, Subscription s2)
		{
			return 0;
		}

		@Override
		public String getGroupTitle(Subscription s, Locale loc)
		{
			return "";
		}

		@Override
		public String getTitle(Locale loc)
		{
			return StringBundle.getString(loc, null, "elert:Sorters.Ungrouped");
		}
	}
}
