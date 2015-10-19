package org.terifan.compression.cabac;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class DelphiDecoder extends DelphiCabac
{
	private InputStream mInputStream;

	private byte [] mInBuffer = new byte[8192];
	private int mInBufferOffset;
	private int mBitsToGo;
	private int mBitBuffer;
	private int mInBufferLength;

	private int mValue;
	private int mRange;


	public DelphiDecoder(InputStream aInputStream) throws IOException
	{
		mInputStream = aInputStream;

		mValue = 0;
		mBitsToGo = 0;
		for (int i = 0; i < CABAC_BITS-1; i++)
		{
			mValue = (mValue << 1) | readBit();
		}
		mRange = CABAC_HALF - 2;
	}


	public int decode(DelphiContext aContext) throws IOException
	{
		int result = aContext.MPS;
		int RLPS = CabacRLPSTable[aContext.State][(mRange >> 6) & 3];
		mRange -= RLPS;

		if (mValue < mRange)
		{
			aContext.State = CabacACNextStateMPS[aContext.State];
		}
		else
		{
			mValue -= mRange;
			mRange = RLPS;
			result = 1 - result;
			if (aContext.State == 0)
			{
				aContext.MPS ^= 1;
			}
			aContext.State = CabacACNextStateLPS[aContext.State];
		}

		while (mRange < CABAC_QUARTER)
		{
			mRange <<= 1;
			mValue = (mValue << 1) | readBit();
		}
		return result;
	}


	public int decodeEqProb() throws IOException
	{
		int result = 0;
		mValue = (mValue << 1) | readBit();
		if (mValue >= mRange)
		{
			result = 1;
			mValue -= mRange;
		}
		return result;
	}


	public int decodeUnaryExpGolomb(DelphiContext aContext) throws IOException
	{
		if (decode(aContext) == 0)
		{
			return 0;
		}
		
		int K = 1;
		int L;
		int result = 0;

		do
		{
			L = decode(aContext);
			result++;
			K++;
		}
		while ((L != 0) && (K != GOLOMB_EXP_START));

		if (L != 0)
		{
			result += decodeExpGolombEqProb(0) + 1;
		}

		return result;
	}


	public int decodeExpGolombEqProb(int K) throws IOException
	{
		int L, binarySymbol;
		int result = 0;
		binarySymbol = 0;
		do
		{
			L = decodeEqProb();
			if (L == 1)
			{
				result += 1 << K;
				K++;
			}
		}
		while (L != 0);
		
		while (K != 0)
		{
			K--;
			if (decodeEqProb() == 1)
			{
				binarySymbol |= 1 << K;
			}
		}

		return result + binarySymbol;
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
}
