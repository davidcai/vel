package samoyan.email;

import javax.mail.Session;
import javax.mail.Transport;

final class TransportSession
{
	private Transport transport;
	private Session session;
	
	public TransportSession(Transport transport, Session session)
	{
		this.transport = transport;
		this.session = session;
	}
	
	public Transport getTransport()
	{
		return transport;
	}

	public Session getSession()
	{
		return session;
	}
}
