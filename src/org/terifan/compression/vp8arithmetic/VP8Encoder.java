package org.terifan.compression.vp8arithmetic;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;


/**
 * Bit writing and boolean coder
 *
 * @author Skal (pascal.massimino@gmail.com)
 */
public class VP8Encoder implements AutoCloseable
{
	static int[] kNorm =
	{  // renorm_sizes[i] = 8 - log2(i)
		7, 6, 6, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4,
		3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		0
	};
	// range = ((range + 1) << kVP8Log2Range[range]) - 1
	static int[] kNewRange =
	{
		127, 127, 191, 127, 159, 191, 223, 127, 143, 159, 175, 191, 207, 223, 239, 127, 
		135, 143, 151, 159, 167, 175, 183, 191, 199, 207, 215, 223, 231, 239, 247, 127, 
		131, 135, 139, 143, 147, 151, 155, 159, 163, 167, 171, 175, 179, 183, 187, 191, 
		195, 199, 203, 207, 211, 215, 219, 223, 227, 231, 235, 239, 243, 247, 251, 127, 
		129, 131, 133, 135, 137, 139, 141, 143, 145, 147, 149, 151, 153, 155, 157, 159, 
		161, 163, 165, 167, 169, 171, 173, 175, 177, 179, 181, 183, 185, 187, 189, 191, 
		193, 195, 197, 199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219, 221, 223, 
		225, 227, 229, 231, 233, 235, 237, 239,	241, 243, 245, 247, 249, 251, 253, 127
	};

	private int mRange;      // range-1
	private int mValue;
	private int mRun;        // number of outstanding bits
	private int mNbBits;     // number of pending bits
	private byte[] mBuffer;
	private int mPos;
	private int mMaxPos;
	private OutputStream mOutputStream;


	public VP8Encoder(OutputStream aOutputStream)
	{
		mRange = 255 - 1;
		mNbBits = -8;
		mOutputStream = aOutputStream;
		
		mMaxPos = 1024;
		mBuffer = new byte[mMaxPos];
	}


	public int encodeBit(int bit, int prob) throws IOException
	{
		int split = (mRange * prob) >> 8;
		if (bit != 0)
		{
			mValue += split + 1;
			mRange -= split + 1;
		}
		else
		{
			mRange = split;
		}
		if (mRange < 127)
		{   // emit 'shift' bits out and renormalize
			int shift = kNorm[mRange];
			mRange = kNewRange[mRange];
			mValue <<= shift;
			mNbBits += shift;
			if (mNbBits > 0)
			{
				flush();
			}
		}
		return bit;
	}


	public int encodeBitEqProb(int bit) throws IOException
	{
		int split = mRange >> 1;
		if (bit != 0)
		{
			mValue += split + 1;
			mRange -= split + 1;
		}
		else
		{
			mRange = split;
		}
		if (mRange < 127)
		{
			mRange = kNewRange[mRange];
			mValue <<= 1;
			mNbBits += 1;
			if (mNbBits > 0)
			{
				flush();
			}
		}
		return bit;
	}


	public void encodeValue(int value, int aNumBits) throws IOException
	{
		for (long mask = 1L << (aNumBits - 1); mask != 0; mask >>= 1)
		{
			encodeBitEqProb((int)(value & mask));
		}
	}


	public void close() throws IOException
	{
		encodeValue(0, 9 - mNbBits);
		mNbBits = 0;   // pad with zeroes
		flush();

		if (mPos > 1 && mBuffer[mPos - 1] == 0)
		{
			mPos--;
		}
		
		mOutputStream.write(Arrays.copyOfRange(mBuffer, 0, mPos));
	}


	private void resize() throws IOException
	{
		mOutputStream.write(mBuffer, 0, mPos - 1);
		mBuffer[0] = mBuffer[mPos-1];
		mPos = 1;
	}


	private void flush() throws IOException
	{
		int s = 8 + mNbBits;
		int bits = mValue >> s;

		assert mNbBits >= 0;

		mValue -= bits << s;
		mNbBits -= 8;
		if ((bits & 0xff) != 0xff)
		{
			if (mPos + mRun >= mMaxPos)
			{
				resize(); //mRun + 1
			}
			if ((bits & 0x100) != 0)
			{  // overflow . propagate carry over pending 0xff's
				if (mPos > 0)
				{
					mBuffer[mPos - 1]++;
				}
			}
			if (mRun > 0)
			{
				int value = (bits & 0x100) != 0 ? 0x00 : 0xff;
				for (; mRun > 0; --mRun)
				{
					mBuffer[mPos++] = (byte) value;
				}
			}
			mBuffer[mPos++] = (byte) bits;
		}
		else
		{
			mRun++;   // delay writing of bytes 0xff, pending eventual carry.
		}
	}
}
