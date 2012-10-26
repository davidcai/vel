package samoyan.twitter;

public class TwitterMessage
{
	private String from;
	private String to;
	private StringBuffer text;
	
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
