package baby.controls;

import samoyan.servlet.RequestContext;
import samoyan.servlet.UserAgent;
import samoyan.servlet.WebPage;

/**
 * A simple control for uploading an image. Should not be included more than once in the same page due to hard-coded DOM IDs.
 * @author brianwillis
 *
 */
public class SimpleImageUploadControl
{
	private WebPage out;
	private String name;
	
	public SimpleImageUploadControl(WebPage outputPage, String name)
	{
		this.out = outputPage;
		this.name = name;
	}
	
	public void render() throws Exception
	{
		// IE doesn't allow Javascript to manipulate the INPUT TYPE=FILE control
		RequestContext ctx = out.getContext();
		UserAgent ua = ctx.getUserAgent();
		boolean canUploadNew = (ctx.getUserAgent().isAppleTouch()==false ||
				(ctx.getUserAgent().isIOS() && ctx.getUserAgent().getVersionIOS()>=6F));
		if (!canUploadNew)
		{
			return;
		}
		
		out.write("<table><tr>");
		
		// Image
		out.write("<td>");
		out.writeImage("icons/standard/photo-camera-24.png", out.getString("baby:UploadCtrl.UploadImage"), ua.isMSIE()? null : "javascript:$('#upload').click();");
		out.write("</td>");

		// Upload control
		out.write("<td>");
		if (!ua.isMSIE())
		{
			out.write("<span class=NoShow>");
		}
		out.write("<input type=file id=upload value=\"\" name=\"");
		out.writeEncode(name);
		out.write("\" accept=\"image/*\" size=20");
		if (!ua.isMSIE())
		{
			// Auto-submit form
			out.write(" onchange=\"var bs=$(this).val().lastIndexOf('\\\\');var s=$(this).val().lastIndexOf('/');$('#label').html($(this).val().substring(Math.max(bs+1,s+1,0)));\"");
		}
		out.write(">");
		if (!ua.isMSIE())
		{
			out.write("</span>");
			
			out.write("<span id=label>");
			out.writeLink(out.getString("baby:UploadCtrl.UploadImage"), "javascript:$('#upload').click();");
			out.write("</span>");
		}
		out.write("</td>");		
		
		out.write("</tr></table>");
	}
}
