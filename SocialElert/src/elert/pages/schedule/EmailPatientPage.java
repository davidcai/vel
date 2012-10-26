package elert.pages.schedule;

import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.database.User;
import samoyan.database.UserStore;
import samoyan.notif.Notifier;
import samoyan.servlet.Channel;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.WebFormException;
import elert.pages.ElertPage;

/**
 * Form for sending an eLert to a patient.
 * @author brian
 *
 */
public final class EmailPatientPage extends ElertPage
{
	public final static String COMMAND = ElertPage.COMMAND_SCHEDULE + "/email-patient";
	public static final String PARAM_PATIENT_ID = "to";
	
	@Override
	public void validate() throws Exception
	{
		User patient = UserStore.getInstance().load(getParameterUUID(PARAM_PATIENT_ID));
		if (patient==null)
		{
			throw new WebFormException(getString("common:Errors.MissingField"));
		}
		
		validateParameterString("subject", 1, 128);

		String html = getParameterRichEdit("body");
		if (Util.isEmptyHTML(html))
		{
			throw new WebFormException("body", getString("common:Errors.MissingField"));
		}
	}

	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();
		Notifier.send(Channel.EMAIL, null, getParameterUUID(PARAM_PATIENT_ID), null, ctx.getCommand(), ctx.getParameters());
	}

	@Override
	public void renderHTML() throws Exception
	{
		User patient = UserStore.getInstance().load(getParameterUUID(PARAM_PATIENT_ID));
		if (patient==null)
		{
			throw new PageNotFoundException();
		}
		
		if (this.isCommitted())
		{
			writeEncode(getString("schedule:EmailPatient.Confirmation"));
			return;
		}
		
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("schedule:EmailPatient.To"));
		twoCol.writeLink(	patient.getDisplayName(),
							getPageURL(PatientProfilePage.COMMAND, new ParameterMap(PatientProfilePage.PARAM_ID, patient.getID().toString())));
		twoCol.write(" <");
		twoCol.writeEncode(patient.getEmail());
		twoCol.write(">");
		
		twoCol.writeSpaceRow();
		
		// Subject
		twoCol.writeRow(getString("schedule:EmailPatient.Subject"));
		twoCol.writeTextInput("subject", null, 80, 128);
		
		// Body
		twoCol.writeRow(getString("schedule:EmailPatient.Message"));
		twoCol.writeRichEditField("body", null, 80, 10);

		twoCol.render();

		write("<br>");
		writeButton("send", getString("schedule:EmailPatient.Send"));
		
		writeHiddenInput(PARAM_PATIENT_ID, null); // Postback patient ID
		
		writeFormClose();
	}

	@Override
	public String getTitle() throws Exception
	{
		if (getContext().getChannel().equals(Channel.EMAIL))
		{
			return getParameterString("subject");
		}
		else
		{
			return getString("schedule:EmailPatient.Title");
		}
	}

	@Override
	public void renderSimpleHTML() throws Exception
	{
		write(getParameterRichEdit("body"));
	}
}
