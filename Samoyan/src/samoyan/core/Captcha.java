package samoyan.core;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.media.jai.JAI;

import samoyan.servlet.Setup;

import com.jhlabs.image.RippleFilter;
import com.jhlabs.image.ShadowFilter;
import com.jhlabs.image.TransformFilter;
import com.sun.media.jai.codec.JPEGEncodeParam;

public class Captcha
{
	private static Map<UUID, Captcha> captchas = new ConcurrentHashMap<UUID, Captcha>();
	private static long lastCleanup = System.currentTimeMillis();
	private static String FONT_FACE = "Arial"; //"Courier New";
	
	private Random rand = new Random();
	private long expires;
	private String text;
	private UUID key;
	private int width;
	private int height;
	
	private Captcha()
	{
		this.expires = System.currentTimeMillis() + Setup.getSessionLength();
		this.key = UUID.randomUUID();
		this.width = 180;
		this.height = 70;
		
//		// 6 letter code
//		this.text = "";
//		String x = "abcdefghijklmnopqrstuvwxyz";
//		for (int i=0; i<6; i++)
//		{
//			this.text += x.charAt(rand.nextInt(x.length()));
//		}
		
		// 6 digit code
		this.text = String.valueOf(rand.nextInt(1000000));
		while (this.text.length()<6)
		{
			this.text = "0" + this.text;
		}
	}
	
	public static Captcha createCaptcha()
	{
		return createCaptcha(null);
	}
	
	public static Captcha createCaptcha(String text)
	{
		Captcha cap = new Captcha();
		if (text!=null)
		{
			cap.setText(text);
		}
		
		captchas.put(cap.getKey(), cap);
		
		cleanup();
		
		return cap;
	}

	public static Captcha getByKey(UUID key)
	{
		cleanup();

		return captchas.get(key);
	}
	
//	public static void disposeByKey(String key)
//	{
//		cleanup();
//
//		lock.lockWrite();
//		try
//		{
//			captchas.remove(key);
//		}
//		finally
//		{
//			lock.unlockWrite();
//		}
//	}
	
	public UUID getKey()
	{
		return this.key;
	}
	
	public String getText()
	{
		return this.text;
	}
	
	public void setText(String text)
	{
		this.text = text;
	}

	public int getHeight()
	{
		return this.height;
	}
	public void setHeight(int height)
	{
		this.height = height;
	}
	public int getWidth()
	{
		return this.width;
	}
	public void setWidth(int width)
	{
		this.width = width;
	}
	
	private static void cleanup()
	{
		long now = System.currentTimeMillis();
		if (lastCleanup + Setup.getSessionLength()/2 > now) return;
		
		Iterator<Captcha> iter = captchas.values().iterator();
		while (iter.hasNext())
		{
			Captcha cap = iter.next();
			if (cap.expires <= now)
			{
				iter.remove();
			}
		}
		
		lastCleanup = now;
	}
	
	/**
	 * Create an image which have witten a distorted text, text given 
	 * as parameter. The result image is put on the output stream
	 * 
	 * @param stream the OutputStrea where the image is written
	 * @throws IOException if an error occurs during the image written on
	 * output stream.
	 */
	public void createImage(OutputStream os) throws IOException
	{
		BufferedImage bi = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
		
//		Color fontColor = new Color(255,200,100);
		
		// Yellow/red
//		Color gradFrom = new Color(255,255,200);
//		Color gradTo = new Color(255,200,200);
//		Color fontColor = new Color(255,100,100);
//		Color shadowColor = Color.BLACK;
		
		// Blue/green
//		Color gradFrom = new Color(190,197,201);
//		Color gradTo = new Color(237,246,251);
//		Color fontColor = new Color(255,221,134);
//		Color shadowColor = Color.BLACK;

		// Black/white
		Color gradFrom = new Color(0xcc,0xcc,0xcc);
		Color gradTo = new Color(0x99,0x99,0x99);
		Color fontColor = new Color(255,255,255);
		Color shadowColor = Color.BLACK;

		// Put the text on the image
		bi = renderWord(bi, text, FONT_FACE, fontColor);
		
		// Distort the image		
		bi = distortShadow(bi, shadowColor);
//		bi = distortRipple(bi);
		
		bi = distortFishEye(bi, fontColor, fontColor);

		bi = rotate(bi);

		// Add background
//		bi = addGradientBackground(bi, new Color(200,200,255), new Color(200,255,200)); // !$!
		bi = addGradientBackground(bi, gradFrom, gradTo);
        
		// Make some noise
		bi = makeNoise(bi, .2f, .2f, .3f, .4f, shadowColor);
		bi = makeNoise(bi, .1f, .25f, .5f, .9f, shadowColor);
		bi = makeNoise(bi, .5f, .6f, .7f, .8f, shadowColor);

		// Encode as JPG
		JPEGEncodeParam param = new JPEGEncodeParam();
		param.setQuality(0.75F);		
		JAI.create("encode", bi, os, "jpeg", param);
	}

	/** 
	 * Render a word to a BufferedImage.
	 * 
	 * @param word The word to be rendered.
	 * @param width The width of the image to be created.
	 * @param height The heigth of the image to be created.
	 * @return The BufferedImage created from the word,
	 */
	private BufferedImage renderWord (BufferedImage image, String word, String fontName, Color fontColor)
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            
//		GraphicsDevice gd = ge.getDefaultScreenDevice();
//		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		

		Graphics2D g2D = image.createGraphics();
		g2D.setColor(Color.BLACK);
        
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
		g2D.setRenderingHints(hints);
		
		Font font = new Font(fontName, Font.BOLD, image.getHeight() * 3 / 4); // !$!
				
		char[] wc = word.toCharArray();
		g2D.setColor(fontColor);
		g2D.setFont(font);
		FontRenderContext frc = g2D.getFontRenderContext();
		int totalWidth = 0;
		int maxHeight = 0;
		for (int i = 0; i<wc.length; i++)
		{
			char[] itchar = new char[]{wc[i]};
			LineMetrics lmet = font.getLineMetrics(itchar,0,itchar.length,frc);
			GlyphVector gv = font.createGlyphVector(frc, itchar);
			
			totalWidth += (int) (gv.getVisualBounds().getWidth() + 3);
			maxHeight = Math.max(maxHeight, (int) gv.getVisualBounds().getHeight());
		}
		
		int startPosX = (image.getWidth() - totalWidth) / 2;
		int startPosY = (image.getHeight() - maxHeight) / 2 + maxHeight - 5;
		for (int i = 0; i<wc.length; i++)
		{
			char[] itchar = new char[]{wc[i]};
//			LineMetrics lmet = font.getLineMetrics(itchar,0,itchar.length,frc);
			GlyphVector gv = font.createGlyphVector(frc, itchar);

			g2D.drawChars(itchar,0,itchar.length,startPosX , startPosY + rand.nextInt(image.getHeight()/5) - image.getHeight()/10);
			startPosX = startPosX+(int) gv.getVisualBounds().getWidth()+3;
		}
		
		return image;
	}

	private BufferedImage addGradientBackground(BufferedImage image, Color from, Color to)
	{
		int width = image.getWidth();
		int height = image.getHeight();
				  
		// Create an opaque image
		BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
		Graphics2D graph = (Graphics2D)resultImage.getGraphics();
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			
		hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY));
		hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
		
		hints.add(new RenderingHints(RenderingHints.KEY_RENDERING,		RenderingHints.VALUE_RENDER_QUALITY));
		
		graph.setRenderingHints(hints);
        
		// Create the gradient color
		GradientPaint ytow = new GradientPaint(0, 0, from, 
			width, height, to);
			
		graph.setPaint(ytow);
		// Draw gradient color
		graph.fill(new Rectangle2D.Double(0, 0, width, height));
        
		// Draw the transparent image over the background
		graph.drawImage(image, 0, 0, null);        
        
		return resultImage;
	}

	private BufferedImage makeNoise(BufferedImage image, float factorOne, float factorTwo, float factorThree, float factorFour, Color mycol)
	{
		//image size
		int width = image.getWidth();
		int height = image.getHeight();
    
		//the points where the line changes the stroke and direction
		Point2D[] pts = null;
	
		//the curve from where the points are taken
		CubicCurve2D cc = new CubicCurve2D.Float( 
			width*factorOne, height*rand.nextFloat(), 
			width*factorTwo, height*rand.nextFloat(), 
			width*factorThree, height*rand.nextFloat(), 
			width*factorFour, height*rand.nextFloat());

		// creates an iterator to define the boundary of the flattened curve 
		PathIterator pi = cc.getPathIterator(null, 2); 
		Point2D tmp[] = new Point2D[200]; 
		int i = 0; 
    
		// while pi is iterating the curve, adds points to tmp array 
		while ( !pi.isDone() ) { 
		  float[] coords = new float[6]; 
		  switch ( pi.currentSegment(coords) ) { 
			case PathIterator.SEG_MOVETO: 
			case PathIterator.SEG_LINETO: 
			  tmp[i] = new Point2D.Float(coords[0], coords[1]); 
			} 
		  i++; 
		  pi.next(); 
		  }
       
		pts = new Point2D[i]; 
		// copies points from tmp to pts 
		System.arraycopy(tmp,0,pts,0,i);
    
		Graphics2D graph = (Graphics2D)image.getGraphics();
		graph.setRenderingHints(new RenderingHints(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON));
				
		graph.setColor(mycol);
    
		//for the maximum 3 point change the stroke and direction 
		for(i = 0; i < pts.length - 1; i++)
		{
			if(i < 3)
			{
				graph.setStroke(new BasicStroke(0.9f * (4-i)));
			}
			graph.drawLine((int)pts[i].getX(), (int)pts[i].getY(), (int)pts[i+1].getX(), (int)pts[i+1].getY());
		}
    
		graph.dispose();
		
		return image;
 	}

//	/**
//	 * Apply Ripple and Water ImageFilters to distort the image.
//	 * 
//	 * @param image the image to be distort
//	 * @return the distort image
//	 */
//	private BufferedImage distortRipple(BufferedImage image)
//	{
//		BufferedImage imageDistorted = 
//			new BufferedImage(image.getWidth(), image.getHeight(), 
//				BufferedImage.TYPE_INT_ARGB);
//
//		Graphics2D graph = (Graphics2D) imageDistorted.getGraphics();
//		
//		//create filter ripple
//		RippleFilter filter = new RippleFilter();
//		filter.setWaveType(RippleFilter.SINGLEFRAME);
//		filter.setXAmplitude(image.getWidth()/30);
//		filter.setYAmplitude(image.getHeight()/30+1.0f);
//		filter.setXWavelength(image.getWidth()/10 + rand.nextInt(5));
//		filter.setYWavelength(image.getHeight()/10 + rand.nextInt(5));
// 		filter.setEdgeAction(TransformFilter.RANDOMPIXELORDER);
// 		
//		//create water filter                
//		WaterFilter water = new WaterFilter();
//		water.setAmplitude(4);
//		water.setAntialias(true);
//		water.setPhase(15);
//		water.setWavelength(70);
//    
//		// Apply filter water              
//		FilteredImageSource filtered = 
//			new FilteredImageSource(image.getSource(), water);
//		Image img = Toolkit.getDefaultToolkit().createImage(filtered);
//    
//		// Apply filter ripple
//		filtered = new FilteredImageSource(img.getSource(), filter);
//		img = Toolkit.getDefaultToolkit().createImage(filtered);
//           
//		graph.drawImage(img, 0, 0, null, null);
//    
//		graph.dispose();
//    
//		return imageDistorted;
//	}

	private BufferedImage distortShadow(BufferedImage image, Color clrShadow)
	{
        
			BufferedImage imageDistorted = 
				new BufferedImage(image.getWidth(), image.getHeight(), 
					BufferedImage.TYPE_INT_ARGB);

			Graphics2D graph = (Graphics2D)imageDistorted.getGraphics();
        
			//create filter ripple
			//SphereFilter filter = new SphereFilter();
			//double d = 1.2;
			//filter.setRefractionIndex(d);
			
			ShadowFilter filter = new ShadowFilter();
			filter.setRadius(10);
			filter.setShadowColor(clrShadow.getRGB());
						
			RippleFilter wfilter = new RippleFilter();
						wfilter.setWaveType(RippleFilter.SINGLEFRAME);
						wfilter.setXAmplitude(image.getWidth()/30);
						wfilter.setYAmplitude(image.getHeight()/30+1.0f);
						wfilter.setXWavelength(image.getWidth()/10 + rand.nextInt(5));
						wfilter.setYWavelength(image.getHeight()/10 + rand.nextInt(5));
						wfilter.setEdgeAction(TransformFilter.RANDOMPIXELORDER);
        
			 wfilter.setEdgeAction(TransformFilter.RANDOMPIXELORDER);
        	 
			//apply filter water              
			
			FilteredImageSource wfiltered = new FilteredImageSource(image.getSource(), wfilter);
			
			Image img = Toolkit.getDefaultToolkit().createImage(wfiltered);
			img = Toolkit.getDefaultToolkit().createImage(wfiltered);
			
			
			FilteredImageSource filtered = new FilteredImageSource(img.getSource(), filter);
			img = Toolkit.getDefaultToolkit().createImage(filtered);
			
               
			graph.drawImage(img, 0, 0, null, null);
        
			graph.dispose();
        
			return imageDistorted;
		}

	/**
	 * Rotate an image from it's center. 
	 *
	 * @param The image to be rotated.
	 * @return The rotated image.
	 */
	private BufferedImage rotate(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();

		//create a clean transparent image
		BufferedImage transform = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            
		Graphics2D g2Dx = (Graphics2D)transform.getGraphics();
		AffineTransform xform = g2Dx.getTransform();
		g2Dx.setBackground(Color.white);
		g2Dx.setColor(Color.white);
		int xRot = width / 2;
		int yRot = height / 2;
        
		// generate an angle between 5 and -5 degrees.
		int angle = rand.nextInt(3) + 2;
		
		int ori = rand.nextInt(2);
		
		if (ori <  1 ) angle = angle * -1;
				
		//rotate the image
		xform.rotate(Math.toRadians(angle), xRot, yRot);

		g2Dx.setTransform(xform);
		g2Dx.drawImage(image, 0, 0, null,  null);

		return transform;
	}

	private BufferedImage distortFishEye(BufferedImage image, Color clrHoriz, Color clrVer)
	{

		Graphics2D graph = (Graphics2D) image.getGraphics();
		int imgH = image.getHeight();
		int imgW = image.getWidth();

		// claculate space between lines
		int hspace = 16;
		int vspace = 16;

		//draw the horizontal stripes
		for (int i = hspace; i < imgH; i = i + hspace)
		{
			graph.setColor(clrHoriz);
			graph.drawLine(0, i, imgW, i);
		}

		// draw the vertical stripes
		for (int i = vspace; i < imgW; i = i + vspace)
		{
			graph.setColor(clrVer);
			graph.drawLine(i, 0, i, imgH);
		}

		// create a pixel array of the original image.
		// we need this later to do the operations on..

		int pix[] = new int[imgH * imgW];
		int j = 0;

		for (int j1 = 0; j1 < imgW; j1++)
		{
			for (int k1 = 0; k1 < imgH; k1++)
			{
				pix[j] = image.getRGB(j1, k1);
				j++;
			}
		}

		double distance = ranInt(imgW / 4, imgW / 3);

		// put the distortion in the (dead) middle
		int wMid = image.getWidth() / 2;
		int hMid = image.getHeight() / 2;

		//again iterate over all pixels..
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				int relX = x - wMid;
				int relY = y - hMid;
				
				double d1 =    Math.sqrt(relX * relX + relY * relY);
				if (d1 < distance)
				{
					int j2 =wMid	+ (int) (((fishEyeFormula(d1 / distance) * distance) / d1)* (double) (x - wMid));
					int k2 =hMid	+ (int) (((fishEyeFormula(d1 / distance) * distance) / d1)* (double) (y - hMid));
					image.setRGB(x, y, pix[j2 * imgH + k2]);
				}
			}
		}

		return image;
	}

	private int ranInt(int i, int j)
	{
		double d = Math.random();
		return (int) ((double) i + (double) ((j - i) + 1) * d);
	}

	private double fishEyeFormula(double s)
	{
		// 		implementation of:
		//		g(s) = - (3/4)s3 + (3/2)s2 + (1/4)s, with s from 0 to 1.
		if (s < 0.0D) 	return 0.0D;
		if (s > 1.0D) 	return s;
		else
			return -0.75D * s * s * s + 1.5D * s * s + 0.25D * s;
	}
}
