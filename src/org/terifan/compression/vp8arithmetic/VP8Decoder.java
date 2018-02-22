package org.terifan.compression.vp8arithmetic;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.compression.vp8arithmetic.Shared.*;


public class VP8Decoder implements AutoCloseable
{
	private InputStream mInputStream;
	private int mRange;            // current range minus 1. In [127, 254] interval.
	private int mValue;            // current value
	private int mMissing;          // number of missing bits in value_ (8bit)


	public VP8Decoder(InputStream aInputStream)
	{
		mRange = 255 - 1;
		mInputStream = aInputStream;
		mMissing = 8;
	}


	public int decodeBit(int prob) throws IOException
	{
		int split = (mRange * prob) >> 8;
		int bit = update(split);
		if (mRange < 0x7f)
		{
			shift();
		}
		return bit;
	}


	public int decodeBitEqProb() throws IOException
	{
		return decodeBit(0x80);
	}


	public int decodeValue(int aNumBits) throws IOException
	{
		int v = 0;
		while (aNumBits-- > 0)
		{
			v |= decodeBitEqProb() << aNumBits;
		}
		return v;
	}


	private int update(int split) throws IOException
	{
		int bit;
		int value_split = (split + 1) << 8;

		if (mMissing > 0)
		{
			mValue |= (0xFF & mInputStream.read()) << mMissing;
			mMissing -= 8;
		}
		bit = (mValue >= value_split) ? 1 : 0;
		if (bit != 0)
		{
			mRange -= split + 1;
			mValue -= value_split;
		}
		else
		{
			mRange = split;
		}
		return bit;
	}


	private void shift()
	{
		int shift = KNORM[mRange];
		mRange = KNEWRANGE[mRange];
		mValue <<= shift;
		mMissing += shift;
	}


	@Override
	public void close() throws IOException
	{
		mInputStream.close();
	}
}
