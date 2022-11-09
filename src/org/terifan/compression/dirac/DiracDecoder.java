package org.terifan.compression.dirac;

import java.io.IOException;
import org.terifan.compression.io.BitInputStream;


public class DiracDecoder implements AutoCloseable
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


	private long decode(int aBin, int aMaxBin) throws IOException
	{
		int ctx = (aMaxBin + 1);
		long value = 1;
		while (!decodeBit(aBin))
		{
			value <<= 1;
			if (decodeBit(ctx))
			{
				value += 1;
			}
			if (aBin < aMaxBin)
			{
				aBin++;
			}
		}
		value--;
		return value;
	}


	public int decodeUInt(int aBin, int aMaxBin) throws IOException
	{
		return (int)decode(aBin, aMaxBin);
	}


	public int decodeSInt(int aBin, int aMaxBin) throws IOException
	{
		long magnitude = decode(aBin, aMaxBin);
		if (magnitude != 0)
		{
			if (decodeBit(aMaxBin + 2))
			{
				return (int)-magnitude;
			}
			return (int)magnitude;
		}
		return 0;
	}


	@Override
	public void close() throws IOException
	{
		if (mInput != null)
		{
			mInput.close();
			mInput = null;
		}
	}
}
