package baby.pages.journey;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import samoyan.controls.ImageControl;
import samoyan.controls.ImageInputControl;
import samoyan.controls.TextAreaInputControl;
import samoyan.core.ParameterMap;
import samoyan.database.Image;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.PageNotFoundException;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.database.MeasureRecord;
import baby.database.Mother;
import baby.database.MotherStore;
import baby.pages.BabyPage;

public class JournalEntryPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_JOURNEY + "/journalentry";
	
	public final static String PARAM_ID = "id";
	
	private final static String PARAM_TEXT = "text";
	private final static String PARAM_PHOTO = "photo";
	private final static String PARAM_SAVE = "save";
	private final static String PARAM_REMOVE = "remove";
	
	private Mother mom;
	private JournalEntry entry;
	private List<MeasureRecord> momRecords;
	private Map<UUID, List<MeasureRecord>> babyRecords;
	
	@Override
	public void init() throws Exception
	{
		UUID userID = getContext().getUserID();
		this.mom = MotherStore.getInstance().loadByUserID(userID);
		
		this.entry = JournalEntryStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.entry == null)
		{
			this.entry = new JournalEntry();
			
			Calendar cal = Calendar.getInstance(getTimeZone(), getLocale());
			this.momRecords = MeasureRecordsPageHelper.getInstance().createMeasureRecordsForMom(this, this.mom, cal);
			this.babyRecords = MeasureRecordsPageHelper.getInstance().createMeasureRecordsForBabies(this, this.mom, cal);
		}
		else if (this.entry.getUserID().equals(userID) == false)
		{
			// Security check: make sure that entry is owned by this user
			throw new PageNotFoundException();
		}
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
		if (isParameter(PARAM_SAVE))
		{
			this.entry.setUserID(getContext().getUserID());
			this.entry.setText(getParameterString(PARAM_TEXT));
			
			Image photo = getParameterImage(PARAM_PHOTO);
			this.entry.setHasPhoto(photo != null);
			this.entry.setPhoto(photo);
			
			if (this.entry.getCreated() == null)
			{
				this.entry.setCreated(Calendar.getInstance(getTimeZone()).getTime());
			}
	
			JournalEntryStore.getInstance().save(this.entry);
		}
		else if (isParameter(PARAM_REMOVE) && this.entry.isSaved())
		{
			JournalEntryStore.getInstance().remove(this.entry.getID());
		}

		// Redirect to Journal page
		throw new RedirectException(JournalPage.COMMAND, null);
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		UserAgent ua = getContext().getUserAgent();
		if (ua.isSmartPhone() == false)
		{
			writeEncode(getString("journey:JournalEntry.Help"));
			write("<br><br>");
		}
		
		writeFormOpen();
		
		// Photo preview
		if (this.entry.isHasPhoto())
		{
			new ImageControl(this)
				.img(this.entry.getPhoto(), BabyConsts.IMAGESIZE_BOX_400X400)
				.url(getPageURL(PhotoPage.COMMAND, new ParameterMap(PhotoPage.PARAM_ID, this.entry.getID().toString())))
				.setAttribute("width", ua.isSmartPhone()? "100%" : null)
				.setAttribute("height", ua.isSmartPhone()? "" : null)
				.render();
			write("<br>");
			new ImageInputControl(this, "photo").showThumbnail(false).setInitialValue(this.entry.getPhoto()).render();
			write("<br>");
		}
		
		// Text
		new TextAreaInputControl(this, "text")
			.setRows(3).setCols(80)
			.setMaxLength(JournalEntry.MAXSIZE_TEXT)
			.setPlaceholder(getString("journey:JournalEntry.ImageDescription"))
			.setInitialValue(this.entry.getText())
			.render();
		write("<br>");
		
		// Image uploader
		if (this.entry.isHasPhoto() == false)
		{
			new ImageInputControl(this, "photo").showThumbnail(false).setInitialValue(this.entry.getPhoto()).render();
		}
		
		// Postbacks
		writeHiddenInput(PARAM_ID, null);
		
		// Measure records
		if (this.momRecords != null || this.babyRecords != null)
		{
			
		}
		
		
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
