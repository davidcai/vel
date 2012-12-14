package baby.pages.content;

import samoyan.servlet.exc.PageNotFoundException;
import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.pages.BabyPage;

public class ResourcePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/res";
	public final static String PARAM_ID = "id";
	
	private Article res = null;
	
	@Override
	public void init() throws Exception
	{
		this.res = ArticleStore.getInstance().load(getParameterUUID(PARAM_ID));
		if (this.res==null || this.res.getSection().equals(BabyConsts.SECTION_RESOURCE)==false)
		{
			throw new PageNotFoundException();
		}
	}
	
	@Override
	public String getTitle() throws Exception
	{
		return this.res.getTitle();
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		write(this.res.getHTML());
	}
}
