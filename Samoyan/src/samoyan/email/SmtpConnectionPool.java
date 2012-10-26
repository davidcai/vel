package samoyan.email;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;

public class SmtpConnectionPool extends StackObjectPool<TransportSession>
{
	public SmtpConnectionPool()
	{
		super(new PoolableObjectFactory<TransportSession>()
		{
			@Override
			public void activateObject(TransportSession ts) throws Exception
			{
				// Nothing
			}

			@Override
			public void destroyObject(TransportSession ts) throws Exception
			{
				Session ses = ts.getSession();
				Debug.logln("SMTP disconnecting from " + ses.getProperty("mail.smtp.host") + ":" + ses.getProperty("mail.smtp.port"));
				ts.getTransport().close();
			}

			@Override
			public TransportSession makeObject() throws Exception
			{
				final Server fed = ServerStore.getInstance().loadFederation();
				if (fed.isSMTPActive()==false)
				{
					throw new IllegalStateException("SMTP channel is inactive");
				}
				
				Debug.logln("SMTP connecting to " + fed.getSMTPUser() + " at " + fed.getSMTPHost() + ":" + fed.getSMTPPort());
				
				// Create the session
				Authenticator auth = null;
				Properties props = new Properties();
				props.put("mail.smtp.host", fed.getSMTPHost());
				props.put("mail.smtp.port", String.valueOf(fed.getSMTPPort()));
				props.put("mail.smtp.timeout", String.valueOf(60000));
				props.put("mail.smtp.connectiontimeout", String.valueOf(20000));
				if (!Util.isEmpty(fed.getSMTPPassword()))
				{
					props.put("mail.smtp.auth", "true");
					props.put("mail.smtp.socketFactory.port", String.valueOf(fed.getSMTPPort()));
					props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
					props.put("mail.smtp.socketFactory.fallback", "false");
					
					auth = new Authenticator()
					{
						@Override
						protected PasswordAuthentication getPasswordAuthentication()
						{
							return new PasswordAuthentication(fed.getSMTPUser(), fed.getSMTPPassword());
						}
					};
				}
				
				// Get the Session
				Session ses = Session.getInstance(props, auth);
				ses.setDebug(false);
				
				// Create the transport
				Transport transport = ses.getTransport("smtp");
				transport.connect();
				
				return new TransportSession(transport, ses);
			}

			@Override
			public void passivateObject(TransportSession ts) throws Exception
			{
				// Nothing
			}

			@Override
			public boolean validateObject(TransportSession ts)
			{
				return ts.getTransport().isConnected();
			}
			
		});
	}
}
