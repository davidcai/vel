package samoyan.controls;

import java.util.ArrayList;
import java.util.List;

import samoyan.core.Util;
import samoyan.database.Image;
import samoyan.servlet.WebPage;

/**
 * Control for rendering a vertical array of links, typically used for navigation in mobile apps.
 * @author brianwillis
 *
 */
public class WideLinkGroupControl
{	
	public class WideLink
	{
		private String title;
		private String value;
		private String extra;
		private String resourceImage;
		private Image image;
		private String imageSizeSpec;
		private String imageAltText;
		private String imageURL;
		private String url;
		
		private WideLink() {}

		public String getTitle()
		{
			return title;
		}

		public WideLink setTitle(String title)
		{
			this.title = title;
			return this;
		}

		public String getValue()
		{
			return value;
		}

		public WideLink setValue(String value)
		{
			this.value = value;
			return this;
		}

		public String getExtra()
		{
			return extra;
		}

		public WideLink setExtra(String extra)
		{
			this.extra = extra;
			return this;
		}

		public String getResourceImage()
		{
			return resourceImage;
		}

		public WideLink setResourceImage(String resourceImage)
		{
			this.resourceImage = resourceImage;
			return this;
		}
		
		public Image getImage()
		{
			return image;
		}
		
		public WideLink setImage(Image image)
		{
			this.image = image;
			return this;
		}
		
		public String getImageSizeSpec()
		{
			return imageSizeSpec;
		}

		public WideLink setImageSizeSpec(String imageSizeSpec)
		{
			this.imageSizeSpec = imageSizeSpec;
			return this;
		}

		public String getImageAltText()
		{
			return imageAltText;
		}

		public WideLink setImageAltText(String imageAltText)
		{
			this.imageAltText = imageAltText;
			return this;
		}

		public String getURL()
		{
			return url;
		}

		public WideLink setURL(String url)
		{
			this.url = url;
			return this;
		}
	}

	private WebPage out;
	private List<WideLink> links = new ArrayList<WideLink>();

	public WideLinkGroupControl(WebPage out)
	{
		this.out = out;
	}
	
	public WideLink addLink()
	{
		WideLink wl = new WideLink();
		links.add(wl);
		return wl;
	}
	
	public void render() throws Exception
	{
		out.write("<div class=WideLinks>");
		
//		<a href="http://google.com">
//		<!--img src="icon.png"-->
//		<span class=Title>Real name</span>
//		<span class=Value>John Crane John Crane John Crane John Crane </span>
//		<br style="clear:right;">
//		<span class=Extra>Small text Small text Small text Small text Small text Small text Small text Small text Small text Small text Small text Small text </span>
//		<span style="clear:both;"></span>
//		</a>

		for (WideLink wl : this.links)
		{
			// A or DIV
			if (!Util.isEmpty(wl.getURL()))
			{
				out.write("<a href=\"");
				out.writeEncode(wl.getURL());
				out.write("\">");
			}
			else
			{
				out.write("<div>");
			}
			
			// Image triumphs over resource image
			if (wl.getImage() != null)
			{
				// Image
				out.writeImage(wl.getImage(), wl.getImageSizeSpec(), wl.getImageAltText(), null);
			}
			else if (!Util.isEmpty(wl.getResourceImage()))
			{
				// Resource image
				out.writeImage(wl.getResourceImage(), wl.getTitle());
			}
			
			// Title
			if (!Util.isEmpty(wl.getTitle()))
			{
				out.write("<span class=Title>");
				out.writeEncode(wl.getTitle());
				out.write("</span>");
			}
			
			// Value
			if (!Util.isEmpty(wl.getValue()))
			{
				out.write("<span class=Value>");
				out.writeEncode(wl.getValue());
				out.write("</span>");
			}
			
			// Extra
			if (!Util.isEmpty(wl.getExtra()))
			{
				out.write("<div class=Extra>");
				out.writeEncode(wl.getExtra());
				out.write("</div>");
			}
			
			// Clear
			out.write("<span></span>");
			
			// Close A or DIV
			if (!Util.isEmpty(wl.getURL()))
			{
				out.write("</a>");
			}
			else
			{
				out.write("</div>");
			}
		}
		
		out.write("</div>"); // WideLinks
	}
}
