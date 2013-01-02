package baby.pages.content;

import java.util.Date;

import samoyan.controls.ImageControl;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.servlet.RequestContext;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.RedirectException;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public final class EditArticlePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/article";
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
			return getString("content:EditArticle.Title");
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
		if (this.article.isByCrawler())
		{
			renderReadOnly();
		}
		else
		{
			renderEditable();
		}
	}
	
	private void renderReadOnly() throws Exception
	{
		UserAgent ua = getContext().getUserAgent();

		write("<div class=Article>");
				
		if (!Util.isEmpty(this.article.getYouTubeVideoID()))
		{
			write("<div align=center>");
			int width = 600;
			if (width > ua.getScreenWidth())
			{
				width = ua.getScreenWidth() - 10;
			}
			int height = width * 2 / 3;
			writeYouTubeVideo(this.article.getYouTubeVideoID(), width, height);
			write("</div><br>");
		}
		
		if (this.article.getPhoto()!=null)
		{
			new ImageControl(this)
				.img(this.article.getPhoto(), getContext().getUserAgent().isSmartPhone()? BabyConsts.IMAGESIZE_BOX_150X150 : BabyConsts.IMAGESIZE_BOX_400X400)
				.setAttribute("align", "right")
				.render();
		}
		
		write(this.article.getHTML());
		
		write("</div>");
	}
	
	private void renderEditable() throws Exception
	{
		writeFormOpen();
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		
		twoCol.writeRow(getString("content:EditArticle.ArticleTitle"));
		twoCol.writeTextInput("title", this.article.getTitle(), 80, Article.MAXSIZE_TITLE);
		
		twoCol.writeRow(getString("content:EditArticle.Summary"));
		twoCol.writeTextAreaInput("summary", this.article.getSummaryRaw(), 80, 2, Article.MAXSIZE_SUMMARY);

		twoCol.writeRow(getString("content:EditArticle.Body"));
		twoCol.writeRichEditField("body", this.article.getHTML(), 80, 10);

		twoCol.writeRow(getString("content:EditArticle.Image"));
		twoCol.writeImageInput("image", this.article.getPhoto());

		twoCol.writeRow(getString("content:EditArticle.YouTube"));
		twoCol.writeTextInput("youtube", this.article.getYouTubeVideoID(), 20, Article.MAXSIZE_YOUTUBE);

		twoCol.writeRow(getString("content:EditArticle.Options"));
		twoCol.writeCheckbox("pinned", getString("content:EditArticle.Pinned"), this.article.getPriority()>0);
		
		twoCol.writeRow(getString("content:EditArticle.Section"));
		SelectInputControl select = new SelectInputControl(twoCol, "section");
		select.setInitialValue(this.article.getSection());
		select.addOption(BabyConsts.SECTION_INFO, BabyConsts.SECTION_INFO);
		select.addOption(BabyConsts.SECTION_RESOURCE, BabyConsts.SECTION_RESOURCE);
		for (String s : BabyConsts.SECTIONS_APPOINTMENT)
		{
			select.addOption(s,s);
		}
		select.render();

		twoCol.writeRow(getString("content:EditArticle.SubSection"));
		twoCol.writeTextInput("subsection", this.article.getSubSection(), 40, Article.MAXSIZE_SUBSECTION);

		twoCol.writeRow(getString("content:EditArticle.Timeline"));

		SelectInputControl from = new SelectInputControl(twoCol, "from");
		populateTimelineCombo(from);
		from.setInitialValue(this.article.getTimelineFrom());
		from.render();
		
		twoCol.write(" ");
		twoCol.writeEncode(getString("content:EditArticle.Through"));
		twoCol.write(" ");
				
		SelectInputControl to = new SelectInputControl(twoCol, "to");
		populateTimelineCombo(to);
		to.setInitialValue(this.article.getTimelineTo());
		to.render();

		twoCol.writeRow(getString("content:EditArticle.Region"));
		twoCol.writeTypeAheadInput("region", this.article.getRegion(), this.article.getRegion(), 40, Article.MAXSIZE_REGION, getPageURL(RegionTypeAhead.COMMAND));
		
		twoCol.writeRow(getString("content:EditArticle.MedicalCenter"));
		twoCol.writeTypeAheadInput("medicalcenter", this.article.getMedicalCenter(), this.article.getMedicalCenter(), 40, Article.MAXSIZE_REGION, getPageURL(MedicalCenterTypeAhead.COMMAND));

		twoCol.render();

		write("<br>");
		writeSaveButton("save", this.article);
		
		writeHiddenInput(PARAM_ID, null);
		
		writeFormClose();
	}
	
	private void populateTimelineCombo(SelectInputControl select)
	{
		select.addOption(getString("content:EditArticle.Preconception"), Stage.preconception().toInteger());
		for (int i=1; i<=40; i++)
		{
			select.addOption(getString("content:EditArticle.Pregnancy", i), Stage.pregnancy(i).toInteger());
		}
		for (int i=1; i<=12; i++)
		{
			select.addOption(getString("content:EditArticle.Infancy", i), Stage.infancy(i).toInteger());
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		if (this.article.isByCrawler())
		{
			throw new WebFormException(getString("content:EditArticle.ReadOnly"));
		}
		
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
			throw new WebFormException(new String[] {"from", "to"}, getString("content:EditArticle.InvalidTimeline"));
		}
		
		validateParameterString("section", 1, Article.MAXSIZE_SECTION);
		validateParameterString("subsection", 0, Article.MAXSIZE_SUBSECTION);
	}
	
	@Override
	public void commit() throws Exception
	{
		boolean saved = this.article.isSaved();
		
		this.article.setTitle(getParameterString("title"));
		this.article.setSummary(getParameterString("summary"));
		
		String body = getParameterRichEdit("body");
		this.article.setHTML(body);
		
		this.article.setPhoto(getParameterImage("image"));
		this.article.setPriority(isParameter("pinned")? 100 : 0);
		this.article.setSection(getParameterString("section"));
		this.article.setSubSection(getParameterString("subsection"));
		
		this.article.setTimelineFrom(getParameterInteger("from"));
		this.article.setTimelineTo(getParameterInteger("to"));
		
		this.article.setUpdatedDate(new Date());
		this.article.setYouTubeVideoID(getParameterString("youtube"));
		
		this.article.setRegion(getParameterString("region"));
		this.article.setMedicalCenter(getParameterString("medicalcenter"));

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
}
