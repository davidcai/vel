package baby.pages.journey;

import java.util.Calendar;

import samoyan.controls.ButtonInputControl;
import samoyan.controls.ImageInputControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.database.Image;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;

public class JournalEntryPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/journalentry";
	
	public final static String PARAM_ID = "id";
	public final static String PARAM_EDIT = "edit";
	
	private final static String PARAM_TEXT = "text";
	private final static String PARAM_PHOTO = "photo";
	private final static String PARAM_SAVE = "save";
	private final static String PARAM_REMOVE = "remove";
	
	private JournalEntry entry;
	private boolean readOnly;
	
	@Override
	public void init() throws Exception
	{
		this.entry = JournalEntryStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.entry == null)
		{
			this.entry = new JournalEntry();
		}
		else if (this.entry.getUserID().equals(getContext().getUserID()) == false)
		{
			// Security check: make sure that entry is owned by this user
			throw new PageNotFoundException();
		}
		
		this.readOnly = (getContext().getUserAgent().isSmartPhone() && isParameter(PARAM_EDIT) == false);
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			validateParameterString(PARAM_TEXT, 0, JournalEntry.MAXSIZE_TEXT);

			// Show errors if both text and photo have no inputs
			if (isParameterNotEmpty(PARAM_TEXT) == false && getParameterImage(PARAM_PHOTO) == null)
			{
				throw new WebFormException(new String[] {PARAM_TEXT, PARAM_PHOTO}, getString("journey:JournalEntry.NoInput"));
			}
		}
	}

	@Override
	public void commit() throws Exception
	{
		this.entry.setUserID(getContext().getUserID());
		this.entry.setText(getParameterString(PARAM_TEXT));
		
		Image photo = getParameterImage(PARAM_PHOTO);
		this.entry.setHasPhoto(photo != null);
		this.entry.setPhoto(photo);
		
		this.entry.setCreated(Calendar.getInstance(getTimeZone()).getTime());

		JournalEntryStore.getInstance().save(this.entry);

		// Redirect to itself
		throw new RedirectException(JournalPage.COMMAND, null);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		if (this.readOnly)
		{
			renderViewOnly();
		}
		else
		{
			renderEditForm();
		}
	}
	
	private void renderViewOnly() throws Exception
	{
		// Edit button
		if (getContext().getUserAgent().isSmartPhone() == false)
		{
			write("<br>");
		}
		
		writeFormOpen("GET", getContext().getCommand());
		new ButtonInputControl(this, null)
			.setValue(getString("journey:JournalEntry.Edit"))
			.setMobileHotAction(true)
			.setAttribute("class", getContext().getUserAgent().isSmartPhone() ? "NoShow" : null)
			.render();
		writeHiddenInput(PARAM_ID, this.entry.getID().toString());
		writeHiddenInput(PARAM_EDIT, "");
		
		writeFormClose();
	}
	
	private void renderEditForm() throws Exception
	{
		UserAgent ua = getContext().getUserAgent();
		if (ua.isSmartPhone() == false)
		{
			writeEncode(getString("journey:JournalEntry.Help"));
			write("<br><br>");
		}
		
		writeFormOpen();
		
		new TextAreaInputControl(this, "text")
			.setRows(3).setCols(80)
			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
			.setPlaceholder(getString("journey:JournalEntry.WhatIsOnYourMind"))
			.setInitialValue(this.entry.getText())
			.render();
		write("<br>");
		new ImageInputControl(this, "photo").showThumbnail(false).render();
		
		// Postbacks
		writeHiddenInput(PARAM_ID, null);
		writeHiddenInput(PARAM_EDIT, null);
		
		// Buttons and links
		write("<br>");
		writeSaveButton(PARAM_SAVE, this.entry);
		if (this.entry.isSaved())
		{
			write("&nbsp;");
			writeRemoveButton(PARAM_REMOVE);
		}
		
		writeFormClose();
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("journey:JournalEntry.Title");
	}
}
