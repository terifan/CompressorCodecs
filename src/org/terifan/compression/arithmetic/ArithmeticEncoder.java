package org.terifan.compression.arithmetic;

import java.io.IOException;
import org.terifan.compression.io.BitOutputStream;


public class ArithmeticEncoder
{
	private BitOutputStream mOutputStream;
	private ArithmeticModel mModel;

	
	public ArithmeticEncoder(ArithmeticModel aModel, BitOutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
		mModel = aModel;
	}


	public void encode(ArithmeticContext aContext, int aCharacter) throws IOException
	{
		int symbol = aContext.mCharToSymbol[aCharacter];
		long range = mModel.mHigh - mModel.mLow;
		mModel.mHigh = mModel.mLow + (range * aContext.mSymbolCum[symbol - 1]) / aContext.mSymbolCum[0];
		mModel.mLow += (range * aContext.mSymbolCum[symbol]) / aContext.mSymbolCum[0];

		for (;;)
		{
			if (mModel.mHigh <= ArithmeticModel.Q2)
			{
				writeBit(0);
			}
			else if (mModel.mLow >= ArithmeticModel.Q2)
			{
				writeBit(1);
				mModel.mLow -= ArithmeticModel.Q2;
				mModel.mHigh -= ArithmeticModel.Q2;
			}
			else if (mModel.mLow >= ArithmeticModel.Q1 && mModel.mHigh <= ArithmeticModel.Q3)
			{
				mModel.mShifts++;
				mModel.mLow -= ArithmeticModel.Q1;
				mModel.mHigh -= ArithmeticModel.Q1;
			}
			else
			{
				break;
			}
			mModel.mLow <<= 1;
			mModel.mHigh <<= 1;
		}

		mModel.increment(aContext, symbol);
	}


	public void encodeEnd() throws IOException
	{
		mModel.mShifts++;
		writeBit(mModel.mLow < ArithmeticModel.Q1 ? 0 : 1);
	}


	private void writeBit(long aBit) throws IOException
	{
		long value = (aBit == 0 ? (1L << mModel.mShifts) - 1 : 0) | aBit << mModel.mShifts;
		mOutputStream.writeBits(value, mModel.mShifts + 1);
		mModel.mShifts = 0;
	}
}