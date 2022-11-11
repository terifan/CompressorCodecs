package org.terifan.compression.cabac265;

import java.io.IOException;
import java.io.OutputStream;
import static org.terifan.compression.cabac265.CabacConstants.*;


// EG = elia-gamma
// TU = unary

public class CabacEncoder265 implements AutoCloseable
{
	private OutputStream mOutputStream;

	// VLC
	private int mVLCBuffer;
	private int mVLCBufferLen;

	// CABAC
	private int mRange;
	private int mLow;
	private int mBitsLeft;
	private int mBufferedByte;
	private int mNumBufferedBytes;


	public CabacEncoder265(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;

		mRange = 510;
		mLow = 0;

		mBitsLeft = 23;
		mBufferedByte = 0xFF;
		mNumBufferedBytes = 0;
	}


	public void encodeBit(int aBit) throws IOException
	{
		encodeBits(aBit, 1);
	}


	public void encodeCABAC_TU_bypass(int aValue, int aMaxLen) throws IOException
	{
		assert aValue <= aMaxLen;

		for (int i = 0; i < aValue; i++)
		{
			encodeCABAC_bypass(1);
		}

		if (aValue < aMaxLen)
		{
			encodeCABAC_bypass(0);
		}
	}


	public void encodeCABAC_TU(int aValue, int aMaxLen, CabacContext265[] aModels) throws IOException
	{
		assert aValue <= aMaxLen;

		for (int i = 0; i < aValue; i++)
		{
			encodeCABAC_bit(1, aModels[i]);
		}

		if (aValue < aMaxLen)
		{
			encodeCABAC_bit(0, aModels[aValue]);
		}
	}


	public void encodeCABAC_FL_bypass(int aValue, int aLength) throws IOException
	{
		while (aLength > 0)
		{
			aLength--;
			encodeCABAC_bypass(aValue & (1 << aLength));
		}
	}


	public float RDBits_for_CABAC_bin(int aBit, CabacContext265 aModel)
	{
		int idx = aModel.state << 1;

		if (aBit != aModel.MPSbit)
		{
			idx++;
		}

		return entropy_table[idx] / (float)(1 << 15);
	}


	public void encodeCABAC_EGk_bypass(int aValue, int aStep) throws IOException
	{
		assert aValue >= 0;

		while (aValue >= (1 << aStep))
		{
			encodeCABAC_bypass(1);
			aValue -= 1 << aStep;
			aStep++;
		}

		encodeCABAC_bypass(0);

		while (aStep > 0)
		{
			aStep--;
			encodeCABAC_bypass((aValue >>> aStep) & 1);
		}
	}


	public void encodeCABAC_EGk(int aValue, int aMinLen, int aMaxLen, CabacContext265[] aCtxMagnitude) throws IOException
	{
		assert aValue >= 0;
		assert aMinLen >= 0 && aMinLen < aMaxLen;
		assert aMaxLen <= aCtxMagnitude.length;

		int i = 0;

		while (aValue >= (1 << aMinLen))
		{
			encodeCABAC_bit(0, aCtxMagnitude[i++]);
			aValue -= 1 << aMinLen;
			aMinLen++;
		}

		if (i < aMaxLen)
		{
			encodeCABAC_bit(1, aCtxMagnitude[i]);
		}

		while (aMinLen > 0)
		{
			aMinLen--;
			encodeCABAC_bypass((aValue >>> aMinLen) & 1);
		}
	}


	public void encodeCABAC_EGk(int aValue, int aMinLen, int aMaxLen, CabacContext265[] aCtxMagnitude, CabacContext265[][] aCtxValues) throws IOException
	{
		assert aValue >= 0;
		assert aMinLen >= 0 && aMinLen < aMaxLen;
		assert aMaxLen <= aCtxMagnitude.length;

		int i = 0;

		while (aValue >= (1 << aMinLen))
		{
			encodeCABAC_bit(0, aCtxMagnitude[i++]);
			aValue -= 1 << aMinLen;
			aMinLen++;
		}

		if (i < aMaxLen)
		{
			encodeCABAC_bit(1, aCtxMagnitude[i]);
		}

		for (int j = 0; aMinLen > 0; j++)
		{
			aMinLen--;
			encodeCABAC_bit((aValue >>> aMinLen) & 1, aCtxValues[i][j]);
		}
	}


	public void encodeUVLC(int aValue) throws IOException
	{
		assert aValue >= 0;

		int nLeadingZeros = 0;
		int base = 0;
		int range = 1;

		while (aValue >= base + range)
		{
			base += range;
			range <<= 1;
			nLeadingZeros++;
		}

		encodeBits((1 << nLeadingZeros) | (aValue - base), 2 * nLeadingZeros + 1);
	}


	public void encodeSVLC(int aValue) throws IOException
	{
		if (aValue == 0)
		{
			encodeBits(1, 1);
		}
		else if (aValue > 0)
		{
			encodeUVLC(2 * aValue - 1);
		}
		else
		{
			encodeUVLC(-2 * aValue);
		}
	}


//	public void addTrailingBits() throws IOException
//	{
//		encodeBit(1);
//		int nZeros = freeBitsInByteCount();
//		encodeBits(0, nZeros);
//	}


	public void encodeFinal(int aBit) throws IOException
	{
		mRange -= 2;

		if (aBit != 0)
		{
			mLow += mRange;

			mLow <<= 7;
			mRange = 2 << 7;
			mBitsLeft -= 7;
		}
		else if (mRange >= 256)
		{
			return;
		}
		else
		{
			mLow <<= 1;
			mRange <<= 1;
			mBitsLeft--;
		}

		testAndWriteOut();
	}


	public void encodeCABAC_bypass(int aBit) throws IOException
	{
		mLow <<= 1;

		if (aBit != 0)
		{
			mLow += mRange;
		}
		mBitsLeft--;

		testAndWriteOut();
	}


	public void testAndWriteOut() throws IOException
	{
		if (mBitsLeft < 12)
		{
			flush();
		}
	}


	public void encodeCABAC_bit(int aBit, CabacContext265 aModel) throws IOException
	{
		int LPS = LPS_table[aModel.state][(mRange >> 6) - 4];
		mRange -= LPS;

		if (aBit != aModel.MPSbit)
		{
			int numBits = renorm_table[LPS >> 3];
			mLow = (mLow + mRange) << numBits;
			mRange = LPS << numBits;

			if (aModel.state == 0)
			{
				aModel.MPSbit = 1 - aModel.MPSbit;
			}

			aModel.state = next_state_LPS[aModel.state];

			mBitsLeft -= numBits;
		}
		else
		{
			aModel.state = next_state_MPS[aModel.state];

			// renorm
			if (mRange >= 256)
			{
				return;
			}

			mLow <<= 1;
			mRange <<= 1;
			mBitsLeft--;
		}

		testAndWriteOut();
	}


	private void flush() throws IOException
	{
		int leadByte = mLow >>> (24 - mBitsLeft);
		mBitsLeft += 8;
		mLow &= 0xffffffff >>> mBitsLeft;

		if (leadByte == 0xff)
		{
			mNumBufferedBytes++;
		}
		else
		{
			if (mNumBufferedBytes > 0)
			{
				int carry = leadByte >> 8;
				int symbol = mBufferedByte + carry;
				mBufferedByte = leadByte & 0xff;
				appendByte(symbol);

				symbol = (0xff + carry) & 0xff;
				while (mNumBufferedBytes > 1)
				{
					appendByte(symbol);
					mNumBufferedBytes--;
				}
			}
			else
			{
				mNumBufferedBytes = 1;
				mBufferedByte = leadByte;
			}
		}
	}


	// output all remaining bits and fill with zeros to next byte boundary
	public void flushVLC() throws IOException
	{
		while (mVLCBufferLen >= 8)
		{
			appendByte((mVLCBuffer >>> (mVLCBufferLen - 8)) & 0xFF);
			mVLCBufferLen -= 8;
		}

		if (mVLCBufferLen > 0)
		{
			appendByte(mVLCBuffer << (8 - mVLCBufferLen));
			mVLCBufferLen = 0;
		}

		mVLCBuffer = 0;
	}


	public void skipBits(int aLength) throws IOException
	{
		while (aLength >= 8)
		{
			encodeBits(0, 8);
			aLength -= 8;
		}

		if (aLength > 0)
		{
			encodeBits(0, aLength);
		}
	}


	public int freeBitsInByteCount()
	{
		if ((mVLCBufferLen & 7) == 0)
		{
			return 0;
		}
		return 8 - (mVLCBufferLen & 7);
	}


	public void appendByte(int aByte) throws IOException
	{
		mOutputStream.write(aByte);
	}


	@Override
	public void close() throws IOException
	{
		if (mOutputStream != null)
		{
			if ((mLow >>> (32 - mBitsLeft)) != 0)
			{
				appendByte(mBufferedByte + 1);
				while (mNumBufferedBytes > 1)
				{
					appendByte(0x00);
					mNumBufferedBytes--;
				}

				mLow -= 1 << (32 - mBitsLeft);
			}
			else
			{
				if (mNumBufferedBytes > 0)
				{
					appendByte(mBufferedByte);
				}

				while (mNumBufferedBytes > 1)
				{
					appendByte(0xff);
					mNumBufferedBytes--;
				}
			}

			encodeBits(mLow >>> 8, 24 - mBitsLeft);

			mOutputStream.close();
			mOutputStream = null;
		}
	}


	public void encodeBits(int aBits, int aLength) throws IOException
	{
		mVLCBuffer <<= aLength;
		mVLCBuffer |= aBits;
		mVLCBufferLen += aLength;

		while (mVLCBufferLen >= 8)
		{
			appendByte((mVLCBuffer >>> (mVLCBufferLen - 8)) & 0xFF);
			mVLCBufferLen -= 8;
		}
	}
}
