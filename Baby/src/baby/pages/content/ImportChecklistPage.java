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
import baby.database.Checklist;
import baby.database.ChecklistStore;
import baby.pages.BabyPage;

public class ImportChecklistPage extends BabyPage
{
	public final static String COMMAND = BabyPage.COMMAND_CONTENT + "/import-checklist";
	
	private List<Checklist> imported = new ArrayList<Checklist>();
	
	@Override
	public String getTitle() throws Exception
	{
		return getString("content:ImportChecklist.Title");
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
		
		writeEncode(getString("content:ImportChecklist.UploadHelp"));
		write("<br><br><small><pre>");
		writeEncode(getString("content:ImportChecklist.Sample"));
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
		writeButton("upload", getString("content:ImportChecklist.Import"));
		
		writeFormClose();
	}
	
	private void renderFileList() throws Exception
	{
		writeEncode(getString("content:ImportChecklist.ImportHelp", this.imported.size()));
		write("<br><br>");

		write("<table>");
		for (Checklist c : this.imported)
		{
			write("<tr><td>");
			writeLink(c.getTitle(), getPageURL(EditChecklistPage.COMMAND, new ParameterMap(EditChecklistPage.PARAM_ID, c.getID())));
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
					InputStream html = zipFile.getInputStream(entry);	
					Checklist checklist = ChecklistStore.getInstance().importFromStream(html);
					html.close();
					
					this.imported.add(checklist);
				}
			}
			
			zipFile.close();
		}
	}
}
