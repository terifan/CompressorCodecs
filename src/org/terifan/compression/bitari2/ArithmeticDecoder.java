package org.terifan.compression.bitari2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class ArithmeticDecoder implements ArithmeticModel
{
	private InputStream mInputStream;

	private byte [] mInBuffer = new byte[8192];
	private int mInBufferOffset;
	private int mInBufferLength;
	private int mBitBuffer;
	private int mBitsToGo;

	private int mHigh;
	private int mLow;
	private int mValue;


	public ArithmeticDecoder(InputStream aInputStream) throws IOException
	{
		mInputStream = aInputStream;
//		mInBufferLength = -1;
		mBitsToGo = 0;
		mHigh = Q4;

		for (int i = 0; i < CODE_VALUE_SIZE + 2; i++)
		{
			mValue <<= 1;
			mValue += readBit();
		}
	}

//	private final static int [] tbl = new int[(MAX_CUMULATIVE_FREQUENCY>>5)*(MAX_CUMULATIVE_FREQUENCY>>5)];
//	static
//	{
//		for (int i = 0; i < MAX_CUMULATIVE_FREQUENCY>>5; i++)
//		{
//			for (int j = 0; j < MAX_CUMULATIVE_FREQUENCY>>5; j++)
//			{
//				tbl[(i<<5)+j] = (int)Math.round((Math.max(1,i<<0) * (1 << 14)) / (double)(Math.max(2,(i+j)<<0)));
//			}
//		}
//	}
	private final static int [] tbl = new int[MAX_CUMULATIVE_FREQUENCY];
	static
	{
		for (int i = 0; i < MAX_CUMULATIVE_FREQUENCY; i++)
		{
			tbl[i] = (1 << 14) / (i <= 1 ? 2 : i);
		}
	}

	public int decodeBit(ArithmeticContext aContext) throws IOException
	{
		int bit;

//		int mid = mLow + (mHigh - mLow) * aContext.mSymbolFreq1 / (aContext.mSymbolFreq0 + aContext.mSymbolFreq1);

		int mid = mLow + (((mHigh - mLow) * aContext.mSymbolFreq1 * tbl[aContext.mSymbolFreq0 + aContext.mSymbolFreq1]) >> 14);

//		int mid = mLow + (((mHigh - mLow) * tbl[(aContext.mSymbolFreq1 & 0x3e0)+((aContext.mSymbolFreq0+aContext.mSymbolFreq1)>>5)]) >> 14);

		if (mValue >= mid)
		{
			bit = 0;
			mLow = mid;
			aContext.mSymbolFreq0++;
		}
		else
		{
			bit = 1;
			mHigh = mid;
			aContext.mSymbolFreq1++;
		}

		if (aContext.mSymbolFreq0 + aContext.mSymbolFreq1 >= MAX_CUMULATIVE_FREQUENCY)
		{
			aContext.scale();
		}

		if (mLow < Q1 && mHigh > Q3)
		{
			return bit;
		}

		for (;;)
		{
			if (mLow >= Q2)
			{
				mValue -= Q2;
				mLow -= Q2;
				mHigh -= Q2;
			}
			else if (mLow >= Q1 && mHigh <= Q3)
			{
				mValue -= Q1;
				mLow -= Q1;
				mHigh -= Q1;
			}
			else if (mHigh > Q2)
			{
				break;
			}
			mLow <<= 1;
			mHigh <<= 1;
			mValue <<= 1;
			mValue += readBit();
		}

		return bit;
	}



	public int decodeBit(int aProb) throws IOException
	{
		int mid = mHigh - (((mHigh - mLow) * aProb) >> 8);
		int bit;

		if (mValue >= mid)
		{
			bit = 0;
			mLow = mid;
		}
		else
		{
			bit = 1;
			mHigh = mid;
		}

		if (mLow < Q1 && mHigh > Q3)
		{
			return bit;
		}

		for (;;)
		{
			if (mLow >= Q2)
			{
				mValue -= Q2;
				mLow -= Q2;
				mHigh -= Q2;
			}
			else if (mLow >= Q1 && mHigh <= Q3)
			{
				mValue -= Q1;
				mLow -= Q1;
				mHigh -= Q1;
			}
			else if (mHigh > Q2)
			{
				break;
			}
			mLow <<= 1;
			mHigh <<= 1;
			mValue <<= 1;
			mValue += readBit();
		}

		return bit;
	}


	public int decodeBitEqProb() throws IOException
	{
		int bit;

		int mid = mLow + ((mHigh - mLow) >> 1);

		if (mValue >= mid)
		{
			bit = 0;
			mLow = mid;
		}
		else
		{
			bit = 1;
			mHigh = mid;
		}

		if (mLow < Q1 && mHigh > Q3)
		{
			return bit;
		}

		for (;;)
		{
			if (mLow >= Q2)
			{
				mValue -= Q2;
				mLow -= Q2;
				mHigh -= Q2;
			}
			else if (mLow >= Q1 && mHigh <= Q3)
			{
				mValue -= Q1;
				mLow -= Q1;
				mHigh -= Q1;
			}
			else if (mHigh > Q2)
			{
				break;
			}
			mLow <<= 1;
			mHigh <<= 1;
			mValue <<= 1;
			mValue += readBit();
		}

		return bit;
	}


	public long decodeExpGolomb(int K, ArithmeticContext aContext) throws IOException
	{
		int x = decodeBitEqProb();

		int L;
		long binarySymbol = 0;
		long result = 0;

		do
		{
			L = decodeBit(aContext);
			if (L == 1)
			{
				result += 1L << K;
				K++;
			}
		}
		while (L != 0);

		while (K != 0)
		{
			K--;
			if (decodeBitEqProb() == 1)
			{
				binarySymbol |= 1L << K;
			}
		}

		return ((result + binarySymbol) << 1) + x;
	}


	public long decodeExpGolombEqProb(int K) throws IOException
	{
		int L;
		long binarySymbol = 0;
		long result = 0;

		do
		{
			L = decodeBitEqProb();
			if (L == 1)
			{
				result += 1L << K;
				K++;
			}
		}
		while (L != 0);

		while (K != 0)
		{
			K--;
			if (decodeBitEqProb() == 1)
			{
				binarySymbol += 1L << K;
			}
		}

		return result + binarySymbol;
	}


	public void decodeBytesEqProb(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		for (int i = 0; i < aLength; i++)
		{
			int b = 0;
			for (int j = 0; j < 8; j++)
			{
				b += decodeBitEqProb() << j;
			}
			aBuffer[i] = (byte)b;
		}
	}


	public long decodeEqProb(int aNumBits) throws IOException
	{
		long symbol = 0;

		for (int i = 0; i < aNumBits; i++)
		{
			symbol |= ((long)decodeBitEqProb()) << i;
		}

		return symbol;
	}


	public void decodeProb(int[] output, int aOutputLength, int aBitsPerSymbol, int aWindow, ArithmeticContext[] aContext) throws IOException
	{
		assert aContext.length == 1 << aWindow;

		int mask = ((1 << aWindow)-1);
		for (int i = 0, ctx = 0; i < aOutputLength; i++)
		{
			int symbol = 0;
			for (int j = 0; j < aBitsPerSymbol; j++)
			{
				int b = decodeBit(aContext[ctx & mask]);
				symbol += b << j;
				ctx <<= 1;
				ctx |= b;
			}
			output[i] = symbol;
		}
	}


	private int readBit() throws IOException
	{
		mBitsToGo--;
		if (mBitsToGo < 0)
		{
			getByte();
		}
		return (mBitBuffer >>> mBitsToGo) & 1;
	}


	private void getByte() throws IOException
	{
		if (mInBufferOffset >= mInBufferLength)
		{
			readBuffer();
		}

		mBitBuffer = 255 & mInBuffer[mInBufferOffset++];
		mBitsToGo = 7;
	}


	private void readBuffer() throws IOException
	{
		mInBufferLength = mInputStream.read(mInBuffer);
		mInBufferOffset = 0;

		if (mInBufferLength <= 0)
		{
			Arrays.fill(mInBuffer, (byte)0);
			mInBufferLength = mInBuffer.length;
		}
	}


	public void stopDecoding()
	{
	}
}