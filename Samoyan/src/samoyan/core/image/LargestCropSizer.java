package samoyan.core.image;


/**
 * Extracts the largest part of the image that matches the width:height aspect ratio.
 * The part is taken from the center of the image and scaled to width x height.
 * For example, extracting a 50x50 box from a 200x100 image will take the central 100x100 section of the original image
 * and scale it down by half.
 * @author brian
 *
 */
public class LargestCropSizer implements ImageSizer
{
	private int width;
	private int height;
	
	public LargestCropSizer(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	@Override
	public JaiImage process(JaiImage src, float pixelRatio) throws Exception
	{
		int w = Math.round( this.width * pixelRatio );
		int h = Math.round( this.height * pixelRatio );
		
		src.thumbnail(w, h);
		
		return src;
	}
}
