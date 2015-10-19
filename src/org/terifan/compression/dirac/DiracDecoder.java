package org.terifan.compression.dirac;

import java.io.IOException;
import org.terifan.compression.io.BitInputStream;


public class DiracDecoder
{
	private DiracContext[] mContextList;
	private BitInputStream mInput;
    private int mCode;
	private int mRange;
	private int mLowCode;

	
	public DiracDecoder(BitInputStream aInput, int aContextCount) throws IOException
	{
		mInput = aInput;
		mRange = 0xFFFF;
		
		mInput.setReturnZeroOnEOF(true);

		mCode = 0;
		for (int i = 0; i < 16; ++i)
		{
			mCode <<= 1;
			mCode += mInput.readBit();
		}

		mContextList = new DiracContext[aContextCount];

		for (int i = 0; i < aContextCount; i++)
		{
			mContextList[i] = new DiracContext();
		}
	}


	public boolean decodeBit(int aContextNum) throws IOException
	{
		DiracContext ctx = mContextList[aContextNum];

        int count = mCode - mLowCode;
		int range_x_prob = (mRange * ctx.getScaledProb()) >>> 16;
		boolean symbol = count >= range_x_prob;

		if (symbol)
		{
			mLowCode += range_x_prob;
			mRange -= range_x_prob;
		}
		else
		{
			mRange = range_x_prob;
		}

		ctx.update(symbol);

		while (mRange <= 0x4000)
		{
			if (((mLowCode + mRange - 1) ^ mLowCode) >= 0x8000)
			{
				mCode ^= 0x4000;
				mLowCode ^= 0x4000;
			}

			mLowCode <<= 1;
			mRange <<= 1;
			mLowCode &= 0xFFFF;

			mCode <<= 1;
			mCode += mInput.readBit();
			mCode &= 0xFFFF;
		}

		return symbol;
	}


	private long decode(int bin, int max_bin) throws IOException
	{
		int info_ctx = (max_bin + 1);
		long value = 1;
		while (!decodeBit(bin))
		{
			value <<= 1;
			if (decodeBit(info_ctx))
			{
				value += 1;
			}
			if (bin < max_bin)
			{
				bin += 1;
			}
		}
		value -= 1;
		return value;
	}
	
	
	public int decodeUInt(int bin, int max_bin) throws IOException
	{
		return (int)decode(bin, max_bin);
	}


	public int decodeSInt(int bin, int max_bin) throws IOException
	{
		long magnitude = decode(bin, max_bin);
		if (magnitude != 0)
		{
			if (decodeBit(max_bin + 2))
			{
				return (int)-magnitude;
			}
			else
			{
				return (int)magnitude;
			}
		}
		return 0;
	}
}
