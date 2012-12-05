package baby.pages.content;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import samoyan.controls.ControlArray;
import samoyan.controls.SelectInputControl;
import samoyan.controls.TwoColFormControl;
import samoyan.core.ParameterMap;
import samoyan.core.Util;
import samoyan.core.image.JaiImage;
import samoyan.database.Image;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.WebFormException;
import baby.app.BabyConsts;
import baby.database.Article;
import baby.database.ArticleStore;
import baby.database.Stage;
import baby.pages.BabyPage;

public class ImportArticlePage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/import-article";
	
	private List<Article> imported = new ArrayList<Article>();
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:ImportArticle.Title");
	}
	
	@Override
	public void renderHTML() throws Exception
	{
		if (isCommitted())
		{
			renderFileList();
		}
		else
		{
			renderUpload();
		}
	}
		
	private void renderUpload() throws Exception
	{
		writeFormOpen();
		
		writeEncode(getString("content:ImportArticle.UploadHelp"));
		write("<br><br>");
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		twoCol.writeRow(getString("content:ImportArticle.ZipFile"));
		new ControlArray<Void>(twoCol, "zips", null)
		{
			@Override
			public void renderRow(int rowNum, Void rowElement) throws Exception
			{
				write("<input type=file name=zip");
				write(rowNum);
				write(" accept=\"application/zip\">");
			}
		}.render();

		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("content:ImportArticle.Section"));
		SelectInputControl select = new SelectInputControl(twoCol, "section");
		select.setInitialValue(BabyConsts.SECTION_INFO);
		select.addOption(BabyConsts.SECTION_INFO, BabyConsts.SECTION_INFO);
		for (String s : BabyConsts.SECTIONS_APPOINTMENT)
		{
			select.addOption(s,s);
		}
		select.render();

		twoCol.writeRow(getString("content:ImportArticle.Timeline"));

		SelectInputControl from = new SelectInputControl(twoCol, "from");
		populateTimelineCombo(from);
		from.render();
		
		twoCol.write(" ");
		twoCol.writeEncode(getString("content:ImportArticle.Through"));
		twoCol.write(" ");
				
		SelectInputControl to = new SelectInputControl(twoCol, "to");
		populateTimelineCombo(to);
		to.render();

		twoCol.render();
				
		write("<br>");
		writeButton("upload", getString("content:ImportArticle.Import"));
		
		writeFormClose();
	}
	
	private void renderFileList() throws Exception
	{
		writeEncode(getString("content:ImportArticle.ImportHelp", this.imported.size()));
		write("<br><br>");

		write("<table>");
		for (Article a : this.imported)
		{
			write("<tr><td>");
			writeLink(a.getTitle(), getPageURL(EditArticlePage.COMMAND, new ParameterMap(EditArticlePage.PARAM_ID, a.getID())));
			write("</td></tr>");
		}
		write("</table>");
	}
	
	private void populateTimelineCombo(SelectInputControl select)
	{
		select.addOption("", 0);

		select.addOption(getString("content:ImportArticle.Preconception"), Stage.preconception().toInteger());
		for (int i=1; i<=40; i++)
		{
			select.addOption(getString("content:ImportArticle.Pregnancy", i), Stage.pregnancy(i).toInteger());
		}
		for (int i=1; i<=12; i++)
		{
			select.addOption(getString("content:ImportArticle.Infancy", i), Stage.infancy(i).toInteger());
		}
	}
	
	@Override
	public void validate() throws Exception
	{
		RequestContext ctx = getContext();
		
		// Validate ZIP files
		int numZips = getParameterInteger("zips");
		for (int i=0; i<numZips; i++)
		{
			File file = ctx.getPostedFile("zip" + i);
			if (file!=null)
			{
				try
				{
					ZipFile zipFile = new ZipFile(file);
					zipFile.close();
				}
				catch (Exception e)
				{
					throw new WebFormException("zip", getString("common:Errors.InvalidValue"));
				}
			}
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
	
	@Override
	public void commit() throws Exception
	{
		RequestContext ctx = getContext();

		int numZips = getParameterInteger("zips");
		for (int fn=0; fn<numZips; fn++)
		{
			File file = ctx.getPostedFile("zip" + fn);
			if (file==null) continue;

			ZipFile zipFile = new ZipFile(file);
			Enumeration<ZipEntry> entriesEnum = (Enumeration<ZipEntry>) zipFile.entries();
			while (entriesEnum.hasMoreElements())
			{
				ZipEntry entry = entriesEnum.nextElement();
				if (entry.getName().endsWith(".html"))
				{
					String baseName = entry.getName().substring(0, entry.getName().length()-5);
					String title = baseName;
					String body = "";
					
					byte[] bytes = Util.inputStreamToBytes(zipFile.getInputStream(entry));
					String html;
					if (bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF) // EF BB BF - UTF-8
					{
						html = Util.inputStreamToString(zipFile.getInputStream(entry), "UTF-8");
					}
					else
					{
						html = Util.inputStreamToString(zipFile.getInputStream(entry), "ISO-8859-1");
					}
					
					int p = html.indexOf("<title>");
					if (p>=0)
					{
						p += 7;
						int q = html.indexOf("</title>", p);
						if (q>=0)
						{
							title = Util.htmlDecode(html.substring(p, q));
						}
					}
					
					p = html.indexOf("<body>");
					if (p>=0)
					{
						p += 6;
						int q = html.indexOf("</body>", p);
						if (q>=0)
						{
							body = Util.htmlDecode(html.substring(p, q));
						}
					}
	
					JaiImage jai = null;
					String[] imgExt = {".png", ".jpg", ".jpeg"};
					for (int i=0; i<imgExt.length; i++)
					{
						ZipEntry imgEntry = zipFile.getEntry(baseName + imgExt[i]);
						if (imgEntry!=null)
						{
							jai = new JaiImage(Util.inputStreamToBytes(zipFile.getInputStream(imgEntry)));
							break;
						}
					}
					
					Article article = new Article();
					article.setSection(getParameterString("section"));
					article.setTitle(title);
					article.setHTML(body);
					article.setPriority(0);
					article.setTimelineFrom(getParameterInteger("from"));
					article.setTimelineTo(getParameterInteger("to"));
					article.setUpdatedDate(new Date());
					if (jai!=null)
					{
						article.setPhoto(new Image(jai));
					}
					
					ArticleStore.getInstance().save(article);
					
					this.imported.add(article);
				}
			}
			
			zipFile.close();
		}
	}
}
