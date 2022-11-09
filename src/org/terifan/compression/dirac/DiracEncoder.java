package org.terifan.compression.dirac;

import java.io.IOException;
import org.terifan.compression.io.BitOutputStream;


public class DiracEncoder implements AutoCloseable
{
	private DiracContext[] mContextList;
	private BitOutputStream mOutput;
	private int mRange;
	private int mLowCode;
	private int mUnderflow;


	public DiracEncoder(BitOutputStream out, int aContextCount)
	{
		mOutput = out;
		mRange = 0xFFFF;

		mContextList = new DiracContext[aContextCount];

		for (int i = 0; i < aContextCount; i++)
		{
			mContextList[i] = new DiracContext();
		}
	}


	public void encodeBit(int aSymbol, int aContextNum) throws IOException
	{
		encodeBit(aSymbol != 0, aContextNum);
	}


	public void encodeBit(boolean aSymbol, int aContextNum) throws IOException
	{
		DiracContext ctx = mContextList[aContextNum];

		int range_x_prob = (mRange * ctx.getScaledProb()) >>> 16;

		if (aSymbol)
		{
			mLowCode += range_x_prob;
			mRange -= range_x_prob;
		}
		else
		{
			mRange = range_x_prob;
		}

		ctx.update(aSymbol);

		while (mRange <= 0x4000)
		{
			if (((mLowCode + mRange - 1) ^ mLowCode) >= 0x8000)
			{
				mLowCode ^= 0x4000;
				mUnderflow++;
			}
			else
			{
				mOutput.writeBit((mLowCode & 0x8000) != 0 ? 1 : 0);

				for (; mUnderflow > 0; mUnderflow--)
				{
					mOutput.writeBit((~mLowCode & 0x8000) != 0 ? 1 : 0);
				}
			}

			mLowCode <<= 1;
			mRange <<= 1;
			mLowCode &= 0xFFFF;
		}
	}


	public void encodeUInt(long aValue, int aBin, int aMaxBin) throws IOException
	{
		long value = aValue + 1;
		int ctx = aMaxBin + 1;
		long topBit = 1;

		long maxValue = 1;
		while (value > maxValue)
		{
			topBit <<= 1;
			maxValue <<= 1;
			maxValue++;
		}

		boolean stop = topBit == 1;
		encodeBit(stop, aBin);

		while (!stop)
		{
			topBit >>>= 1;
			encodeBit((value & topBit) != 0, ctx);
			if (aBin < aMaxBin)
			{
				aBin++;
			}
			stop = topBit == 1;
			encodeBit(stop, aBin);
		}
	}


	public void encodeSInt(int aValue, int aBin, int aMaxBin) throws IOException
	{
		encodeUInt(Math.abs((long)aValue), aBin, aMaxBin);

		if (aValue != 0)
		{
			encodeBit((aValue < 0), aMaxBin + 2);
		}
	}


	@Override
	public void close() throws IOException
	{
		if (mOutput != null)
		{
			while (((mLowCode + mRange - 1) ^ mLowCode) < 0x8000)
			{
				mOutput.writeBit((mLowCode & 0x8000) != 0 ? 1 : 0);
				for (; mUnderflow > 0; mUnderflow--)
				{
					mOutput.writeBit((~mLowCode & 0x8000) != 0 ? 1 : 0);
				}

				mLowCode <<= 1;
				mLowCode &= 0xFFFF;
				mRange <<= 1;
			}

			while (((mLowCode & 0x4000) != 0) && !(((mLowCode + mRange - 1) & 0x4000) != 0))
			{
				mUnderflow++;
				mLowCode ^= 0x4000;
				mLowCode <<= 1;
				mLowCode &= 0xFFFF;
				mRange <<= 1;
			}

			mOutput.writeBit((mLowCode & 0x4000) != 0 ? 1 : 0);
			while (mUnderflow >= 0)
			{
				mOutput.writeBit((~mLowCode & 0x4000) != 0 ? 1 : 0);
				mUnderflow--;
			}

			mOutput.close();
			mOutput = null;
		}
	}
}