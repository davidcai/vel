package samoyan.controls;

import java.io.FileNotFoundException;
import java.io.InputStream;

import samoyan.core.Cache;
import samoyan.core.Util;
import samoyan.core.image.JaiImage;
import samoyan.database.Image;
import samoyan.database.ImageStore;
import samoyan.servlet.Controller;
import samoyan.servlet.UrlGenerator;
import samoyan.servlet.WebPage;

public class ImageControl extends TagControl
{
	private WebPage out;
	private Image image = null;
	private String size = null;
	private String resName = null;
	private int width = 0;
	private int height = 0;
	private int overrideWidth = 0;
	private int overrideHeight = 0;
	private String url = null;
	private String altText = null;
	
	public ImageControl(WebPage outputPage)
	{
		super(outputPage);
		this.out = outputPage;
	}
	
	public ImageControl altText(String text)
	{
		this.altText = text;
		return this;
	}
	
	public ImageControl url(String url)
	{
		this.url = url;
		return this;
	}

	public ImageControl width(int width)
	{
		this.overrideWidth = width;
		return this;
	}

	public ImageControl height(int height)
	{
		this.overrideHeight = height;
		return this;
	}

	public ImageControl img(Image image, String sizeSpec) throws Exception
	{
		float ratio = out.getContext().getUserAgent().getPixelRatio();
		Image resized = ImageStore.getInstance().loadAndResize(image.getID(), sizeSpec, ratio);
		
		this.width = Math.round( resized.getWidth() / ratio );
		this.height = Math.round( resized.getHeight() / ratio );		
		this.image = image;
		this.size = sizeSpec;
		
		return this;
	}
	
	public ImageControl resource(String resourceFileName) throws Exception
	{
		class Rect
		{
			int width;
			int height;
		}
		
		Rect rect = (Rect) Cache.get("res.dim:" + resourceFileName);
		if (rect==null)
		{
			InputStream stm = Controller.getResourceAsStream(UrlGenerator.COMMAND_RESOURCE + "/" + resourceFileName);
			if (stm==null)
			{
				throw new FileNotFoundException("Resource image not found: " + resourceFileName);
			}
			JaiImage jai = new JaiImage(Util.inputStreamToBytes(stm));
			rect = new Rect();
			rect.width = jai.getWidth();
			rect.height = jai.getHeight();
			Cache.insert("res.dim:" + resourceFileName, rect);
		}
		
		this.width = rect.width;
		this.height = rect.height;
		
		this.resName = resourceFileName;
		
		return this;
	}
	
	public void render()
	{
		String src = null;
		if (this.image!=null)
		{
			src = out.getImageURL(this.image, this.size, this.altText);	
		}
		else if (this.resName!=null)
		{
			src = out.getResourceURL(this.resName);
		}
		else
		{
			src = out.getResourceURL("blank.png");
		}
		
		// Write the optional A tag
		if (this.url!=null)
		{
			out.write("<a href=\"");
			out.writeEncode(this.url);
			out.write("\">");
		}
		
		// Write the IMG tag
		super.setAttribute("src", src);
		super.setAttribute("alt", this.altText);
		if (this.overrideHeight<=0 && this.overrideWidth<=0)
		{
			if (this.width>0 && super.getAttribute("width")==null)
			{
				super.setAttribute("width", String.valueOf(this.width));
			}
			if (this.height>0 && super.getAttribute("height")==null)
			{
				super.setAttribute("height", String.valueOf(this.height));
			}
		}
		else
		{
			if (this.overrideWidth>0 && super.getAttribute("width")==null)
			{
				super.setAttribute("width", String.valueOf(this.overrideWidth));
			}
			if (this.overrideHeight>0 && super.getAttribute("height")==null)
			{
				super.setAttribute("height", String.valueOf(this.overrideHeight));
			}
		}
		super.writeTag("img");

		// Close the optional A tag
		if (this.url!=null)
		{
			out.write("</a>");
		}
	}	
}
