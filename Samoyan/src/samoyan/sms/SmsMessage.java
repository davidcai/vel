package samoyan.sms;

import java.util.UUID;

public class SmsMessage
{
	private String from;
	private String to;
	private StringBuffer text;
	private UUID carrierID;
	
	public String getSender()
	{
		return from;
	}
	public void setSender(String from)
	{
		this.from = from;
	}
	
	public String getDestination()
	{
		return to;
	}
	public void setDestination(String to)
	{
		this.to = to;
	}
	
	public UUID getCarrierID()
	{
		return carrierID;
	}
	public void setCarrierID(UUID carrierID)
	{
		this.carrierID = carrierID;
	}
		
	public void write(String text)
	{
		if (this.text==null)
		{
			this.text = new StringBuffer();
		}
		this.text.append(text);
	}
	public String getText()
	{
		if (this.text==null)
		{
			return null;
		}
		else
		{
			return text.toString();
		}
	}
}
