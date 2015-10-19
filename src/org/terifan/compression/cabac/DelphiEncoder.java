package org.terifan.compression.cabac;

import java.io.IOException;
import java.io.OutputStream;


public class DelphiEncoder extends DelphiCabac
{
	private OutputStream mOutputStream;

	private byte [] mOutBuffer = new byte[8192]; //  A buffer used for speeding up reading to and writing from files
	private int mOutBufferOffset;
	private int mBitsToGo;
	private int mBitBuffer;

	private int mBitsToFollow;
	private int mRange;
	private int mLow;
	

	public DelphiEncoder(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;

		mBitsToGo = 9;
		mRange = CABAC_HALF - 2;
	}


	public void encode(int aBit, DelphiContext aContext) throws IOException
	{
		int rLPS = CabacRLPSTable[aContext.State][(mRange >> 6) & 3];
		mRange -= rLPS;

		if (aBit != aContext.MPS)
		{
			mLow += mRange;
			mRange = rLPS;
			if (aContext.State == 0)
			{
				aContext.MPS ^= 1;
			}
			aContext.State = CabacACNextStateLPS[aContext.State];
		}
		else
		{
			aContext.State = CabacACNextStateMPS[aContext.State];
		}

		// Renormalisation
		while (mRange < CABAC_QUARTER)
		{
			if ((mLow >= CABAC_HALF))
			{
				putOneBitPlusOutstanding(1);
				mLow -= CABAC_HALF;
			}
			else if (mLow < CABAC_QUARTER)
			{
				putOneBitPlusOutstanding(0);
			}
			else
			{
				mBitsToFollow++;
				mLow -= CABAC_QUARTER;
			}
			mLow <<= 1;
			mRange <<= 1;
		}
	}


	public void encodeEqProb(int aBit) throws IOException
	{
		mLow <<= 1;

		if (aBit == 1)
		{
			mLow += mRange;
		}

		if (mLow >= CABAC_ONE)
		{
			putOneBitPlusOutstanding(1);
			mLow -= CABAC_ONE;
		}
		else if (mLow < CABAC_HALF)
		{
			putOneBitPlusOutstanding(0);
		}
		else
		{
			mBitsToFollow++;
			mLow -= CABAC_HALF;
		}
	}


	public void encodeUnaryExpGolomb(int aValue, DelphiContext aContext) throws IOException
	{
		if (aValue == 0)
		{
			encode(0, aContext);
		}
		else
		{
			encode(1, aContext);
			int L = aValue;
			int K = 1;
			while ((--L > 0) && (++K <= GOLOMB_EXP_START))
			{
				encode(1, aContext);
			}
			if (aValue < GOLOMB_EXP_START)
			{
				encode(0, aContext);
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
		putOneBitPlusOutstanding((mLow >> (CABAC_BITS - 1)) & 1);
		putOneBit((mLow >> (CABAC_BITS - 2)) & 1);
		putOneBit(1);
		
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
}