package samoyan.email;

import java.io.*;
import java.util.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.*;
import samoyan.core.Util;

public class EmailMessage
{
	private class Attachment
	{
		public String fileName;
		public String mime;
		public InputStream content;
	}
	
	private HashMap<String, StringBuffer> mimes;
	private String subject;
	private InternetAddress from;
	private InternetAddress to;
	private InternetAddress replyTo;
	private ArrayList<Object> attachments;
	private ArrayList<Object> inlines;
	
	private int sendFailures;
	private Date sendAfter;
	private Date date;
	
	public EmailMessage()
	{
		this.mimes = new HashMap<String, StringBuffer>();
		this.subject = "";
		this.from = null;
		this.replyTo = null;
		this.to = null;
		this.attachments = new ArrayList<Object>();
		this.inlines = new ArrayList<Object>();
		this.date = new Date();
		
		this.sendFailures = 0;
		this.sendAfter = new Date();
	}
		
	/**
	 * Append text to the given MIME type of the message.
	 */
	public void write(Object content, String mimeType)
	{
		mimeType = mimeType.toLowerCase();
		
		StringBuffer canvas = mimes.get(mimeType);
		if (canvas==null)
		{
			canvas = new StringBuffer();
			mimes.put(mimeType, canvas);
		}
		canvas.append(content.toString());
	}
	
	public String getContent(String mimeType)
	{
		StringBuffer canvas = mimes.get(mimeType);
		if (canvas==null)
		{
			return null;
		}
		return canvas.toString();
	}
	
	public void setContent(String mimeType, String content)
	{
		mimes.remove(mimeType);
		write(content, mimeType);
	}

	/**
	 * Append text to the text/html MIME type of the message.
	 */
	public void write(Object content)
	{
		write(content.toString(), "text/html");
	}
	
	/**
	 * Append text to the text/html MIME type of the message but encode it first.
	 */
	public void writeEncode(Object content)
	{
		write(Util.htmlEncode(content.toString()), "text/html");
	}
	
	/**
	 * Append text to the text/plain MIME type of the message.
	 * @param string
	 */
	public void writePlain(Object content)
	{
		write(content.toString(), "text/plain");
	}
		
	/**
	 * Will append the attachment to the email.
	 * @param file The attachment's location.
	 */
	public void addAttachment(File file)
	{
		attachments.add(file);
	}

	/**
	 * Will append the attachment to the email.
	 * @param stm The input stream to the attachment's content.
	 * @param mimeType The MIME type of the attachment.
	 * @param fileName The filename to display to the user.
	 */
	public void addAttachment(InputStream stm, String mimeType, String fileName)
	{
		Attachment atmt = new Attachment();
		atmt.content = stm;
		atmt.mime = mimeType;
		atmt.fileName = fileName;
		
		attachments.add(atmt);
	}
			
	/**
	 * Sets the subject of the email.
	 * @param subject The subject
	 */
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
			
	/**
	 * Returns the subject of the email.
	 */
	public String getSubject()
	{
		return this.subject;
	}

	/**
	 * Sets a recipient of the "to" list of this email.
	 * @param address The email address to send the message to.
	 * @param name The name to show in the email.
	 */
	public void setRecipient(String address, String name) throws Exception
	{
		if (Util.isEmpty(address)) return;
		this.to = new InternetAddress(address, Util.emailSafe(name));
	}
	public void setRecipientName(String name) throws UnsupportedEncodingException
	{
		if (this.to==null)
		{
			this.to = new InternetAddress(null, Util.emailSafe(name));
		}
		else
		{
			this.to.setPersonal(Util.emailSafe(name));
		}
	}
	public void setRecipientAddress(String address) throws UnsupportedEncodingException
	{
		if (this.to==null)
		{
			this.to = new InternetAddress(address, beforeAt(address));
		}
		else
		{
			this.to.setAddress(address);
		}
	}
	
	public String getRecipientName()
	{
		return this.to==null? null : this.to.getPersonal();
	}
	public String getRecipientAddress()
	{
		return this.to==null? null : this.to.getAddress();
	}
		
	/**
	 * Adds a recipient to the "reply to" list of this email.
	 * @param address The email address to send the message to.
	 * @param name The name to show in the email.
	 */
	public void setReplyTo(String address, String name) throws Exception
	{
		if (Util.isEmpty(address)) return;
		this.replyTo = new InternetAddress(address, Util.emailSafe(name));
	}
	
	public String getReplyToName()
	{
		return this.replyTo==null? null : this.replyTo.getPersonal();
	}
	public String getReplyToAddress()
	{
		return this.replyTo==null? null : this.replyTo.getAddress();
	}

	public void setReplyToName(String name) throws UnsupportedEncodingException
	{
		if (this.replyTo==null)
		{
			this.replyTo = new InternetAddress(null, Util.emailSafe(name));
		}
		else
		{
			this.replyTo.setPersonal(Util.emailSafe(name));
		}
	}
	public void setReplyToAddress(String address) throws UnsupportedEncodingException
	{
		if (this.replyTo==null)
		{
			this.replyTo = new InternetAddress(address, beforeAt(address));
		}
		else
		{
			this.replyTo.setAddress(address);
		}
	}

	private String beforeAt(String emailAddress)
	{
		int plus = emailAddress.indexOf("+");
		if (plus>=0)
		{
			emailAddress = emailAddress.substring(0, plus);
		}
		int at = emailAddress.indexOf("@");
		if (at>=0)
		{
			return emailAddress.substring(0, at);
		}
		return emailAddress;
	}

	/**
	 * Sets the sender "from" of this email. Must be set on each message.
	 * @param address The email address to send the message to.
	 * @param name The name to show in the email.
	 */
	public void setSender(String address, String name) throws Exception
	{
		if (Util.isEmpty(address)) return;
		this.from = new InternetAddress(address, Util.emailSafe(name));
	}
	
	public String getSenderName()
	{
		return this.from==null? null : this.from.getPersonal();
	}
	public String getSenderAddress()
	{
		return this.from==null? null : this.from.getAddress();
	}

	public void setSenderName(String name) throws UnsupportedEncodingException
	{
		if (this.from==null)
		{
			this.from = new InternetAddress(null, Util.emailSafe(name));
		}
		else
		{
			this.from.setPersonal(Util.emailSafe(name));
		}
	}
	public void setSenderAddress(String address) throws UnsupportedEncodingException
	{
		if (this.from==null)
		{
			this.from = new InternetAddress(address, beforeAt(address));
		}
		else
		{
			this.from.setAddress(address);
		}
	}

	public void setDate(Date dt)
	{
		this.date = dt;
	}
	public Date getDate()
	{
		return this.date;
	}
	
	// - - - - - - - - - - - - - - - - - - - - 
	
	int getSendFailures()
	{
		return sendFailures;
	}
	void incrementSendFailures()
	{
		this.sendFailures ++;
	}
	void setSendAfter(Date dt)
	{
		this.sendAfter = dt;
	}
	Date getSendAfter()
	{
		return this.sendAfter;
	}
	
	/**
	 * Fills in a <code>javax.mail.Message</code> object from the member properties.
	 * @param msg A newly created <code>javax.mail.Message</code>
	 * @throws MessagingException 
	 * @throws IOException 
	 */
	void compose(javax.mail.Message msg) throws MessagingException, IOException
	{
		// Set the sent time
		msg.setSentDate(new Date());
		
		// Set the subject
		msg.setSubject(this.subject);
		
		// Set the "From" field
		msg.setFrom(this.from);
		
		// Set the "Reply To" field
		if (this.replyTo!=null)
		{
			InternetAddress[] replytoArr = new InternetAddress[1];
			replytoArr[0] = this.replyTo;
			msg.setReplyTo(replytoArr);
		}
		
		// Set the "To" field
		if (this.to!=null)
		{
			InternetAddress[] toArr = new InternetAddress[1];
			toArr[0] = this.to;
			msg.setRecipients(javax.mail.Message.RecipientType.TO, toArr);
		}
		
		// ---------------------------------------------------------------------- //
		
		MimeMultipart mp = new MimeMultipart("related");

		// Set the multi-part textual content
		for (Iterator<String> iter = mimes.keySet().iterator(); iter.hasNext();)
		{
			String mimeType = iter.next();
			String content = getContent(mimeType);
			
			MimeBodyPart part = new MimeBodyPart();
			part.setContent(content, mimeType + "; charset=UTF-8");
			
			mp.addBodyPart(part);
		}
		
		// Set the inline objects
		for (int i=0; i<inlines.size(); i++)
		{
			Object inline = inlines.get(i);
			
			// Fetch the inline object and associate to part
			MimeBodyPart part = new MimeBodyPart();
			if (inline instanceof File)
			{
				File file = (File) inline;
				DataSource fds = new FileDataSource(file);
				part.setDataHandler(new DataHandler(fds));
			}
			else if (inline instanceof Attachment)
			{
				Attachment atmt = (Attachment) inline;
				
				// Encode the content
				ByteArrayOutputStream ostm = new ByteArrayOutputStream(2048);
				Util.base64Encode(atmt.content, ostm, true);
				
				// *** Construct new MimeBodyPart! ***
				part = new MimeBodyPart(new InternetHeaders(), ostm.toByteArray());

				part.setHeader("Content-Transfer-Encoding", "base64");
				part.setHeader("Content-Type", atmt.mime);
			}
				
			part.setHeader("Content-ID", "inline" + String.valueOf(i+1));
			part.setDisposition(Part.INLINE);
			mp.addBodyPart(part);
		}
		
		// Set attachments
		for (Iterator<Object> iter = attachments.iterator(); iter.hasNext();)
		{
			Object attachment = iter.next();

			MimeBodyPart part = new MimeBodyPart();
			String fileName = "";
			DataSource source = null;
			
			if (attachment instanceof File)
			{
				File file = (File) attachment;
				fileName = file.getName();
				source = new FileDataSource(file);
				part.setDataHandler(new DataHandler(source));
			}
			else if (attachment instanceof Attachment)
			{
				Attachment atmt = (Attachment) attachment;
				fileName = atmt.fileName;
												
				// Encode the content
				ByteArrayOutputStream ostm = new ByteArrayOutputStream(2048);
				Util.base64Encode(atmt.content, ostm, true);

				// *** Construct new MimeBodyPart! ***
				part = new MimeBodyPart(new InternetHeaders(), ostm.toByteArray());

				part.setHeader("Content-Transfer-Encoding", "base64");
				part.setHeader("Content-Type", atmt.mime);
			}

			part.setFileName(fileName);
			part.setDisposition(Part.ATTACHMENT);
			mp.addBodyPart(part);
		}
		
		msg.setContent(mp);

		// ---------------------------------------------------------------------- //

//		if (Debug.ON)
//		{
//			ByteArrayOutputStream debugStm = new ByteArrayOutputStream(4096);
//			msg.writeTo(debugStm);
//			byte[] array = debugStm.toByteArray();
//			StringBuffer buf = new StringBuffer(array.length);
//			for (int j=0; j<array.length; j++)
//			{
//				buf.append((char) array[j]);
//			}
//			Debug.println(buf.toString());
//		}
	}
	
}
