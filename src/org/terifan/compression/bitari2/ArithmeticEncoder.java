package org.terifan.compression.bitari2;

import java.io.IOException;
import java.io.OutputStream;
import static org.terifan.compression.bitari2.ArithmeticModel.Q1;


public class ArithmeticEncoder implements ArithmeticModel
{
	private final static boolean VERBOSE = false;

	private OutputStream mOutputStream;

	private byte [] mOutBuffer = new byte[8192];
	private int mOutBufferOffset;
	private int mBitsToGo;
	private int mBitBuffer;

	private int mBitsToFollow;
	private int mHigh;
	private int mLow;


	public ArithmeticEncoder(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
		mBitsToGo = 8;
		mHigh = Q4;
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


	public void encodeBit(int aBit, ArithmeticContext aContext) throws IOException
	{
		assert aBit == 0 || aBit == 1;

//		int mid = mLow + (mHigh - mLow) * aContext.mSymbolFreq1 / (aContext.mSymbolFreq0 + aContext.mSymbolFreq1);

		int mid = mLow + (((mHigh - mLow) * aContext.mSymbolFreq1 * tbl[aContext.mSymbolFreq0 + aContext.mSymbolFreq1]) >> 14);

//		System.out.println(mid+" "+midX);

//		int mid = mLow + (((mHigh - mLow) * tbl[(aContext.mSymbolFreq1 & 0x3e0)+((aContext.mSymbolFreq0+aContext.mSymbolFreq1)>>5)]) >> 14);

//		int t = tbl[aContext.mSymbolFreq0 + aContext.mSymbolFreq1];
//		System.out.println(aBit+" ["+aContext.mSymbolFreq1+"]["+(aContext.mSymbolFreq0+aContext.mSymbolFreq1)+"]="+t/(double)(1<<13)+" "+t+" low="+mLow+" mid="+mid+" high="+mHigh+" Q="+Q1+"/"+Q2+"/"+Q3+"/"+Q4);

//		if (mid <= mLow || mid >= mHigh) System.exit(0);

		if (aBit != 0)
		{
			mHigh = mid;
			aContext.mSymbolFreq1++;
		}
		else
		{
			mLow = mid;
			aContext.mSymbolFreq0++;
		}

		if (aContext.mSymbolFreq0 + aContext.mSymbolFreq1 >= MAX_CUMULATIVE_FREQUENCY)
		{
			aContext.scale();
		}

		if (mLow < Q1 && mHigh > Q3)
		{
			return;
		}

		for (;;)
		{
			if (mHigh <= Q2)
			{
				putOneBitPlusOutstanding(0);
			}
			else if (mLow >= Q2)
			{
				putOneBitPlusOutstanding(1);
				mLow -= Q2;
				mHigh -= Q2;
			}
			else if (mLow >= Q1 && mHigh <= Q3)
			{
				mBitsToFollow++;
				mLow -= Q1;
				mHigh -= Q1;
			}
			else
			{
				break;
			}
			mLow <<= 1;
			mHigh <<= 1;
		}
	}



	/**
	 * Write single bit.
	 *
	 * @param aBit
	 *   one or zero
	 * @param aProb
	 *   the probability the bit is a zero ranging from 0 to 255. The decoder must use the same probability to decode the bit.
	 */
	public void encodeBit(int aBit, int aProb) throws IOException
	{
		assert aBit == 0 || aBit == 1;
		assert aProb >= 0 && aProb < 256;

		int mid = mLow + (((mHigh - mLow) * aProb) >> 8);

		if (aBit != 0)
		{
			mHigh = mid;
		}
		else
		{
			mLow = mid;
		}

		if (mLow < Q1 && mHigh > Q3)
		{
			return;
		}

		for (;;)
		{
			if (mHigh <= Q2)
			{
				putOneBitPlusOutstanding(0);
			}
			else if (mLow >= Q2)
			{
				putOneBitPlusOutstanding(1);
				mLow -= Q2;
				mHigh -= Q2;
			}
			else if (mLow >= Q1 && mHigh <= Q3)
			{
				mBitsToFollow++;
				mLow -= Q1;
				mHigh -= Q1;
			}
			else
			{
				break;
			}
			mLow <<= 1;
			mHigh <<= 1;
		}
	}


	public void encodeBitEqProb(int aBit) throws IOException
	{
		assert aBit == 0 || aBit == 1;

		int mid = mLow + ((mHigh - mLow) >> 1);

		if (aBit != 0)
		{
			mHigh = mid;
		}
		else
		{
			mLow = mid;
		}

		if (mLow < Q1 && mHigh > Q3)
		{
			return;
		}

		for (;;)
		{
			if (mHigh <= Q2)
			{
				putOneBitPlusOutstanding(0);
			}
			else if (mLow >= Q2)
			{
				putOneBitPlusOutstanding(1);
				mLow -= Q2;
				mHigh -= Q2;
			}
			else if (mLow >= Q1 && mHigh <= Q3)
			{
				mBitsToFollow++;
				mLow -= Q1;
				mHigh -= Q1;
			}
			else
			{
				break;
			}
			mLow <<= 1;
			mHigh <<= 1;
		}
	}


	public void encodeExpGolomb(long aSymbol, int aStep, ArithmeticContext aContext) throws IOException
	{
		encodeBitEqProb((int)aSymbol & 1);
		aSymbol >>>= 1;

		while (aSymbol >= (1L << aStep))
		{
			encodeBit(1, aContext);

			aSymbol -= 1L << aStep;
			aStep++;
		}

		encodeBit(0, aContext);

		while (aStep-- > 0)
		{
			encodeBitEqProb((int)(aSymbol >>> aStep) & 1);
		}
	}


	public void encodeExpGolombEqProb(long aSymbol, int aStep) throws IOException
	{
		while (aSymbol >= (1L << aStep))
		{
			encodeBitEqProb(1);

			aSymbol -= 1L << aStep;
			aStep++;
		}

		encodeBitEqProb(0);

		while (aStep-- > 0)
		{
			encodeBitEqProb((int)(aSymbol >>> aStep) & 1);
		}
	}


	public void encodeBytesEqProb(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		for (int i = 0; i < aLength; i++)
		{
			int b = 255 & aBuffer[i];
			for (int j = 0; j < 8; j++)
			{
				encodeBitEqProb(b & 1);
				b >>= 1;
			}
		}
	}


	public void encodeEqProb(long aSymbol, int aNumBits) throws IOException
	{
		for (int i = 0; i < aNumBits; i++)
		{
			encodeBitEqProb((int)(aSymbol & 1));
			aSymbol >>= 1;
		}
	}


	public void encodeProb(int[] aSymbol, int aBitsPerSymbol, int aWindow, ArithmeticContext[] aContext) throws IOException
	{
		assert aContext.length == 1 << aWindow;

		int mask = ((1 << aWindow)-1);
		for (int i = 0, ctx = 0; i < aSymbol.length; i++)
		{
			for (int j = 0; j < aBitsPerSymbol; j++)
			{
				int b = (aSymbol[i] >>> j) & 1;
				encodeBit(b, aContext[ctx & mask]);
				ctx <<= 1;
				ctx |= b;
			}
		}
	}


	private void putOneBitPlusOutstanding(int aBit) throws IOException
	{
		putOneBit(aBit);

		for (int i = 0; i < mBitsToFollow; i++)
		{
			putOneBit(1 - aBit);
		}

		mBitsToFollow = 0;
	}


	private void putOneBit(int aBit) throws IOException
	{
		if (VERBOSE) System.out.print(aBit);

		mBitBuffer = (mBitBuffer << 1) | aBit;
		mBitsToGo--;

		if (mBitsToGo == 0)
		{
			putByte();
		}
	}


	private void putByte() throws IOException
	{
		mOutBuffer[mOutBufferOffset++] = (byte)mBitBuffer;
		mBitsToGo = 8;

		if (mOutBufferOffset >= mOutBuffer.length)
		{
			flushBuffer();
		}
	}


	private void flushBuffer() throws IOException
	{
		mOutputStream.write(mOutBuffer, 0, mOutBufferOffset);
		mOutBufferOffset = 0;
	}


	public OutputStream getOutputStream()
	{
		return mOutputStream;
	}


	public void stopEncoding() throws IOException
	{
		mBitsToFollow++;
		putOneBitPlusOutstanding(mLow < Q1 ? 0 : 1);

		while (mBitsToGo < 8)
		{
			putOneBit(0);
		}

		flushBuffer();
	}
}