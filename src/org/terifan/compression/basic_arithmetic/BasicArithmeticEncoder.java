package org.terifan.compression.basic_arithmetic;

import java.io.IOException;
import org.terifan.compression.io.BitOutputStream;


public class BasicArithmeticEncoder
{
	private BitOutputStream mOutputStream;
	private BasicArithmeticModel mModel;


	public BasicArithmeticEncoder(BitOutputStream aOutputStream)
	{
		this(aOutputStream, new BasicArithmeticModel());
	}


	public BasicArithmeticEncoder(BitOutputStream aOutputStream, BasicArithmeticModel aModel)
	{
		mOutputStream = aOutputStream;
		mModel = aModel;
	}


	public void encode(int aCharacter, BasicArithmeticContext aContext) throws IOException
	{
		int symbol = aContext.mCharToSymbol[aCharacter];
		long range = mModel.mHigh - mModel.mLow;
		mModel.mHigh = mModel.mLow + (range * aContext.mSymbolCum[symbol - 1]) / aContext.mSymbolCum[0];
		mModel.mLow += (range * aContext.mSymbolCum[symbol]) / aContext.mSymbolCum[0];

		for (;;)
		{
			if (mModel.mHigh <= BasicArithmeticModel.Q2)
			{
				writeBit(0);
			}
			else if (mModel.mLow >= BasicArithmeticModel.Q2)
			{
				writeBit(1);
				mModel.mLow -= BasicArithmeticModel.Q2;
				mModel.mHigh -= BasicArithmeticModel.Q2;
			}
			else if (mModel.mLow >= BasicArithmeticModel.Q1 && mModel.mHigh <= BasicArithmeticModel.Q3)
			{
				mModel.mShifts++;
				mModel.mLow -= BasicArithmeticModel.Q1;
				mModel.mHigh -= BasicArithmeticModel.Q1;
			}
			else
			{
				break;
			}
			mModel.mLow <<= 1;
			mModel.mHigh <<= 1;
		}

		if (aContext.isAdaptive())
		{
			mModel.increment(aContext, symbol);
		}
	}


	public void close() throws IOException
	{
		mModel.mShifts++;
		writeBit(mModel.mLow < BasicArithmeticModel.Q1 ? 0 : 1);
	}


	private void writeBit(long aBit) throws IOException
	{
		long value = (aBit == 0 ? (1L << mModel.mShifts) - 1 : 0) | aBit << mModel.mShifts;
		mOutputStream.writeBits(value, mModel.mShifts + 1);
		mModel.mShifts = 0;
	}
}