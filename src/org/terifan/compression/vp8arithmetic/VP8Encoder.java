package org.terifan.compression.vp8arithmetic;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import static org.terifan.compression.vp8arithmetic.Shared.*;


/**
 * Bit writing and boolean coder
 *
 * @author Skal (pascal.massimino@gmail.com)
 */
public class VP8Encoder implements AutoCloseable
{
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


	@Override
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
