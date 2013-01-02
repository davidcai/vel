package baby.pages.info;

import samoyan.controls.ImageControl;
import samoyan.core.Util;
import samoyan.servlet.UserAgent;
import samoyan.servlet.exc.PageNotFoundException;
import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.pages.BabyPage;

public class ViewArticlePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_INFORMATION + "/article";
	public final static String PARAM_ID = "id";

	private Article article;
	
	@Override
	public void init() throws Exception
	{
		this.article = ArticleStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.article==null)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		if (Util.isEmpty(this.article.getTitle()) || getContext().getUserAgent().isSmartPhone())
		{
			if (this.article.getSection().equalsIgnoreCase(BabyConsts.SECTION_RESOURCE))
			{
				return getString("information:Article.Resource");
			}
			else
			{
				return getString("information:Article.Article");
			}
		}
		else
		{
			return this.article.getTitle();
		}
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		UserAgent ua = getContext().getUserAgent();
		
//		writeEncode(this.article.getSourceURL());
//		write("<br><br>");
		
//		boolean healthyBeginnings = this.article.getSection().equals(BabyConsts.SECTION_INFO);
//		writeHorizontalNav(healthyBeginnings? ViewArticleListPage.COMMAND : ViewResourceListPage.COMMAND);
		
		write("<div class=Article>");
		
		if (!Util.isEmpty(this.article.getSubSection()))
		{
			write("<div class=Subsection>");
			writeEncode(this.article.getSubSection());
			write("</div>");
		}
		
		if (ua.isSmartPhone() && !Util.isEmpty(this.article.getTitle()))
		{
			write("<h2>");
			writeEncode(this.article.getTitle());
			write("</h2>");
		}
		
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
}
