package baby.pages.content;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import samoyan.controls.ControlArray;
import samoyan.core.ParameterMap;
import samoyan.servlet.RequestContext;
import samoyan.servlet.exc.WebFormException;
import baby.database.Article;
import baby.database.ArticleStore;
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
		write("<br><br><small><pre>");
		writeEncode(getString("content:ImportArticle.Sample"));
		write("</pre></small>");
		write("<br>");
		
		new ControlArray<Void>(this, "zips", null)
		{
			@Override
			public void renderRow(int rowNum, Void rowElement) throws Exception
			{
				write("<input type=file name=zip");
				write(rowNum);
				write(" accept=\"application/zip\">");
			}
		}.render();
		
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
				if (entry.getName().startsWith("__MACOSX")) continue; // Hack for MacOS generated ZIPs
				
				if (entry.getName().endsWith(".html") || entry.getName().endsWith(".txt"))
				{
					String baseName = entry.getName().substring(0, entry.getName().lastIndexOf("."));

					InputStream html = zipFile.getInputStream(entry);
					InputStream img = null;
					
					String[] imgExt = {".png", ".jpg", ".jpeg"};
					for (int i=0; i<imgExt.length; i++)
					{
						ZipEntry imgEntry = zipFile.getEntry(baseName + imgExt[i]);
						if (imgEntry!=null)
						{
							img = zipFile.getInputStream(imgEntry);
							break;
						}
					}
				
					Article article = ArticleStore.getInstance().importFromStream(html, img);
					
					html.close();
					if (img!=null) img.close();
					
					this.imported.add(article);
				}
			}
			
			zipFile.close();
		}
	}
}
