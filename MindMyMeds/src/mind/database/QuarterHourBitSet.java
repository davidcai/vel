package mind.database;

public class QuarterHourBitSet
{
	private byte[] bitmap;
	
	QuarterHourBitSet()
	{
		this.bitmap = new byte[12];
	}

	QuarterHourBitSet(byte[] bytes)
	{
		if (bytes==null)
		{
			this.bitmap = new byte[12];
		}
		else
		{
			this.bitmap = new byte[bytes.length];
			System.arraycopy(bytes, 0, this.bitmap, 0, this.bitmap.length);
		}
	}
	
	void setBitmap(byte[] bytes)
	{
		this.bitmap = bytes;
	}
	
	byte[] getBitmap()
	{
		return this.bitmap;
	}
	
	/**
	 * Turn on the bit for the given quarter hour period.
	 * @param hr A number from 0-23.
	 * @param min 0, 15, 30 or 45.
	 * @return
	 */
	public QuarterHourBitSet set(int hr, int min)
	{
		int offset = hr*4 + min/15;
		int byteIndex = offset / 8;
		int bitIndex = offset % 8;
		
		bitmap[byteIndex] = (byte) (bitmap[byteIndex] | (byte) (1<<bitIndex));

		return this;
	}
	
	public QuarterHourBitSet clear(int hr, int min)
	{
		int offset = hr*4 + min/15;
		int byteIndex = offset / 8;
		int bitIndex = offset % 8;
		
		bitmap[byteIndex] = (byte) (bitmap[byteIndex] & ~(byte) (1<<bitIndex)); 

		return this;
	}

	public boolean get(int hr, int min)
	{
		int offset = hr*4 + min/15;
		int byteIndex = offset / 8;
		int bitIndex = offset % 8;
		
		return (bitmap[byteIndex] & ((byte) 1)<<bitIndex) != 0;
	}
}
