package samoyan.apps.admin.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import samoyan.apps.admin.AdminPage;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.Server;
import samoyan.database.ServerStore;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;

public class ApplePushConfigPage extends AdminPage
{
	public final static String COMMAND = AdminPage.COMMAND + "/apn-config";

	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		
		String ks = getParameterString("ks");
		File ksf = ctx.getPostedFile("ksfile");
		
		if (ks.equals("2") && ksf!=null)
		{
			// Validate certificate
			String pw = validateParameterString("password", 1, Server.MAXSIZE_PASSWORD);

			try
			{
				KeyStore p12 = KeyStore.getInstance("PKCS12");;
				p12.load(new FileInputStream(ksf), pw.toCharArray());
				Enumeration<String> aliases = p12.aliases();
				while (aliases.hasMoreElements())
				{
					String alias = aliases.nextElement();
					Certificate cert = p12.getCertificate(alias);
	//				Debug.logln(alias);
	//				Debug.logln(cert.getType());
					if (cert instanceof X509Certificate)
					{
						X509Certificate x509 = (X509Certificate) cert;
						x509.checkValidity();
					}
				}
			}
			catch (Exception e)
			{
				throw new WebFormException("ksfile", getString("admin:ApplePushConfig.InvalidCert"));
			}
		}
		
		if (ks.equals("2") && ksf==null)
		{
			throw new WebFormException("ksfile", getString("common:Errors.MissingField"));
		}
		
		if (isParameter("active") && ks.equals("0"))
		{
			throw new WebFormException("ks", getString("common:Errors.MissingField"));
		}
	}

	@Override
	public void commit() throws Exception
	{
		Server fed = ServerStore.getInstance().openFederation();

		fed.setApplePushActive(isParameter("active"));
		fed.setApplePushProduction(isParameter("prod"));
		fed.setApplePushKeystorePassword(getParameterString("password"));
		fed.setApplePushDownloadURL(getParameterString("download"));
		
		RequestContext ctx = getContext();
		
		File ksf = ctx.getPostedFile("ksfile");
		String ks = getParameterString("ks");
		if (ks.equals("0"))
		{
			fed.setApplePushKeystore(null);
		}
		else if (ks.equals("2"))
		{
			fed.setApplePushKeystore( Util.inputStreamToBytes(new FileInputStream(ksf)) );
		}
		
		ServerStore.getInstance().save(fed);

		// Redirect to self
		throw new RedirectException(getContext().getCommand(), new ParameterMap(RequestContext.PARAM_SAVED, ""));
	}

	@Override
	public boolean isSecureSocket() throws Exception
	{
		return true;
	}

	@Override
	public void renderHTML() throws Exception
	{
		Server fed = ServerStore.getInstance().loadFederation(); 
		
		writeFormOpen();
				
		TwoColFormControl twoCol = new TwoColFormControl(this);			
		
		twoCol.writeRow(getString("admin:ApplePushConfig.Status"));
		twoCol.writeCheckbox("active", getString("admin:ApplePushConfig.Active"), fed.isApplePushActive());
		twoCol.writeCheckbox("prod", getString("admin:ApplePushConfig.Production"), fed.isApplePushProduction());
		
		twoCol.writeRow(getString("admin:ApplePushConfig.Keystore"));
		if (fed.getApplePushKeystore()!=null)
		{
			twoCol.writeRadioButton("ks", null, "1", fed.getApplePushKeystore()!=null?"1":"0");

			KeyStore p12 = KeyStore.getInstance("PKCS12");;
			p12.load(new ByteArrayInputStream(fed.getApplePushKeystore()), fed.getApplePushKeystorePassword().toCharArray());
			Enumeration<String> aliases = p12.aliases();
			while (aliases.hasMoreElements())
			{
				String alias = aliases.nextElement();
				Certificate cert = p12.getCertificate(alias);
				if (cert instanceof X509Certificate)
				{
					X509Certificate x509 = (X509Certificate) cert;
					twoCol.write(" ");
					twoCol.writeEncode(getString("admin:ApplePushConfig.CertificateInfo", alias, x509.getNotAfter()));
					twoCol.write("<br>");
				}
			}
		}
		twoCol.writeRadioButton("ks", null, "2", fed.getApplePushKeystore()!=null?"1":"0");
		twoCol.write("<input type=file name=ksfile size=60 onclick=\"$('INPUT[name=ks]').val('2');\">");
		twoCol.write("<br>");
		twoCol.writeRadioButton("ks", getString("admin:ApplePushConfig.NoKeystore"), "0", fed.getApplePushKeystore()!=null?"1":"0");
		
		twoCol.writeRow(getString("admin:ApplePushConfig.Password"));
		twoCol.writePasswordInput("password", fed.getApplePushKeystorePassword(), 20, Server.MAXSIZE_PASSWORD);
		
		twoCol.writeRow(getString("admin:ApplePushConfig.Download"));
		twoCol.writeTextInput("download", fed.getApplePushDownloadURL(), 60, 2048);
		
		twoCol.render();
		write("<br>");
		writeSaveButton(fed);
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("admin:ApplePushConfig.Title");
	}
	
}
