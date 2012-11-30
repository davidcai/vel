package samoyan.controls;

import java.io.File;
import java.util.Locale;

import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.servlet.RequestContext;
import samoyan.servlet.WebPage;

public class ImageInputControl
{
	private WebPage outputPage;
	private String name;
	private Image initialValue;

	public ImageInputControl(WebPage outputPage, String name)
	{
		this.outputPage = outputPage;
		this.name = name;
		
		this.initialValue = null;
	}
	
	public ImageInputControl setInitialValue(Image val)
	{
		this.initialValue = val;
		return this;
	}
	public Image getInitialValue()
	{
		return this.initialValue;
	}
	
	public void render()
	{
		WebPage out = this.outputPage;
		RequestContext ctx = out.getContext();
		
		boolean canUploadNew = (ctx.getUserAgent().isAppleTouch()==false ||
								(ctx.getUserAgent().isIOS() && ctx.getUserAgent().getVersionIOS()>=6F));
		boolean visibleBrowseButton = ctx.getUserAgent().isMSIE(); // && ctx.getUserAgent().getVersionMSIE()<9;
		
		String state = ctx.getParameter("_state_"+this.name);
		if (state==null)
		{
			state = "empty";
			if (this.initialValue!=null)
			{
				state = "current";
			}
		}
		
		if (this.initialValue!=null)
		{
			out.writeHiddenInput("_current_"+this.name, this.initialValue.getID().toString());
		}
		File postedFile = ctx.getPostedFile(this.name);
		if (postedFile!=null)
		{
			// Set new hidden value
			out.write("<input type=hidden name=\"_uploaded_");
			out.writeEncode(this.name);
			out.write("\" value=\"");
			out.writeEncode(postedFile.getName());
			out.write("\">");
			out.write("<input type=hidden name=\"_uploadedname_");
			out.writeEncode(this.name);
			out.write("\" value=\"");
			out.writeEncode(ctx.getParameter(this.name));
			out.write("\">");
			state = "uploaded";
		}
		else if (state.equals("uploaded"))
		{
			// Repost previously posted value
			out.writeHiddenInput("_uploaded_"+this.name, null);
			out.writeHiddenInput("_uploadedname_"+this.name, null);
		}
		
		// Set new state
		out.write("<input type=hidden name=\"_state_");
		out.writeEncode(this.name);
		out.write("\" value=\"");
		out.writeEncode(state);
		out.write("\">");
				
		out.write("<table class=\"State");
		out.writeEncode(state.substring(0, 1).toUpperCase(Locale.US));
		out.writeEncode(state.substring(1));
		out.write(" ImageUploader\"><tr><td rowspan=2 class=Images>");
		
		// Images
		String error = "";
		if (out.isFormException(this.name))
		{
			error = " Error";
		}
		out.write("<img class=\"Empty");
		out.write(error);
		out.write("\" src=\"");
		out.write(out.getResourceURL("imgupload-empty.png"));
		out.write("\">");
		if (this.initialValue!=null)
		{
			out.write("<img class=\"Current");
			out.write(error);
			out.write("\" src=\"");
			out.write(out.getImageURL(this.initialValue, Image.SIZE_THUMBNAIL, null));
			out.write("\">");
		}
		if (canUploadNew)
		{
			out.write("<img class=\"New");
			out.write(error);
			out.write("\" src=\"");
			out.write(out.getResourceURL("imgupload-new.png"));
			out.write("\">");
		}
		if (state.equals("uploaded"))
		{
			out.write("<img class=\"Uploaded");
			out.write(error);
			out.write("\" src=\"");
			out.write(out.getResourceURL("imgupload-new.png"));
			out.write("\">");
		}
		
		out.write("</td><td class=Descs valign=top>");
		
		// Descriptions
		out.write("<span class=Empty>");
		out.writeEncode(out.getString("controls:ImageUploader.NoImage"));
		out.write("</span>");
		if (this.initialValue!=null)
		{
			out.write("<span class=Current>");
			out.write(this.initialValue.getWidth());
			out.write(" x ");
			out.write(this.initialValue.getHeight());
			out.write("<br>");
			out.write(this.initialValue.getLengthBytes() / 1024);
			out.write(" KB");
			out.write("</span>");
		}
		if (canUploadNew)
		{
			out.write("<span class=New>?</span>");
		}
		if (state.equals("uploaded"))
		{
			out.write("<span class=Uploaded>");
			String postedFileName = ctx.getParameter(this.name);
			if (Util.isEmpty(postedFileName))
			{
				postedFileName = ctx.getParameter("_uploadedname_"+this.name);
			}
			if (!Util.isEmpty(postedFileName))
			{
				int p = postedFileName.lastIndexOf("\\");
				if (p>=0)
				{
					postedFileName = postedFileName.substring(p+1);
				}
				p = postedFileName.lastIndexOf("/");
				if (p>=0)
				{
					postedFileName = postedFileName.substring(p+1);
				}
				out.writeEncode(postedFileName);
			}
			out.write("</span>");
		}
		
		out.write("</td></tr><tr><td valign=bottom class=Actions>");
		
		// Actions
		if (canUploadNew)
		{
			if (visibleBrowseButton==false)
			{
				// The Browse... button is hidden and is opened with Javascript (preferred behavior)
				out.write("<span upload class=\"Current New Uploaded Empty\" onclick=\"imgUploadNew('");
				out.writeEncode(this.name);
				out.write("',true);\">");
				out.writeEncode(out.getString("controls:ImageUploader.UploadNew"));
								
				out.write("</span>");
				
				out.write("<input type=file value=\"\" name=\"");
				out.writeEncode(this.name);
				out.write("\" accept=\"image/*\" class=NoShow>");
			}
			else
			{
				// The Browse... button is visible and is opened by click
				out.write("<span upload class=\"Current New Uploaded Empty NoHover\">");
				
				out.write("<input type=file value=\"\" name=\"");
				out.writeEncode(this.name);
				out.write("\" accept=\"image/*\" size=10 onclick=\"imgUploadNew('");
				out.writeEncode(this.name);
				out.write("',false);\">");
				
				out.write("</span>");
			}
		}
		else
		{
			out.write("<input type=hidden value=\"\" name=\"");
			out.writeEncode(this.name);
			out.write("\">");
		}
		out.write("<span delete class=\"Current New Uploaded\" onclick=\"imgUploadClear('");
		out.writeEncode(this.name);
		out.write("');\">");
		out.writeEncode(out.getString("controls:ImageUploader.Clear"));
		out.write("</span>");
		if (this.initialValue!=null)
		{
			out.write("<span undo class=\"New Empty Uploaded\" onclick=\"imgUploadUndo('");
			out.writeEncode(this.name);
			out.write("');\">");
			out.writeEncode(out.getString("controls:ImageUploader.Undo"));
			out.write("</span>");
		}
		
		out.write("</td></tr></table>");
		
//		if (!canUploadNew)
//		{
//			out.write("<table><tr><td>");
//			out.writeImage("icons/basic1/warning_16.png", getString("controls:ImageUploader.UnsupportedDevice"));
//			out.write("</td><td><small>");
//			out.writeEncode(getString("controls:ImageUploader.UnsupportedDevice"));
//			out.write("</small></td></tr></table>");
//		}
	}

}
