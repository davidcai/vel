package elert.database;

import java.util.Date;
import java.util.UUID;

import samoyan.database.DataBean;

public class Elert extends DataBean
{
	public final static int MAXSIZE_CHANNEL = 8;
	
	public final static int REPLY_NONE = 0;
	public final static int REPLY_DECLINED = -1;
	public final static int REPLY_ACCEPTED = 1;

	public final static int DECISION_NONE = 0;
	public final static int DECISION_CHOSEN = 1;
	public final static int DECISION_NOT_CHOSEN = -1;

	public UUID getSubscriptionID()
	{
		return (UUID) get("SubscriptionID");
	}
	public void setSubscriptionID(UUID id)
	{
		set("SubscriptionID", id);
	}
	
	public UUID getOpeningID()
	{
		return (UUID) get("OpeningID");
	}
	public void setOpeningID(UUID id)
	{
		set("OpeningID", id);
	}
	
	public UUID getSchedulerID()
	{
		return (UUID) get("SchedulerID");
	}
	public void setSchedulerID(UUID id)
	{
		set("SchedulerID", id);
	}
	
	public UUID getRegionID()
	{
		return (UUID) get("RegionID");
	}
	public void setRegionID(UUID id)
	{
		set("RegionID", id);
	}
	
	public UUID getServiceAreaID()
	{
		return (UUID) get("ServiceAreaID");
	}
	public void setServiceAreaID(UUID id)
	{
		set("ServiceAreaID", id);
	}
	
	public UUID getFacilityID()
	{
		return (UUID) get("FacilityID");
	}
	public void setFacilityID(UUID id)
	{
		set("FacilityID", id);
	}
	
	public UUID getPatientID()
	{
		return (UUID) get("PatientID");
	}
	public void setPatientID(UUID id)
	{
		set("PatientID", id);
	}
	
	public Date getDateSent()
	{
		return (Date) get("SentDate");
	}
	public void setDateSent(Date d)
	{
		set("SentDate", d);
	}

	public int getReply()
	{
		return (Integer) get("Reply", REPLY_NONE);
	}
	public void setReply(int reply)
	{
		set("Reply", reply);
	}

	public Date getDateReply()
	{
		return (Date) get("ReplyDate");
	}
	public void setDateReply(Date d)
	{
		set("ReplyDate", d);
	}

	public String getReplyChannel()
	{
		return (String) get("ReplyChannel");
	}
	public void setReplyChannel(String replyChannel)
	{
		set("ReplyChannel", replyChannel);
	}

	public int getDecision()
	{
		return (Integer) get("Decision", DECISION_NONE);
	}
	public void setDecision(int decision)
	{
		set("Decision", decision);
	}

	public Date getDateDecision()
	{
		return (Date) get("DecisionDate");
	}
	public void setDateDecision(Date d)
	{
		set("DecisionDate", d);
	}

	public Date getDateOpening()
	{
		return (Date) get("OpeningDate");
	}
	public void setDateOpening(Date d)
	{
		set("OpeningDate", d);
	}
	/**
	 * Indicates if the patient has hidden this eLert from his Wall.
	 * @return
	 */
	public boolean isHidden()
	{
		return (Boolean) get("Hidden", false);
	}
	public void setHidden(boolean hidden)
	{
		set("Hidden", hidden);
	}
}
