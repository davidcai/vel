package baby.pages.content;

import java.util.Date;

import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class EditHealthBegPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/hb";
	public final static String PARAM_ID = "id";
	
	private Article article;
	
	@Override
	public String getTitle() throws Exception
	{
		if (this.article.isSaved())
		{
			return this.article.getTitle();
		}
		else
		{
			return getString("content:EditHealthBeg.Title");
		}
	}
	
	@Override
	public void init() throws Exception
	{
		this.article = ArticleStore.getInstance().open(getParameterUUID(PARAM_ID));
		if (this.article==null)
		{
			this.article = new Article();
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{	
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("content:EditHealthBeg.ArticleTitle"));
		twoCol.writeTextInput("title", this.article.getTitle(), 80, Article.MAXSIZE_TITLE);
		
		twoCol.writeRow(getString("content:EditHealthBeg.Summary"));
		twoCol.writeTextAreaInput("summary", this.article.getSummary(), 80, 2, Article.MAXSIZE_SUMMARY);

		twoCol.writeRow(getString("content:EditHealthBeg.Body"));
		twoCol.writeRichEditField("body", this.article.getHTML(), 80, 10);

		twoCol.writeRow(getString("content:EditHealthBeg.Image"));
		twoCol.writeImageInput("image", this.article.getPhoto());

		twoCol.writeRow(getString("content:EditHealthBeg.Options"));
		twoCol.writeCheckbox("pinned", getString("content:EditHealthBeg.Pinned"), this.article.getPriority()>0);
		
		twoCol.writeRow(getString("content:EditHealthBeg.Timeline"));

		SelectInputControl from = new SelectInputControl(twoCol, "from");
		populateTimelineCombo(from);
		from.setInitialValue(this.article.getTimelineFrom());
		from.render();
		
		twoCol.write(" ");
		twoCol.writeEncode(getString("content:EditHealthBeg.Through"));
		twoCol.write(" ");
				
		SelectInputControl to = new SelectInputControl(twoCol, "to");
		populateTimelineCombo(to);
		to.setInitialValue(this.article.getTimelineTo());
		to.render();

		twoCol.render();

		write("<br>");
		writeSaveButton("save", this.article);
		if (this.article.isSaved())
		{
			write(" ");
			writeRemoveButton("remove");
		}
		
		writeHiddenInput(PARAM_ID, null);
		
		writeFormClose();
	}
	
	private void populateTimelineCombo(SelectInputControl select)
	{
		Stage stage;
		select.addOption("", 0);

		select.addOption(getString("content:EditHealthBeg.Preconception"), Stage.preconception().toInteger());
		for (int i=1; i<=40; i++)
		{
			select.addOption(getString("content:EditHealthBeg.Pregnancy", i), Stage.pregnancy(i).toInteger());
		}
		for (int i=1; i<=12; i++)
		{
			select.addOption(getString("content:EditHealthBeg.Infancy", i), Stage.infancy(i).toInteger());
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (isParameter("save"))
		{
			validateParameterString("title", 1, Article.MAXSIZE_TITLE);
			validateParameterString("summary", 0, Article.MAXSIZE_SUMMARY);
			
			String body = getParameterRichEdit("body");
			if (Util.isEmptyHTML(body))
			{
				throw new WebFormException("body", getString("common:Errors.MissingField"));
			}
			
			// Validate "from" and "to"
			Stage from = Stage.fromInteger(getParameterInteger("from"));
			if (from.isValid()==false)
			{
				throw new WebFormException("from", getString("common:Errors.MissingField"));
			}
			Stage to = Stage.fromInteger(getParameterInteger("to"));
			if (to.isValid()==false)
			{
				throw new WebFormException("to", getString("common:Errors.MissingField"));
			}
			if (from.toInteger() > to.toInteger())
			{
				throw new WebFormException(new String[] {"from", "to"}, getString("content:EditHealthBeg.InvalidTimeline"));
			}
		}
	}
	
	@Override
	public void commit() throws Exception
	{
		if (isParameter("save"))
		{
			boolean saved = this.article.isSaved();
			
			this.article.setTitle(getParameterString("title"));
			this.article.setSummary(getParameterString("summary"));
			
			String body = getParameterRichEdit("body");
			this.article.setHTML(body);
			
			this.article.setPhoto(getParameterImage("image"));
			this.article.setPriority(isParameter("pinned")? 100 : 0);
			this.article.setSection(BabyConsts.SECTION_HEALTHY_BEGINNINGS);
			
			this.article.setTimelineFrom(getParameterInteger("from"));
			this.article.setTimelineTo(getParameterInteger("to"));
			
			this.article.setUpdatedDate(new Date());
			
			ArticleStore.getInstance().save(this.article);
			
			// For now, redirect to self
			if (saved)
			{
				throw new RedirectException(getContext().getCommand(), new ParameterMap(PARAM_ID, this.article.getID().toString()).plus(RequestContext.PARAM_SAVED, ""));
			}
			else
			{
				throw new RedirectException(getContext().getCommand(), null);
			}
		}
		
		if (isParameter("remove"))
		{
			ArticleStore.getInstance().remove(this.article.getID());
			throw new RedirectException(HealthBegListPage.COMMAND, null);
		}
	}
}
