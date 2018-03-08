package org.terifan.compression.bitari;

import java.io.IOException;
import java.io.OutputStream;
import static org.terifan.compression.bitari.ArithmeticModel.*;


public class ArithmeticEncoder
{
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

	public void encode(int aBit, ArithmeticContext aContext) throws IOException
	{
//		int mid = mLow + (mHigh - mLow) * aContext.mSymbolFreq1 / (aContext.mSymbolFreq0 + aContext.mSymbolFreq1);

		int mid = mLow + (((mHigh - mLow) * aContext.mSymbolFreq1 * tbl[aContext.mSymbolFreq0 + aContext.mSymbolFreq1]) >> 14);

//		System.out.println(mid+" "+midX);

//		int mid = mLow + (((mHigh - mLow) * tbl[(aContext.mSymbolFreq1 & 0x3e0)+((aContext.mSymbolFreq0+aContext.mSymbolFreq1)>>5)]) >> 14);

//		int t = tbl[aContext.mSymbolFreq0 + aContext.mSymbolFreq1];
//		System.out.println(aBit+" ["+aContext.mSymbolFreq1+"]["+(aContext.mSymbolFreq0+aContext.mSymbolFreq1)+"]="+t/(double)(1<<13)+" "+t+" low="+mLow+" mid="+mid+" high="+mHigh+" Q="+Q1+"/"+Q2+"/"+Q3+"/"+Q4);

//		if (mid <= mLow || mid >= mHigh) System.exit(0);

		if (aBit == 0)
		{
			mLow = mid;
			aContext.mSymbolFreq0++;
		}
		else
		{
			mHigh = mid;
			aContext.mSymbolFreq1++;
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


	public void encode(int aBit, int aProb) throws IOException
	{
		assert aBit == 0 || aBit == 1;
		assert aProb >= 0 && aProb < 256;

		int mid = mHigh - (((mHigh - mLow) * aProb) >>> 8);

		if (aBit == 0)
		{
			mLow = mid;
		}
		else
		{
			mHigh = mid;
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


	public void encodeEqProb(int aBit) throws IOException
	{
		int mid = mLow + ((mHigh - mLow) >> 1);

		if (aBit == 0)
		{
			mLow = mid;
		}
		else
		{
			mHigh = mid;
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


	public void encodeExpGolomb(int aValue, ArithmeticContext[] aContext) throws IOException
	{
		if (aValue == 0)
		{
			encode(0, aContext[0]);
		}
		else
		{
			int i = 0;
			encode(1, aContext[1]);
			int L = aValue;
			int K = 1;
			while ((--L > 0) && (++K <= GOLOMB_EXP_START))
			{
				encode(1, aContext[1]);
			}
			if (aValue < GOLOMB_EXP_START)
			{
				encode(0, aContext[1]);
			}
			else
			{
				encodeExpGolombEqProb(aValue - GOLOMB_EXP_START, 0);
			}
		}
	}


	public void encodeExpGolombEqProb(int aValue, int K) throws IOException
	{
		for (;;)
		{
			if (aValue >= (1 << K))
			{
				encodeEqProb(1);
				aValue -= 1 << K;
				K++;
			}
			else
			{
				encodeEqProb(0);
				while (K-- > 0)
				{
					encodeEqProb((aValue >> K) & 1);
				}
				break;
			}
		}
	}


	public void stopEncoding() throws IOException
	{
		mBitsToFollow++;
		putOneBitPlusOutstanding(mLow < Q1 ? 0 : 1);

		while (mBitsToGo != 8)
		{
			putOneBit(0);
		}

		flushBuffer();
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
}