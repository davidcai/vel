package samoyan.core.image;


public interface ImageSizer
{
	/**
	 * Subclasses should implement this method to process the <code>src</code> image.
	 * @param src The <code>JaiImage</code> that will be manipulated. This method is not required to make a copy.
	 * @param pixelRatio Implementations should scale the result by a multiple of <code>pixelRatio</code>.
	 * For example, Retina displays and other high DPI screens will have a <code>pixelRatio</code> of 2.
	 * @return The processed image, typically <code>src</code> after manipulation.
	 * @throws Exception
	 */
	public JaiImage process(JaiImage src, float pixelRatio) throws Exception;
}
