package org.terifan.compression.basic_arithmetic;

import java.io.IOException;
import org.terifan.compression.io.BitInputStream;


public class BasicArithmeticDecoder
{
	private BitInputStream mInputStream;
	private BasicArithmeticModel mModel;


	public BasicArithmeticDecoder(BasicArithmeticModel aModel, BitInputStream aInputStream) throws IOException
	{
		aInputStream.setReturnZeroOnEOF(true);

		mModel = aModel;
		mInputStream = aInputStream;
		for (int i = 0; i < BasicArithmeticModel.CODE_VALUE_SIZE + 2; i++)
		{
			mModel.mValue <<= 1;
			mModel.mValue += readBit();
		}
	}


	public int decode(BasicArithmeticContext aContext) throws IOException
	{
		long range = mModel.mHigh - mModel.mLow;
		int symbol = binarySearchSymbol(aContext, ((mModel.mValue - mModel.mLow + 1) * aContext.mSymbolCum[0] - 1) / range);
		mModel.mHigh = mModel.mLow + (range * aContext.mSymbolCum[symbol - 1]) / aContext.mSymbolCum[0];
		mModel.mLow += (range * aContext.mSymbolCum[symbol]) / aContext.mSymbolCum[0];

		for (;;)
		{
			if (mModel.mLow >= BasicArithmeticModel.Q2)
			{
				mModel.mValue -= BasicArithmeticModel.Q2;
				mModel.mLow -= BasicArithmeticModel.Q2;
				mModel.mHigh -= BasicArithmeticModel.Q2;
			}
			else if (mModel.mLow >= BasicArithmeticModel.Q1 && mModel.mHigh <= BasicArithmeticModel.Q3)
			{
				mModel.mValue -= BasicArithmeticModel.Q1;
				mModel.mLow -= BasicArithmeticModel.Q1;
				mModel.mHigh -= BasicArithmeticModel.Q1;
			}
			else if (mModel.mHigh > BasicArithmeticModel.Q2)
			{
				break;
			}
			mModel.mLow <<= 1;
			mModel.mHigh <<= 1;
			mModel.mValue <<= 1;
			mModel.mValue += readBit();
		}

		int character = aContext.mSymbolToChar[symbol];

		if (aContext.isAdaptive())
		{
			mModel.increment(aContext, symbol);
		}

		return character;
	}


	private int binarySearchSymbol(BasicArithmeticContext aContext, long aSymbolCumFreq)
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