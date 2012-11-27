package baby.pages.scrapbook;

import java.util.Date;

import samoyan.controls.TwoColFormControl;
import samoyan.database.Image;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.database.JournalEntry;
import baby.database.JournalEntryStore;
import baby.pages.BabyPage;

public class JournalEntryPage extends BabyPage
{
	public static final String COMMAND = BabyPage.COMMAND_SCRAPBOOK + "/entry";
	public static final String PARAM_ID = "id";
	public static final String PARAM_SAVE = "save";
	public static final String PARAM_REMOVE = "remove";
	
	private JournalEntry entry = null;
	
	@Override
	public void init() throws Exception
	{
		this.entry = JournalEntryStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.entry == null)
		{
			this.entry = new JournalEntry();
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter(PARAM_SAVE))
		{
			validateParameterString("text", 0, JournalEntry.MAXSIZE_TEXT);
			
			// Show errors if both text and photo have no inputs
			if (isParameterNotEmpty("text") == false && getParameterImage("photo") == null)
			{
				throw new WebFormException(new String[] {"text", "photo"}, getString("scrapbook:JournalEntry.NoInput"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		boolean isSaved = this.entry.isSaved();
		
		if (isParameter(PARAM_SAVE))
		{
			this.entry.setUserID(getContext().getUserID());
			this.entry.setText(getParameterString("text"));
			
			Image photo = getParameterImage("photo");
			this.entry.setHasPhoto(photo != null);
			this.entry.setPhoto(photo);
			
			if (isSaved == false)
			{
				this.entry.setCreated(new Date());
			}
			
			JournalEntryStore.getInstance().save(this.entry);
		}
		else if (isParameter(PARAM_REMOVE) && isSaved)
		{
			JournalEntryStore.getInstance().remove(this.entry.getID());
		}

		// Redirect to journal page
		throw new RedirectException(JournalPage.COMMAND, null);
	}

	@Override
	public String getTitle() throws Exception
	{
		return getString("scrapbook:JournalEntry.Title");
	}

	@Override
	public void renderHTML() throws Exception
	{
		RequestContext ctx = getContext();
		
		writeHorizontalNav(JournalPage.COMMAND);

		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("scrapbook:JournalEntry.Text"));
		twoCol.writeTextAreaInput("text", this.entry.getText(), 80, 5, JournalEntry.MAXSIZE_TEXT);
		twoCol.writeRow(getString("scrapbook:JournalEntry.Photo"));
		twoCol.writeImageInput("photo", this.entry.getPhoto());
		twoCol.render();
		
		write("<br>");
		writeButton(PARAM_SAVE, getString("scrapbook:JournalEntry.Save"));
		write("&nbsp;");
		writeRemoveButton(PARAM_REMOVE);
		
		// postback
		writeHiddenInput(PARAM_ID, this.entry.getID().toString());
		
		writeFormClose();
	}
}
