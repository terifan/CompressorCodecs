package org.terifan.compression.arithmetic;

import java.io.IOException;
import org.terifan.compression.io.BitInputStream;


public class ArithmeticDecoder
{
	private BitInputStream mInputStream;
	private ArithmeticModel mModel;


	public ArithmeticDecoder(ArithmeticModel aModel, BitInputStream aInputStream) throws IOException
	{
		aInputStream.setReturnZeroOnEOF(true);

		mModel = aModel;
		mInputStream = aInputStream;
		for (int i = 0; i < ArithmeticModel.CODE_VALUE_SIZE + 2; i++)
		{
			mModel.mValue <<= 1;
			mModel.mValue += readBit();
		}
	}


	public int decode(ArithmeticContext aContext) throws IOException
	{
		long range = mModel.mHigh - mModel.mLow;
		int symbol = binarySearchSymbol(aContext, ((mModel.mValue - mModel.mLow + 1) * aContext.mSymbolCum[0] - 1) / range);
		mModel.mHigh = mModel.mLow + (range * aContext.mSymbolCum[symbol - 1]) / aContext.mSymbolCum[0];
		mModel.mLow += (range * aContext.mSymbolCum[symbol]) / aContext.mSymbolCum[0];

		for (;;)
		{
			if (mModel.mLow >= ArithmeticModel.Q2)
			{
				mModel.mValue -= ArithmeticModel.Q2;
				mModel.mLow -= ArithmeticModel.Q2;
				mModel.mHigh -= ArithmeticModel.Q2;
			}
			else if (mModel.mLow >= ArithmeticModel.Q1 && mModel.mHigh <= ArithmeticModel.Q3)
			{
				mModel.mValue -= ArithmeticModel.Q1;
				mModel.mLow -= ArithmeticModel.Q1;
				mModel.mHigh -= ArithmeticModel.Q1;
			}
			else if (mModel.mHigh > ArithmeticModel.Q2)
			{
				break;
			}
			mModel.mLow <<= 1;
			mModel.mHigh <<= 1;
			mModel.mValue <<= 1;
			mModel.mValue += readBit();
		}

		int character = aContext.mSymbolToChar[symbol];
		mModel.increment(aContext, symbol);
		return character;
	}


	private int binarySearchSymbol(ArithmeticContext aContext, long aSymbolCumFreq)
	{
		int min = 1;
		int max = aContext.mSymbolCount;
		while (min < max)
		{
			int mid = (min + max) >> 1;
			if (aContext.mSymbolCum[mid] > aSymbolCumFreq)
			{
				min = mid + 1;
			}
			else
			{
				max = mid;
			}
		}
		return min;
	}


	private int readBit() throws IOException
	{
		return mInputStream.readBit();
	}
}