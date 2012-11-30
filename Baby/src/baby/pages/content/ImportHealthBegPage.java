package baby.pages.content;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

public class ImportHealthBegPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/import-hb";
	
	private List<Article> imported = new ArrayList<Article>();
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:ImportHealthBeg.Title");
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
		
		writeEncode(getString("content:ImportHealthBeg.UploadHelp"));
		write("<br><br>");
		
		TwoColFormControl twoCol = new TwoColFormControl(this);
		twoCol.writeRow(getString("content:ImportHealthBeg.ZipFile"));
		twoCol.write("<input type=file name=zip accept=\"application/zip\">");

		twoCol.writeSpaceRow();
		
		twoCol.writeRow(getString("content:ImportHealthBeg.Timeline"));

		SelectInputControl from = new SelectInputControl(twoCol, "from");
		populateTimelineCombo(from);
		from.render();
		
		twoCol.write(" ");
		twoCol.writeEncode(getString("content:ImportHealthBeg.Through"));
		twoCol.write(" ");
				
		SelectInputControl to = new SelectInputControl(twoCol, "to");
		populateTimelineCombo(to);
		to.render();

		twoCol.render();
				
		write("<br>");
		writeButton("upload", getString("content:ImportHealthBeg.Import"));
		
		writeFormClose();
	}
	
	private void renderFileList() throws Exception
	{
		RequestContext ctx = getContext();
				
		writeEncode(getString("content:ImportHealthBeg.ImportHelp", this.imported.size()));
		write("<br><br>");

		write("<table>");
		for (Article a : this.imported)
		{
			write("<tr><td>");
			writeLink(a.getTitle(), getPageURL(EditHealthBegPage.COMMAND, new ParameterMap(EditHealthBegPage.PARAM_ID, a.getID())));
			write("</td></tr>");
		}
		write("</table>");
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
		RequestContext ctx = getContext();
		
		// Validate ZIP file
		File file = ctx.getPostedFile("zip");
		if (file==null)
		{
			throw new WebFormException("zip", getString("common:Errors.MissingField"));
		}
		try
		{
			ZipFile zipFile = new ZipFile(file);
		}
		catch (Exception e)
		{
			throw new WebFormException("zip", getString("common:Errors.InvalidValue"));
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

		File file = ctx.getPostedFile("zip");
		ZipFile zipFile = new ZipFile(file);
		Enumeration<ZipEntry> entriesEnum = (Enumeration<ZipEntry>) zipFile.entries();
		while (entriesEnum.hasMoreElements())
		{
			ZipEntry entry = entriesEnum.nextElement();
			if (entry.getName().endsWith(".html") || entry.getName().endsWith(".htm"))
			{
				String baseName = entry.getName().substring(0, entry.getName().length()-5);
				String title = baseName;
				String body = "";
				String html = Util.inputStreamToString(zipFile.getInputStream(entry), "UTF-8");
				
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
				article.setSection(BabyConsts.SECTION_HEALTHY_BEGINNINGS);
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
	}
}
