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


	public int decodeBit(int aProb) throws IOException
	{
		int split = (mRange * aProb) >> 8;

		int valueSplit = (split + 1) << 8;

		if (mMissing > 0)
		{
			mValue |= (0xFF & mInputStream.read()) << mMissing;
			mMissing -= 8;
		}

		int bit = (mValue >= valueSplit) ? 1 : 0;

		if (bit != 0)
		{
			mRange -= split + 1;
			mValue -= valueSplit;
		}
		else
		{
			mRange = split;
		}

		if (mRange < 0x7f)
		{
			int shift = KNORM[mRange];
			mRange = KNEWRANGE[mRange];
			mValue <<= shift;
			mMissing += shift;
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


	public long decodeExpGolomb(int aStep) throws IOException
	{
		int x = decodeBitEqProb();

		long result = 0;

		while (decodeBit(240) == 0)
		{
			result += 1L << aStep;
			aStep++;
		}

		long binarySymbol = 0;
		while (aStep-- > 0)
		{
			if (decodeBitEqProb()== 1)
			{
				binarySymbol |= 1L << aStep;
			}
		}

		return ((result + binarySymbol) << 1) + x;
	}


	public int decodeUnary() throws IOException
	{
		int symbol = 0;

		while (decodeBit(240) == 0)
		{
			symbol++;
		}

		return symbol;
	}


	@Override
	public void close() throws IOException
	{
		mInputStream.close();
	}
}
