package samoyan.core.image;


/**
 * Resize the image to fit within <code>maxWidth</code> and <code>maxHeight</code>. The aspect ratio
 * of the original image is maintained.
 * If the image is small enough to fit within <code>maxWidth</code> and <code>maxHeight</code>,
 * no action is taken.
 * @author brian
 *
 */
public class ShrinkToFitSizer implements ImageSizer
{
	private int width;
	private int height;
	
	public ShrinkToFitSizer(int maxWidth, int maxHeight)
	{
		this.width = maxWidth;
		this.height = maxHeight;
	}

	@Override
	public JaiImage process(JaiImage src, int pixelRatio) throws Exception
	{
		int w = this.width * pixelRatio;
		int h = this.height * pixelRatio;
		
		src.resizeToFit(w, h);
		
		return src;
	}
}
