package samoyan.core.image;

import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.registry.RIFRegistry;
import javax.media.jai.registry.RenderedRegistryMode;

import com.sun.imageio.plugins.jpeg.JPEGImageWriter;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ForwardSeekableStream;

/**
 * Encapsulates an image at a low level.
 * @author Yaniv Gvily
 * @version 1.0
 */
public final class JaiImage implements Cloneable
{	
	private RenderedOp op = null;

	private static double[][] MATRIX_ALPHA_ONLY = {{0.0D, 0.0D, 0.0D, 1.0D, 0.0D}};
	private static double[][] MATRIX_REMOVE_ALPHA = { { 1.0D, 0.0D, 0.0D, 0.0D, 0.0D },
													  { 0.0D, 1.0D, 0.0D, 0.0D, 0.0D },
													  { 0.0D, 0.0D, 1.0D, 0.0D, 0.0D }};
	private static double[][] MATRIX_ADD_ALPHA = { { 1.0D, 0.0D, 0.0D, 0.0D },
		  										   { 0.0D, 1.0D, 0.0D, 0.0D },
		  										   { 0.0D, 0.0D, 1.0D, 0.0D },
		  										   { 0.0D, 0.0D, 0.0D, 255.0D }};

	public JaiImage(RenderedOp op)
	{
		this.op = op;
	}
	
	public JaiImage(File file) throws IOException
	{
		// Loading with ImageIO.read will recognize RGBA images, and fail on CMYK.
		// ImageIO.read would recognize CMYK images if the CLib libraries were installed,
		// but that caused the VM to crash.
		Exception throwBack = null;
		try
		{
			BufferedImage bufImg = ImageIO.read(file);
			if (bufImg!=null)
			{
				ParameterBlock pb = new ParameterBlock();
				pb.add(bufImg);
				this.op = JAI.create("AWTImage", pb);
			}
		}
		catch (Exception cmmExc)
		{
			// Most likely an invalid file format
			throwBack = cmmExc;
		}
		
		if (this.op==null)
		{
			// This method does not recognize CMYK files (it confused CMYK with ARGB)
			// but it does support reading RGB images with embedded color profiles. 
			FileSeekableStream fss = new FileSeekableStream(file);
			this.op = JAI.create("stream", fss);
			
			// Make sure we're not reading CMYK and confusing it to be ARGB 
			if (this.op.getNumBands()!=3)
			{
				this.op = null;
			}
		}
		
		if (this.op==null)
		{
			if (throwBack!=null)
			{
				throw new IOException(throwBack);
			}
			else
			{
				throw new IOException();
			}
		}
	}
	public JaiImage(byte[] bytes) throws IOException
	{
		// Loading with ImageIO.read will recognize RGBA images, and fail on CMYK.
		// ImageIO.read would recognize CMYK images if the CLib libraries were installed,
		// but that caused the VM to crash.
		Exception throwBack = null;
		try
		{
			BufferedImage bufImg = ImageIO.read(new ByteArrayInputStream(bytes));
			if (bufImg!=null)
			{
				ParameterBlock pb = new ParameterBlock();
				pb.add(bufImg);
				this.op = JAI.create("AWTImage", pb);
			}
		}
		catch (Exception cmmExc)
		{
			// Most likely an invalid file format
			throwBack = cmmExc;
		}
		
		if (this.op==null)
		{
			// This method does not recognize CMYK files (it confused CMYK with ARGB)
			// but it does support reading RGB images with embedded color profiles. 
			ForwardSeekableStream fss = new ForwardSeekableStream(new ByteArrayInputStream(bytes));
			this.op = JAI.create("stream", fss);
			
			// Make sure we're not reading CMYK and confusing it to be ARGB 
			if (this.op.getNumBands()!=3)
			{
				this.op = null;
			}
		}
		
		if (this.op==null)
		{
			if (throwBack!=null)
			{
				throw new IOException(throwBack);
			}
			else
			{
				throw new IOException();				
			}
		}
	}
	public JaiImage(BufferedImage bufImg) throws IOException
	{
		ParameterBlock pb = new ParameterBlock();
		pb.add(bufImg);
		this.op = JAI.create("AWTImage", pb);
	}
	public JaiImage(int width, int height, int bands)
	{
		Byte[] bandValues = new Byte[bands];
		for (int b=0; b<bands; b++)
		{
			bandValues[b] = (byte)0;
		}
		ParameterBlock pb = new ParameterBlock();
		pb.add(new Float(width));
		pb.add(new Float(height));
		pb.add(bandValues);

		// Create the constant operation.
		this.op = JAI.create("constant", pb);
//		this.op.getWidth(); // Calc
	}
	
	@Override
	public JaiImage clone()
	{
		return new JaiImage(this.op);
	}

	// - - - - - - - - -
	// Save methods
	
	/**
	 * Encode this image as a JPEG.
	 * @param quality An integer between 0 and 100.
	 * @return The JPEG image, as a byte array.
	 * @throws IOException
	 */
	public byte[] encodeJPEG(int quality) throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
		encodeJPEG(os, quality);
        return os.toByteArray();
	}

	/**
	 * Encode this image as a JPEG image.
	 * @param quality An integer between 0 and 100.
	 * @return The JPEG image, as a byte array.
	 * @throws IOException
	 */
	public void encodeJPEG(OutputStream os, int quality) throws IOException
	{
//		JPEGEncodeParam param = new JPEGEncodeParam();
//		param.setQuality((float) quality / 100.0F);
//		
//		JAI.create("encode", op, os, "jpeg", param);
//		os.close();
//		return os.toByteArray();

		BufferedImage bi = this.op.getAsBufferedImage();
		
		ImageOutputStream out = ImageIO.createImageOutputStream(os);
		
//		ImageWriter writer = (ImageWriter) ImageIO.getImageWritersBySuffix("jpg").next();
		
		Iterator<ImageWriter> i = ImageIO.getImageWritersBySuffix("jpg");
		while (i.hasNext())
		{
			ImageWriter writer = i.next();
			
			// Hack: the CLibJPEGImageWriter does not save the file correctly
			if (writer instanceof JPEGImageWriter)
			{		
				writer.setOutput(out);
				ImageWriteParam param = writer.getDefaultWriteParam();
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality((float) quality / 100.0F);	
				writer.write(null, new IIOImage(bi, null, null), param);
				break;
			}
		}
        
        os.flush();
        os.close();
	}

	/**
	 * Encode this image as a PNG.
	 * @return The PNG image, as a byte array.
	 * @throws IOException
	 */
	public byte[] encodePNG() throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
		encodePNG(os);
        return os.toByteArray();
	}

	/**
	 * Encode this image as a PNG to the <code>OutputStream</code>
	 * @throws IOException
	 */
	public void encodePNG(OutputStream os) throws IOException
	{
		BufferedImage bi = this.op.getAsBufferedImage();

		ImageOutputStream out = ImageIO.createImageOutputStream(os);
//		ImageWriter writer = (ImageWriter) ImageIO.getImageWritersBySuffix("png").next();
//		writer.setOutput(out);
//		writer.write(null, new IIOImage(bi, null, null),null);
		ImageIO.write(bi, "png", out);
        
        os.flush();
        os.close();
	}
	
	/**
	 * Encode this image as a BMP.
	 * @return The BMP image, as a byte array.
	 * @throws IOException
	 */
	public byte[] encodeBMP() throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
		encodeBMP(os);
        return os.toByteArray();
	}

	/**
	 * Encode this image as a BMP to the <code>OutputStream</code>
	 * @throws IOException
	 */
	public void encodeBMP(OutputStream os) throws IOException
	{
		BufferedImage bi = this.op.getAsBufferedImage();

		ImageOutputStream out = ImageIO.createImageOutputStream(os);
//		ImageWriter writer = (ImageWriter) ImageIO.getImageWritersBySuffix("bmp").next();
//		writer.setOutput(out);
//		writer.write(null, new IIOImage(bi, null, null),null);
        ImageIO.write(bi, "bmp", out);
        
        os.flush();
        os.close();
	}

	/**
	 * Synonym for <code>toFile(file, 100)</code>.
	 * @param file
	 * @throws IOException
	 */
	public void toFile(File file) throws IOException
	{
		toFile(file, 100);
	}
	
	public void toFile(File file, int quality) throws IOException
	{
		FileOutputStream out = new FileOutputStream(file);
		if (file.getName().toLowerCase(Locale.US).endsWith(".png"))
		{
			encodePNG(out);
		}
		else if (file.getName().toLowerCase(Locale.US).endsWith(".bmp"))
		{
			encodeBMP(out);
		}
		else
		{
			encodeJPEG(out, quality);
		}
		out.flush();
		out.close();
		out = null;
	}
		
	// - - - - - - - - -
	// Image info
	
	public int getHeight()
	{
		return this.op.getHeight();
	}
	public int getWidth()
	{
		return this.op.getWidth();
	}
	public boolean hasAlpha()
	{
		ColorModel colorModel = this.op.getColorModel();
		return colorModel.hasAlpha();
	}
	public boolean isRGB()
	{
		ColorModel colorModel = this.op.getColorModel();
		ColorSpace colorSpace = colorModel.getColorSpace();
		int type = colorSpace.getType();
		return (type==ColorSpace.TYPE_RGB);
	}
	public boolean isCMYK()
	{
		ColorModel colorModel = this.op.getColorModel();
		ColorSpace colorSpace = colorModel.getColorSpace();
		int type = colorSpace.getType();
		return (type==ColorSpace.TYPE_CMYK);
	}
	public boolean isYCCK()
	{
		ColorModel colorModel = this.op.getColorModel();
		ColorSpace colorSpace = colorModel.getColorSpace();
		int type = colorSpace.getType();
		return (type==ColorSpace.TYPE_YCbCr);
	}
	public int getNumBands()
	{
		return this.op.getNumBands();
	}
	public int getBitDepth()
	{
		return this.op.getColorModel().getComponentSize(0);
	}
	
//	private int a(int color)
//	{
//		return ((color&0xff000000)>>24)&0xff;
//	}
	private int r(int color)
	{
		return ((color&0x00ff0000)>>16)&0xff;
//		return (color/256/256%256);
	}
	private int g(int color)
	{
		return ((color&0x0000ff00)>>8)&0xff;
//		return (color/256%256);
	}
	private int b(int color)
	{
		return (color&0x000000ff);
//		return color%256;
	}
	private int rgb(int color)
	{
		return color&0xffffff;
	}
	
	// - - - - - - - - -
	// Image manipulation
	
	/**
	 * Maps the R, G and B colors of the image to the corresponding colors.
	 * @param rColor The red component of any pixel in the image will be mapped to this color, maintaining intensity.
	 * Must be a 6 digit hexadecimal RGB value, e.g. 0x00FF00 for green.
	 * @param gColor The green component of any pixel in the image will be mapped to this color, maintaining intensity.
	 * Must be a 6 digit hexadecimal RGB value, e.g. 0x00FF00 for green.
	 * @param bColor The blue component of any pixel in the image will be mapped to this color, maintaining intensity.
	 * Must be a 6 digit hexadecimal RGB value, e.g. 0x00FF00 for green.
	 */
	public void mapRgb(int rColor, int gColor, int bColor)
	{
		double rr = ((double)r(rColor))/255.0D;
		double rg = ((double)g(rColor))/255.0D;
		double rb = ((double)b(rColor))/255.0D;
			
		double gr = ((double)r(gColor))/255.0D;
		double gg = ((double)g(gColor))/255.0D;
		double gb = ((double)b(gColor))/255.0D;
		
		double br = ((double)r(bColor))/255.0D;
		double bg = ((double)g(bColor))/255.0D;
		double bb = ((double)b(bColor))/255.0D;
		
		double rm = 1/(rr+gr+br);
		double gm = 1/(rg+gg+bg);
		double bm = 1/(rb+gb+bb);
				rm = 1;
				gm = 1;
				bm = 1;
				
		// Conversion matrixes
		double[][] matrixRGB = {
				{ rr*rm, gr*rm, br*rm, 0.0D },
				{ rg*gm, gg*gm, bg*gm, 0.0D },
				{ rb*bm, gb*bm, bb*bm, 0.0D }
			};
		double[][] matrixRGBAlpha = {
				{ rr, gr, br, 0.0D, 0.0D },
				{ rg, gg, bg, 0.0D, 0.0D },
				{ rb, gb, bb, 0.0D, 0.0D },
				{ 0.0D, 0.0D, 0.0D, 1.0D, 0.0D }
			};
		
		// Create the ParameterBlock.
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(this.op);
		pb.add(this.hasAlpha()? matrixRGBAlpha : matrixRGB);
		
		// Perform the band combine operation.
		this.op = JAI.create("bandcombine", pb, null);
	}
	
	/**
	 * Maps the alpha channel of the image to the corresponding color and converts the image to a 3-band image.
	 * @param color The transparent parts of the image will show this color.
	 * Must be a 6 digit hexadecimal RGB value, e.g. 0x00FF00 for green.
	 */
	public void addBackground(int color)
	{
		// Check if image is transparent
		if (this.hasAlpha()==false) return;
		
		// Create a constant image from the color provided
		Byte[] bandValues = new Byte[3];
		bandValues[0] = new Byte((byte)r(color));
		bandValues[1] = new Byte((byte)g(color));
		bandValues[2] = new Byte((byte)b(color));
		ParameterBlock pb = new ParameterBlock();
		pb.add((float) this.op.getWidth());
		pb.add((float) this.op.getHeight());
		pb.add(bandValues);
		RenderedOp bg = JAI.create("constant", pb, null);

		
		// This line seems to be necessary for this method to work.
		bg.getAsBufferedImage().getRGB(0, 0);

		
		// Get the alpha channel only
		pb = new ParameterBlock();
		pb.addSource(this.op);
		pb.add(MATRIX_ALPHA_ONLY);
		RenderedOp alpha = JAI.create("bandcombine", pb, null);

		// Get the RGB channels only
		pb = new ParameterBlock();
		pb.addSource(this.op);
		pb.add(MATRIX_REMOVE_ALPHA);
		RenderedOp rgb = JAI.create("bandcombine", pb, null);

		
		// Combine the original image with the constant BG using the alpha map
		pb = new ParameterBlock();
		pb.addSource(rgb);
		pb.addSource(bg);
		pb.add(alpha);
		pb.add(null);
		pb.add(new Boolean(false));
		pb.add(CompositeDescriptor.NO_DESTINATION_ALPHA);
		this.op = JAI.create("composite", pb);
		
		
		// Identify the slight color shift
		boolean exit = false;
		int shifted = color;
		BufferedImage alphaBuf = alpha.getAsBufferedImage();
		BufferedImage resultBuf = this.op.getAsBufferedImage();
		for (int y=0; y<resultBuf.getHeight(); y++)
		{
			for (int x=0; x<resultBuf.getWidth(); x++)
			{
				int argb = alphaBuf.getRGB(x,y);
//System.out.print(Integer.toHexString(argb) + " ");
				if (rgb(argb)==0)
				{
					shifted = resultBuf.getRGB(x, y);
					shifted = rgb(shifted);
					exit = true;
					break;
				}
			}
			if (exit==true) break;
		}
//System.out.println();
//System.out.println(Integer.toHexString(color) + " -> " + Integer.toHexString(shifted));
		
		
		// Fix the color shifting of the background caused by the compositing
		if (shifted!=color)
		{
//System.out.println(Integer.toHexString(color) + " -> " + Integer.toHexString(shifted));
			double dr = (double)r(color) - (double)r(shifted);
			double dg = (double)g(color) - (double)g(shifted);
			double db = (double)b(color) - (double)b(shifted);
			double[][] matrix = {
					{1.0D, 0.0D, 0.0D, dr},
					{0.0D, 1.0D, 0.0D, dg},
					{0.0D, 0.0D, 1.0D, db}
				};
			pb = new ParameterBlock();
			pb.addSource(this.op);
			pb.add(matrix);
			this.op = JAI.create("bandcombine", pb, null);
		}
	}
	
	/**
	 * @see http://forums.sun.com/thread.jspa?threadID=5383115
	 */
	public void toRGB()
	{
		if (this.isRGB()) return;

		// Convert color model
		BufferedImage src = this.op.getAsBufferedImage();
		BufferedImage dst = null;
		if (this.isCMYK())
		{
			dst = createJPEG4(src, 0);
		}
		else if (this.isYCCK())
		{
			dst = createJPEG4(src, 2);
		}
		else // Grayscale and other color models
		{
			dst = new BufferedImage(this.op.getWidth(), this.op.getHeight(), BufferedImage.TYPE_INT_RGB);
		
//			ICC_ColorSpace srcCS = (ICC_ColorSpace) src.getColorModel().getColorSpace();
//			ICC_Profile srcProf = srcCS.getProfile();
//			byte[] header = srcProf.getData(ICC_Profile.icSigHead);
//			intToBigEndian(ICC_Profile.icSigInputClass, header, 12);
//			srcProf.setData(ICC_Profile.icSigHead, header);
		
			ColorConvertOp op = new ColorConvertOp(null);
			op.filter(src, dst);
		}
		
		ParameterBlock pb = new ParameterBlock();
		pb.add(dst);
		this.op = JAI.create("AWTImage", pb);
	}
//	private static void intToBigEndian(int value, byte[] array, int index)
//	{
//	    array[index]   = (byte) (value >> 24);
//	    array[index+1] = (byte) (value >> 16);
//	    array[index+2] = (byte) (value >>  8);
//	    array[index+3] = (byte) (value);
//	}
	
	/**
	 * @see http://forums.sun.com/thread.jspa?threadID=5383115
	 */
	private static BufferedImage createJPEG4(BufferedImage src, int xform)
	{
		Raster raster = src.getData();
		int w = raster.getWidth();
		int h = raster.getHeight();
		byte[] rgb = new byte[w*h*3];

		// if (Adobe_APP14 and transform==2) then YCCK else CMYK                                                                                    
		if (xform==2)	// YCCK -- Adobe
		{                                                                                                         
			float[] Y = raster.getSamples(0,0,w,h, 0, (float[])null);
			float[] Cb = raster.getSamples(0,0,w,h, 1, (float[])null);
			float[] Cr = raster.getSamples(0,0,w,h, 2, (float[])null);
			float[] K = raster.getSamples(0,0,w,h, 3, (float[])null);

			for (int i=0,imax=Y.length, base=0; i<imax; i++, base+=3)
			{
				// faster to track last cmyk and save computations on stretches of same color?                                                      
				// better to use ColorConvertOp?                                                                                                    
				float k=K[i], y = Y[i], cb=Cb[i], cr=Cr[i];
				double val = y + 1.402*(cr-128) - k;
				rgb[base] = val<0.0? (byte)0 : val>255.0? (byte)0xff : (byte)(val+0.5);
				val = y - 0.34414*(cb-128) - 0.71414*(cr-128) - k;
				rgb[base+1] = val<0.0? (byte)0 : val>255.0? (byte)0xff : (byte)(val+0.5);
				val = y + 1.772 * (cb-128) - k;
				rgb[base+2] = val<0.0? (byte)0 : val>255.0? (byte)0xff : (byte)(val+0.5);
			}
		}
		else if (xform==0)	// CMYK
		{
			int[] C = raster.getSamples(0,0,w,h, 0, (int[])null);
			int[] M = raster.getSamples(0,0,w,h, 1, (int[])null);
			int[] Y = raster.getSamples(0,0,w,h, 2, (int[])null);
			int[] K = raster.getSamples(0,0,w,h, 3, (int[])null);
			
			double MAX = Math.pow(2, src.getColorModel().getComponentSize(0)) - 1; // 255 or 65535
			
			for (int i=0,imax=C.length, base=0; i<imax; i++, base+=3)
			{
//                int c = 255 - C[i];
//                int m = 255 - M[i];
//                int y = 255 - Y[i];
//                int k = 255 - K[i];
//                float kk = k / 255f;
// 
//                rgb[base] = (byte) (255 - Math.min(255f, c * kk + k));
//                rgb[base + 1] = (byte) (255 - Math.min(255f, m * kk + k));
//                rgb[base + 2] = (byte) (255 - Math.min(255f, y * kk + k));

                
            	double c = MAX - C[i];
            	double m = MAX - M[i];
            	double y = MAX - Y[i];
            	double k = MAX - K[i];

            	c = c / MAX;
            	m = m / MAX;
            	y = y / MAX;
            	k = k / MAX;

            	double r = c * (1.0 - k) + k;
            	double g = m * (1.0 - k) + k;
            	double b = y * (1.0 - k) + k;

            	r = (1.0 - r) * 255.0 + 0.5;
            	g = (1.0 - g) * 255.0 + 0.5;
            	b = (1.0 - b) * 255.0 + 0.5;

            	rgb[base] = (byte) r;
            	rgb[base+1] = (byte) g;
            	rgb[base+2] = (byte) b;
			
			}
		}

		// from other image types we know InterleavedRaster's can be 
		// manipulated by AffineTransformOp, so create one of those.                      
		raster = Raster.createInterleavedRaster(new DataBufferByte(rgb, rgb.length), w, h, w*3,3, new int[] {0,1,2}, null);
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

		return new BufferedImage(cm, (WritableRaster)raster, true, null);
	}	
		
	public void luminance()
	{
//		double[][] m1 = {	{0.2126D, 0.7152D, 0.0722D, 0D},
//							{0.2126D, 0.7152D, 0.0722D, 0D},
//							{0.2126D, 0.7152D, 0.0722D, 0D}};
//		double[][] m2 = {	{0.2126D, 0.7152D, 0.0722D, 0D, 0D},
//							{0.2126D, 0.7152D, 0.0722D, 0D, 0D},
//							{0.2126D, 0.7152D, 0.0722D, 0D, 0D},
//							{0D, 0D, 0D, 1D, 0D}};
		double[][] m1 = {	{0.299D, 0.587D, 0.114D, 0D},
							{0.299D, 0.587D, 0.114D, 0D},
							{0.299D, 0.587D, 0.114D, 0D}};
		double[][] m2 = {	{0.299D, 0.587D, 0.114D, 0D, 0D},
							{0.299D, 0.587D, 0.114D, 0D, 0D},
							{0.299D, 0.587D, 0.114D, 0D, 0D},
							{0D, 0D, 0D, 1D, 0D}};

		// Create the ParameterBlock.
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(op);
		pb.add(hasAlpha()?m2:m1);

		this.op = JAI.create("bandcombine", pb, null);
	}
		
	public void scale(float xModifier, float yModifier) throws IOException
	{
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
		hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); 
		hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		
		// Eliminate tile caching because this operation seems to allocate memory and not release it
		// Operation actually works faster without caching
		TileCache zeroCache = JAI.createTileCache(0L);
		hints.put(JAI.KEY_TILE_CACHE, zeroCache);
		
		ParameterBlock params = new ParameterBlock();
		params.addSource(this.op);
		params.add(xModifier); //x scale factor
		params.add(yModifier); //y scale factor
		params.add(0.0F); //x translate
		params.add(0.0F); //y translate
		params.add(new InterpolationBilinear()); //interpolation method
	
		this.op = JAI.create("scale", params, hints);

		// The scale operator is memory intensive due to creating large memory buffers.
		// Having many scale operators in the chain, especially on larger images,
		// may cause deadlock with memory overrun and 100% CPU consumption.
		// We therefore render the image to clear the operator pipeline. 
		// This makes scale inefficient, but at least it won't crash the system.
		{
			renderAndReload();
		}
		
		// Release cache memory
		zeroCache.flush();
		zeroCache = null;
		
//		System.out.println("scale " + xModifier + ((yModifier!=xModifier)?" " + yModifier:""));
	}
	
	private void renderAndReload() throws IOException
	{
		// BMP is lossless and fastest to encode due to no compression
		// but PNG is required if image has transparency
		String ext = ".bmp";
		if (this.hasAlpha())
		{
			ext = ".png";
		}
		
		File tmpFile = File.createTempFile("scaleimage", ext);
		toFile(tmpFile);
		FileSeekableStream fss = new FileSeekableStream(tmpFile);
		this.op = JAI.create("stream", fss);
	}
	
	/**
	 * Resize the image to fit within <code>maxWidth</code> and <code>maxHeight</code>. The aspect ratio
	 * of the original image is maintained.
	 * If the image is small enough to fit within <code>maxWidth</code> and <code>maxHeight</code>,
	 * no action is taken.
	 * @param maxWidth The max width to resize to.
	 * @param maxHeight The max height to resize to.
	 * @throws IOException
	 */
	public void resizeToFit(int maxWidth, int maxHeight) throws IOException
	{
		double w = (float) op.getWidth();
		double h = (float) op.getHeight();

		double wRatio = ((double) maxWidth) / w;
		double hRatio = ((double) maxHeight) / h;
		double modifier = Math.min(wRatio, hRatio);

		if (modifier<1.0D)
		{
			resize(modifier);
		}
	}
	
	public void resize(double modifier) throws IOException
	{
		resize(modifier, modifier);
	}
	
	public void resize(double xModifier, double yModifier) throws IOException
	{
		final double SUBSAMPLE_THREASHOLD = 0.5D; // Must be <=0.5D

		if (xModifier==1D && yModifier==1D) return;
	
// Commented out because the scaling operator is inefficient due to need to dump to disk (see JaiImage.scale)
//		// Phased scaling: scale down by exact halves first
//		int xHalves = 0;
//		while (xModifier<=SUBSAMPLE_THREASHOLD)
//		{
//			xModifier *= 2D;
//			xHalves ++;
//		}
//
//		int yHalves = 0;
//		while (yModifier<=SUBSAMPLE_THREASHOLD)
//		{
//			yModifier *= 2D;
//			yHalves ++;
//		}
//
//		while (xHalves>0 || yHalves>0)
//		{
//			float xFactor = 1F;
//			if (xHalves>0)
//			{
//				xFactor = 0.5F;
//				xHalves--;
//			}
//			
//			float yFactor = 1F;
//			if (yHalves>0)
//			{
//				yFactor = 0.5F;
//				yHalves--;
//			}
//			
//			scale(xFactor, yFactor);
//		}
		
		// Scale by the remainder
		boolean macOS = System.getProperty("os.name").toLowerCase().indexOf("mac")>=0;
		if (!macOS && (xModifier<SUBSAMPLE_THREASHOLD || yModifier<SUBSAMPLE_THREASHOLD))
		{
			// Subsample produces good results if resizing to under 50%, otherwise it produces pixelation.
			// Subsample is slow because it is not using acceleation (see JaiImage.init)
			// Note: should not enter here if using phased scaling
			// subsample does not work on iOS
			subsample(xModifier, yModifier);
		}
		else
		{
			// Scale produces good results when scaling to any size 50% or larger
			// Scale is inefficient due to need to dump to disk due to memory overusage (see JaiImage.scale)
			scale((float) xModifier, (float) yModifier);
		}
	}
	
	/**
	 * Warning: in MacOS, calling this method will result in subsequent calls to getAsBufferedImage to fail with
	 * "Out of bounds" exception. As a workaround, call {@link #scale(float, float)} instead.
	 * @param xModifier
	 * @param yModifier
	 * @throws IOException
	 */
	public void subsample(double xModifier, double yModifier) throws IOException
	{
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
		hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); 
		hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//		hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		
		ParameterBlock params = new ParameterBlock();
		params.addSource(this.op);
		params.add(xModifier); //x scale factor
		params.add(yModifier); //y scale factor
	
		this.op = JAI.create("subsampleaverage", params, hints);
		
//		System.out.println("subsample " + xModifier + ((yModifier!=xModifier)?" " + yModifier:""));
	}
		
	public void aspect(int longEdge, int shortEdge) throws IOException
	{
		int minEdge = longEdge<shortEdge? longEdge : shortEdge;
		int maxEdge = longEdge>shortEdge? longEdge : shortEdge;
		float edgeRatio = ((float) maxEdge / (float) minEdge);
		
		boolean done = false;
		if (this.getWidth()>this.getHeight())
		{
			// Image is wide
			float imgRatio = ((float) this.getWidth() / (float) this.getHeight());
			if (imgRatio > edgeRatio)
			{
				thumbnail(maxEdge, minEdge);
				done = true;
			}
		}
		else
		{
			// Image is tall
			float imgRatio = ((float) this.getHeight() / (float) this.getWidth());
			if (imgRatio > edgeRatio)
			{
				thumbnail(minEdge, maxEdge);
				done = true;
			}
		}
		
		if (done==false)
		{
			resizeToFit(maxEdge, maxEdge);
		}
	}
	
	/**
	 * Creates a thumbnail of the given image by cutting the largest area of the image with
	 * the same aspect ratio as <code>maxWidth<code>:</code>maxHeight</code>, then resizes 
	 * that area to <code>maxWidth<code> and </code>maxHeight</code>. If the original image
	 * is horizontal, the center of the image is chosen; if vertical, the upper portion
	 * is chosen.
	 * @param maxWidth The max width of the thumbnail.
	 * @param maxHeight The max height of the thumbnail.
	 * @throws IOException
	 */
	public void thumbnail(int maxWidth, int maxHeight) throws IOException
	{
		float w = this.getWidth();
		float h = this.getHeight();
		if (w<=maxWidth && h<=maxHeight)
		{
			return;
		}

		boolean tall = ((float) h / (float) w) > ((float) maxHeight / (float) maxWidth); 
		
		int left, top, right, bottom;
		if (tall)
		{
			float ratio = w / (float) maxWidth;
			left = 0;
			right = (int) w - 1;
			
			int cropHeight = (int) (ratio * (float) maxHeight);
			top = ((int) h - cropHeight) / 4;
			bottom = top + cropHeight - 1;
		}
		else // wide
		{
			float ratio = h / (float) maxHeight;
			top = 0;
			bottom = (int) h - 1;
			
			int cropWidth = (int) (ratio * (float) maxWidth);
			left = ((int) w - cropWidth) / 2;
			right = left + cropWidth - 1;
		}
		
		crop(left, top, right, bottom);
		resizeToFit(maxWidth, maxHeight);
	}
	
	/**
	 * Crop the image.
	 * @param left The left boundary.
	 * @param top The top boundary.
	 * @param right The right boundary.
	 * @param bottom The bottom boundary.
	 * @throws IOException 
	 */
	public void crop(int left, int top, int right, int bottom) throws IOException
	{
		if (top<0) top=0;
		if (left<0) left=0;
		if (right>=op.getWidth()) right=op.getWidth()-1;
		if (bottom>=op.getHeight()) bottom=op.getHeight()-1;
		
		if (top==0 && left==0 && right==op.getWidth()-1 && bottom==op.getHeight()-1)
		{
			// Void action
			return;
		}
		
		top += op.getMinY();
		bottom += op.getMinY();
		left += op.getMinX();
		right += op.getMinX();
		
		if (bottom<top)
		{
			int temp = top;
			top = bottom;
			bottom = temp;
		}
		if (right<left)
		{
			int temp = left;
			left = right;
			right = temp;
		}
		
		ParameterBlock params = new ParameterBlock();
		params.addSource(op);
		params.add((float) left); //x origin
		params.add((float) top); //y origin
		params.add((float) (right-left+1)); //width
		params.add((float) (bottom-top+1)); //height
		
		this.op = JAI.create("crop", params);
		
		// Hack: crop operation messes up the image coordinates.
		// We therefore render the image to clear the operator pipeline. 
		// This makes scale inefficient, but at least it works.
		{
			renderAndReload();
		}
	}

	/**
	 * Fix perspective distortion that are caused by shooting a subject at an angle.
	 * @param ulx Upper left X coord (of the trapezoid subject to use as base for the correction).
	 * @param uly Upper left Y coord.
	 * @param urx Upper right X coord.
	 * @param ury Upper right Y coord.
	 * @param lrx Lower right X coord.
	 * @param lry Lower right Y coord.
	 * @param llx Lower left X coord.
	 * @param lly Lower left Y coord.
	 */
	public void perspective(int ulx, int uly, int urx, int ury, int lrx, int lry, int llx, int lly)
	{
		// Upper
		int dx = (urx-ulx);
		int dy = (ury-uly);
		double upperWidth = Math.sqrt(dx*dx+dy*dy);
		
		// Lower
		dx = (lrx-llx);
		dy = (lry-lly);
		double lowerWidth = Math.sqrt(dx*dx+dy*dy);

		// Right
		dx = (lrx-urx);
		dy = (lry-ury);
		double rightHeight = Math.sqrt(dx*dx+dy*dy);
		
		// Left
		dx = (llx-ulx);
		dy = (lly-uly);
		double leftHeight = Math.sqrt(dx*dx+dy*dy);
		
		// Avgs
		int newWidth = (int) ((upperWidth+lowerWidth)/2);
		int newHeight = (int) ((rightHeight+leftHeight)/2);
		
//		// Max
//		int newWidth = (int) (upperWidth>lowerWidth ? upperWidth : lowerWidth);
//		int newHeight = (int) (rightHeight>leftHeight ? rightHeight : leftHeight);

		PerspectiveTransform transform = PerspectiveTransform.getQuadToQuad(
			0,0,    0,newHeight,    newWidth,newHeight,       newWidth,0,
		ulx,uly,   llx,lly,   lrx,lry,    urx,ury);
        WarpPerspective warp = new WarpPerspective(transform);
		
		// Specify the boundries as rendering hints
		ImageLayout layout = new ImageLayout();
		layout.setMinX(0);
		layout.setMinY(0);
        layout.setWidth(newWidth);
        layout.setHeight(newHeight);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);

		// Warp
        ParameterBlock pb = new ParameterBlock();
		pb.addSource(this.op);
		pb.add(warp);
		pb.add(new InterpolationBilinear()); // interpolation method
	        
		this.op = JAI.create("warp", pb, hints);
	}
	
	/**
	 * Adjusts the brightness and contrast of the image.
	 * @param brightness The amount to brighten by, between -255 and 255.
	 * @param contrast The contrast increase, between -127.5 to 127.5.
	 * @see http://forums.java.net/jive/thread.jspa?messageID=105664
	 */
	public void brightnessContrast(int brightness, float contrast)
	{		
		// Pre-adjust Brightness to set a centre for continuative contrast-modifications
		if (brightness!=0)
		{
			if (brightness<-255) brightness = -255;
			if (brightness>255) brightness = 255;
			
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(this.op);
			if (this.hasAlpha())
			{
				double[] constants = {(float)brightness, (float)brightness, (float)brightness, 0.0F};
				pb.add(constants);
			}
			else
			{
				double[] constants = {(float)brightness, (float)brightness, (float)brightness};
				pb.add(constants);
			}
			this.op = JAI.create("addconst", pb, null);
		}
		
		// Contrast
		int numBands = this.getNumBands();
		
		// bp is used to compress or pull colorrange
		// bp = BoostPixel
		float[][][] bp = new float[numBands][2][];
		
		// Decrease contrast
		if (contrast<0)
		{
			if (contrast<-127.5F) contrast = -127.5F;
			contrast = 127.5F + contrast;
			
			float innerMax = (255.0F / 2.0F) + contrast;
			float innerMin = (255.0F / 2.0F) - contrast;
			
			for (int i=0; i<numBands; i++)
			{
				bp[i][0] = new float[] { -0.1F, 0.0F, 255.0F, 255.1F };
				bp[i][1] = new float[] { 0.0F, innerMin, innerMax, 255.0F };
			}
			if (this.hasAlpha())
			{
				bp[numBands-1][0] = new float[] { 0.0F, 255.0F };
				bp[numBands-1][1] = new float[] { 0.0F, 255.0F };
			}
		}
		
		// Increase contrast
		else if (contrast>0)
		{
			// if (contrast==0.0F) contrast = 0.00001F;
			if (contrast>127.5F) contrast = 127.5F;
			contrast = 127.5F - contrast;
			
			float innerMax = (255.0F / 2.0F) + contrast;
			float innerMin = (255.0F / 2.0F) - contrast;
			
			for (int i=0; i<numBands; i++)
			{
				bp[i][0] = new float[] { 0.0F, innerMin, innerMax, 255.0F };
				bp[i][1] = new float[] { 0.0F, 0.0F, 255.0F, 255.0F };
			}
			if (this.hasAlpha())
			{
				bp[numBands-1][0] = new float[] { 0.0F, 255.0F };
				bp[numBands-1][1] = new float[] { 0.0F, 255.0F };
			}
		}
		
		if (contrast!=0)
		{
			this.op = JAI.create("piecewise", this.op, bp);
		}
	}
	
//	/**
//	 * Reduces the color space of the image.
//	 * @param colors Max number of colors.
//	 * @throws IOException
//	 */
//	public void dither(int colors) throws IOException
//	{
//		this.op = ColorQuantizerDescriptor.create(this.op, ColorQuantizerDescriptor.MEDIANCUT, new Integer(colors), null, null, null, null, null);
//	}
	
	/**
	 * Rotates the image 90, 180, or 270 degrees.
	 * @param degrees Must be 90, 180 or 270.
	 */
	public void rotate(int degrees)
	{
		if (degrees!=90 && degrees!=180 && degrees!=270) return;
		
		TransposeType type = null;
		if (degrees==90)
		{
			type = TransposeDescriptor.ROTATE_90;
		}
		else if (degrees==180)
		{
			type = TransposeDescriptor.ROTATE_180;
		}
		else if (degrees==270)
		{
			type = TransposeDescriptor.ROTATE_270;
		}
		
		this.op = JAI.create("transpose", this.op, type);
	}
	
	/**
	 * Flips the image horitontally or vertically.
	 */
	public void flip(boolean horizontal, boolean vertical)
	{
		TransposeType type = null;
		if (horizontal && vertical)
		{
			type = TransposeDescriptor.ROTATE_180;
		}
		else if (horizontal)
		{
			type = TransposeDescriptor.FLIP_HORIZONTAL;
		}
		else if (vertical)
		{
			type = TransposeDescriptor.FLIP_VERTICAL;
		}
		else
		{
			return;
		}
		
		this.op = JAI.create("transpose", this.op, type);
	}
	
	/**
	 *  Histogram-equalize the image
	 */
	public void equalize(int binCount)
	{
		calcHistogram(binCount);

		int numBands = this.op.getNumBands();
		
		// Create an equalization CDF.
		float[][] cdfEq = new float[numBands][];
		for(int b = 0; b < numBands; b++)
		{
			cdfEq[b] = new float[binCount];
			for(int i = 0; i < binCount; i++)
			{
				cdfEq[b][i] = (float)(i+1)/(float)binCount;
			}
		}
		
		this.op = JAI.create("matchcdf", this.op, cdfEq);
	}

	/**
	 *  Histogram-normalize the image
	 */
	public void normalize(int binCount)
	{
		calcHistogram(binCount);
		
		int numBands = this.op.getNumBands();
		
		// Create a normalization CDF.
		double[] mean = {128.0, 128.0, 128.0, 128.0};
		double[] stDev = {64.0, 64.0, 64.0, 64.0};
		float[][] cdfNorm = new float[numBands][];
		for(int b = 0; b < numBands; b++)
		{
			cdfNorm[b] = new float[binCount];
			double mu = mean[b];
			double twoSigmaSquared = 2.0*stDev[b]*stDev[b];
			cdfNorm[b][0] = (float)Math.exp(-mu*mu/twoSigmaSquared);
			for(int i = 1; i < binCount; i++)
			{
				double deviation = i - mu;
				cdfNorm[b][i] = cdfNorm[b][i-1] + (float)Math.exp(-deviation*deviation/twoSigmaSquared);
			}
		}
		for(int b = 0; b < numBands; b++)
		{
			double cdfNormLast = cdfNorm[b][binCount-1];
			for(int i = 0; i < binCount; i++)
			{
				cdfNorm[b][i] /= cdfNormLast;
			}
		}
		this.op = JAI.create("matchcdf", this.op, cdfNorm);
	}
	
    // Retrieves a histogram for the image.
    private void calcHistogram(int binCount)
    {
    	int numBands = this.op.getNumBands();

        // Allocate histogram memory.
        int[] numBins = new int[numBands];
        double[] lowValue = new double[numBands];
        double[] highValue = new double[numBands];
        for(int i = 0; i < numBands; i++)
        {
        	numBins[i] = binCount;
        	lowValue[i] = 0.0;
        	highValue[i] = 255.0;
        }

//        // Create the Histogram object.
//        Histogram hist = new Histogram(numBins, lowValue, highValue);
//
//        // Set the ROI to the entire image.
//        ROIShape roi = new ROIShape(this.op.getBounds());

        // Create the histogram op.
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(this.op);  
		pb.add(null);											// The ROI
		pb.add(1); pb.add(1);									// Sampling
		pb.add(new int[]{binCount});							// Bins
		pb.add(new double[]{0});
		pb.add(new double[]{256});	// Range for inclusion
		this.op = JAI.create("histogram", pb, null);
    }
        
    // - - - -
    
    public final static void init(boolean acceleration)
    {
		System.setProperty("com.sun.media.jai.disableMediaLib", String.valueOf(!acceleration));

		if (acceleration)
		{
			// Hack: subsampleaverage misbehaves when accelerated, creating lines on top of the image.
			// for example, take a 2160x2685 image and subsample to 1287x1600
			setNativeAccelerationAllowed("subsampleaverage", false);
		}
    }
    
    /**
     * Allows or disallow native acceleration for the specified JAI operation. By default, JAI uses
     * hardware accelerated methods when available. For example, it make use of MMX instructions on
     * Intel processors. Unfortunately, some native method crash the Java Virtual Machine under some
     * circumstances.  For example on JAI 1.1.2, the "Affine" operation on an image with float data
     * type, bilinear interpolation and an {@link javax.media.jai.ImageLayout} rendering hint cause
     * an exception in medialib native code.  Disabling the native acceleration (i.e using the pure
     * Java version) is a convenient workaround until Sun fix the bug.
     * <p>
     * <strong>Implementation note:</strong> the current implementation assumes that factories for
     * native implementations are declared in the {@code com.sun.media.jai.mlib} package, while
     * factories for pure java implementations are declared in the {@code com.sun.media.jai.opimage}
     * package. It work for Sun's 1.1.2 implementation, but may change in future versions. If this
     * method doesn't recognize the package, it does nothing.
     *
     * @param operation The operation name (e.g. "Affine").
     * @param allowed {@code false} to disallow native acceleration.
     *
     * @see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4906854">JAI bug report 4906854</a>
     */
    private static void setNativeAccelerationAllowed(String operation, boolean allowed)
    {
    	String product = "com.sun.media.jai";
    	OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
    	List<?> factories = registry.getOrderedFactoryList(RenderedRegistryMode.MODE_NAME, operation, product);
    	if (factories != null)
    	{
    		RenderedImageFactory javaFactory = null;
    		RenderedImageFactory nativeFactory = null;
    		Boolean currentState = null;
    		for (Iterator<?> it=factories.iterator(); it.hasNext();)
    		{
    			RenderedImageFactory factory = (RenderedImageFactory) it.next();
    			String pack = factory.getClass().getPackage().getName();
    			if (pack.equals("com.sun.media.jai.mlib")) {
    				nativeFactory = factory;
    				if (javaFactory != null) {
    					currentState = Boolean.FALSE;
    				}
    			}
    			if (pack.equals("com.sun.media.jai.opimage")) {
    				javaFactory = factory;
    				if (nativeFactory != null) {
    					currentState = Boolean.TRUE;
    				}
    			}
    		}
    		if (currentState!=null && currentState.booleanValue()!=allowed)
    		{
    			RIFRegistry.unsetPreference(registry,
    										operation,
    										product,
    										allowed ? javaFactory : nativeFactory,
    										allowed ? nativeFactory : javaFactory);
    			RIFRegistry.setPreference(	registry,
    										operation,
    										product,
    										allowed ? nativeFactory : javaFactory,
    										allowed ? javaFactory : nativeFactory);
    		}
    	}
    }
    
	public void composite(JaiImage stamp) throws IOException
	{
		ParameterBlock pb;
		
		int origWidth = this.op.getWidth();
		int origHeight = this.op.getHeight();
					
		// Separate the alpha channel from the RGB channels of the stamp, if needed
		RenderedOp stampOp = stamp.op;
		RenderedOp stampRgbOp = null;
		RenderedOp stampAlphaOp = null;
		if (stamp.hasAlpha())
		{
			// Get the alpha channel of the stamp
			pb = new ParameterBlock();
			pb.addSource(stampOp);
			pb.add(MATRIX_ALPHA_ONLY);
			stampAlphaOp = JAI.create("bandcombine", pb, null);
			
			// Get the RGB channels of the stamp
			pb = new ParameterBlock();
			pb.addSource(stampOp);
			pb.add(MATRIX_REMOVE_ALPHA);
			stampRgbOp = JAI.create("bandcombine", pb, null);
		}
		else
		{
			stampRgbOp = stampOp;
			
			Byte[] bandValues = new Byte[1];
			bandValues[0] = new Byte((byte) 255);
			pb = new ParameterBlock();
			pb.add(new Float(stampOp.getWidth()));
			pb.add(new Float(stampOp.getHeight()));
			pb.add(bandValues);

			// Create the constant operation.
			stampAlphaOp = JAI.create("constant", pb);
		}
		
		if (stampRgbOp.getWidth()<origWidth || stampRgbOp.getHeight()<origHeight)
		{
			pb = new ParameterBlock();
			pb.addSource(stampRgbOp);
			pb.add(0); // left
			pb.add(Math.max(0, origWidth - stampRgbOp.getWidth())); // right
			pb.add(0); // top
			pb.add(Math.max(0, origHeight - stampRgbOp.getHeight())); // bottom
			pb.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
			stampRgbOp = JAI.create("border", pb);
			
			pb = new ParameterBlock();
			pb.addSource(stampAlphaOp);
			pb.add(0); // left
			pb.add(Math.max(0, origWidth - stampAlphaOp.getWidth())); // right
			pb.add(0); // top
			pb.add(Math.max(0, origHeight - stampAlphaOp.getHeight())); // bottom
			pb.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
			stampAlphaOp = JAI.create("border", pb);
		}

		// Without these, the composite operation fails.
		stampAlphaOp.getWidth(); // Calc
		stampRgbOp.getWidth(); // Calc
		
		// Separate the alpha channel from the RGB channels of the source, if needed
		RenderedOp srcRGB = this.op;		
		RenderedOp srcAlpha = null;
		if (this.hasAlpha())
		{
			pb = new ParameterBlock();
			pb.addSource(this.op);
			pb.add(MATRIX_ALPHA_ONLY);
			srcAlpha = JAI.create("bandcombine", pb, null);

			pb = new ParameterBlock();
			pb.addSource(this.op);
			pb.add(MATRIX_REMOVE_ALPHA);
			srcRGB = JAI.create("bandcombine", pb, null);
		}
				
		// Combine the original image with the gradient using the alpha map
		// A slight shifting of the original color will occur
		pb = new ParameterBlock();
		pb.addSource(stampRgbOp);
		pb.addSource(srcRGB);
		pb.add(stampAlphaOp);
		pb.add(srcAlpha);
		pb.add(new Boolean(false));
		if (this.hasAlpha())
		{
			pb.add(CompositeDescriptor.DESTINATION_ALPHA_LAST);
		}
		else
		{
			pb.add(CompositeDescriptor.NO_DESTINATION_ALPHA);
		}
				
		this.op = JAI.create("composite", pb);
	}
	
	public void addAlpha()
	{
		if (this.hasAlpha()==false)
		{
			ParameterBlock pb = new ParameterBlock();
			pb.addSource(this.op);
			pb.add(MATRIX_ADD_ALPHA);
			this.op = JAI.create("bandcombine", pb, null);
		}
	}
	
	public void opacity(int pct)
	{
		if (pct==100) return;
		
		double[][] m1 = {	{1D, 0D, 0D, 0D},
							{0D, 1D, 0D, 0D},
							{0D, 0D, 1D, 0D},
							{0D, 0D, 0D, 255D*((double)pct)/100D}};
		double[][] m2 = {	{1D, 0D, 0D, 0D, 0D},
							{0D, 1D, 0D, 0D, 0D},
							{0D, 0D, 1D, 0D, 0D},
							{0D, 0D, 0D, ((double)pct)/100D, 0D}};
		
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(this.op);
		pb.add(hasAlpha()?m2:m1);
		this.op = JAI.create("bandcombine", pb, null);
	}
	
	public void pad(int left, int top, int right, int bottom) throws IOException
	{
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(this.op);
		pb.add(left); // left
		pb.add(right); // right
		pb.add(top); // top
		pb.add(bottom); // bottom
		pb.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
		this.op = JAI.create("border", pb);
		
		// Hack: This will zero-base the coordiate system of the image
		pb = new ParameterBlock();
		pb.add(this.op.getAsBufferedImage());
		this.op = JAI.create("AWTImage", pb);
	}
}
