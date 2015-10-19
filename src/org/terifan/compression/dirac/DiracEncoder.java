package org.terifan.compression.dirac;

import java.io.IOException;
import org.terifan.compression.io.BitOutputStream;


public class DiracEncoder
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
				// Bits agree - output them
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


	public void encodeUInt(long the_int, int bin, int max_bin) throws IOException
	{
		long value = the_int + 1;
		int info_ctx = max_bin + 1;
		long top_bit = 1;
		{
			long max_value = 1;
			while (value > max_value)
			{
				top_bit <<= 1;
				max_value <<= 1;
				max_value += 1;
			}
		}
		boolean stop = top_bit == 1;
		encodeBit(stop, bin);
		while (!stop)
		{
			top_bit >>= 1;
			encodeBit((value & top_bit) != 0, info_ctx);
			if (bin < max_bin)
			{
				bin++;
			}
			stop = top_bit == 1;
			encodeBit(stop, bin);
		}
	}


	public void encodeSInt(int value, int bin1, int max_bin) throws IOException
	{
		encodeUInt(Math.abs((long)value), bin1, max_bin);

		if (value != 0)
		{
			encodeBit((value < 0), max_bin + 2);
		}
	}


	public void finish() throws IOException
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
	}
}
